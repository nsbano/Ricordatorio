package com.example.data

import androidx.room.TypeConverter
import com.example.model.EventCategory
import com.example.model.RecurrenceType

class Converters {
    @TypeConverter
    fun fromRecurrenceType(value: RecurrenceType?): String {
        return value?.name ?: RecurrenceType.NONE.name
    }

    @TypeConverter
    fun toRecurrenceType(value: String?): RecurrenceType {
        return try {
            if (value != null) RecurrenceType.valueOf(value) else RecurrenceType.NONE
        } catch (e: Exception) {
            RecurrenceType.NONE
        }
    }

    @TypeConverter
    fun fromEventCategory(value: EventCategory?): String {
        return value?.name ?: EventCategory.APPOINTMENT.name
    }

    @TypeConverter
    fun toEventCategory(value: String?): EventCategory {
        return try {
            if (value != null) EventCategory.valueOf(value) else EventCategory.APPOINTMENT
        } catch (e: Exception) {
            EventCategory.APPOINTMENT
        }
    }
}
