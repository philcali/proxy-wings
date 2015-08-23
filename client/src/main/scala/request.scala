package carwings
package client
package request

import java.io.StringWriter
import java.lang.System
import com.ning.http.client.Response

import dispatch._, Defaults._
import xml.XML

case object Url extends (String => Req) {
  def apply(baseUrl: String) = url(baseUrl)
}
case class Service(name: String) extends (Req => Req) {
  def apply(req: Req) =
  (req / name <:< Seq("Content-Type" -> "text/xml"))
}
case class Authed(credentials: Credentials) extends (Req => Req) {
  def apply(req: Req) = {
    req <:< Seq("Cookie" -> credentials.sessions.mkString(";"))
  }
}
case object Post extends (Req => Future[Either[CarwingsError, Response]]) {
  def exception(t: Throwable) = CarwingsError(Carwings.Errors.General.id, t.getMessage)
  def apply(req: Req) = Http(req.POST).fold(t => Left(exception(t)), Right(_))
}

sealed trait SoapRequest extends (Req => Req) {
  def template: xml.Elem
  def apply(req: Req) = req << toString()
  override def toString() = {
    val writer = new StringWriter();
    XML.write(writer, template, "UTF-8", true, null);
    writer.toString()
  }
}

case class LoginTemplate(username: String, password: String) extends SoapRequest {
  def template = {
    val now = System.currentTimeMillis
    <ns2:SmartphoneLoginWithAdditionalOperationRequest
      xmlns:ns4="urn:com:airbiquity:smartphone.reportservice:v1"
      xmlns:ns7="urn:com:airbiquity:smartphone.common:v1"
      xmlns:ns3="urn:com:hitachi:gdc:type:report:v1"
      xmlns:ns5="urn:com:hitachi:gdc:type:vehicle:v1"
      xmlns:ns2="urn:com:airbiquity:smartphone.userservices:v1"
      xmlns:ns6="urn:com:airbiquity:smartphone.vehicleservice:v1">
      <SmartphoneLoginInfo>
        <UserLoginInfo>
          <userId>{ username }</userId>
          <userPassword>{ password }</userPassword>
        </UserLoginInfo>
        <DeviceToken>{ "PEBBLE" + username + ":" + now }</DeviceToken>
        <UUID>{ "carwingspebble:" + username + ":" + now }</UUID>
        <Locale>US</Locale>
        <AppVersion>1.7</AppVersion>
        <SmartphoneType>IPHONE</SmartphoneType>
      </SmartphoneLoginInfo>
      <SmartphoneOperationType>SmartphoneLatestBatteryStatusRequest</SmartphoneOperationType>
    </ns2:SmartphoneLoginWithAdditionalOperationRequest>
  }
}

case class VehicleStatusTemplate(vin: String) extends SoapRequest {
  def template = {
    <ns2:SmartphoneGetVehicleInfoRequest
      xmlns:ns2="urn:com:airbiquity:smartphone.userservices:v1">
      <VehicleInfo>
        <Vin>{ vin }</Vin>
      </VehicleInfo>
      <SmartphoneOperationType>SmartphoneLatestBatteryStatusRequest</SmartphoneOperationType>
      <checkVehicle>false</checkVehicle>
    </ns2:SmartphoneGetVehicleInfoRequest>
  }
}

case class RequestUpdateTemplate(vin: String) extends SoapRequest {
  def template = {
    <ns4:SmartphoneRemoteBatteryStatusCheckRequest
      xmlns:ns2="urn:com:hitachi:gdc:type:portalcommon:v1"
      xmlns:ns4="urn:com:airbiquity:smartphone.vehicleservice:v1"
      xmlns:ns3="urn:com:hitachi:gdc:type:vehicle:v1">
      <ns3:BatteryStatusCheckRequest>
        <ns3:VehicleServiceRequestHeader>
          <ns2:VIN>{ vin }</ns2:VIN>
        </ns3:VehicleServiceRequestHeader>
      </ns3:BatteryStatusCheckRequest>
    </ns4:SmartphoneRemoteBatteryStatusCheckRequest>
  }
}
