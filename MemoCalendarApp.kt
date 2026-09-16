package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import com.example.data.AppDatabase
import com.example.data.EventRepository
import com.example.tts.TextToSpeechManager

class MemoCalendarApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var repository: EventRepository
        private set
    lateinit var ttsManager: TextToSpeechManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
        repository = EventRepository(database.eventDao(), this)
        ttsManager = TextToSpeechManager.getInstance(this)

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // High-importance channel for vocal reminder alarms
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channelName = "Promemoria Parlanti e Allarmi"
            val channelDescription = "Notifiche ad alta priorità con sintesi vocale e vibrazione per supporto memoria"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(CHANNEL_ID_REMINDERS, channelName, importance).apply {
                description = channelDescription
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 800, 300, 800, 300, 800)
                setSound(alarmSound, audioAttributes)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            // Foreground service channel for speech announcement
            val serviceChannel = NotificationChannel(
                CHANNEL_ID_VOICE_SERVICE,
                "Annuncio Vocale in Corso",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifica attiva durante la lettura a voce alta del promemoria"
            }

            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    companion object {
        const val CHANNEL_ID_REMINDERS = "memo_voice_reminders_channel"
        const val CHANNEL_ID_VOICE_SERVICE = "memo_voice_service_channel"

        lateinit var instance: MemoCalendarApp
            private set
    }
}
