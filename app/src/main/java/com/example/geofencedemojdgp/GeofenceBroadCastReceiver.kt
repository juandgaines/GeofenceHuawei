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
            //val geofencingEvent = GeofencingEvent.fromIntent(intent) GMS
            val geofencingEvent =GeofenceData.getDataFromIntent(intent) //HMS

            //if (geofencingEvent.hasError()) { GMS
            if (geofencingEvent.isFailure) { //HMS
                val errorMessage = errorMessage(context!!, geofencingEvent.errorCode) //ERROR codes constansta re different in HMS and GMS
                Log.e(Companion.TAG, errorMessage)
                return
            }

            //if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) { GMS
            if (geofencingEvent.conversion == Geofence.ENTER_GEOFENCE_CONVERSION) { //HMS
                Log.v(TAG, context?.getString(R.string.geofence_entered)!!)

                val fenceId = when {
                    //geofencingEvent.triggeringGeofences.isNotEmpty() -> GMS
                    geofencingEvent.convertingGeofenceList.isNotEmpty() ->//HMS
                        //geofencingEvent.triggeringGeofences[0].requestId  GMS
                        geofencingEvent.convertingGeofenceList[0].uniqueId  //HMS
                    else -> {
                        Log.e(TAG, "No Geofence Trigger Found! Abort mission!")
                        return
                    }
                }
                // Check geofence against the constants listed in GeofenceUtil.kt to see if the
                // user has entered any of the locations we track for geofences.
                val foundIndex = GeofencingConstants.LANDMARK_DATA.find { it.identificator==fenceId}

                val store = foundIndex?.store
                val promo = foundIndex?.promo

                val notificationManager = ContextCompat.getSystemService(
                    context,
                    NotificationManager::class.java
                ) as NotificationManager

                notificationManager.sendGeofenceEnteredNotification(
                    context, store?:"No store", promo?:"No promo"
                )
            }

        }


    }
    companion object {
        private const val TAG = "GeofenceReceiver"
    }
}