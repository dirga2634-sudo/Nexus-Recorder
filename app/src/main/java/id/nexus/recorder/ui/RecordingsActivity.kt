package id.nexus.recorder.ui

import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import id.nexus.recorder.databinding.ActivityRecordingsBinding
import id.nexus.recorder.model.RecordingItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecordingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dir = java.io.File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "NexusRecorder")
        val files = dir.listFiles { f -> f.extension == "mp4" }?.sortedByDescending { it.lastModified() } ?: emptyList()

        val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        val items = files.map {
            RecordingItem(
                file = it,
                sizeMb = it.length() / (1024.0 * 1024.0),
                dateLabel = sdf.format(Date(it.lastModified()))
            )
        }.toMutableList()

        binding.recyclerRecordings.layoutManager = LinearLayoutManager(this)
        binding.recyclerRecordings.adapter = RecordingAdapter(this, items) { }
    }
}
