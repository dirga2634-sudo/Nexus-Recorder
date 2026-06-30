package id.nexus.recorder.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import id.nexus.recorder.databinding.ActivitySettingsBinding
import id.nexus.recorder.util.Prefs

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Prefs.load(this)

        val resAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, Prefs.resolutionOptions)
        binding.spinnerResolution.adapter = resAdapter
        binding.spinnerResolution.setSelection(Prefs.resolutionOptions.indexOf(Prefs.resolution).coerceAtLeast(0))

        val fpsAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, Prefs.fpsOptions.map { "$it fps" })
        binding.spinnerFps.adapter = fpsAdapter
        binding.spinnerFps.setSelection(Prefs.fpsOptions.indexOf(Prefs.fps).coerceAtLeast(0))

        val bitrateAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, Prefs.bitrateOptions.map { "$it Mbps" })
        binding.spinnerBitrate.adapter = bitrateAdapter
        binding.spinnerBitrate.setSelection(Prefs.bitrateOptions.indexOf(Prefs.bitrateMbps).coerceAtLeast(0))

        val audioOptions = listOf("Mikrofon Aktif", "Tanpa Audio")
        val audioAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, audioOptions)
        binding.spinnerAudio.adapter = audioAdapter
        binding.spinnerAudio.setSelection(if (Prefs.useMic) 0 else 1)

        binding.switchFacecam.isChecked = Prefs.facecamEnabled
        binding.switchFloating.isChecked = Prefs.floatingWidgetEnabled
    }

    override fun onPause() {
        super.onPause()
        Prefs.resolution = Prefs.resolutionOptions[binding.spinnerResolution.selectedItemPosition]
        Prefs.fps = Prefs.fpsOptions[binding.spinnerFps.selectedItemPosition]
        Prefs.bitrateMbps = Prefs.bitrateOptions[binding.spinnerBitrate.selectedItemPosition]
        Prefs.useMic = binding.spinnerAudio.selectedItemPosition == 0
        Prefs.facecamEnabled = binding.switchFacecam.isChecked
        Prefs.floatingWidgetEnabled = binding.switchFloating.isChecked
        Prefs.save(this)
    }
}
