package id.nexus.recorder.util

import android.content.Context

object Prefs {
    private const val NAME = "nexus_recorder_prefs"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    // Resolution presets stored as "widthxheight"
    var resolution: String = "1080x1920"
    var fps: Int = 60
    var bitrateMbps: Int = 12
    var useMic: Boolean = true
    var facecamEnabled: Boolean = false
    var floatingWidgetEnabled: Boolean = true

    val resolutionOptions = listOf("640x360", "1280x720", "1920x1080", "1440x2560", "2160x3840")
    val fpsOptions = listOf(24, 30, 60, 90, 120)
    val bitrateOptions = listOf(4, 8, 12, 20, 30, 50)

    fun load(ctx: Context) {
        val p = sp(ctx)
        resolution = p.getString("resolution", resolution) ?: resolution
        fps = p.getInt("fps", fps)
        bitrateMbps = p.getInt("bitrate", bitrateMbps)
        useMic = p.getBoolean("use_mic", useMic)
        facecamEnabled = p.getBoolean("facecam", facecamEnabled)
        floatingWidgetEnabled = p.getBoolean("floating_widget", floatingWidgetEnabled)
    }

    fun save(ctx: Context) {
        sp(ctx).edit()
            .putString("resolution", resolution)
            .putInt("fps", fps)
            .putInt("bitrate", bitrateMbps)
            .putBoolean("use_mic", useMic)
            .putBoolean("facecam", facecamEnabled)
            .putBoolean("floating_widget", floatingWidgetEnabled)
            .apply()
    }

    fun widthHeight(): Pair<Int, Int> {
        val parts = resolution.split("x")
        return Pair(parts[0].toInt(), parts[1].toInt())
    }
}
