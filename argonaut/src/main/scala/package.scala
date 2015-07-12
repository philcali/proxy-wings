package carwings

import java.text.SimpleDateFormat
import java.util.Date

import argonaut._, Argonaut._

package object json {
  private def jsonDate = new SimpleDateFormat("yyy-MM-dd'T'HH:mm:ss'Z'")

  implicit def DateEncodeJson: EncodeJson[Date] =
    EncodeJson((date: Date) =>
      jString(jsonDate.format(date)))

  implicit def RangeEncodeJson: EncodeJson[Range] =
    EncodeJson((range: Range) =>
      ("acOn" := range.acOn) ->:
      ("acOff" := range.acOff) ->: jEmptyObject)

  implicit def BatterEncodeJson: EncodeJson[Battery] =
    EncodeJson((battery: Battery) =>
      ("charging" := battery.charging) ->:
      ("pluginState" := battery.pluginState) ->:
      ("lastCheck" := battery.lastCheck) ->:
      ("capacity" := battery.capacity) ->:
      ("remaining" := battery.remaining) ->:
      ("range" := battery.range) ->: jEmptyObject)

  implicit def VehicleEncodeJson: EncodeJson[Vehicle] =
    EncodeJson((vehicle: Vehicle) =>
      ("nickname" := vehicle.nickname) ->:
      ("vin" := vehicle.vin) ->:
      ("battery" := vehicle.battery) ->: jEmptyObject)

  implicit def OwnerEncodeJson: EncodeJson[Owner] =
    // Note: we explicitly do not expose the credentials
    // as they add no value to our app
    EncodeJson((owner: Owner) =>
      ("id" := owner.id) ->:
      ("vehicle" := owner.vehicle) ->:
      ("modified" := owner.modified) ->: jEmptyObject)
}
