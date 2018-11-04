package com.wahibhaq.locationservicelivedata

import android.arch.lifecycle.LiveData
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import com.wahibhaq.locationservicelivedata.entity.PermissionStatus

/**
 * Listens to Runtime Permission Status of provided [permissionToListen] which comes under the
 * category of "Dangerous" and then responds with appropriate state specified in {@link PermissionStatus}
 */
class PermissionStatusListener(private val context: Context,
                               private val permissionToListen: String) : LiveData<PermissionStatus>() {

    override fun onActive() = handlePermissionCheck()

    private fun handlePermissionCheck() {
        val isPermissionGranted = ActivityCompat.checkSelfPermission(context,
                permissionToListen) == PackageManager.PERMISSION_GRANTED

        postValue(if (isPermissionGranted) PermissionStatus.Granted() else PermissionStatus.Denied())
    }
}