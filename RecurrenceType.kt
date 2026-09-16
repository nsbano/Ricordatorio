package com.example.model

enum class RecurrenceType(val label: String, val spokenDescription: String) {
    NONE("Nessuna (Evento singolo)", "Non si ripete"),
    DAILY("Ogni giorno", "Ripetuto ogni giorno"),
    WEEKLY("Ogni settimana", "Ripetuto ogni settimana"),
    MONTHLY("Ogni mese", "Ripetuto ogni mese"),
    YEARLY("Ogni anno", "Ripetuto ogni anno")
}
