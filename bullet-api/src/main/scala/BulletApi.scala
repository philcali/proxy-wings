package carwings.bullet
package api

import java.net.URI
import java.net.URLEncoder
import util.Try

import client.BulletClient
import unfiltered.request._
import unfiltered.response._
import unfiltered.directives._, Directives._

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

  val logins: PushBulletStore
  val client: BulletClient

  def intent[A, B] = Directive.Intent.Path {
    case Seg(List("pushbullet", action)) => action match {
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
          logins.save(accountId.get, client.convert(code.get).apply()).map({
            case login =>
            Redirect(s"/?accountId=${accountId.get}&pushbullet=1#${options.get}")
          }).orElse({
            Some(Redirect(s"/?accountId=${accountId.get}&error=1#${options.get}"))
          }).get
        }
      }
      case "login" =>
      for {
        _ <- GET
        accountId <- Uri.Search("accountId")
      } yield {
        logins.read(accountId.get).map({
          case login =>
          NoContent
        }).orElse({
          Some(ResponseString(client.authorize))
        }).get
      }
    }
  }
}
