package carwings

trait VehicleStore {
  def save(ownerId: String, credentials: Credentials, vehicle: Vehicle): Option[Owner]
  def read(ownerId: String): Option[Owner]
  def delete(ownerId: String)
}
