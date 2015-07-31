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

case class VehicleStoreDynamo(db: DynamoDB) extends VehicleStore {
  lazy val logger = LoggerFactory.getLogger(classOf[VehicleStoreDynamo])

  def owners = db.getTable("Owners")

  def save(ownerId: String, credentials: Credentials, vehicle: Vehicle) = {
    Try(owners.putItem(new Item()
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
          .withMap("range", new Item()
            .withInt("acOn", vehicle.battery.range.acOn)
            .withInt("acOff", vehicle.battery.range.acOff)
            .asMap())
          .asMap())
        .asMap()))) match {
        case Success(p) => Some(OwnerModel(p.getItem()))
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
