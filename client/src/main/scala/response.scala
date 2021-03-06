package carwings
package client
package response

import language.postfixOps

import java.text.SimpleDateFormat

case class UserInfo(node: xml.NodeSeq) {
  private val info = node \\ "SmartphoneUserInfoType"
  lazy val nickname = info \ "Nickname" text
  lazy val vin = info \\ "Vin" text
}

case class VehicleResponse(credentials: Credentials, vehicle: Option[Vehicle])

case class VehicleNode(vin: String, node: xml.NodeSeq) extends Vehicle {
  lazy val battery = BatteryNode(node \\ "SmartphoneLatestBatteryStatusResponse")
}

case class StaticVehicleNode(vin: String, nickname: String, node: xml.NodeSeq) extends Vehicle {
  lazy val battery = BatteryNode(node \\ "SmartphoneLatestBatteryStatusResponse")
}

case class BatteryNode(node: xml.NodeSeq) extends Battery {
  private val batteryRecords = node \\ "BatteryStatusRecords"
  private val batteryStatus = batteryRecords \ "BatteryStatus"
  private def date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  lazy val range = RangeNode(batteryRecords)
  def charging = batteryStatus \ "BatteryChargingStatus" text
  def capacity = (batteryStatus \ "BatteryCapacity" text).toInt
  def remaining = (batteryStatus \ "BatteryRemainingAmount" text).toInt
  def pluginState = batteryRecords \ "PluginState" text
  def lastCheck = date.parse(node \\ "lastBatteryStatusCheckExecutionTime" text)
  def chargingTimes = Map(List(
      "120V" -> "TimeRequiredToFull",
      "240V" -> "TimeRequiredToFull200"
    ).map(kv => kv._1 -> batteryRecords \\ kv._2)
     .filter(!_._2.isEmpty)
     .map(kv => kv._1 -> TimeToChargeNode(kv._2)):_*)
}

case class TimeToChargeNode(node: xml.NodeSeq) extends TimeToCharge {
  lazy val hours = (node \ "HourRequiredToFull" text).toInt
  lazy val minutes = (node \ "MinutesRequiredToFull" text).toInt
}

case class RangeNode(node: xml.NodeSeq) extends Range {
  lazy val acOn = (node \ "CruisingRangeAcOn" text).toDouble
  lazy val acOff = (node \ "CruisingRangeAcOff" text).toDouble
}
