package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

@Entity(tableName = "calendar_events")
data class CalendarEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val startEpochMillis: Long,
    val endEpochMillis: Long = startEpochMillis + 30 * 60 * 1000L, // default 30 min duration
    val recurrence: RecurrenceType = RecurrenceType.NONE,
    val reminderMinutesBefore: Int = 3, // Default 3 minutes before as requested by user
    val speakAnnouncement: Boolean = true,
    val vibrateAndRing: Boolean = true,
    val category: EventCategory = EventCategory.APPOINTMENT,
    val googleEventId: Long? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Checks if this event occurs on the given date (day, month, year).
     */
    fun occursOnDate(targetYear: Int, targetMonth: Int, targetDayOfMonth: Int): Boolean {
        val eventCal = Calendar.getInstance().apply { timeInMillis = startEpochMillis }
        val eventYear = eventCal.get(Calendar.YEAR)
        val eventMonth = eventCal.get(Calendar.MONTH)
        val eventDay = eventCal.get(Calendar.DAY_OF_MONTH)

        val targetCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, targetYear)
            set(Calendar.MONTH, targetMonth)
            set(Calendar.DAY_OF_MONTH, targetDayOfMonth)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }

        // If target date is before event start date, it doesn't occur yet
        if (targetCal.timeInMillis < startEpochMillis - (24 * 60 * 60 * 1000L)) {
            return false
        }

        return when (recurrence) {
            RecurrenceType.NONE -> {
                eventYear == targetYear && eventMonth == targetMonth && eventDay == targetDayOfMonth
            }
            RecurrenceType.DAILY -> {
                // Occurs every day starting from event date
                true
            }
            RecurrenceType.WEEKLY -> {
                val eventDayOfWeek = eventCal.get(Calendar.DAY_OF_WEEK)
                val targetDayOfWeek = targetCal.get(Calendar.DAY_OF_WEEK)
                eventDayOfWeek == targetDayOfWeek
            }
            RecurrenceType.MONTHLY -> {
                eventDay == targetDayOfMonth
            }
            RecurrenceType.YEARLY -> {
                eventMonth == targetMonth && eventDay == targetDayOfMonth
            }
        }
    }

    /**
     * Returns the next start time epoch millis >= currentTimeMillis, taking into account recurrence.
     */
    fun getNextOccurrenceTime(fromTimeMillis: Long = System.currentTimeMillis()): Long? {
        if (recurrence == RecurrenceType.NONE) {
            return if (startEpochMillis >= fromTimeMillis) startEpochMillis else null
        }

        val eventCal = Calendar.getInstance().apply { timeInMillis = startEpochMillis }
        val hour = eventCal.get(Calendar.HOUR_OF_DAY)
        val minute = eventCal.get(Calendar.MINUTE)
        val second = eventCal.get(Calendar.SECOND)

        val candidateCal = Calendar.getInstance().apply {
            timeInMillis = fromTimeMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, second)
            set(Calendar.MILLISECOND, 0)
        }

        // If candidate time today is already in the past, advance to next step
        if (candidateCal.timeInMillis < fromTimeMillis) {
            when (recurrence) {
                RecurrenceType.DAILY -> candidateCal.add(Calendar.DAY_OF_YEAR, 1)
                RecurrenceType.WEEKLY -> candidateCal.add(Calendar.DAY_OF_YEAR, 1)
                RecurrenceType.MONTHLY -> candidateCal.add(Calendar.MONTH, 1)
                RecurrenceType.YEARLY -> candidateCal.add(Calendar.YEAR, 1)
                RecurrenceType.NONE -> return null
            }
        }

        when (recurrence) {
            RecurrenceType.DAILY -> return candidateCal.timeInMillis
            RecurrenceType.WEEKLY -> {
                val targetDayOfWeek = eventCal.get(Calendar.DAY_OF_WEEK)
                while (candidateCal.get(Calendar.DAY_OF_WEEK) != targetDayOfWeek) {
                    candidateCal.add(Calendar.DAY_OF_YEAR, 1)
                }
                return candidateCal.timeInMillis
            }
            RecurrenceType.MONTHLY -> {
                val targetDayOfMonth = eventCal.get(Calendar.DAY_OF_MONTH)
                val maxDay = candidateCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                candidateCal.set(Calendar.DAY_OF_MONTH, targetDayOfMonth.coerceAtMost(maxDay))
                if (candidateCal.timeInMillis < fromTimeMillis) {
                    candidateCal.add(Calendar.MONTH, 1)
                    val nextMax = candidateCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    candidateCal.set(Calendar.DAY_OF_MONTH, targetDayOfMonth.coerceAtMost(nextMax))
                }
                return candidateCal.timeInMillis
            }
            RecurrenceType.YEARLY -> {
                candidateCal.set(Calendar.MONTH, eventCal.get(Calendar.MONTH))
                candidateCal.set(Calendar.DAY_OF_MONTH, eventCal.get(Calendar.DAY_OF_MONTH))
                if (candidateCal.timeInMillis < fromTimeMillis) {
                    candidateCal.add(Calendar.YEAR, 1)
                }
                return candidateCal.timeInMillis
            }
            RecurrenceType.NONE -> return null
        }
    }

    /**
     * Next alarm trigger time in epoch millis.
     * Takes next occurrence minus reminderMinutesBefore.
     */
    fun getNextAlarmTriggerTime(fromTimeMillis: Long = System.currentTimeMillis()): Long? {
        val nextOccurrence = getNextOccurrenceTime(fromTimeMillis + (reminderMinutesBefore * 60 * 1000L))
            ?: return null
        val reminderOffsetMillis = reminderMinutesBefore * 60 * 1000L
        val alarmTime = nextOccurrence - reminderOffsetMillis
        return if (alarmTime > fromTimeMillis - 60_000L) alarmTime else null
    }

    /**
     * Builds the friendly spoken message in clear Italian for the voice synthesizer.
     */
    fun buildSpokenReminder(minutesRemaining: Int = reminderMinutesBefore): String {
        val timeNotice = when (minutesRemaining) {
            0 -> "è adesso"
            1 -> "è tra un minuto"
            else -> "è tra $minutesRemaining minuti"
        }

        val detailsNotice = if (description.isNotBlank()) {
            "Dettagli importanti: $description."
        } else {
            ""
        }

        return "Attenzione, promemoria! La tua attività, $title, $timeNotice. $detailsNotice Ripeto: $title, $timeNotice."
    }
}
