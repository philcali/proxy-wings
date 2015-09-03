package carwings
package client

import com.ning.http.client.Response

import dispatch._, Defaults._
import collection.JavaConversions._
import language.postfixOps

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
  import verbs._
  import response._

  def login(username: String, password: String) = for {
    response <- Login(username, password)(baseUrl).right
    node <- (Convert andThen Guard)(response).right
  } yield {
    val info = UserInfo(node)
    val credentials = Credentials(username, password, info.nickname,
      sessions = response.getCookies().map({
        case cookie => s"${cookie.getName}=${cookie.getValue}"
      }).toList)
    VehicleResponse(credentials, Some(VehicleNode(info.vin, node)))
  }

  def vehicleStatus(credentials: Credentials, vin: String) = for {
    response <- VehicleStatus(credentials, vin)(baseUrl).right
    guard <- (Convert andThen Guard)(response)
  } yield {
    guard.fold(Retry(credentials, this).andThen(_.apply()), {
      case node =>
      Right(VehicleResponse(credentials, Some(VehicleNode(vin, node))))
    })
  }

  def requestUpdate(credentials: Credentials, vin: String, retry: Boolean = true): Future[Either[CarwingsError, VehicleResponse]] = for {
    response <- RequestUpdate(credentials, vin)(baseUrl).right
    guard <- (Convert andThen Guard)(response)
  } yield {
    guard.fold(Retry(credentials, this).andThen(_.apply() match {
      case Right(VehicleResponse(newCreds, _)) if retry =>
      requestUpdate(newCreds, vin, false).apply()
      case other => other
    }), {
      case node =>
      Right(VehicleResponse(credentials, None))
    })
  }
}
