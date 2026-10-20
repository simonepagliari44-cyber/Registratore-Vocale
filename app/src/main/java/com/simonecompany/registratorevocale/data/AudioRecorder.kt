package com.simonecompany.registratorevocale.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import com.simonecompany.registratorevocale.model.Recording
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var currentUri: Uri? = null
    private var currentFile: File? = null
    private var pendingPfd: ParcelFileDescriptor? = null
    var isRecording: Boolean = false
        private set
    var isPaused: Boolean = false
        private set
    var startTime: Long = 0L
        private set
    private var pausedTotalMs = 0L
    private var pauseStartTime = 0L

    fun start(quality: AudioQuality = AudioQuality.MEDIUM): Boolean {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "record_$timestamp.m4a"

        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        r.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(quality.bitRate)
            setAudioSamplingRate(quality.samplingRate)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                    put(
                        MediaStore.Audio.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_MUSIC}/${StorageHelper.FOLDER_NAME}"
                    )
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }
                val collection = MediaStore.Audio.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY
                )
                val uri = context.contentResolver.insert(collection, values) ?: return false
                val pfd = context.contentResolver.openFileDescriptor(uri, "w") ?: return false
                r.setOutputFile(pfd.fileDescriptor)
                currentUri = uri
                currentFile = null
                pendingPfd = pfd
            } else {
                val dir = StorageHelper.legacyDirectory(context)
                val file = File(dir, fileName)
                r.setOutputFile(file.absolutePath)
                currentUri = Uri.fromFile(file)
                currentFile = file
                pendingPfd = null
            }

            r.prepare()
            r.start()
        } catch (e: Exception) {
            r.release()
            recorder = null
            currentUri = null
            currentFile = null
            runCatching { pendingPfd?.close() }
            pendingPfd = null
            return false
        }

        recorder = r
        isRecording = true
        isPaused = false
        pausedTotalMs = 0L
        pauseStartTime = 0L
        startTime = System.currentTimeMillis()
        return true
    }

    fun pause(): Boolean {
        val r = recorder ?: return false
        if (!isRecording || isPaused) return false
        return try {
            r.pause()
            isPaused = true
            pauseStartTime = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun resume(): Boolean {
        val r = recorder ?: return false
        if (!isRecording || !isPaused) return false
        return try {
            r.resume()
            pausedTotalMs += System.currentTimeMillis() - pauseStartTime
            pauseStartTime = 0L
            isPaused = false
            true
        } catch (e: Exception) {
            false
        }
    }

    fun elapsedMillis(): Long {
        if (startTime == 0L) return 0L
        val end = if (isPaused) pauseStartTime else System.currentTimeMillis()
        return (end - startTime - pausedTotalMs).coerceAtLeast(0L)
    }

    fun currentAmplitude(): Int {
        if (!isRecording || isPaused) return 0
        return runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
    }

    fun stop(): Recording? {
        val uri = currentUri
        val endRef = if (isPaused) pauseStartTime else System.currentTimeMillis()
        val durationMillis =
            if (startTime > 0) (endRef - startTime - pausedTotalMs).coerceAtLeast(0L) else 0L

        try {
            recorder?.let {
                if (isRecording) it.stop()
                it.reset()
                it.release()
            }
        } catch (e: IllegalStateException) {
            // Recorder not started or invalid state
        }
        recorder = null

        runCatching { pendingPfd?.close() }
        pendingPfd = null
        isRecording = false
        isPaused = false
        pausedTotalMs = 0L
        pauseStartTime = 0L

        if (uri != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.IS_PENDING, 0)
                    put(MediaStore.Audio.Media.DURATION, durationMillis)
                }
                runCatching { context.contentResolver.update(uri, values, null, null) }
            }
            val name = uri.lastPathSegment ?: "reg.mp4"
            val createdAt = currentFile?.lastModified()
                ?: (System.currentTimeMillis() / 1000L) * 1000L
            return Recording(
                id = name,
                name = name,
                uri = uri,
                durationMillis = durationMillis,
                createdAt = createdAt
            )
        }
        return null
    }

    fun cancel() {
        stop()
        currentUri?.let { uri ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching { context.contentResolver.delete(uri, null, null) }
            } else {
                currentFile?.delete()
            }
        }
        currentUri = null
        currentFile = null
    }
}
