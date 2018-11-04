package com.wahibhaq.locationservicelivedata.entity

import com.wahibhaq.locationservicelivedata.R

sealed class GpsStatus {
    data class Enabled(val message: Int = R.string.gps_status_enabled) : GpsStatus()
    data class Disabled(val message: Int = R.string.gps_status_disabled) : GpsStatus()
}