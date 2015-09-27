package carwings.bullet

trait PushBulletStore {
  def save(accountId: String, login: PushBulletToken): Option[PushBulletLogin]
  def read(accountId: String): Option[PushBulletLogin]
  def delete(accountId: String)
}
