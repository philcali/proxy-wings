package carwings.bullet

import argonaut._, Argonaut._

package object json {
  implicit def PushBulletTokenJson: DecodeJson[PushBulletToken] =
    DecodeJson(p => for {
      accessToken <- (p --\ "access_token").as[String]
      tokenType <- (p --\ "token_type").as[String]
    } yield PushBulletToken(accessToken, tokenType))

  implicit def PushBulletConvertJson: EncodeJson[PushBulletConvert] =
    EncodeJson((request: PushBulletConvert) =>
      ("client_id" := request.clientId) ->:
      ("client_secret" := request.clientSecret) ->:
      ("code" := request.code) ->:
      ("grant_type" := "authorization_code") ->: jEmptyObject)

  implicit class ArgonautConvert(convert: PushBulletConvert) {
    def asJson = PushBulletConvertJson(convert)
  }
}
