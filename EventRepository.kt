package com.example.data

import android.content.Context
import com.example.alarm.VoiceReminderAlarmScheduler
import com.example.model.CalendarEvent
import com.example.model.EventCategory
import com.example.model.RecurrenceType
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class EventRepository(
    private val eventDao: EventDao,
    private val context: Context
) {
    private val alarmScheduler = VoiceReminderAlarmScheduler(context)

    val allEvents: Flow<List<CalendarEvent>> = eventDao.getAllEvents()

    suspend fun getEventById(id: Long): CalendarEvent? {
        return eventDao.getEventById(id)
    }

    suspend fun insertEvent(event: CalendarEvent): Long {
        val id = eventDao.insertEvent(event)
        val eventWithId = event.copy(id = id)
        alarmScheduler.scheduleReminder(eventWithId)
        return id
    }

    suspend fun updateEvent(event: CalendarEvent) {
        eventDao.updateEvent(event)
        alarmScheduler.cancelReminder(event.id)
        alarmScheduler.scheduleReminder(event)
    }

    suspend fun deleteEvent(event: CalendarEvent) {
        alarmScheduler.cancelReminder(event.id)
        eventDao.deleteEvent(event)
    }

    suspend fun deleteEventById(id: Long) {
        alarmScheduler.cancelReminder(id)
        eventDao.deleteEventById(id)
    }

    /**
     * Seeds initial memory aid events if database is fresh.
     */
    suspend fun seedInitialDataIfEmpty() {
        val existing = eventDao.getAllEventsSync()
        if (existing.isNotEmpty()) return

        val now = Calendar.getInstance()

        // 1. Daily morning medication (starts at 09:00, repeats daily, 3 min before voice reminder)
        val medCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val medEvent = CalendarEvent(
            title = "Prendere compressa della pressione",
            description = "Prendere con un bicchiere d'acqua pieno dopo colazione",
            startEpochMillis = medCal.timeInMillis,
            endEpochMillis = medCal.timeInMillis + 15 * 60 * 1000L,
            recurrence = RecurrenceType.DAILY,
            reminderMinutesBefore = 3,
            speakAnnouncement = true,
            vibrateAndRing = true,
            category = EventCategory.MEDICINE
        )
        insertEvent(medEvent)

        // 2. Doctor checkup (tomorrow afternoon)
        val docCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
        }
        val docEvent = CalendarEvent(
            title = "Visita dal Dottore Rossi",
            description = "Portare i referti delle analisi del sangue e la tessera sanitaria",
            startEpochMillis = docCal.timeInMillis,
            endEpochMillis = docCal.timeInMillis + 45 * 60 * 1000L,
            recurrence = RecurrenceType.NONE,
            reminderMinutesBefore = 3,
            speakAnnouncement = true,
            vibrateAndRing = true,
            category = EventCategory.DOCTOR
        )
        insertEvent(docEvent)

        // 3. Weekly family call (Sunday / Weekly)
        val familyCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val famEvent = CalendarEvent(
            title = "Telefonata con i figli",
            description = "Chiamare per sapere come stanno e salutare i nipoti",
            startEpochMillis = familyCal.timeInMillis,
            endEpochMillis = familyCal.timeInMillis + 30 * 60 * 1000L,
            recurrence = RecurrenceType.WEEKLY,
            reminderMinutesBefore = 3,
            speakAnnouncement = true,
            vibrateAndRing = true,
            category = EventCategory.FAMILY
        )
        insertEvent(famEvent)

        // 4. Today evening reminder
        val eveningCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
        }
        val eveEvent = CalendarEvent(
            title = "Bere tisana rilassante e chiudere il gas",
            description = "Controllare i fornelli in cucina prima di andare a dormire",
            startEpochMillis = eveningCal.timeInMillis,
            endEpochMillis = eveningCal.timeInMillis + 15 * 60 * 1000L,
            recurrence = RecurrenceType.DAILY,
            reminderMinutesBefore = 3,
            speakAnnouncement = true,
            vibrateAndRing = true,
            category = EventCategory.ROUTINE
        )
        insertEvent(eveEvent)
    }
}
