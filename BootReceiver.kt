package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.MemoCalendarApp
import com.example.alarm.VoiceReminderAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val app = context.applicationContext as? MemoCalendarApp ?: return
            val scheduler = VoiceReminderAlarmScheduler(context)

            CoroutineScope(Dispatchers.IO).launch {
                val allEvents = app.database.eventDao().getAllEventsSync()
                for (event in allEvents) {
                    scheduler.scheduleReminder(event)
                }
            }
        }
    }
}
