package com.wahibhaq.locationservicelivedata

import android.content.Context
import com.nabinbhandari.android.permissions.PermissionHandler
import timber.log.Timber
import java.util.*

class PermissionHandler(
        private val actionsToGranted: List<() -> Unit>,
        private val actionToDenied: () -> Unit,
        private val actionToOnJustBlocked: () -> Unit
) : PermissionHandler() {


    override fun onGranted() {
        Timber.i("Activity: %s", R.string.permission_status_granted)
        actionsToGranted.forEach { it.invoke() }
    }

    override fun onDenied(context: Context?, deniedPermissions: ArrayList<String>?) {
        Timber.w("Activity: %s", R.string.permission_status_denied)
        actionToDenied.invoke()
    }

    override fun onJustBlocked(context: Context?, justBlockedList: ArrayList<String>?, deniedPermissions: ArrayList<String>?) {
        Timber.w("Activity: %s", R.string.permission_status_blocked)
        actionToOnJustBlocked.invoke()
    }
}