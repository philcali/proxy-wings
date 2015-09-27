package carwings.bullet
package api

import java.net.URI
import util.Try

import client.BulletClient
import unfiltered.request._
import unfiltered.response._
import unfiltered.directives._, Directives._

object Uri {
  def Search(name: String) = queryParams
    .map(_(name)(0))
    .orElse(
      failure(new ResponseJoiner(s"$name is missing")(s =>
        Unauthorized ~>
        ResponseString(s.mkString))))

  def Fragment = uri
    .map({
      case str => Try(new URI(str))
        .map(_.getFragment)
        .filter(frag => frag != null && !frag.isEmpty)
        .get
    })
    .orElse(
      failure(new ResponseJoiner("Missing options")(s =>
        Unauthorized ~>
        ResponseString(s.mkString))))
}

trait Api {
  import dispatch._, Defaults._

  val logins: PushBulletStore
  val client: BulletClient

  def intent[A, B] = Directive.Intent.Path {
    case Seg(List("bullet", action)) => action match {
      case "auth" =>
      for {
        _ <- GET
        code <- Uri.Search("code")
        accountId <- Uri.Search("accountId")
        options <- Uri.Fragment
      } yield {
        logins.save(accountId, client.convert(code).apply()).map({
          case login =>
          Redirect(s"/?accountId=$accountId#$options")
        }).orElse({
          Some(Redirect(s"/?accountId=$accountId&error=1#$options"))
        }).get
      }
      case "login" =>
      for {
        _ <- GET
        accountId <- Uri.Search("accountId")
      } yield {
        logins.read(accountId).map({
          case login =>
          NoContent
        }).orElse({
          Some(ResponseString(client.authorize))
        }).get
      }
    }
  }
}
