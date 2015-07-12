package carwings
package client
package response

import language.postfixOps

import java.text.SimpleDateFormat

case class LoginResponse(credentials: Credentials, vehicle: Vehicle)

case class VehicleNode(node: xml.NodeSeq) extends Vehicle {
  private val userInfo = node \\ "SmartphoneUserInfoType"
  val battery = BatteryNode(node \\ "ns4:SmartphoneLatestBatteryStatusResponse")
  def nickname = userInfo \ "Nickname" text
  def vin = userInfo \ "VehicleInfo" \ "Vin" text
}

case class BatteryNode(node: xml.NodeSeq) extends Battery {
  private val batteryRecords = node \\ "ns3:BatteryStatusRecords"
  private val batteryStatus = batteryRecords \ "ns3:BatteryStatus"
  private def date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  val range = RangeNode(batteryRecords)
  def charging = batteryStatus \ "ns3:BatteryChargingStatus" text
  def capacity = (batteryStatus \ "ns3:BatteryCapacity" text).toInt
  def remaining = (batteryStatus \ "ns3:BatteryRemainingAmount" text).toInt
  def pluginState = batteryRecords \ "ns3:PluginState" text
  def lastCheck = date.parse(node \\ "lastBatteryStatusCheckExecutionTime" text)
}

case class RangeNode(node: xml.NodeSeq) extends Range {
  val acOn = (node \ "ns3:CruisingRangeAcOn" text).toInt
  val acOff = (node \ "ns3:CruisingRangeAcOff" text).toInt
}
