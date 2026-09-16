package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.model.CalendarEvent
import com.example.receiver.ReminderAlarmReceiver

class VoiceReminderAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun scheduleReminder(event: CalendarEvent) {
        if (alarmManager == null) return

        val triggerTime = event.getNextAlarmTriggerTime()
        if (triggerTime == null) {
            Log.d("AlarmScheduler", "No future alarm trigger time for event ${event.title}")
            return
        }

        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_REMINDER_ALARM
            putExtra(ReminderAlarmReceiver.EXTRA_EVENT_ID, event.id)
            putExtra(ReminderAlarmReceiver.EXTRA_EVENT_TITLE, event.title)
            putExtra(ReminderAlarmReceiver.EXTRA_EVENT_DESCRIPTION, event.description)
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_MINUTES, event.reminderMinutesBefore)
            putExtra(ReminderAlarmReceiver.EXTRA_VIBRATE, event.vibrateAndRing)
            putExtra(ReminderAlarmReceiver.EXTRA_SPEAK, event.speakAnnouncement)
            putExtra(ReminderAlarmReceiver.EXTRA_CATEGORY, event.category.name)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.toInt(),
            intent,
            flags
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    setExactAlarm(triggerTime, pendingIntent)
                } else {
                    // Fallback to inexact or allowWhileIdle
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setExactAlarm(triggerTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
            Log.d("AlarmScheduler", "Scheduled alarm for '${event.title}' at $triggerTime")
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "Security exception scheduling alarm: ${e.message}")
            // Fallback
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    private fun setExactAlarm(triggerTime: Long, pendingIntent: PendingIntent) {
        // Use AlarmClockInfo for the highest reliability during Doze mode
        val showIntent = Intent(context, com.example.MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            0,
            showIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)
        alarmManager?.setAlarmClock(alarmClockInfo, pendingIntent)
    }

    fun cancelReminder(eventId: Long) {
        if (alarmManager == null) return
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_REMINDER_ALARM
        }
        val flags = PendingIntent.FLAG_NO_CREATE or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventId.toInt(),
            intent,
            flags
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("AlarmScheduler", "Cancelled alarm for eventId $eventId")
        }
    }
}
