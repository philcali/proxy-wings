package carwings
package client

import dispatch._, Defaults._
import collection.JavaConversions._
import language.postfixOps

import java.util.Date

import com.ning.http.client.Response

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

  def vehicleStatus(credentials: Credentials, vin: String, retry: Boolean = true): Future[Either[CarwingsError, VehicleResponse]] = for {
    response <- VehicleStatus(credentials, vin)(baseUrl).right
    guard <- (Convert andThen Guard)(response)
  } yield {
    guard.fold(Retry(credentials, this, retry)(vehicleStatus(_, vin, _)).andThen(_.apply()), {
      case node =>
      Right(VehicleResponse(credentials, Some(VehicleNode(vin, node))))
    })
  }

  def startClimateControl(credentials: Credentials, vin: String, date: Option[Date] = None, retry: Boolean = true): Future[Either[CarwingsError, VehicleResponse]] = for {
    response <- StartClimate(credentials, vin, date)(baseUrl).right
    guard <- (Convert andThen Guard)(response)
  } yield {
    guard.fold(Retry(credentials, this, retry)(startClimateControl(_, vin, date, _)).andThen(_.apply()), {
      case node =>
      Right(VehicleResponse(credentials, None))
    })
  }

  def stopClimateControl(credentials: Credentials, vin: String, retry: Boolean = false): Future[Either[CarwingsError, VehicleResponse]] = for {
    response <- StopClimate(credentials, vin)(baseUrl).right
    guard <- (Convert andThen Guard)(response)
  } yield {
    guard.fold(Retry(credentials, this, retry)(stopClimateControl(_, vin, _)).andThen(_.apply()), {
      case node =>
      Right(VehicleResponse(credentials, None))
    })
  }

  def requestUpdate(credentials: Credentials, vin: String, retry: Boolean = true): Future[Either[CarwingsError, VehicleResponse]] = for {
    response <- RequestUpdate(credentials, vin)(baseUrl).right
    guard <- (Convert andThen Guard)(response)
  } yield {
    guard.fold(Retry(credentials, this, retry)(requestUpdate(_, vin, _)).andThen(_.apply()), {
      case node =>
      Right(VehicleResponse(credentials, None))
    })
  }
}
