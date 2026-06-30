package id.nexus.recorder.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.DisplayMetrics
import androidx.core.app.NotificationCompat
import id.nexus.recorder.R
import id.nexus.recorder.ui.MainActivity
import id.nexus.recorder.util.Prefs
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenRecordService : Service() {

    companion object {
        const val ACTION_START = "id.nexus.recorder.action.START"
        const val ACTION_STOP = "id.nexus.recorder.action.STOP"
        const val ACTION_PAUSE = "id.nexus.recorder.action.PAUSE"
        const val ACTION_RESUME = "id.nexus.recorder.action.RESUME"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val CHANNEL_ID = "nexus_recording_channel"
        const val NOTIF_ID = 1001

        var isRunning = false
        var isPaused = false
        var startTimeMs = 0L
        var outputFile: File? = null
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            stopRecordingInternal()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
                @Suppress("DEPRECATION")
                val data: Intent? = intent.getParcelableExtra(EXTRA_RESULT_DATA)
                if (resultCode != -1 && data != null) {
                    startForegroundNotification()
                    startRecording(resultCode, data)
                }
            }
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
            ACTION_STOP -> {
                stopRecordingInternal()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_recording),
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }

        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, ScreenRecordService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_recording_title))
            .setContentText(getString(R.string.notif_recording_text))
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun startRecording(resultCode: Int, data: Intent) {
        Prefs.load(this)
        val (reqWidth, reqHeight) = Prefs.widthHeight()
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager().defaultDisplay.getRealMetrics(metrics)
        val density = metrics.densityDpi

        val moviesDir = File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "NexusRecorder")
        if (!moviesDir.exists()) moviesDir.mkdirs()
        val fileName = "NexusRec_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.mp4"
        val file = File(moviesDir, fileName)
        outputFile = file

        mediaRecorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else MediaRecorder()).apply {
            if (Prefs.useMic) setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(file.absolutePath)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            if (Prefs.useMic) setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoSize(reqWidth, reqHeight)
            setVideoFrameRate(Prefs.fps)
            setVideoEncodingBitRate(Prefs.bitrateMbps * 1_000_000)
            if (Prefs.useMic) {
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44_100)
            }
            prepare()
        }

        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)
        mediaProjection?.registerCallback(projectionCallback, null)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "NexusRecorderDisplay",
            reqWidth, reqHeight, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder?.surface, null, null
        )

        mediaRecorder?.start()
        isRunning = true
        isPaused = false
        startTimeMs = System.currentTimeMillis()

        if (Prefs.floatingWidgetEnabled) {
            startService(Intent(this, FloatingControlService::class.java))
        }
    }

    private fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isRunning && !isPaused) {
            mediaRecorder?.pause()
            isPaused = true
        }
    }

    private fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isRunning && isPaused) {
            mediaRecorder?.resume()
            isPaused = false
        }
    }

    private fun stopRecordingInternal() {
        if (!isRunning) return
        try {
            mediaRecorder?.stop()
        } catch (_: Exception) {
        }
        mediaRecorder?.reset()
        mediaRecorder?.release()
        mediaRecorder = null

        virtualDisplay?.release()
        virtualDisplay = null

        mediaProjection?.unregisterCallback(projectionCallback)
        mediaProjection?.stop()
        mediaProjection = null

        isRunning = false
        isPaused = false

        stopService(Intent(this, FloatingControlService::class.java))
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun windowManager() = getSystemService(WINDOW_SERVICE) as android.view.WindowManager

    override fun onDestroy() {
        stopRecordingInternal()
        super.onDestroy()
    }
}
