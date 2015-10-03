package carwings.bullet

import argonaut._, Argonaut._

package object json {
  implicit def PushBulletUserEncodeJson: EncodeJson[PushBulletUser] =
    EncodeJson((user: PushBulletUser) =>
      ("name" := user.name) ->:
      ("email" := user.email) ->:
      ("imageUrl" := user.imageUrl) ->: jEmptyObject)

  implicit def PushBulletDeviceEncodeJson: EncodeJson[PushBulletDevice] =
    EncodeJson((device: PushBulletDevice) =>
      ("iden" := device.iden) ->:
      ("manufacturer" := device.manufacturer.getOrElse("NA")) ->:
      ("nickname" := device.nickname.getOrElse("NA")) ->:
      ("model" := device.model.getOrElse("NA")) ->: jEmptyObject)

  implicit def PushBulletLoginEncodeJson: EncodeJson[PushBulletLogin] =
    EncodeJson((token: PushBulletLogin) =>
      ("user" := token.user) ->:
      ("devices" := token.devices) ->: jEmptyObject)

  implicit def PushBulletConvertJson: EncodeJson[PushBulletConvert] =
    EncodeJson((request: PushBulletConvert) =>
      ("client_id" := request.clientId) ->:
      ("client_secret" := request.clientSecret) ->:
      ("code" := request.code) ->:
      ("grant_type" := "authorization_code") ->: jEmptyObject)

  implicit def LoginResponseJson: EncodeJson[LoginResponse] =
    EncodeJson((response: LoginResponse) =>
      ("authorizeUrl" := response.authUrl) ->:
      ("isAuthed" := response.login.isDefined) ->:
      ("auth" := response.login) ->: jEmptyObject)

  implicit def PushBulletCreatePushJson: EncodeJson[PushBulletCreatePush] =
    EncodeJson((push: PushBulletCreatePush) =>
      ("title" := push.title) ->:
      ("body" := push.body) ->:
      ("iden" := push.receiver.iden) ->:
      ("source_device_iden" := push.senderDevice.iden) ->:
      ("device_iden" := push.receiverDevice.iden) ->:
      ("direction" := push.direction) ->:
      ("type" := push.pushType) ->:
      ("active" := push.active) ->:
      ("source_email" := push.sender.email) ->:
      ("source_email_normalized" := push.sender.emailNormalized) ->:
      ("receiver_email" := push.receiver.email) ->:
      ("receiver_email_normalized" := push.receiver.emailNormalized) ->:
      ("receiver_iden" := push.receiver.iden) ->:
      ("sender_name" := push.sender.name) ->:
      ("sender_iden" := push.sender.iden) ->:
      ("url" := push.url.getOrElse("")) ->: jEmptyObject)

  implicit def PushBulletApiPushDecode: DecodeJson[PushBulletApiPush] =
    DecodeJson(p => for {
      accountId <- (p --\ "accountId").as[String]
      title <- (p --\ "title").as[String]
      body <- (p --\ "body").as[String]
      deviceIden <- (p --\ "deviceIden").as[String]
      coords <- (p --\ "coords").as[Map[String, Double]]
    } yield PushBulletApiPush(accountId, title, body, deviceIden, coords))

  implicit def PushBulletTokenJson: DecodeJson[PushBulletToken] =
    DecodeJson(p => for {
      accessToken <- (p --\ "access_token").as[String]
      tokenType <- (p --\ "token_type").as[String]
    } yield PushBulletToken(accessToken, tokenType))

  implicit def PushBulletUserDecode: DecodeJson[PushBulletUser] =
    DecodeJson(p => for {
      iden <- (p --\ "iden").as[String]
      name <- (p --\ "name").as[String]
      email <- (p --\ "email").as[String]
      normalized <- (p --\ "email_normalized").as[String]
      image <- (p --\ "image_url").as[String]
    } yield PushBulletUser(iden, name, email, normalized, image))

  implicit def PushBulletDeviceDecodeJson: DecodeJson[PushBulletDevice] =
    DecodeJson(p => for {
      active <- (p --\ "active").as[Boolean]
      iden <- (p --\ "iden").as[String]
      manufacturer <- (p --\ "manufacturer").as[Option[String]]
      model <- (p --\ "model").as[Option[String]]
      nickname <- (p --\ "nickname").as[Option[String]]
    } yield PushBulletDevice(active, iden, manufacturer, model, nickname))

  implicit def ListEmphemeralsDecodeJson: DecodeJson[ListEmphemerals] =
    DecodeJson(p => for {
      devices <- (p --\ "devices").as[List[PushBulletDevice]]
    } yield ListEmphemerals(devices))

  implicit class ArgonautConvert(convert: PushBulletConvert) {
    def asJson = PushBulletConvertJson(convert)
  }

  implicit class ArgonautLogin(response: LoginResponse) {
    def asJson = LoginResponseJson(response)
  }

  implicit class ArgonautCreatPush(push: PushBulletCreatePush) {
    def asJson = PushBulletCreatePushJson(push)
  }
}
