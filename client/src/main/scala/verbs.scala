package carwings
package client
package verbs

import request._
import dispatch._, Defaults._
import language.postfixOps
import com.ning.http.client.Response
import xml.XML

case class Login(username: String, password: String) extends (String =>Future[Either[CarwingsError, Response]]) {
  def apply(baseUrl: String) = {
    Url andThen
    Service("userService") andThen
    LoginTemplate(username, password) andThen
    Post apply baseUrl
  }
}

case class VehicleStatus(credentials: Credentials, vin: String) extends (String => Future[Either[CarwingsError, Response]]) {
  def apply(baseUrl: String) = {
    Url andThen
    Service("userService") andThen
    VehicleStatusTemplate(vin) andThen
    Authed(credentials) andThen
    Post apply baseUrl
  }
}

case class RequestUpdate(credentials: Credentials, vin: String) extends (String => Future[Either[CarwingsError, Response]]) {
  def apply(baseUrl: String) = {
    Url andThen
    Service("vehicleService") andThen
    RequestUpdateTemplate(vin) andThen
    Authed(credentials) andThen
    Post apply baseUrl
  }
}

case object Convert extends (Response => xml.NodeSeq) {
  def apply(resp: Response) = resp. getResponseBody match {
    case str if str.isEmpty() => xml.NodeSeq.Empty
    case str => XML.loadString(str)
  }
}

case object Guard extends (xml.NodeSeq => Future[Either[CarwingsError, xml.NodeSeq]]) {
  import Carwings.Errors._

  def apply(node: xml.NodeSeq) = Future {
    val errorCode = (node \\ "ErrorCode")
    if (!errorCode.isEmpty) {
      Left((errorCode text) match {
      case "9001" => CarwingsError(InvalidCredentials.id, "Error authenticating")
      case "9003" => CarwingsError(InvalidSession.id, "Invalid session")
      case code => CarwingsError(code.toInt, "Unknown error")
      })
    } else Right(node)
  }
}

case class Retry(creds: Credentials, wings: Carwings) extends (CarwingsError => Future[Either[CarwingsError, response.VehicleResponse]]) {
  def apply(error: CarwingsError) = error match {
  case CarwingsError(9003, _) =>
  wings.login(creds.username, creds.password)
  case _ =>
  Future(Left(error))
  }
}
