package carwings
package api
import json._

import java.util.UUID

import util.Try
import unfiltered.request._
import unfiltered.response._
import unfiltered.directives._, Directives._
import client._, Carwings._

object Authorized {
  def header(name: String, message: String) = headers(name)
    .map({
      case headers if headers.hasNext => headers.next
    })
    .orElse(
      failure(new ResponseJoiner(message)(s =>
        Unauthorized ~> ResponseString(CarwingsError(401, s.mkString(";")).asJson.toString())
      ))
    )

  def Identity = header("X-Authorized-Identity", "Invalid Identity")
  def Region = data.Requiring[Carwings]
    .fail(name => new ResponseJoiner("Missing Region")(s =>
      BadRequest ~> ResponseString(CarwingsError(400, s.mkString(";")).asJson.toString())
    ))
    .named("region")(data.Interpreter((regions: Seq[String]) => Try(Carwings(regions.mkString)).toOption))
}

trait Api {
  import dispatch._, Defaults._

  val vehicles: VehicleStore

  implicit def require[T] = data.Requiring[T].fail({
    case name =>
    BadRequest ~>
    ResponseString(CarwingsError(400, s"$name is missing").asJson.toString())
  })

  def notFound = Some(
    NotFound ~>
    JsonContent ~>
    ResponseString(CarwingsError(404, "Vehicle information does not exist").asJson.toString())
  )

  def store(ownerId: String, vresp: response.VehicleResponse) = vresp match {
    case response.VehicleResponse(credentials, Some(vehicle)) =>
    vehicles.save(ownerId, credentials, vehicle).map({
      case owner =>
      JsonContent ~>
      ResponseString(owner.asJson.toString())
    }).orElse(Some(
      InternalServerError ~>
      JsonContent ~>
      ResponseString(CarwingsError(500, "Unable to save vehicle information").asJson.toString())
    )).get
  }

  def error(error: CarwingsError) = {
    Unauthorized ~>
    JsonContent ~>
    ResponseString(error.asJson.toString)
  }

  def intent[A, B] = Directive.Intent.Path {
    case Seg(List("status")) =>
      for (_ <- GET) yield Ok
    case Seg(List("owners", action)) => action match {
      case "login" =>
      for {
        _ <- POST
        carwings <- Authorized.Region
        username <- data.as.Required[String] named "username"
        password <- data.as.Required[String] named "password"
      } yield {
        val ownerId = UUID.randomUUID().toString()
        carwings.login(username, password).apply().fold(error, store(ownerId, _))
      }
      case "vehicleStatus" =>
      for {
        _ <- POST
        ownerId <- Authorized.Identity
        carwings <- Authorized.Region
      } yield {
        vehicles.read(ownerId).map({
          case owner =>
          carwings.vehicleStatus(owner.credentials, owner.vehicle.vin).apply()
            .fold(error, store(ownerId, _))
        }).orElse(notFound).get
      }
      case "requestUpdate" =>
      for {
        _ <- POST
        ownerId <- Authorized.Identity
        carwings <- Authorized.Region
      } yield {
        vehicles.read(ownerId).map({
          case owner =>
          carwings.requestUpdate(owner.credentials, owner.vehicle.vin).apply().fold(error, {
            case response.VehicleResponse(credentials, None) =>
            JsonContent ~> ResponseString(owner.asJson.toString)
            case resp => store(ownerId, resp)
          })
        }).orElse(notFound).get
      }
    }
    case Seg(List("owners")) =>
    val read = for {
      _ <- GET
      ownerId <- Authorized.Identity
    } yield {
      vehicles.read(ownerId).map({
        case owner =>
        Ok ~>
        JsonContent ~>
        ResponseString(owner.asJson.toString())
      }).orElse(notFound).get
    }
    val delete = for {
      _ <- DELETE
      ownerId <- Authorized.Identity
    } yield {
      vehicles.delete(ownerId)
      NoContent
    }
    read | delete
  }
}
