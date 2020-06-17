package com.example.geofencedemojdgp

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.geofencedemojdgp.MainActivity.Companion.ACTION_GEOFENCE_EVENT
import com.example.geofencedemojdgp.Utils.Companion.errorMessage
import com.huawei.hms.location.Geofence
import com.huawei.hms.location.GeofenceData



class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        if (intent?.action == ACTION_GEOFENCE_EVENT) {

            val geofencingEvent =GeofenceData.getDataFromIntent(intent)

            if (geofencingEvent.isFailure) {
                val errorMessage = errorMessage(context!!, geofencingEvent.errorCode)
                Log.e(Companion.TAG, errorMessage)
                return
            }

            if (geofencingEvent.conversion == Geofence.ENTER_GEOFENCE_CONVERSION) {
                Log.v(Companion.TAG, context?.getString(R.string.geofence_entered)!!)

                val fenceId = when {
                    geofencingEvent.convertingGeofenceList.isNotEmpty() ->
                        geofencingEvent.convertingGeofenceList[0].uniqueId
                    else -> {
                        Log.e(Companion.TAG, "No Geofence Trigger Found! Abort mission!")
                        return
                    }
                }
                // Check geofence against the constants listed in GeofenceUtil.kt to see if the
                // user has entered any of the locations we track for geofences.
                val foundIndex = GeofencingConstants.LANDMARK_DATA.random()

                val store = foundIndex.store
                val promo = foundIndex.promo

                val notificationManager = ContextCompat.getSystemService(
                    context,
                    NotificationManager::class.java
                ) as NotificationManager

                notificationManager.sendGeofenceEnteredNotification(
                    context, store, promo
                )
            }

        }


    }
    companion object {
        private const val TAG = "GeofenceReceiver"
    }
}