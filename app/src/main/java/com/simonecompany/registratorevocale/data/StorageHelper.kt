package com.simonecompany.registratorevocale.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.simonecompany.registratorevocale.model.Recording
import java.io.File

object StorageHelper {

    const val FOLDER_NAME = "Registratore vocale"

    fun getRecordings(context: Context): List<Recording> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            queryMediaStore(context)
        } else {
            legacyDirectory(context).listFiles()
                ?.filter { it.isFile && it.name.endsWith(".m4a") }
                ?.mapNotNull { toRecording(context, Uri.fromFile(it), it.name, it.lastModified()) }
                ?.sortedByDescending { it.createdAt } ?: emptyList()
        }
    }

    fun delete(context: Context, recording: Recording): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.delete(recording.uri, null, null) > 0
        } else {
            recording.uri.path?.let { File(it).delete() } ?: false
        }
    }

    fun rename(context: Context, recording: Recording, newName: String): String? {
        val cleaned = newName.trim().trimEnd('.')
        if (cleaned.isEmpty()) return null
        val target = if (cleaned.endsWith(".m4a", true)) cleaned else "$cleaned.m4a"
        if (target == recording.name) return null

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, target)
            }
            val updated = context.contentResolver.update(recording.uri, values, null, null)
            if (updated > 0) target else null
        } else {
            val oldFile = recording.uri.path?.let { File(it) }
            if (oldFile != null && oldFile.exists()) {
                val newFile = File(oldFile.parentFile, target)
                if (oldFile.renameTo(newFile)) target else null
            } else {
                null
            }
        }
    }

    fun legacyDirectory(context: Context): File {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            FOLDER_NAME
        )
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun queryMediaStore(context: Context): List<Recording> {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED
        )
        val selection = "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%$FOLDER_NAME%")

        val recordings = mutableListOf<Recording>()
        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: "record.m4a"
                val duration = cursor.getLong(durCol)
                val dateAdded = cursor.getLong(dateCol) * 1000L
                val uri = Uri.withAppendedPath(collection, id.toString())
                recordings.add(
                    Recording(
                        id = id.toString(),
                        name = name,
                        uri = uri,
                        durationMillis = duration,
                        createdAt = dateAdded
                    )
                )
            }
        }
        return recordings
    }

    private fun toRecording(
        context: Context,
        uri: Uri,
        name: String,
        updatedAt: Long
    ): Recording {
        val duration = readDuration(context, uri)
        return Recording(
            id = name,
            name = name,
            uri = uri,
            durationMillis = duration,
            createdAt = updatedAt
        )
    }

    fun readDuration(context: Context, uri: Uri): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val duration = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            retriever.release()
            duration
        } catch (e: Exception) {
            0L
        }
    }
}
