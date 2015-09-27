package carwings.bullet
package dynamodb

import java.util.Date

import com.amazonaws.services.dynamodbv2.document.Item

case class PushBulletModel(item: Item) extends PushBulletLogin {
  lazy val accountId = item.getString("accountId")
  lazy val createdAt = new Date(item.getLong("createdAt"))
  lazy val login = {
    val login = Item.fromMap(item.getRawMap("login"))
    PushBulletToken(
      login.getString("accessToken"),
      login.getString("tokenType"))
  }
}
