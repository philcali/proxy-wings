package carwings
package dynamodb

import collection.JavaConversions._
import util.Try

import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.Item

case class VehicleStoreDynamo(db: DynamoDB) extends VehicleStore {
  def owners = db.getTable("Owners")

  def save(ownerId: String, credentials: Credentials, vehicle: Vehicle) = {
    Try (owners.putItem(new Item()
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
        .asMap())))
      .map(p => OwnerModel(p.getItem()))
      .toOption
  }

  def read(ownerId: String) = Try(owners.getItem("id", ownerId))
    .filter(_ != null)
    .map(OwnerModel(_))
    .toOption

  def delete(ownerId: String) {
    Try(owners.deleteItem("id", ownerId))
  }
}
