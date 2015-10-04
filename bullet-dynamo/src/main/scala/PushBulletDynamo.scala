package carwings.bullet
package dynamodb

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.Failure
import util.Success
import util.Try

import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement
import com.amazonaws.services.dynamodbv2.model.KeyType
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput

case class PushBulletDynamo(db: DynamoDB) extends PushBulletStore {
  lazy val logger = LoggerFactory.getLogger(classOf[PushBulletDynamo])

  lazy val logins = Try(db.createTable(new CreateTableRequest()
    .withTableName("PushBulletLogins")
    .withProvisionedThroughput(new ProvisionedThroughput(1l, 1l))
    .withKeySchema(new KeySchemaElement("accountId", KeyType.HASH))
    .withAttributeDefinitions(new AttributeDefinition("accountId", "S"))))
    .recover({
      case riue: ResourceInUseException =>
      db.getTable("PushBulletLogins")
    })
    .get

  def save(accountId: String, login: PushBulletToken, user: PushBulletUser, devices: List[PushBulletDevice]) = {
    val item = new Item()
      .withPrimaryKey("accountId", accountId)
      .withLong("modifiedAt", System.currentTimeMillis)
      .withMap("login", new Item()
        .withString("accessToken", login.accessToken)
        .withString("tokenType", login.tokenType)
        .asMap())
      .withMap("user", new Item()
        .withString("iden", user.iden)
        .withString("name", user.name)
        .withString("email", user.email)
        .withString("email_normalized", user.emailNormalized)
        .withString("image_url", user.imageUrl)
        .asMap())
      .withList("devices", devices
        .map({
          case device =>
          val it = new Item()
            .withBoolean("active", device.active)
            .withString("iden", device.iden)
          device.manufacturer.foreach(it.withString("manufacturer", _))
          device.model.foreach(it.withString("model", _))
          device.nickname.foreach(it.withString("nickname", _))
          it.asMap()
        }):_*)
    Try(logins.putItem(item)) match {
      case Success(p) => Some(PushBulletModel(item))
      case Failure(e) => logger.error("DynamoDB save: ", e)
      None
    }
  }

  def read(accountId: String) = Try(logins.getItem("accountId", accountId))
    .filter(_ != null) match {
      case Success(p) => Some(PushBulletModel(p))
      case Failure(e) => logger.error("DynamoDB read:", e)
      None
    }

  def delete(accountId: String) {
    Try(logins.deleteItem("accountId", accountId)) match {
      case Success(_) => logger.info("DynamoDB deleted: {}", accountId)
      case Failure(e) => logger.error("DynamoDB delete:", e)
    }
  }
}
