package carwings.bullet
package dynamodb

import collection.JavaConversions._

import java.util.Date
import java.util.Map

import com.amazonaws.services.dynamodbv2.document.Item

case class PushBulletModel(item: Item) extends PushBulletLogin {
  lazy val accountId = item.getString("accountId")
  lazy val modifiedAt = new Date(item.getLong("modifiedAt"))
  lazy val login = {
    val login = Item.fromMap(item.getRawMap("login"))
    PushBulletToken(
      login.getString("accessToken"),
      login.getString("tokenType"))
  }
  lazy val user = {
    val user = Item.fromMap(item.getRawMap("user"))
    PushBulletUser(
      user.getString("iden"),
      user.getString("name"),
      user.getString("email"),
      user.getString("email_normalized"),
      user.getString("image_url"))
  }
  lazy val devices = item.getList("devices")
    .map((any: Any) => {
      val item = Item.fromMap(any.asInstanceOf[Map[String, Object]])
      PushBulletDevice(
        item.getBoolean("active"),
        item.getString("iden"),
        if (item.isPresent("manufacturer"))
          Some(item.getString("manufacturer"))
        else None,
        if (item.isPresent("model"))
          Some(item.getString("model"))
        else None,
        if (item.isPresent("nickname"))
          Some(item.getString("nickname"))
        else None)
    })
    .toList
}
