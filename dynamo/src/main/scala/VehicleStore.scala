package carwings
package dynamodb

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import collection.JavaConversions._
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

case class VehicleStoreDynamo(db: DynamoDB) extends VehicleStore {
  lazy val logger = LoggerFactory.getLogger(classOf[VehicleStoreDynamo])

  def owners = Try(db.createTable(new CreateTableRequest()
    .withTableName("Owners")
    .withProvisionedThroughput(new ProvisionedThroughput(1l, 1l))
    .withKeySchema(new KeySchemaElement("id", KeyType.HASH))
    .withAttributeDefinitions(new AttributeDefinition("id", "S"))))
    .recover({
      // Only recover an existing table exception
      case riue: ResourceInUseException =>
      db.getTable("Owners")
    })
    .get

  def save(ownerId: String, credentials: Credentials, vehicle: Vehicle) = {
    val vehicleItem = new Item()
      .withPrimaryKey("id", ownerId)
      .withLong("modified", System.currentTimeMillis)
      .withMap("credentials", new Item()
        .withString("nickname", credentials.nickname)
        .withString("username", credentials.username)
        .withString("password", credentials.password)
        .withStringSet("sessions", credentials.sessions.toSet)
        .asMap())
      .withMap("vehicle", new Item()
        .withString("vin", vehicle.vin)
        .withMap("battery", new Item()
          .withString("charging", vehicle.battery.charging)
          .withString("pluginState", vehicle.battery.pluginState)
          .withLong("lastCheck", vehicle.battery.lastCheck.getTime())
          .withInt("capacity", vehicle.battery.capacity)
          .withInt("remaining", vehicle.battery.remaining)
          .withMap("chargingTimes", vehicle.battery.chargingTimes.foldLeft(new Item)({
            case (item, (key, timeToCharge)) => item.withMap(key, new Item()
              .withInt("hours", timeToCharge.hours)
              .withInt("minutes", timeToCharge.minutes)
              .asMap())
            })
            .asMap())
          .withMap("range", new Item()
            .withInt("acOn", vehicle.battery.range.acOn)
            .withInt("acOff", vehicle.battery.range.acOff)
            .asMap())
          .asMap())
        .asMap())
    Try(owners.putItem(vehicleItem)) match {
        case Success(p) => Some(OwnerModel(vehicleItem))
        case Failure(e) => logger.error("DyanmoDB save: ", e)
        None
      }
  }

  def read(ownerId: String) = Try(owners.getItem("id", ownerId))
    .filter(_ != null) match {
      case Success(p) => Some(OwnerModel(p))
      case Failure(e) => logger.error("DyanmoDB read:", e)
      None
    }

  def delete(ownerId: String) {
    Try(owners.deleteItem("id", ownerId)) match {
      case Success(_) => logger.debug("DyanmoDB delete success.")
      case Failure(e) => logger.error("DyanmoDB delete:", e)
    }
  }
}
