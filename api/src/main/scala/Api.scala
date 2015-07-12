package carwings
package api

import unfiltered.request._
import unfiltered.response._
import unfiltered.directives._, Directives._

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
      }).orElse({
        Some(NotFound)
      }).get
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
    } yield {
      // TODO: perform the save
      NoContent
    }
    read | save | delete
  }
}
