package carwings

case class CarwingsError(code: Int, message: String) extends RuntimeException(message)
