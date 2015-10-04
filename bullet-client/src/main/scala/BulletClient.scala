package carwings.bullet
package client

import json._

import dispatch._, Defaults._
import argonaut._, Argonaut._
import java.net.URLEncoder

object BulletClient {
  type BulletCreds = PushBulletCreds with PushBulletAccessToken
  lazy val baseUrl = "https://api.pushbullet.com"
  lazy val authUrl = "https://www.pushbullet.com/authorize"

  def apply(creds: BulletCreds) = new BulletClient(baseUrl, creds)
}

class BulletClient(baseUrl: String, creds: BulletClient.BulletCreds) {
  private def headers(req: Req)(token: PushBulletAccessToken) =
    req <:< Seq(
      "Content-Type" -> "application/json",
      "Access-Token" -> token.accessToken)

  def authorize() = {
    new StringBuilder(BulletClient.authUrl)
      .append(s"?client_id=${creds.clientId}")
      .append("&response_type=code")
      .append("&redirect_uri=")
      .toString()
  }

  def convert(code: String): Future[PushBulletToken] =
    Http((headers(url(baseUrl) / "oauth2" / "token")(creds) << PushBulletConvert(
        clientId = creds.clientId,
        clientSecret = creds.clientSecret,
        code = code).asJson.toString).POST >
      (as.String andThen (_.decodeOption[PushBulletToken].get)))

  def me(creds: PushBulletToken): Future[PushBulletUser] =
    Http((headers(url(baseUrl) / "v2" / "users" / "me")(creds)).GET >
      (as.String andThen (_.decodeOption[PushBulletUser].get)))

  def devices(creds: PushBulletToken): Future[ListEmphemerals] =
    Http((headers(url(baseUrl) / "v2" / "devices")(creds)).GET >
      (as.String andThen (_.decodeOption[ListEmphemerals].get)))

  def push(data: PushBulletCreatePush): Future[Boolean] =
    Http((headers(url(baseUrl) / "v2" / "pushes")(creds) << data.asJson.toString).POST >
    (as.String andThen (_.trim == "{}")))
}
