package carwings
package client

import com.ning.http.client.Response

import dispatch._, Defaults._
import collection.JavaConversions._
import language.postfixOps
import xml.XML

object Carwings {
  object Errors extends Enumeration {
    type Code = Value
    val General = Value(1000)
    val InvalidCredentials = Value(9001)
    val InvalidSession = Value(9003)
  }

  private val baseUrls = Map(
    "us" -> "https://nissan-na-smartphone-biz.viaaq.com/aqPortal/smartphoneProxy",
    "eu" -> "https://nissan-eu-smartphone-biz.viaaq.eu/aqPortal/smartphoneProxy"
  )

  def apply(region: String) = baseUrls
    .get(region)
    .map(new Carwings(_))
    .getOrElse(throw CarwingsError(Errors.General.id, s"Region ${region} is not supported"))
}

class Carwings(baseUrl: String) {
  import Carwings._
  import request._
  import response._

  def exception(t: Throwable) = CarwingsError(Errors.General.id, t.getMessage)

  def service(name: String, soap: SoapRequest) = (req: Req) => (req / name
    <:< (Seq("Content-Type" -> "text/xml"))
    << (soap.toString()))

  def authed(credentials: Credentials) = (req: Req) =>
    req <:< (Seq("Cookie" -> credentials.sessions.mkString(";")))

  def post(req: Req) = Http(req.POST).fold(t => Left(exception(t)), Right(_))

  def convert(response: Response) = XML.loadString(response.getResponseBody())

  def guard(node: xml.NodeSeq) = if (node.head.label == "ns7:SmartphoneErrorType") {
    Left((node \ "ErrorCode" text) match {
    case "9001" => CarwingsError(Errors.InvalidCredentials.id, "Error authenticating")
    case "9003" => CarwingsError(Errors.InvalidSession.id, "Invalid session")
    case code => CarwingsError(code.toInt, "Unknown error")
    })
  } else Right(node)

  def login(username: String, password: String) = for {
    response <- post(service("userService", Login(username, password))(url(baseUrl))).right
    node <- Future(guard(convert(response))).right
  } yield {
    val info = UserInfo(node)
    val credentials = Credentials(username, password, info.nickname,
      sessions = response.getCookies().map({
        case cookie => s"${cookie.getName}=${cookie.getValue}"
      }).toList)
    VehicleResponse(credentials, VehicleNode(info.vin, node))
  }

  def vehicleStatus(credentials: Credentials, vin: String) = for {
    response <- post(authed(credentials).andThen(service("userService", VehicleStatus(vin)))(url(baseUrl))).right
  } yield {
    guard(convert(response)).fold({
      case CarwingsError(9003, _) =>
      login(credentials.username, credentials.password).apply()
      case error =>
      Future(Left(error))
    }, {
      case node =>
      Future(Right(VehicleResponse(credentials, VehicleNode(vin, node))))
    })
  }
}
