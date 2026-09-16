package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SettingsVoice
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalendarEvent
import com.example.ui.components.AddEditEventDialog
import com.example.ui.components.CalendarMonthView
import com.example.ui.components.EventCard
import com.example.ui.components.GoogleCalendarSyncDialog
import com.example.ui.components.TodayHeroCard
import com.example.ui.components.VoiceAlarmPopup
import com.example.ui.components.VoiceSettingsDialog
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.IndigoPrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val allEvents by viewModel.allEvents.collectAsState()
    val todayEvents by viewModel.todayEvents.collectAsState()
    val eventsForSelectedDate by viewModel.eventsForSelectedDate.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val isMonthView by viewModel.isMonthView.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val speechRate by viewModel.speechRate.collectAsState()

    val showAddEditDialog by viewModel.showAddEditDialog.collectAsState()
    val editingEvent by viewModel.editingEvent.collectAsState()
    val showSyncDialog by viewModel.showSyncDialog.collectAsState()
    val showVoiceSettingsDialog by viewModel.showVoiceSettingsDialog.collectAsState()
    val availableCalendars by viewModel.availableCalendars.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val activeAlert by viewModel.activeAlert.collectAsState()

    // Request notification permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* result handled */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val selectedDateFormatted = remember(selectedDate) {
        val format = SimpleDateFormat("EEEE d MMMM", Locale.ITALY)
        format.format(selectedDate.timeInMillis).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ITALY) else it.toString()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = AmberWarm,
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = Color(0xFF451A03),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Promemoria Vocale",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    // Google Calendar Sync Button
                    IconButton(
                        onClick = { viewModel.openSyncDialog() },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("open_sync_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Sincronizza Google Calendar",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Voice settings button
                    IconButton(
                        onClick = { viewModel.openVoiceSettings() },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("voice_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SettingsVoice,
                            contentDescription = "Impostazioni voce e sintesi vocale",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openAddDialog() },
                containerColor = IndigoPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("add_event_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Nuovo Promemoria",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Card: Today's Summary & Spoken Action
            item {
                TodayHeroCard(
                    todayEvents = todayEvents,
                    isSpeaking = isSpeaking,
                    onSpeakToday = { viewModel.speakTodaySchedule() },
                    onStopSpeaking = { viewModel.stopSpeaking() },
                    onTestVoice = { viewModel.testVoiceSpeech() }
                )
            }

            // 2. View Toggle: "Agenda / Lista" vs "Calendario Mese"
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Visualizzazione:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !isMonthView,
                            onClick = { viewModel.setMonthView(false) },
                            label = { Text("Agenda") },
                            leadingIcon = {
                                Icon(Icons.Default.ViewAgenda, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )

                        FilterChip(
                            selected = isMonthView,
                            onClick = { viewModel.setMonthView(true) },
                            label = { Text("Mese") },
                            leadingIcon = {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // 3. Calendar Month Grid (if Month View is active)
            if (isMonthView) {
                item {
                    CalendarMonthView(
                        selectedDate = selectedDate,
                        events = allEvents,
                        onDateSelected = { newDate ->
                            viewModel.selectDate(newDate)
                        }
                    )
                }
            }

            // 4. Section Header for Selected Day's Agenda
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = selectedDateFormatted,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (eventsForSelectedDate.isEmpty()) "Nessun impegno" else "${eventsForSelectedDate.size} attività",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (eventsForSelectedDate.isNotEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { viewModel.speakSelectedDaySchedule() }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Ascolta giornata",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Ascolta",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Events List for Selected Day
            if (eventsForSelectedDate.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Nessuna attività in questa data",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tocca '+ Nuovo Promemoria' in basso per aggiungere una medicina, una visita o un impegno.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            } else {
                items(eventsForSelectedDate, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        onSpeak = { viewModel.speakEvent(event) },
                        onEdit = { viewModel.openEditDialog(event) },
                        onDelete = { viewModel.deleteEvent(event) }
                    )
                }
            }
        }
    }

    // Modal Dialogs
    if (showAddEditDialog) {
        AddEditEventDialog(
            initialEvent = editingEvent,
            initialDate = selectedDate,
            onDismiss = { viewModel.closeAddEditDialog() },
            onSave = { event -> viewModel.saveEvent(event) }
        )
    }

    if (showSyncDialog) {
        GoogleCalendarSyncDialog(
            availableCalendars = availableCalendars,
            isSyncing = isSyncing,
            syncMessage = syncMessage,
            onDismiss = { viewModel.closeSyncDialog() },
            onLoadCalendars = { viewModel.loadAvailableCalendars() },
            onStartSync = { calId -> viewModel.syncWithGoogle(calId) }
        )
    }

    if (showVoiceSettingsDialog) {
        VoiceSettingsDialog(
            currentRate = speechRate,
            onRateChange = { viewModel.setSpeechRate(it) },
            onTestVoice = { viewModel.testVoiceSpeech() },
            onDismiss = { viewModel.closeVoiceSettings() }
        )
    }

    // Active Spoken Alarm Popup (when triggered 3 min before event)
    activeAlert?.let { alert ->
        VoiceAlarmPopup(
            alert = alert,
            onRepeatVoice = { viewModel.repeatActiveAlertVoice() },
            onDismiss = { viewModel.dismissActiveAlert() }
        )
    }
}
