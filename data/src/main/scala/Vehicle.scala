package carwings

import java.util.Date

case class Credentials(
  username: String,
  password: String,
  nickname: String,
  sessions: Seq[String])

trait Owner {
  def id: String
  def credentials: Credentials
  def vehicle: Vehicle
  def modified: Date
}

trait Vehicle {
  def vin: String
  def battery: Battery
}

trait Battery {
  def charging: String
  def pluginState: String
  def lastCheck: Date
  def capacity: Int
  def remaining: Int
  def range: Range
  def chargingTimes: Map[String, TimeToCharge]
}

trait TimeToCharge {
  def hours: Int
  def minutes: Int
}

trait Range {
  def acOn: Int
  def acOff: Int
}
