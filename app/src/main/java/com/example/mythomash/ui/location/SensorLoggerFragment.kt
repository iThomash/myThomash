// SensorLoggerFragment.kt
package com.example.mythomash.ui.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.mythomash.CronetClient
import com.example.mythomash.R
import com.example.mythomash.ui.location.LocationLog
import com.google.android.gms.location.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class SensorLoggerFragment : Fragment(), SensorEventListener {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sensorManager: SensorManager
    private var barometer: Sensor? = null
    private var gravityX: Float? = null
    private var gravityY: Float? = null
    private var gravityZ: Float? = null
    private var gravitySensor: Sensor? = null

    private lateinit var viewModel: SensorLoggerViewModel
    private lateinit var cronetClient: CronetClient

    private lateinit var statusText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var uploadButton: Button

    private var currentPressure: Float? = null
    private var isLogging = false

    private val handler = android.os.Handler()
    private val logRunnable = object : Runnable {
        override fun run() {
            logCurrentData()
            handler.postDelayed(this, 3000) // log every 3 seconds
        }
    }

    private var cachedLogs: List<LocationLog> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_data_collection, container, false)
        statusText = view.findViewById(R.id.statusText)
        startButton = view.findViewById(R.id.button_start)
        stopButton = view.findViewById(R.id.button_stop)
        uploadButton = view.findViewById(R.id.button_upload)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[SensorLoggerViewModel::class.java]
        cronetClient = CronetClient(requireContext())

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        barometer = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

        viewModel.allLogs.observe(viewLifecycleOwner) { logs ->
            cachedLogs = logs
            Log.d("SensorLoggerFragment", "Cached logs updated, count: ${logs.size}")
            updateUiButtons()
        }

        startButton.setOnClickListener {
            if (isLogging) {
                // Shouldn't happen because startButton disabled when logging,
                // but just in case ignore clicks while logging
                return@setOnClickListener
            }
            if (cachedLogs.isNotEmpty()) {
                // Reset data
                resetData()
            } else {
                // Start logging
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
                    return@setOnClickListener
                }
                startLogging()
            }
        }

        stopButton.setOnClickListener {
            stopLogging()
        }

        uploadButton.setOnClickListener {
            uploadDataToServer()
        }

        // Initial UI setup
        updateUiButtons()
        statusText.text = "Ready"
    }

    private fun updateUiButtons() {
        if (isLogging) {
            startButton.isEnabled = false
            startButton.text = "Start"
            stopButton.isEnabled = true
            uploadButton.isEnabled = false
        } else {
            stopButton.isEnabled = false
            uploadButton.isEnabled = cachedLogs.isNotEmpty()
            if (cachedLogs.isNotEmpty()) {
                startButton.text = "Reset"
                startButton.isEnabled = true
            } else {
                startButton.text = "Start"
                startButton.isEnabled = true
            }
        }
    }

    private fun startLogging() {
        isLogging = true
        statusText.text = "Logging..."
        startButton.isEnabled = false
        stopButton.isEnabled = true
        uploadButton.isEnabled = false
        startButton.text = "Start"

        barometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        gravitySensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }

        handler.post(logRunnable)
    }

    private fun stopLogging() {
        isLogging = false
        handler.removeCallbacks(logRunnable)
        sensorManager.unregisterListener(this)
        statusText.text = "Logging stopped."
        updateUiButtons()
    }

    private fun resetData() {
        viewModel.clearLogs()
        cachedLogs = emptyList()
//        statusText.text = "Data cleared."
        isLogging = false  // ensure logging flag reset
        updateUiButtons()
    }

    private fun logCurrentData() {
        if (!isLogging) return

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w("SensorLogger", "Location permission not granted. Skipping log.")
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val pressure = currentPressure ?: -1f

                val log = LocationLog(
                    timestamp = System.currentTimeMillis(),
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = location.altitude,
                    pressure = currentPressure?.toDouble() ?: -1.0,
                    gravityX = gravityX?.toDouble() ?: -1.0,
                    gravityY = gravityY?.toDouble() ?: -1.0,
                    gravityZ = gravityZ?.toDouble() ?: -1.0
                )
                viewModel.insertLog(log)
                Log.d("SensorLogger", "Logged: $log")
            }
        }
    }

    private fun uploadDataToServer() {
        val logs = cachedLogs
        if (logs.isEmpty()) {
            statusText.text = "No data to upload"
            updateUiButtons()
            return
        }

        startButton.isEnabled = true
        stopButton.isEnabled = false
        uploadButton.isEnabled = false
        statusText.text = "Uploading..."

        val jsonArray = JSONArray()
        for (log in logs) {
            val obj = JSONObject().apply {
                put("timestamp", log.timestamp)
                put("latitude", log.latitude)
                put("longitude", log.longitude)
                put("altitude", log.altitude)
                put("pressure", log.pressure)
                put("gravityX", log.gravityX)
                put("gravityY", log.gravityY)
                put("gravityZ", log.gravityZ)
            }
            jsonArray.put(obj)
        }

        val payload = JSONObject().apply {
            put("records", jsonArray)
        }

        val serverUrl = "http://192.168.1.66:3000/uploadLogs"
        cronetClient.postJsonData(serverUrl, payload.toString(), object : CronetClient.ResponseCallback {
            override fun onResponse(response: String) {
                Log.d("Upload", "Upload success: $response")
                statusText.text = "Upload successful"
                // Clear logs AND update UI buttons properly
                resetData()
            }

            override fun onError(error: String) {
                Log.e("Upload", "Upload failed: $error")
                statusText.text = "Upload failed"
                updateUiButtons()
            }
        })
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_PRESSURE -> {
                currentPressure = event.values[0]
            }
            Sensor.TYPE_GRAVITY -> {
                gravityX = event.values[0]
                gravityY = event.values[1]
                gravityZ = event.values[2]
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroyView() {
        super.onDestroyView()
        sensorManager.unregisterListener(this)
        handler.removeCallbacks(logRunnable)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLogging()
            } else {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}


