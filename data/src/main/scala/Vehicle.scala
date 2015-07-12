package carwings

import java.util.Date

case class Credentials(
  username: String,
  password: String,
  sessions: List[String])

trait Owner {
  def id: String
  def credentials: Credentials
  def vehicle: Vehicle
  def modified: Date
}

trait Vehicle {
  def nickname: String
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
}

trait Range {
  def acOn: Int
  def acOff: Int
}
