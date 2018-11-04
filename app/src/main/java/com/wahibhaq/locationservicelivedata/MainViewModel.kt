package com.wahibhaq.locationservicelivedata

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.content.Context
import android.content.Intent
import com.wahibhaq.locationservicelivedata.entity.GpsStatus
import com.wahibhaq.locationservicelivedata.entity.PermissionStatus


class MainViewModel(application: Application) : AndroidViewModel(application) {

    val gpsStatusLiveData: LiveData<GpsStatus> = GpsStatusListener(application)

    val locationPermissionStatusLiveData: LiveData<PermissionStatus> = PermissionStatusListener(
            application, Manifest.permission.ACCESS_FINE_LOCATION
    )

    private val notificationsUtil = NotificationsUtil(
            application,
            application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    )

    private val locationServiceListener = LocationServiceListener(
            application,
            Intent(application, LocationService::class.java)
    )

    fun startLocationTracking() = locationServiceListener.subscribeToLocationUpdates()

    fun stopLocationTracking() {
        locationServiceListener.unsubscribeFromLocationUpdates()
        notificationsUtil.cancelAlertNotification()
    }
}