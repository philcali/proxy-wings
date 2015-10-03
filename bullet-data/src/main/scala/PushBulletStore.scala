package carwings.bullet

trait PushBulletStore {
  def save(accountId: String, login: PushBulletToken, user: PushBulletUser, devices: List[PushBulletDevice]): Option[PushBulletLogin]
  def read(accountId: String): Option[PushBulletLogin]
  def delete(accountId: String)
}
