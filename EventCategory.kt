package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

enum class EventCategory(
    val displayName: String,
    val colorHex: String,
    val spokenCategory: String
) {
    MEDICINE("Medicine e Farmaci", "#E11D48", "Medicina o farmaco"),
    DOCTOR("Visita Medica", "#0D9488", "Visita dal dottore"),
    FAMILY("Famiglia e Amici", "#7C3AED", "Telefonata o incontro con familiari"),
    MEALS("Pasti e Idratazione", "#EA580C", "Pasto o promemoria acqua"),
    SHOPPING("Spesa e Commissioni", "#059669", "Spesa"),
    APPOINTMENT("Impegno e Ufficio", "#2563EB", "Appuntamento"),
    ROUTINE("Attività Quotidiana", "#D97706", "Attività quotidiana"),
    OTHER("Altro", "#475569", "Promemoria");

    fun getIcon(): ImageVector {
        return when (this) {
            MEDICINE -> Icons.Filled.Medication
            DOCTOR -> Icons.Filled.LocalHospital
            FAMILY -> Icons.Filled.People
            MEALS -> Icons.Filled.Restaurant
            SHOPPING -> Icons.Filled.ShoppingCart
            APPOINTMENT -> Icons.Filled.Event
            ROUTINE -> Icons.Filled.CheckCircle
            OTHER -> Icons.Filled.Bookmark
        }
    }
}
