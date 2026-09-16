package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.MemoCalendarApp
import com.example.R
import com.example.tts.TextToSpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ReminderVoiceService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MemoCal:VoiceServiceWakeLock").apply {
            acquire(60_000L) // 1 minute maximum
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val eventTitle = intent?.getStringExtra(EXTRA_TITLE) ?: "Attività programmata"
        val eventDescription = intent?.getStringExtra(EXTRA_DESCRIPTION) ?: ""
        val minutesRemaining = intent?.getIntExtra(EXTRA_MINUTES, 3) ?: 3
        val shouldVibrate = intent?.getBooleanExtra(EXTRA_VIBRATE, true) ?: true
        val shouldSpeak = intent?.getBooleanExtra(EXTRA_SPEAK, true) ?: true

        startForeground(NOTIFICATION_ID, buildForegroundNotification(eventTitle))

        if (shouldVibrate) {
            triggerVibration()
        }

        if (shouldSpeak) {
            speakReminder(eventTitle, eventDescription, minutesRemaining)
        } else {
            // Stop quickly if speaking is disabled
            serviceScope.launch {
                delay(3000)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun buildForegroundNotification(title: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        return NotificationCompat.Builder(this, MemoCalendarApp.CHANNEL_ID_VOICE_SERVICE)
            .setContentTitle("Promemoria Vocale Attivo")
            .setContentText("Lettura ad alta voce: $title")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun triggerVibration() {
        try {
            val pattern = longArrayOf(0, 800, 300, 800, 300, 800)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun speakReminder(title: String, description: String, minutes: Int) {
        val ttsManager = TextToSpeechManager.getInstance(this)
        val timeNotice = when (minutes) {
            0 -> "è adesso"
            1 -> "è tra un minuto"
            else -> "è tra $minutes minuti"
        }

        val detailsNotice = if (description.isNotBlank()) "Nota: $description." else ""

        // Clear, reassuring Italian text tailored for cognitive memory reinforcement
        val announcement = "Attenzione! Promemoria per $title $timeNotice. $detailsNotice Ripeto: $title $timeNotice."

        serviceScope.launch {
            // First spoken announcement
            ttsManager.speak(announcement, flush = true)

            // Wait a moment, then repeat once more for maximum memory retention
            delay(8000)
            val repetition = "Ricorda: $title $timeNotice."
            ttsManager.speak(repetition, flush = false) {
                // Once completed, stop foreground service smoothly
                serviceScope.launch {
                    delay(1500)
                    stopSelf()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val NOTIFICATION_ID = 9001
        const val ACTION_STOP = "com.aistudio.promemoriavocale.ACTION_STOP_SERVICE"

        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val EXTRA_MINUTES = "extra_minutes"
        const val EXTRA_VIBRATE = "extra_vibrate"
        const val EXTRA_SPEAK = "extra_speak"

        fun start(
            context: Context,
            title: String,
            description: String,
            minutes: Int,
            vibrate: Boolean,
            speak: Boolean
        ) {
            val intent = Intent(context, ReminderVoiceService::class.java).apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_DESCRIPTION, description)
                putExtra(EXTRA_MINUTES, minutes)
                putExtra(EXTRA_VIBRATE, vibrate)
                putExtra(EXTRA_SPEAK, speak)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
