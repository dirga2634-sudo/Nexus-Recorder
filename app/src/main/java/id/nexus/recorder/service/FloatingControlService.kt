package id.nexus.recorder.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import id.nexus.recorder.R
import id.nexus.recorder.util.Prefs

class FloatingControlService : Service(), LifecycleOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private lateinit var windowManager: WindowManager
    private var controlView: View? = null
    private var facecamView: View? = null
    private val handler = Handler(Looper.getMainLooper())

    private var cameraProvider: ProcessCameraProvider? = null
    private var facecamShown = false

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (ScreenRecordService.isRunning) {
                val elapsed = (System.currentTimeMillis() - ScreenRecordService.startTimeMs) / 1000
                val mm = elapsed / 60
                val ss = elapsed % 60
                controlView?.findViewById<TextView>(R.id.floatTimer)?.text =
                    String.format("%02d:%02d", mm, ss)
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showControlBubble()
        if (Prefs.facecamEnabled) showFacecam()
        handler.post(timerRunnable)
        startMinimalForeground()
    }

    private fun startMinimalForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "nexus_floating_channel", "Tombol Mengambang",
                NotificationManager.IMPORTANCE_MIN
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notif: Notification = NotificationCompat.Builder(this, "nexus_floating_channel")
            .setContentTitle("Kontrol perekaman aktif")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(2002, notif, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)
        } else {
            startForeground(2002, notif)
        }
    }

    private fun showControlBubble() {
        controlView = LayoutInflater.from(this).inflate(R.layout.floating_widget, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 200

        makeDraggable(controlView!!, params)

        controlView!!.findViewById<ImageButton>(R.id.btnFloatStop).setOnClickListener {
            startService(Intent(this, ScreenRecordService::class.java).setAction(ScreenRecordService.ACTION_STOP))
        }

        val pauseBtn = controlView!!.findViewById<ImageButton>(R.id.btnFloatPauseResume)
        pauseBtn.setOnClickListener {
            if (ScreenRecordService.isPaused) {
                startService(Intent(this, ScreenRecordService::class.java).setAction(ScreenRecordService.ACTION_RESUME))
                pauseBtn.setImageResource(android.R.drawable.ic_media_pause)
            } else {
                startService(Intent(this, ScreenRecordService::class.java).setAction(ScreenRecordService.ACTION_PAUSE))
                pauseBtn.setImageResource(android.R.drawable.ic_media_play)
            }
        }

        controlView!!.findViewById<ImageButton>(R.id.btnFloatCamera).setOnClickListener {
            if (facecamShown) hideFacecam() else showFacecam()
        }

        windowManager.addView(controlView, params)
    }

    private fun showFacecam() {
        if (facecamShown) return
        facecamView = LayoutInflater.from(this).inflate(R.layout.facecam_widget, null)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.BOTTOM or Gravity.END
        params.x = 16
        params.y = 200

        makeDraggable(facecamView!!, params)
        windowManager.addView(facecamView, params)
        facecamShown = true

        val previewView = facecamView!!.findViewById<PreviewView>(R.id.facecamPreview)
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            cameraProvider = future.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val selector = CameraSelector.DEFAULT_FRONT_CAMERA
            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(this, selector, preview)
            } catch (_: Exception) {
            }
        }, androidx.core.content.ContextCompat.getMainExecutor(this))
    }

    private fun hideFacecam() {
        if (!facecamShown) return
        cameraProvider?.unbindAll()
        facecamView?.let { windowManager.removeView(it) }
        facecamView = null
        facecamShown = false
    }

    private fun makeDraggable(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f

        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - touchX).toInt()
                    params.y = initialY + (event.rawY - touchY).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(timerRunnable)
        cameraProvider?.unbindAll()
        controlView?.let { runCatching { windowManager.removeView(it) } }
        facecamView?.let { runCatching { windowManager.removeView(it) } }
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }
}
