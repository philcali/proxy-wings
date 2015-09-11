package carwings
package dynamodb

import collection.JavaConversions._

import java.util.Date
import com.amazonaws.services.dynamodbv2.document.Item

case class OwnerModel(item: Item) extends Owner {
  lazy val id = item.getString("id")
  lazy val modified = new Date(item.getLong("modified"))
  lazy val vehicle = VehicleModel(Item.fromMap(item.getRawMap("vehicle")))
  lazy val credentials = {
    val credentials = Item.fromMap(item.getRawMap("credentials"));
    Credentials(
      credentials.getString("username"),
      credentials.getString("password"),
      credentials.getString("nickname"),
      credentials.getStringSet("sessions").toSeq)
  }
}

case class VehicleModel(item: Item) extends Vehicle {
  lazy val vin = item.getString("vin")
  lazy val battery = BatteryModel(Item.fromMap(item.getRawMap("battery")))
}

case class BatteryModel(item: Item) extends Battery {
  lazy val charging = item.getString("charging")
  lazy val pluginState = item.getString("pluginState")
  lazy val lastCheck = new Date(item.getLong("lastCheck"))
  lazy val capacity = item.getInt("capacity")
  lazy val remaining = item.getInt("remaining")
  lazy val range = RangeModel(Item.fromMap(item.getRawMap("range")))
  def chargingTimes = if (item.isPresent("chargingTimes")) {
    val times = Item.fromMap(item.getRawMap("chargingTimes"))
    Map(List("120V", "240V").filter(times.isPresent).map(s => {
      s -> TimeToChargeModel(Item.fromMap(times.getRawMap(s)))
    }):_*)
  } else {
    Map.empty
  }
}

case class TimeToChargeModel(item: Item) extends TimeToCharge {
  lazy val hours = item.getInt("hours")
  lazy val minutes = item.getInt("minutes")
}

case class RangeModel(item: Item) extends Range {
  lazy val acOn = item.getInt("acOn")
  lazy val acOff = item.getInt("acOff")
}
