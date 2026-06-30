package id.nexus.recorder.ui

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import id.nexus.recorder.databinding.ActivityMainBinding
import id.nexus.recorder.service.ScreenRecordService
import id.nexus.recorder.util.Prefs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var projectionManager: MediaProjectionManager
    private var isRecording = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.all { it }) {
            requestOverlayThenCapture()
        } else {
            Toast.makeText(this, "Izin diperlukan untuk merekam", Toast.LENGTH_SHORT).show()
        }
    }

    private val overlayLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            launchCaptureRequest()
        } else {
            Toast.makeText(this, "Izin overlay dibutuhkan untuk tombol mengambang", Toast.LENGTH_SHORT).show()
        }
    }

    private val captureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            startRecordingService(result.resultCode, result.data!!)
        } else {
            Toast.makeText(this, "Izin perekaman layar ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Prefs.load(this)
        projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        binding.btnStart.setOnClickListener {
            if (isRecording) {
                stopRecordingService()
            } else {
                checkPermissionsAndStart()
            }
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.btnRecordings.setOnClickListener {
            startActivity(Intent(this, RecordingsActivity::class.java))
        }
    }

    private fun checkPermissionsAndStart() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) needed.add(android.Manifest.permission.RECORD_AUDIO)

        if (Prefs.facecamEnabled && ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) needed.add(android.Manifest.permission.CAMERA)

        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) needed.add(android.Manifest.permission.POST_NOTIFICATIONS)

        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        } else {
            requestOverlayThenCapture()
        }
    }

    private fun requestOverlayThenCapture() {
        if (Prefs.floatingWidgetEnabled && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            overlayLauncher.launch(intent)
        } else {
            launchCaptureRequest()
        }
    }

    private fun launchCaptureRequest() {
        captureLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    private fun startRecordingService(resultCode: Int, data: Intent) {
        val intent = Intent(this, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_START
            putExtra(ScreenRecordService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenRecordService.EXTRA_RESULT_DATA, data)
        }
        ContextCompat.startForegroundService(this, intent)
        isRecording = true
        binding.btnStart.text = getString(id.nexus.recorder.R.string.stop_recording)
        binding.statusText.text = "Merekam..."
        moveTaskToBack(true)
    }

    private fun stopRecordingService() {
        val intent = Intent(this, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_STOP
        }
        startService(intent)
        isRecording = false
        binding.btnStart.text = getString(id.nexus.recorder.R.string.start_recording)
        binding.statusText.text = "Siap merekam"
    }
}
