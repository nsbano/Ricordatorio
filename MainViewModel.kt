package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MemoCalendarApp
import com.example.model.CalendarEvent
import com.example.receiver.ActiveAlarmAlert
import com.example.receiver.ReminderAlarmReceiver
import com.example.sync.GoogleCalendarAccount
import com.example.sync.GoogleCalendarSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MemoCalendarApp
    private val repository = app.repository
    val ttsManager = app.ttsManager
    private val syncManager = GoogleCalendarSyncManager(application, repository)

    val allEvents: StateFlow<List<CalendarEvent>> = repository.allEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDate = MutableStateFlow(Calendar.getInstance())
    val selectedDate: StateFlow<Calendar> = _selectedDate.asStateFlow()

    private val _isMonthView = MutableStateFlow(false)
    val isMonthView: StateFlow<Boolean> = _isMonthView.asStateFlow()

    private val _showAddEditDialog = MutableStateFlow(false)
    val showAddEditDialog: StateFlow<Boolean> = _showAddEditDialog.asStateFlow()

    private val _editingEvent = MutableStateFlow<CalendarEvent?>(null)
    val editingEvent: StateFlow<CalendarEvent?> = _editingEvent.asStateFlow()

    private val _showSyncDialog = MutableStateFlow(false)
    val showSyncDialog: StateFlow<Boolean> = _showSyncDialog.asStateFlow()

    private val _showVoiceSettingsDialog = MutableStateFlow(false)
    val showVoiceSettingsDialog: StateFlow<Boolean> = _showVoiceSettingsDialog.asStateFlow()

    private val _availableCalendars = MutableStateFlow<List<GoogleCalendarAccount>>(emptyList())
    val availableCalendars: StateFlow<List<GoogleCalendarAccount>> = _availableCalendars.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _activeAlert = MutableStateFlow<ActiveAlarmAlert?>(null)
    val activeAlert: StateFlow<ActiveAlarmAlert?> = _activeAlert.asStateFlow()

    val isSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking
    val speechRate: StateFlow<Float> = ttsManager.speechRate

    // Derived events for selected date (taking recurrence into account)
    val eventsForSelectedDate: StateFlow<List<CalendarEvent>> = combine(allEvents, _selectedDate) { events, selDate ->
        val year = selDate.get(Calendar.YEAR)
        val month = selDate.get(Calendar.MONTH)
        val day = selDate.get(Calendar.DAY_OF_MONTH)

        events.filter { it.occursOnDate(year, month, day) }
            .sortedBy { it.startEpochMillis }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Events for today
    val todayEvents: StateFlow<List<CalendarEvent>> = allEvents.combine(_selectedDate) { events, _ ->
        val today = Calendar.getInstance()
        val year = today.get(Calendar.YEAR)
        val month = today.get(Calendar.MONTH)
        val day = today.get(Calendar.DAY_OF_MONTH)

        events.filter { it.occursOnDate(year, month, day) }
            .sortedBy { it.startEpochMillis }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Seed initial events if database is newly installed
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }

        // Listen for active alarm alerts from receiver
        viewModelScope.launch {
            ReminderAlarmReceiver.activeAlertFlow.collect { alert ->
                _activeAlert.value = alert
            }
        }
    }

    fun selectDate(date: Calendar) {
        val newCal = Calendar.getInstance().apply {
            timeInMillis = date.timeInMillis
        }
        _selectedDate.value = newCal
    }

    fun toggleMonthView() {
        _isMonthView.value = !_isMonthView.value
    }

    fun setMonthView(isMonth: Boolean) {
        _isMonthView.value = isMonth
    }

    fun openAddDialog() {
        _editingEvent.value = null
        _showAddEditDialog.value = true
    }

    fun openEditDialog(event: CalendarEvent) {
        _editingEvent.value = event
        _showAddEditDialog.value = true
    }

    fun closeAddEditDialog() {
        _showAddEditDialog.value = false
        _editingEvent.value = null
    }

    fun saveEvent(event: CalendarEvent) {
        viewModelScope.launch {
            if (event.id == 0L) {
                repository.insertEvent(event)
                ttsManager.speak("Promemoria salvato con successo per ${event.title}.")
            } else {
                repository.updateEvent(event)
                ttsManager.speak("Promemoria aggiornato per ${event.title}.")
            }
            closeAddEditDialog()
        }
    }

    fun deleteEvent(event: CalendarEvent) {
        viewModelScope.launch {
            repository.deleteEvent(event)
            ttsManager.speak("Promemoria eliminato: ${event.title}.")
        }
    }

    fun speakEvent(event: CalendarEvent) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.ITALY)
        val timeStr = timeFormat.format(event.startEpochMillis)
        val descStr = if (event.description.isNotBlank()) "Nota: ${event.description}." else ""
        val recurrenceStr = event.recurrence.spokenDescription
        val reminderStr = "Notifica vocale impostata a ${event.reminderMinutesBefore} minuti prima."

        val message = "Attività: ${event.title}. Orario previsto: alle ore $timeStr. $descStr $recurrenceStr. $reminderStr"
        ttsManager.speak(message)
    }

    fun speakTodaySchedule() {
        val dateFormat = SimpleDateFormat("EEEE d MMMM", Locale.ITALY)
        val dateLabel = dateFormat.format(System.currentTimeMillis())
        ttsManager.speakDaySchedule(dateLabel, todayEvents.value)
    }

    fun speakSelectedDaySchedule() {
        val dateFormat = SimpleDateFormat("EEEE d MMMM", Locale.ITALY)
        val dateLabel = dateFormat.format(_selectedDate.value.timeInMillis)
        ttsManager.speakDaySchedule(dateLabel, eventsForSelectedDate.value)
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    fun testVoiceSpeech() {
        ttsManager.speak(
            "Prova di sintesi vocale riuscita! Il volume e la pronuncia sono attivi e chiari. Ti avviserò 3 minuti prima di ogni evento programmato.",
            flush = true
        )
    }

    fun setSpeechRate(rate: Float) {
        ttsManager.setSpeedRate(rate)
    }

    fun dismissActiveAlert() {
        _activeAlert.value = null
        ttsManager.stop()
    }

    fun repeatActiveAlertVoice() {
        val alert = _activeAlert.value ?: return
        val minutesNotice = if (alert.minutesBefore > 0) "tra ${alert.minutesBefore} minuti" else "adesso"
        val descNotice = if (alert.description.isNotBlank()) "Nota: ${alert.description}." else ""
        val text = "Attenzione! Ripeto il promemoria: ${alert.title}, $minutesNotice. $descNotice"
        ttsManager.speak(text)
    }

    // Google Calendar Sync
    fun openSyncDialog() {
        _showSyncDialog.value = true
        loadAvailableCalendars()
    }

    fun closeSyncDialog() {
        _showSyncDialog.value = false
        _syncMessage.value = null
    }

    fun loadAvailableCalendars() {
        viewModelScope.launch {
            val calendars = syncManager.getAvailableCalendars()
            _availableCalendars.value = calendars
        }
    }

    fun syncWithGoogle(calendarId: Long) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "Sincronizzazione in corso con Google Calendar..."
            val result = syncManager.syncWithGoogleCalendar(calendarId)
            _isSyncing.value = false

            if (result.errorMessage != null) {
                _syncMessage.value = "Errore: ${result.errorMessage}"
                ttsManager.speak("Errore durante la sincronizzazione con Google Calendar.")
            } else {
                val successMsg = "Sincronizzazione completata: ${result.importedCount} eventi importati, ${result.exportedCount} eventi esportati."
                _syncMessage.value = successMsg
                ttsManager.speak("Sincronizzazione con Google Calendar completata con successo.")
            }
        }
    }

    fun openVoiceSettings() {
        _showVoiceSettingsDialog.value = true
    }

    fun closeVoiceSettings() {
        _showVoiceSettingsDialog.value = false
    }
}
