package com.wahibhaq.locationservicelivedata

import android.Manifest
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.databinding.DataBindingUtil
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.nabinbhandari.android.permissions.Permissions
import com.wahibhaq.locationservicelivedata.LocationService.Companion.isServiceRunning
import com.wahibhaq.locationservicelivedata.LocationService.Companion.isTrackingRunning
import com.wahibhaq.locationservicelivedata.databinding.ActivityMainBinding
import com.wahibhaq.locationservicelivedata.entity.GpsStatus
import com.wahibhaq.locationservicelivedata.entity.PermissionStatus
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var binding: ActivityMainBinding

    private var alertDialog: AlertDialog? = null

    private val permissionHandler = PermissionHandler(
            listOf({ updatePermissionCheckUI(PermissionStatus.Granted()) }, { handleGpsAlertDialog() }),
            { updatePermissionCheckUI(PermissionStatus.Denied()) },
            { updatePermissionCheckUI(PermissionStatus.Blocked()) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        subscribesUi()
    }

    private fun subscribesUi() {
        viewModel.gpsStatusLiveData.observe(this, gpsObserver)
        viewModel.locationPermissionStatusLiveData.observe(this, permissionObserver)

    }

    private val gpsObserver = Observer<GpsStatus?> { status ->
        status?.let(::updateGpsCheckUI)
    }

    private val permissionObserver = Observer<PermissionStatus?> { status ->
        status?.let {
            updatePermissionCheckUI(status)
            when (status) {
                is PermissionStatus.Granted -> handleGpsAlertDialog()
                is PermissionStatus.Denied -> showLocationPermissionNeededDialog()
            }
        }
    }

    private fun updateGpsCheckUI(status: GpsStatus) {
        when (status) {
            is GpsStatus.Enabled -> {
                binding.textViewGpsStatusDisplay.apply {
                    text = getString(status.message)
                    isEnabled = false
                    setTextColor(Color.BLUE)
                }
                handleGpsAlertDialog(GpsStatus.Enabled())
            }

            is GpsStatus.Disabled -> {
                binding.textViewGpsStatusDisplay.apply {
                    isEnabled = true
                    text = getString(status.message).plus(getString(R.string.click_to_retry))
                    setTextColor(Color.RED)
                }
            }
        }

        toggleButtonClickableState()
    }

    private fun updatePermissionCheckUI(status: PermissionStatus) {
        when (status) {
            is PermissionStatus.Granted -> {
                binding.textViewPermissionStatusDisplay.apply {
                    isEnabled = false
                    text = getString(status.message)
                    setTextColor(Color.BLUE)
                }
            }

            is PermissionStatus.Denied -> {
                binding.textViewPermissionStatusDisplay.apply {
                    isEnabled = true
                    text = getString(status.message).plus(getString(R.string.click_to_retry))
                    setTextColor(Color.RED)
                }
            }

            is PermissionStatus.Blocked -> {
                binding.textViewPermissionStatusDisplay.apply {
                    text = getString(status.message).plus(getString(R.string.click_to_retry))
                    setTextColor(Color.RED)
                    isEnabled = true
                }
            }
        }

        toggleButtonClickableState()
    }

    private fun isTrackingRunningAlready() = isTrackingRunning && isServiceRunning

    private fun setupUI() {

        //Start Tracking Only if there is a need and it's valid to start tracking
        if (isTrackingRunningAlready()) binding.buttonControlTracking.text = getString(R.string.button_text_end)
        binding.buttonControlTracking.setOnClickListener {
            if (isTrackingRunningAlready().not()) startTracking() else stopTracking()
        }

        /**
         * This is to simulate how user is alerted via notifications when activity start is detected
         * in background by [LocationService]
         */
        binding.buttonSimulateNotification.setOnClickListener {
            Handler().apply {
                postDelayed({ viewModel.startLocationTracking() }, 3000)
            }
        }

        binding.textViewGpsStatusDisplay.setOnClickListener { handleGpsAlertDialog() }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.textViewPermissionStatusDisplay.visibility = View.VISIBLE
            binding.textViewPermissionStatusDisplay.setOnClickListener { showLocationPermissionNeededDialog() }
        }
    }

    private fun toggleButtonClickableState() {
        binding.apply {
            buttonControlTracking.isEnabled = textViewGpsStatusDisplay.isEnabled.not() && textViewPermissionStatusDisplay.isEnabled.not()
            buttonSimulateNotification.isEnabled = buttonControlTracking.isEnabled.not()
        }
    }

    private fun startTracking() {
        Timber.i("Tracking start triggered from Button on Activity")
        viewModel.startLocationTracking()
        binding.buttonControlTracking.text = getString(R.string.button_text_end)
    }

    private fun stopTracking() {
        Timber.i("Tracking stop triggered from Activity")
        viewModel.stopLocationTracking()
        binding.buttonControlTracking.text = getString(R.string.button_text_start)
    }

    override fun onResume() {
        super.onResume()
        setupUI()
    }

    /**
     *  Using current value of [GpsStatusListener] livedata as default
     */
    private fun handleGpsAlertDialog(status: GpsStatus? = viewModel.gpsStatusLiveData.value) {
        when (status) {
            is GpsStatus.Enabled -> hideGpsNotEnabledDialog()
            is GpsStatus.Disabled -> showGpsNotEnabledDialog()
        }
    }

    private fun showGpsNotEnabledDialog() {
        if (alertDialog?.isShowing == true) {
            return // null or already being shown
        }

        alertDialog = AlertDialog.Builder(this)
                .setTitle(R.string.gps_required_title)
                .setMessage(R.string.gps_required_body)
                .setPositiveButton(R.string.action_settings) { _, _ ->
                    // Open app's settings.
                    val intent = Intent().apply {
                        action = Settings.ACTION_LOCATION_SOURCE_SETTINGS
                    }
                    startActivity(intent)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
    }

    private fun hideGpsNotEnabledDialog() {
        if (alertDialog?.isShowing == true) alertDialog?.dismiss()
    }

    private fun showLocationPermissionNeededDialog() {
        if (alertDialog?.isShowing == true) {
            return // null or dialog already being shown
        }

        alertDialog = AlertDialog.Builder(this)
                .setTitle(R.string.permission_required_title)
                .setMessage(R.string.permission_required_body)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    //Using 3rd party lib *Permissions* for showing dialogs and handling response
                    Permissions.check(this,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            null,
                            permissionHandler)
                }
                .setCancelable(false) //to disable outside click for cancel
                .create()

        alertDialog?.apply {
            show()
        }
    }
}
