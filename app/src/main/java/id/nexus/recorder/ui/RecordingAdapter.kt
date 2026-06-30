package id.nexus.recorder.ui

import android.content.Context
import android.content.Intent
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import id.nexus.recorder.databinding.ItemRecordingBinding
import id.nexus.recorder.model.RecordingItem

class RecordingAdapter(
    private val context: Context,
    private val items: MutableList<RecordingItem>,
    private val onDeleted: (RecordingItem) -> Unit
) : RecyclerView.Adapter<RecordingAdapter.VH>() {

    inner class VH(val binding: ItemRecordingBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemRecordingBinding.inflate(LayoutInflater.from(context), parent, false)
        return VH(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.fileName.text = item.file.name
        holder.binding.fileInfo.text = "${"%.1f".format(item.sizeMb)} MB • ${item.dateLabel}"

        try {
            val thumb = ThumbnailUtils.createVideoThumbnail(
                item.file.absolutePath, MediaStore.Images.Thumbnails.MINI_KIND
            )
            holder.binding.thumb.setImageBitmap(thumb)
        } catch (_: Exception) {
        }

        holder.binding.root.setOnClickListener {
            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", item.file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        }

        holder.binding.btnShare.setOnClickListener {
            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", item.file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Bagikan rekaman"))
        }

        holder.binding.btnDelete.setOnClickListener {
            if (item.file.delete()) {
                val pos = items.indexOf(item)
                items.removeAt(pos)
                notifyItemRemoved(pos)
                onDeleted(item)
            }
        }
    }
}
