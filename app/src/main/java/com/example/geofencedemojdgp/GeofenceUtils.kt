package com.example.geofencedemojdgp

import com.huawei.hms.location.Geofence
import com.huawei.hms.location.GeofenceRequest


data class LandmarkDataObject(
    val identificator: String,
    val store: String,
    val promo: String,
    val latitude: Double,
    val longitude: Double
)

internal object GeofencingConstants {

    val expirationDuration = 1200000L
    var loiteringDelay = 5 * 1000
    var notificationResponsiveness = 10 * 1000

    const val GEOFENCE_RADIUS_IN_METERS = 50f
    const val EXTRA_GEOFENCE_INDEX = "GEOFENCE_INDEX"

    //HMS
    //LANDMARK_DATA is a list of object that contains dummy data for the geofence and the notification
    val geofences: List<Geofence>
        get() = this.LANDMARK_DATA.map {
            val builder = Geofence.Builder()
            builder.setUniqueId(it.identificator)
            builder.setRoundArea(it.latitude, it.longitude, GEOFENCE_RADIUS_IN_METERS)
            builder.setValidContinueTime(expirationDuration)
            builder.setDwellDelayTime(loiteringDelay)
            builder.setConversions(
                GeofenceRequest.ENTER_INIT_CONVERSION )
            builder.setNotificationInterval(notificationResponsiveness)
            builder.build()
        }

    /*GMS
    val geofences: List<Geofence>
        get() = this.LANDMARK_DATA.map {
            val builder = Geofence.Builder()
            builder..setRequestId(it.identificator)
            builder.setCircularRegion(it.latitude, it.longitude, GEOFENCE_RADIUS_IN_METERS)
            builder.setExpirationDuration(expirationDuration)
            builder.setLoiteringDelay(loiteringDelay)
            builder.setTransitionTypes(
               Geofence.GEOFENCE_TRANSITION_ENTER )
            builder.setNotificationResponsiveness(notificationResponsiveness)
            builder.build()
        }
     */

    val LANDMARK_DATA = arrayOf(
        LandmarkDataObject(
            identificator = "promo_fence_1",
            store =
            "Promo1",
            promo =
            "Hey! 20% Percent discount of Coffee",
            latitude = 4.710216, longitude = -74.062312
        ),

        LandmarkDataObject(
            identificator = "promo_fence_2",
            store = "Promo2",
            promo = "Hey! This is a temporal coupon for your next buy in our store that is near of you",
            latitude = 4.710361, longitude = -74.059439
        ),

        LandmarkDataObject(
            identificator = "promo_fence_3",
            store = "Promo3",
            promo = "Final Hours! Mystery flash sale on Store",
            latitude = 4.708163, longitude = -74.059336
        ),

        LandmarkDataObject(
            identificator = "promo_fence_4",
            store = "Promo4",
            promo = "Limited time only! Save up to 40%",
            latitude = 4.707489, longitude = -74.062161
        ),
        LandmarkDataObject(
            identificator = "promo_fence_5",
            store = "Promo5",
            promo = "Extra 30% off & free shipping Use code SETSALE",
            latitude = 4.711357, longitude = -74.064588
        )
    )

    val NUM_LANDMARKS = LANDMARK_DATA.size

}