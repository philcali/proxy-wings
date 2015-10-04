package carwings.bullet
package api

import java.net.URI
import java.net.URLEncoder
import util.Try

import client.BulletClient
import unfiltered.request._
import unfiltered.response._
import unfiltered.directives._, Directives._
import argonaut._, Argonaut._

object Uri {
  def Search(name: String) = queryParams
    .map(_.get(name)
      .filter(!_.isEmpty)
      .map(_(0))
      .map(URLEncoder.encode(_, "UTF8")))
    .orElse(
      failure(new ResponseJoiner(s"$name is missing")(s =>
        Unauthorized ~>
        ResponseString(s.mkString))))
}

trait Api {
  import dispatch._, Defaults._
  import json._

  val logins: PushBulletStore
  val client: BulletClient
  val mapsPrefix = "http://maps.google.com/maps?q="

  def intent[A, B] = Directive.Intent.Path {
    case Seg(List("pushbullet", action)) => action match {
      case "push" =>
      for {
        _ <- POST
        req <- request[Any]
      } yield {
        Body.string(req).decodeOption[PushBulletApiPush].map({
          case push =>
          logins.read(push.accountId).map({
            case token =>
            token.devices.find(_.iden == push.deviceIden).map({
              case device =>
              val latitude = push.coords("latitude")
              val longitude = push.coords("longitude")
              PushBulletCreatePush(
                title = push.title,
                body = push.body,
                url = Some(mapsPrefix + latitude + "," + longitude),
                receiver = token.user,
                sender = token.user,
                receiverDevice = device,
                senderDevice = device
              )
            }).map({
              case push =>
              client.push(push).apply()
              NoContent
            }).orElse({
              Some(BadRequest ~> ResponseString("Invalid device"))
            }).get
          }).orElse({
            Some(NotFound)
          }).get
        }).orElse({
          Some(BadRequest ~> ResponseString("Bad JSON"))
        }).get
      }
      case "auth" =>
      for {
        _ <- GET
        code <- Uri.Search("code")
        error <- Uri.Search("error")
        accountId <- Uri.Search("accountId")
        options <- Uri.Search("options")
      } yield {
        if (error.isDefined) {
          Redirect(s"/?accountId=${accountId.get}&error=${error.get}#${options.get}")
        } else {
          (for {
            token <- client.convert(code.get)
            user <- client.me(token)
            emphemerals <- client.devices(token)
          } yield {
            logins.save(accountId.get, token, user, emphemerals.devices).map( {
              case login =>
              Redirect(s"/?accountId=${accountId.get}&pushbullet=1#${options.get}")
            }).orElse({
              Some(Redirect(s"/?accountId=${accountId.get}&error=1#${options.get}"))
            }).get
          }).apply()
        }
      }
      case "login" =>
      for {
        _ <- GET
        accountId <- Uri.Search("accountId")
      } yield {
        logins.read(accountId.get).map({
          case token =>
          (for {
            emphemerals <- client.devices(token.login)
          } yield {
            logins.save(accountId.get, token.login, token.user, emphemerals.devices).map({
              case login =>
              JsonContent ~>
              ResponseString(LoginResponse(client.authorize, Some(login)).asJson.toString)
            }).orElse({
              Some(JsonContent ~>
              ResponseString(LoginResponse(client.authorize, Some(token)).asJson.toString))
            }).get
          }).apply()
        }).orElse({
          Some(JsonContent ~>
          ResponseString(LoginResponse(client.authorize, None).asJson.toString))
        }).get
      }
    }
  }
}
