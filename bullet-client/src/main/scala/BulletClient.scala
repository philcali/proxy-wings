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
  def authorize() = {
    new StringBuilder(BulletClient.authUrl)
      .append(s"?client_id=${creds.clientId}")
      .append("&response_type=code")
      .append("&redirect_uri=")
      .toString()
  }

  def convert(code: String): Future[PushBulletToken] =
    Http(((url(baseUrl) / "oauth2" / "token") <:< Seq(
      "Content-Type" -> "application/json",
      "Access-Token" -> creds.accessToken) << PushBulletConvert(
        clientId = creds.clientId,
        clientSecret = creds.clientSecret,
        code = code).asJson.toString).POST >
      (as.String andThen (_.decodeOption[PushBulletToken].get)))
}
