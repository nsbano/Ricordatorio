package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.CalendarEvent
import com.example.model.EventCategory
import com.example.model.RecurrenceType
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.IndigoPrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditEventDialog(
    initialEvent: CalendarEvent?,
    initialDate: Calendar,
    onDismiss: () -> Unit,
    onSave: (CalendarEvent) -> Unit
) {
    var title by remember { mutableStateOf(initialEvent?.title ?: "") }
    var description by remember { mutableStateOf(initialEvent?.description ?: "") }

    val eventCal = remember {
        Calendar.getInstance().apply {
            if (initialEvent != null) {
                timeInMillis = initialEvent.startEpochMillis
            } else {
                timeInMillis = initialDate.timeInMillis
                // If it's today, set to next full hour
                val currentHour = get(Calendar.HOUR_OF_DAY)
                set(Calendar.HOUR_OF_DAY, (currentHour + 1) % 24)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
        }
    }

    var selectedYear by remember { mutableIntStateOf(eventCal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(eventCal.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableIntStateOf(eventCal.get(Calendar.DAY_OF_MONTH)) }
    var selectedHour by remember { mutableIntStateOf(eventCal.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(eventCal.get(Calendar.MINUTE)) }

    var selectedCategory by remember { mutableStateOf(initialEvent?.category ?: EventCategory.MEDICINE) }
    var selectedRecurrence by remember { mutableStateOf(initialEvent?.recurrence ?: RecurrenceType.NONE) }
    var reminderMinutes by remember { mutableIntStateOf(initialEvent?.reminderMinutesBefore ?: 3) }
    var speakAnnouncement by remember { mutableStateOf(initialEvent?.speakAnnouncement ?: true) }
    var vibrateAndRing by remember { mutableStateOf(initialEvent?.vibrateAndRing ?: true) }

    var titleError by remember { mutableStateOf(false) }

    // Quick suggestions for memory aid
    val quickSuggestions = listOf(
        "Prendere le medicine" to EventCategory.MEDICINE,
        "Visita dal dottore" to EventCategory.DOCTOR,
        "Telefonare ai figli" to EventCategory.FAMILY,
        "Fare la spesa" to EventCategory.SHOPPING,
        "Bere un bicchiere d'acqua" to EventCategory.MEALS,
        "Chiudere gas e porta" to EventCategory.ROUTINE
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .testTag("add_edit_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialEvent == null) "Nuovo Promemoria" else "Modifica Promemoria",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick suggestions helper for cognitive memory ease
                Text(
                    text = "Suggerimenti rapidi:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    quickSuggestions.forEach { (suggTitle, suggCat) ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    title = suggTitle
                                    selectedCategory = suggCat
                                    titleError = false
                                }
                        ) {
                            Text(
                                text = "+ $suggTitle",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = false
                    },
                    label = { Text("Nome attività o evento *") },
                    placeholder = { Text("Es. Visita cardiologo, pillola pressione...") },
                    isError = titleError,
                    supportingText = {
                        if (titleError) {
                            Text("Inserisci un titolo per il promemoria", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("event_title_input"),
                    shape = RoundedCornerShape(16.dp),
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Description / Notes Input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Note o dettagli aggiuntivi (opzionale)") },
                    placeholder = { Text("Es. Prendere a stomaco pieno con acqua...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("event_desc_input"),
                    shape = RoundedCornerShape(16.dp),
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Date & Time Picker Area
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Date picker row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Data:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.weight(1f))

                            // Day selector
                            IconButton(onClick = {
                                val c = Calendar.getInstance().apply {
                                    set(selectedYear, selectedMonth, selectedDay)
                                    add(Calendar.DAY_OF_MONTH, -1)
                                }
                                selectedYear = c.get(Calendar.YEAR)
                                selectedMonth = c.get(Calendar.MONTH)
                                selectedDay = c.get(Calendar.DAY_OF_MONTH)
                            }) {
                                Icon(Icons.Default.Remove, contentDescription = "Giorno prima")
                            }

                            val formattedDate = remember(selectedYear, selectedMonth, selectedDay) {
                                val c = Calendar.getInstance().apply {
                                    set(selectedYear, selectedMonth, selectedDay)
                                }
                                SimpleDateFormat("d MMM yyyy", Locale.ITALY).format(c.time)
                            }
                            Text(
                                text = formattedDate,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 15.sp
                            )

                            IconButton(onClick = {
                                val c = Calendar.getInstance().apply {
                                    set(selectedYear, selectedMonth, selectedDay)
                                    add(Calendar.DAY_OF_MONTH, 1)
                                }
                                selectedYear = c.get(Calendar.YEAR)
                                selectedMonth = c.get(Calendar.MONTH)
                                selectedDay = c.get(Calendar.DAY_OF_MONTH)
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "Giorno dopo")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Time picker row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Orario:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.weight(1f))

                            // Hour Adjust
                            IconButton(onClick = {
                                selectedHour = if (selectedHour == 0) 23 else selectedHour - 1
                            }) {
                                Icon(Icons.Default.Remove, contentDescription = "Meno un'ora")
                            }

                            val timeText = String.format(Locale.ITALY, "%02d:%02d", selectedHour, selectedMinute)
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = timeText,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            IconButton(onClick = {
                                selectedHour = (selectedHour + 1) % 24
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "Più un'ora")
                            }

                            // Quick 15 min toggle
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedMinute = (selectedMinute + 15) % 60
                                    }
                            ) {
                                Text(
                                    text = "+15m",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Recurrence Selector (Giorno / Settimana / Mese / Anno)
                Text(
                    text = "Ripetizione evento:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RecurrenceType.values().forEach { rec ->
                        FilterChip(
                            selected = selectedRecurrence == rec,
                            onClick = { selectedRecurrence = rec },
                            label = { Text(rec.label, fontSize = 13.sp) },
                            leadingIcon = if (rec != RecurrenceType.NONE) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Repeat,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category Selector
                Text(
                    text = "Categoria:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EventCategory.values().forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.displayName, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = cat.getIcon(),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Reminder Minutes Before (Default 3 minutes before as requested by user)
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = Color(0xFF92400E),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Anticipo notifica vocale:",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF78350F),
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val reminderOptions = listOf(
                            3 to "3 min prima (Consigliato)",
                            0 to "All'ora esatta",
                            5 to "5 min prima",
                            10 to "10 min prima",
                            15 to "15 min prima",
                            30 to "30 min prima"
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            reminderOptions.forEach { (mins, label) ->
                                FilterChip(
                                    selected = reminderMinutes == mins,
                                    onClick = { reminderMinutes = mins },
                                    label = {
                                        Text(
                                            text = label,
                                            fontWeight = if (mins == 3) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AmberAccent,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Voice & Vibration Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Ripeti notifica a voce",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Il cellulare legge a voce alta il promemoria",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = speakAnnouncement,
                        onCheckedChange = { speakAnnouncement = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IndigoPrimary)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Vibra e suona",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Vibrazione energica e suoneria di allarme",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = vibrateAndRing,
                        onCheckedChange = { vibrateAndRing = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IndigoPrimary)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons: Cancel and Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Annulla", fontSize = 15.sp)
                    }

                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                titleError = true
                                return@Button
                            }

                            val finalCal = Calendar.getInstance().apply {
                                set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute, 0)
                                set(Calendar.MILLISECOND, 0)
                            }

                            val event = CalendarEvent(
                                id = initialEvent?.id ?: 0L,
                                title = title.trim(),
                                description = description.trim(),
                                startEpochMillis = finalCal.timeInMillis,
                                endEpochMillis = finalCal.timeInMillis + 30 * 60 * 1000L,
                                recurrence = selectedRecurrence,
                                reminderMinutesBefore = reminderMinutes,
                                speakAnnouncement = speakAnnouncement,
                                vibrateAndRing = vibrateAndRing,
                                category = selectedCategory,
                                googleEventId = initialEvent?.googleEventId
                            )
                            onSave(event)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("save_event_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                    ) {
                        Text("Salva Promemoria", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}
