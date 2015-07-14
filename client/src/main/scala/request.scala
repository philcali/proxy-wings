package carwings
package client
package request

import xml.XML

import java.io.StringWriter
import java.lang.System

sealed trait SoapRequest {
  def template: xml.Elem
  override def toString() = {
    val writer = new StringWriter();
    XML.write(writer, template, "UTF-8", true, null);
    writer.toString()
  }
}

case class Login(username: String, password: String) extends SoapRequest {
  def template = {
    <ns:SmartphoneLoginWithAdditionalOperationRequest
      xmlns:ns4="urn:com:hitachi:gdc:type:report:v1"
      xmlns:ns7="urn:com:airbiquity:smartphone.vehicleservice:v1"
      xmlns:ns3="http://www.nissanusa.com/owners/schemas/api/0"
      xmlns:ns5="urn:com:airbiquity:smartphone.reportservice:v1"
      xmlns:ns2="urn:com:airbiquity:smartphone.userservices:v1"
      xmlns:ns6="urn:com:hitachi:gdc:type:vehicle:v1">
      <SmartphoneLoginInfo>
        <UserLoginInfo>
          <userId>{ username }</userId>
          <userPassword>{ password }</userPassword>
        </UserLoginInfo>
        <DeviceToken>{ "PEBBLE" + System.currentTimeMillis }</DeviceToken>
        <UUID>{ "carwings:" + username }</UUID>
        <Locale>US</Locale>
        <AppVersion>1.70</AppVersion>
        <SmartphoneType>IPHONE</SmartphoneType>
      </SmartphoneLoginInfo>
      <SmartphoneOperationType>SmartphoneLatestBatteryStatusRequest</SmartphoneOperationType>
    </ns:SmartphoneLoginWithAdditionalOperationRequest>
  }
}

case class VehicleStatus(vin: String) extends SoapRequest {
  def template = {
    <ns2:SmartphoneGetVehicleInfoRequest
      xmlns:ns2="urn:com:airbiquity:smartphone.userservices:v1">
      <VehicleInfo>
        <VIN>{ vin }</VIN>
      </VehicleInfo>
      <SmartphoneOperationType>SmartphoneLatestBatteryStatusRequest</SmartphoneOperationType>
      <changeVehicle>false</changeVehicle>
    </ns2:SmartphoneGetVehicleInfoRequest>
  }
}

case class RequestUpdate(vin: String) extends SoapRequest {
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
