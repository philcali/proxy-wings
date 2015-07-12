package carwings
package api

import unfiltered.request._
import unfiltered.response._
import unfiltered.directives._, Directives._
import client._, Carwings._

object Authorized {
  def Identity = headers("X-Authorized-Identity")
    .map({
      case headers if headers.hasNext => headers.next
    })
    .orElse(
      failure(new ResponseJoiner("Invalid identity")(message =>
        Unauthorized ~> ResponseString(message.mkString("\n"))
      ))
    )
}

trait Api {
  import argonaut._, Argonaut._
  import json._

  val vehicles: VehicleStore

  // TODO: is this the best place?
  implicit def CarwingsErrorEncodeJson: EncodeJson[CarwingsError] =
    EncodeJson((error: CarwingsError) ->:
      ("message" := error.message) ->:
      ("code" := error.code) ->: jEmptyObject)

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
        ResponseString(CarwingsError(404, "Vehicle information does not exist"))
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
      username <- data.as.Option[String] named "username"
      password <- data.as.Option[String] named "password"
      region <- data.as.Option[String] named "region"
      response <- Carwings(region).login(username, password).apply()
    } yield {
      response.fold({
        case error =>
        Unauthorized ~>
        JsonContent ~>
        ResponseString(error.asJson.toString())
      }, {
        case response.LoginResponse(credentials, vehicle) =>
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
