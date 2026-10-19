package com.simonecompany.registratorevocale.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.simonecompany.registratorevocale.MainActivity
import com.simonecompany.registratorevocale.R
import com.simonecompany.registratorevocale.model.Recording

class PlaybackService : Service() {

    companion object {
        private const val CHANNEL_ID = "playback_channel"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_TOGGLE = "com.simonecompany.registratorevocale.action.TOGGLE"
        const val ACTION_STOP = "com.simonecompany.registratorevocale.action.STOP"
        private const val ACTION_REFRESH = "com.simonecompany.registratorevocale.action.REFRESH"

        fun ensureStarted(context: Context) {
            runCatching {
                context.startForegroundService(
                    Intent(context, PlaybackService::class.java)
                )
            }
        }

        fun refresh(context: Context) {
            runCatching {
                context.startService(
                    Intent(context, PlaybackService::class.java).setAction(ACTION_REFRESH)
                )
            }
        }

        fun stopSelf(context: Context) {
            runCatching { context.stopService(Intent(context, PlaybackService::class.java)) }
        }
    }

    private lateinit var notificationManager: NotificationManager
    private var mediaSession: MediaSessionCompat? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Riproduzione",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        mediaSession = MediaSessionCompat(this, "PlaybackService").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() = PlaybackController.resume(applicationContext)
                override fun onPause() = PlaybackController.pause(applicationContext)
                override fun onStop() = PlaybackController.stop(applicationContext)
                override fun onSeekTo(pos: Long) = PlaybackController.seekTo(pos)
            })
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> PlaybackController.toggle(applicationContext)
            ACTION_STOP -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                PlaybackController.stop(applicationContext)
                stopSelf()
                return START_NOT_STICKY
            }
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        if (PlaybackController.current.value == null) stopSelf()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        notificationManager.cancel(NOTIFICATION_ID)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val recording = PlaybackController.current.value
        val playing = PlaybackController.isPlaying.value
        val position = PlaybackController.positionMs.value
        val duration = PlaybackController.durationMs.value

        updateSession(recording, playing, position, duration)

        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val toggleIntent = PendingIntent.getForegroundService(
            this, 1,
            Intent(this, PlaybackService::class.java).setAction(ACTION_TOGGLE),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getForegroundService(
            this, 2,
            Intent(this, PlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(recording?.name ?: "Registratore Vocale")
            .setContentText(if (playing) "Riproduzione in corso" else "In pausa")
            .setContentIntent(contentIntent)
            .setOngoing(playing)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setOnlyAlertOnce(true)
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1)
            )

        if (playing) {
            builder.addAction(android.R.drawable.ic_media_pause, "Pausa", toggleIntent)
        } else {
            builder.addAction(android.R.drawable.ic_media_play, "Riproduci", toggleIntent)
        }
        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Stop",
            stopIntent
        )
        return builder.build()
    }

    private fun updateSession(
        recording: Recording?,
        playing: Boolean,
        position: Long,
        duration: Long
    ) {
        val session = mediaSession ?: return
        session.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_SEEK_TO
                )
                .setState(
                    if (playing) PlaybackStateCompat.STATE_PLAYING
                    else PlaybackStateCompat.STATE_PAUSED,
                    position,
                    1f
                )
                .build()
        )
        if (recording != null) {
            session.setMetadata(
                MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, recording.name)
                    .putString(
                        MediaMetadataCompat.METADATA_KEY_ARTIST,
                        "Registratore Vocale"
                    )
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                    .build()
            )
        }
    }
}
