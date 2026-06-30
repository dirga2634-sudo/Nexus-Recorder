package id.nexus.recorder.model

import java.io.File

data class RecordingItem(
    val file: File,
    val sizeMb: Double,
    val dateLabel: String
)
