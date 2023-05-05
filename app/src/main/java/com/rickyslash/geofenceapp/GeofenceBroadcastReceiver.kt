package com.rickyslash.geofenceapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    // this called when getting PendingIntent to this receiver
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_GEOFENCE_EVENT) {
            // assign GeofencingEvent from intent
            val geofencingEvent = GeofencingEvent.fromIntent(intent)

            // checks if there is error when getting Geofence from intent
            if (geofencingEvent.hasError()) {
                val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
                Log.e(TAG, errorMessage)
                sendNotification(context, errorMessage)
                return
            }

            // variable that indicates which transition happened
            val geofenceTransition = geofencingEvent.geofenceTransition

            // sets geofenceTransitionString based on transition
            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
                val geofenceTransitionString = when (geofenceTransition) {
                    Geofence.GEOFENCE_TRANSITION_ENTER -> "You have entered the area"
                    Geofence.GEOFENCE_TRANSITION_DWELL -> "Hungry? Grab something on "
                    else -> "Invalid transition type"
                }

                // get list of Geofence objects that is triggered geofence transition
                val triggeringGeofences  = geofencingEvent.triggeringGeofences
                // get the requestId for the geofence
                val requestId = triggeringGeofences[0].requestId

                // make string to be displayed in notification
                val geofenceTransitionDetails = "$geofenceTransitionString $requestId"

                Log.i(TAG, geofenceTransitionDetails)
                // sending notification
                sendNotification(context, geofenceTransitionDetails)
            } else {
                val errorMessage = "Invalid transition type: $geofenceTransition"
                Log.e(TAG, errorMessage)
                sendNotification(context, errorMessage)
            }
        }
    }

    // function to build notification
    private fun sendNotification(context: Context, geofenceTransitionDetails: String) {
        val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val mBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(geofenceTransitionDetails)
            .setContentText("You are on the Gudeg Legendaris area!")
            .setSmallIcon(R.drawable.ic_baseline_restaurant_black_24)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            mBuilder.setChannelId(CHANNEL_ID)
            mNotificationManager.createNotificationChannel(channel)
        }

        val notification = mBuilder.build()
        mNotificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "GeofenceBroadcast"
        const val ACTION_GEOFENCE_EVENT = "GeofenceEvent"
        private const val CHANNEL_ID = "1"
        private const val CHANNEL_NAME = "Geofence Channel"
        private const val NOTIFICATION_ID = 1
    }
}