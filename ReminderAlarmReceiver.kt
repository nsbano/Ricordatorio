package com.example.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.MemoCalendarApp
import com.example.alarm.VoiceReminderAlarmScheduler
import com.example.model.CalendarEvent
import com.example.service.ReminderVoiceService
import com.example.tts.TextToSpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

data class ActiveAlarmAlert(
    val eventId: Long,
    val title: String,
    val description: String,
    val minutesBefore: Int,
    val triggerTime: Long = System.currentTimeMillis()
)

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        when (action) {
            ACTION_REMINDER_ALARM -> {
                handleAlarmTrigger(context, intent)
            }
            ACTION_REPEAT_VOICE -> {
                val title = intent.getStringExtra(EXTRA_EVENT_TITLE) ?: ""
                val desc = intent.getStringExtra(EXTRA_EVENT_DESCRIPTION) ?: ""
                val minutes = intent.getIntExtra(EXTRA_REMINDER_MINUTES, 3)
                ReminderVoiceService.start(context, title, desc, minutes, vibrate = true, speak = true)
            }
            ACTION_DISMISS_ALARM -> {
                val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
                if (eventId != -1L) {
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.cancel(eventId.toInt())
                }
                TextToSpeechManager.getInstance(context).stop()
            }
        }
    }

    private fun handleAlarmTrigger(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, 0L)
        val title = intent.getStringExtra(EXTRA_EVENT_TITLE) ?: "Attività in programma"
        val description = intent.getStringExtra(EXTRA_EVENT_DESCRIPTION) ?: ""
        val minutesRemaining = intent.getIntExtra(EXTRA_REMINDER_MINUTES, 3)
        val vibrate = intent.getBooleanExtra(EXTRA_VIBRATE, true)
        val speak = intent.getBooleanExtra(EXTRA_SPEAK, true)

        // 1. Post rich heads-up Notification
        postAlarmNotification(context, eventId, title, description, minutesRemaining)

        // 2. Start foreground voice service to speak the reminder aloud and vibrate
        ReminderVoiceService.start(
            context,
            title,
            description,
            minutesRemaining,
            vibrate = vibrate,
            speak = speak
        )

        // 3. Emit in-app alert event for when app is currently open
        val alert = ActiveAlarmAlert(
            eventId = eventId,
            title = title,
            description = description,
            minutesBefore = minutesRemaining
        )
        CoroutineScope(Dispatchers.Default).launch {
            _activeAlertFlow.emit(alert)
        }

        // 4. If this is a recurring event, re-schedule the next occurrence
        val app = context.applicationContext as? MemoCalendarApp
        if (app != null && eventId > 0) {
            CoroutineScope(Dispatchers.IO).launch {
                val event = app.database.eventDao().getEventById(eventId)
                if (event != null && event.recurrence != com.example.model.RecurrenceType.NONE) {
                    val scheduler = VoiceReminderAlarmScheduler(context)
                    scheduler.scheduleReminder(event)
                }
            }
        }
    }

    private fun postAlarmNotification(
        context: Context,
        eventId: Long,
        title: String,
        description: String,
        minutesRemaining: Int
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent when clicking the notification body
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_EVENT_ID, eventId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            eventId.toInt(),
            contentIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        // Action: "Ascolta di nuovo a voce"
        val repeatIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_REPEAT_VOICE
            putExtra(EXTRA_EVENT_TITLE, title)
            putExtra(EXTRA_EVENT_DESCRIPTION, description)
            putExtra(EXTRA_REMINDER_MINUTES, minutesRemaining)
        }
        val repeatPendingIntent = PendingIntent.getBroadcast(
            context,
            (eventId + 10000).toInt(),
            repeatIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        // Action: "Ho capito / Chiudi"
        val dismissIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_DISMISS_ALARM
            putExtra(EXTRA_EVENT_ID, eventId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            (eventId + 20000).toInt(),
            dismissIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val timeSub = if (minutesRemaining > 0) {
            "Tra $minutesRemaining minuti!"
        } else {
            "Inizia adesso!"
        }

        val bodyText = if (description.isNotBlank()) {
            "$timeSub • $description"
        } else {
            "$timeSub Promemoria vocale attivo."
        }

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, MemoCalendarApp.CHANNEL_ID_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("🔔 $title")
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$bodyText\nAscolta l'annuncio o tocca per aprire."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setSound(alarmSound)
            .setVibrate(longArrayOf(0, 800, 300, 800, 300, 800))
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_btn_speak_now, "Ascolta di nuovo", repeatPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Ho capito", dismissPendingIntent)

        notificationManager.notify(eventId.toInt(), builder.build())
    }

    companion object {
        const val ACTION_REMINDER_ALARM = "com.aistudio.promemoriavocale.ACTION_REMINDER_ALARM"
        const val ACTION_REPEAT_VOICE = "com.aistudio.promemoriavocale.ACTION_REPEAT_VOICE"
        const val ACTION_DISMISS_ALARM = "com.aistudio.promemoriavocale.ACTION_DISMISS_ALARM"

        const val EXTRA_EVENT_ID = "extra_event_id"
        const val EXTRA_EVENT_TITLE = "extra_event_title"
        const val EXTRA_EVENT_DESCRIPTION = "extra_event_description"
        const val EXTRA_REMINDER_MINUTES = "extra_reminder_minutes"
        const val EXTRA_VIBRATE = "extra_vibrate"
        const val EXTRA_SPEAK = "extra_speak"
        const val EXTRA_CATEGORY = "extra_category"

        private val _activeAlertFlow = MutableSharedFlow<ActiveAlarmAlert>(extraBufferCapacity = 5)
        val activeAlertFlow: SharedFlow<ActiveAlarmAlert> = _activeAlertFlow.asSharedFlow()
    }
}
