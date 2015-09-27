package carwings.bullet

import java.util.Date

import util.Properties

case class PushBulletToken(
  accessToken: String,
  tokenType: String
) extends PushBulletAccessToken

case class PushBulletConvert(
  clientId: String,
  clientSecret: String,
  code: String
) extends PushBulletCreds

trait PushBulletLogin {
  def accountId: String
  def createdAt: Date
  def login: PushBulletToken
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
