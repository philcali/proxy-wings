package carwings
package api
import json._

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

  def intent[A, B] = Directive.Intent.Path {
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
      }).orElse(Some(
        NotFound ~>
        JsonContent ~>
        ResponseString(CarwingsError(404, "Vehicle information does not exist").asJson.toString())
      )).get
    }
    val delete = for {
      _ <- DELETE
      ownerId <- Authorized.Identity
    } yield {
      vehicles.delete(ownerId)
      NoContent
    }
    val save = for {
      _ <- POST
      ownerId <- Authorized.Identity
      carwings <- Authorized.Region
      username <- data.as.Required[String] named "username"
      password <- data.as.Required[String] named "password"
    } yield {
      // TODO: kind of hate this, fixme
      carwings.login(username, password).apply().fold({
        case error =>
        Unauthorized ~>
        JsonContent ~>
        ResponseString(error.asJson.toString())
      }, {
        case response.VehicleResponse(credentials, vehicle) =>
        vehicles.save(ownerId, credentials, vehicle).map({
          case owner =>
          JsonContent ~>
          ResponseString(owner.asJson.toString())
        }).orElse(Some(
          InternalServerError ~>
          JsonContent ~>
          ResponseString(CarwingsError(500, "Unable to save vehicle information").asJson.toString())
        )).get
      })
    }
    read | save | delete
  }
}
