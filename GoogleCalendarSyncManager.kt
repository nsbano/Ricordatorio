package com.example.sync

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.EventRepository
import com.example.model.CalendarEvent
import com.example.model.EventCategory
import com.example.model.RecurrenceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone

data class GoogleCalendarAccount(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val isPrimary: Boolean
)

data class SyncResult(
    val importedCount: Int,
    val exportedCount: Int,
    val errorMessage: String? = null
)

class GoogleCalendarSyncManager(
    private val context: Context,
    private val repository: EventRepository
) {

    fun hasCalendarPermissions(): Boolean {
        val readPerm = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR)
        val writePerm = ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_CALENDAR)
        return readPerm == PackageManager.PERMISSION_GRANTED && writePerm == PackageManager.PERMISSION_GRANTED
    }

    suspend fun getAvailableCalendars(): List<GoogleCalendarAccount> = withContext(Dispatchers.IO) {
        if (!hasCalendarPermissions()) return@withContext emptyList()

        val calendarList = mutableListOf<GoogleCalendarAccount>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.IS_PRIMARY
        )

        val uri: Uri = CalendarContract.Calendars.CONTENT_URI
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${CalendarContract.Calendars.IS_PRIMARY} DESC, ${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC"
            )

            cursor?.let {
                val idIdx = it.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
                val nameIdx = it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accountIdx = it.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
                val primaryIdx = it.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)

                while (it.moveToNext()) {
                    val id = it.getLong(idIdx)
                    val name = it.getString(nameIdx) ?: "Calendario Principale"
                    val account = it.getString(accountIdx) ?: "Google"
                    val isPrimary = if (primaryIdx != -1) it.getInt(primaryIdx) == 1 else false
                    calendarList.add(GoogleCalendarAccount(id, name, account, isPrimary))
                }
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "Error querying calendars: ${e.message}")
        } finally {
            cursor?.close()
        }

        return@withContext calendarList
    }

    suspend fun syncWithGoogleCalendar(selectedCalendarId: Long): SyncResult = withContext(Dispatchers.IO) {
        if (!hasCalendarPermissions()) {
            return@withContext SyncResult(0, 0, "Permessi di lettura/scrittura calendario non concessi")
        }

        var imported = 0
        var exported = 0

        try {
            // 1. IMPORT: Read events from selected Google Calendar (from current time to next 60 days)
            val now = System.currentTimeMillis()
            val sixtyDaysLater = now + (60L * 24 * 60 * 60 * 1000L)

            val projection = arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.RRULE
            )

            val selection = "${CalendarContract.Events.CALENDAR_ID} = ? AND ${CalendarContract.Events.DTSTART} >= ?"
            val selectionArgs = arrayOf(selectedCalendarId.toString(), (now - 24 * 60 * 60 * 1000L).toString())

            val cursor = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )

            cursor?.use {
                val idIdx = it.getColumnIndexOrThrow(CalendarContract.Events._ID)
                val titleIdx = it.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
                val descIdx = it.getColumnIndex(CalendarContract.Events.DESCRIPTION)
                val startIdx = it.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
                val endIdx = it.getColumnIndex(CalendarContract.Events.DTEND)
                val rruleIdx = it.getColumnIndex(CalendarContract.Events.RRULE)

                while (it.moveToNext()) {
                    val gId = it.getLong(idIdx)
                    val title = it.getString(titleIdx) ?: "Evento Google Calendar"
                    val desc = if (descIdx != -1) it.getString(descIdx) ?: "" else ""
                    val dtStart = it.getLong(startIdx)
                    val dtEnd = if (endIdx != -1 && !it.isNull(endIdx)) it.getLong(endIdx) else dtStart + 30 * 60 * 1000L
                    val rrule = if (rruleIdx != -1) it.getString(rruleIdx) else null

                    val recurrence = when {
                        rrule?.contains("DAILY", ignoreCase = true) == true -> RecurrenceType.DAILY
                        rrule?.contains("WEEKLY", ignoreCase = true) == true -> RecurrenceType.WEEKLY
                        rrule?.contains("MONTHLY", ignoreCase = true) == true -> RecurrenceType.MONTHLY
                        rrule?.contains("YEARLY", ignoreCase = true) == true -> RecurrenceType.YEARLY
                        else -> RecurrenceType.NONE
                    }

                    // Categorize based on keywords for memory aid
                    val category = categorizeTitle(title)

                    val app = context.applicationContext as com.example.MemoCalendarApp
                    val existing = app.database.eventDao().getEventByGoogleId(gId)
                    if (existing == null) {
                        val newEvent = CalendarEvent(
                            title = title,
                            description = desc,
                            startEpochMillis = dtStart,
                            endEpochMillis = dtEnd,
                            recurrence = recurrence,
                            reminderMinutesBefore = 3, // 3 minutes before by default
                            speakAnnouncement = true,
                            vibrateAndRing = true,
                            category = category,
                            googleEventId = gId
                        )
                        repository.insertEvent(newEvent)
                        imported++
                    }
                }
            }

            // 2. EXPORT: Push app events created locally that do not yet have a googleEventId
            val app = context.applicationContext as com.example.MemoCalendarApp
            val allEvents = app.database.eventDao().getAllEventsSync()

            for (event in allEvents) {
                if (event.googleEventId == null) {
                    val values = ContentValues().apply {
                        put(CalendarContract.Events.DTSTART, event.startEpochMillis)
                        put(CalendarContract.Events.DTEND, event.endEpochMillis)
                        put(CalendarContract.Events.TITLE, event.title)
                        put(CalendarContract.Events.DESCRIPTION, event.description)
                        put(CalendarContract.Events.CALENDAR_ID, selectedCalendarId)
                        put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)

                        when (event.recurrence) {
                            RecurrenceType.DAILY -> put(CalendarContract.Events.RRULE, "FREQ=DAILY")
                            RecurrenceType.WEEKLY -> put(CalendarContract.Events.RRULE, "FREQ=WEEKLY")
                            RecurrenceType.MONTHLY -> put(CalendarContract.Events.RRULE, "FREQ=MONTHLY")
                            RecurrenceType.YEARLY -> put(CalendarContract.Events.RRULE, "FREQ=YEARLY")
                            RecurrenceType.NONE -> {}
                        }
                    }

                    val eventUri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                    if (eventUri != null) {
                        val insertedId = ContentUris.parseId(eventUri)
                        repository.updateEvent(event.copy(googleEventId = insertedId))
                        exported++
                    }
                }
            }

            SyncResult(imported, exported)
        } catch (e: Exception) {
            Log.e("SyncManager", "Sync failed: ${e.message}", e)
            SyncResult(imported, exported, e.localizedMessage ?: "Errore durante la sincronizzazione")
        }
    }

    private fun categorizeTitle(title: String): EventCategory {
        val lower = title.lowercase()
        return when {
            lower.contains("medicin") || lower.contains("pastigl") || lower.contains("farmac") || lower.contains("terapia") || lower.contains("gocce") -> EventCategory.MEDICINE
            lower.contains("dottor") || lower.contains("medic") || lower.contains("visita") || lower.contains("ospedal") || lower.contains("dentist") -> EventCategory.DOCTOR
            lower.contains("chiam") || lower.contains("telefon") || lower.contains("figli") || lower.contains("nipote") || lower.contains("mamma") || lower.contains("papa") -> EventCategory.FAMILY
            lower.contains("pranzo") || lower.contains("cena") || lower.contains("mangiar") || lower.contains("acqua") || lower.contains("bere") -> EventCategory.MEALS
            lower.contains("spesa") || lower.contains("supermercat") || lower.contains("comprar") || lower.contains("negozio") -> EventCategory.SHOPPING
            lower.contains("banca") || lower.contains("posta") || lower.contains("notaio") || lower.contains("ufficio") || lower.contains("appuntament") -> EventCategory.APPOINTMENT
            else -> EventCategory.ROUTINE
        }
    }
}
