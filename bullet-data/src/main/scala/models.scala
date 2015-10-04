package carwings.bullet

import java.util.Date

import util.Properties

case class PushBulletToken(
  accessToken: String,
  tokenType: String
) extends PushBulletAccessToken

case class PushBulletUser(
  iden: String,
  name: String,
  email: String,
  emailNormalized: String,
  imageUrl: String
)

case class PushBulletDevice(
  active: Boolean,
  iden: String,
  manufacturer: Option[String],
  model: Option[String],
  nickname: Option[String]
)

case class PushBulletCreatePush(
  title: String,
  body: String,
  receiver: PushBulletUser,
  receiverDevice: PushBulletDevice,
  sender: PushBulletUser,
  senderDevice: PushBulletDevice,
  url: Option[String] = None,
  active: Boolean = true,
  pushType: String = "link",
  direction: String = "self"
)

case class PushBulletApiPush(
  accountId: String,
  title: String,
  body: String,
  deviceIden: String,
  coords: Map[String, Double]
)

trait PushBulletLogin {
  def accountId: String
  def modifiedAt: Date
  def login: PushBulletToken
  def user: PushBulletUser
  def devices: List[PushBulletDevice]
}

trait PushBulletCreds {
  def clientId: String
  def clientSecret: String
}

trait PushBulletAccessToken {
  def accessToken: String
}

object SystemBullets extends PushBulletCreds with PushBulletAccessToken {
  private def required(prop: String) = Properties
    .propOrNone(prop)
    .getOrElse(throw new IllegalArgumentException(s"Expected '${prop}'"))

  lazy val clientId = required("pushbullet.clientId")
  lazy val clientSecret = required("pushbullet.clientSecret")
  lazy val accessToken = required("pushbullet.accessToken")
}

case class LoginResponse(authUrl: String, login: Option[PushBulletLogin])
case class ListEmphemerals(devices: List[PushBulletDevice])
case class PushBulletConvert(
  clientId: String,
  clientSecret: String,
  code: String
) extends PushBulletCreds
