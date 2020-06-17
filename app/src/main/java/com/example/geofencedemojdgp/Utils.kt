package com.example.geofencedemojdgp

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.huawei.hms.common.ResolvableApiException
import com.huawei.hms.location.GeofenceErrorCodes
import com.huawei.hms.location.LocationRequest
import com.huawei.hms.location.LocationServices
import com.huawei.hms.location.LocationSettingsRequest
import java.math.RoundingMode

class Utils {

    companion object {
        const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
        const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
        const val REQUEST_TURN_DEVICE_LOCATION_ON = 29

        const val LOCATION_PERMISSION_INDEX = 0
        const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1

        private val TAG = Utils::class.java.simpleName

        private val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval=10000
        }

        fun isPermissionGranted(context: Context) = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) === PackageManager.PERMISSION_GRANTED

        @TargetApi(29)
        fun foregroundAndBackgroundLocationPermissionApproved(
            context: Context,
            runningQOrLater: Boolean
        ): Boolean {
            val foregroundLocationApproved = (
                    PackageManager.PERMISSION_GRANTED ==
                            ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ))
            val backgroundPermissionApproved =
                if (runningQOrLater) {
                    PackageManager.PERMISSION_GRANTED ==
                            ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION
                            )
                } else {
                    true
                }
            return foregroundLocationApproved && backgroundPermissionApproved
        }

        /*
*  Requests ACCESS_FINE_LOCATION and (on Android 10+ (Q) ACCESS_BACKGROUND_LOCATION.
*/
        @TargetApi(29)
        fun requestForegroundAndBackgroundLocationPermissions(
            context: Context,
            runningQOrLater: Boolean,
            activity: Activity
        ) {
            if (foregroundAndBackgroundLocationPermissionApproved(context, runningQOrLater))
                return

            // Else request the permission
            // this provides the result[LOCATION_PERMISSION_INDEX]
            var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

            val resultCode = when {
                runningQOrLater -> {
                    // this provides the result[BACKGROUND_LOCATION_PERMISSION_INDEX]
                    permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
                }
                else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
            }
            ActivityCompat.requestPermissions(
                activity,
                permissionsArray,
                resultCode
            )
        }


        fun checkDeviceLocationSettingsAndStartGeofence(
            resolve: Boolean = true,
            activity: Activity,
            onSuccess: (LocationRequest) -> Unit,
            onError: (exception: ResolvableApiException) -> Unit
        ) {


            val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
            val settingsClient = LocationServices.getSettingsClient(activity)
            val locationSettingsResponseTask =
                settingsClient.checkLocationSettings(builder.build())

            locationSettingsResponseTask.addOnFailureListener { exception ->
                if (exception is ResolvableApiException && resolve) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        onError(exception)
                    } catch (sendEx: IntentSender.SendIntentException) {
                        Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                    }
                } else {
                    Log.d(TAG, "Fallamos a lo mal papu")
                }
            }.addOnCompleteListener {
                if (it.isSuccessful) {
                    onSuccess(locationRequest)
                }
            }
        }

       fun errorMessage(context: Context, errorCode: Int): String {
            val resources = context.resources
            return when (errorCode) {
                GeofenceErrorCodes.GEOFENCE_UNAVAILABLE -> resources.getString(
                    R.string.geofence_not_available
                )
                GeofenceErrorCodes.GEOFENCE_NUMBER_OVER_LIMIT -> resources.getString(
                    R.string.geofence_too_many_geofences
                )
                GeofenceErrorCodes.GEOFENCE_PENDINGINTENT_OVER_LIMIT -> resources.getString(
                    R.string.geofence_too_many_pending_intents
                )
                else -> resources.getString(R.string.unknown_geofence_error)
            }
        }

    }
}

fun Double.setDecimals(decimals:Int):Double{

    return this.toBigDecimal().setScale(decimals, RoundingMode.HALF_EVEN).toDouble()

}