package com.example.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.model.CalendarEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Locale

class TextToSpeechManager private constructor(context: Context) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _speechRate = MutableStateFlow(0.88f) // Calm, highly intelligible cadence
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    init {
        tts = TextToSpeech(appContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.ITALIAN)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("TTSManager", "Italian TTS not fully supported, falling back to default locale")
                tts?.language = Locale.getDefault()
            }
            tts?.setSpeechRate(_speechRate.value)
            tts?.setPitch(1.0f)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                }
            })
            isInitialized = true
        } else {
            Log.e("TTSManager", "TTS initialization failed with status $status")
        }
    }

    fun setSpeedRate(rate: Float) {
        val safeRate = rate.coerceIn(0.5f, 1.5f)
        _speechRate.value = safeRate
        tts?.setSpeechRate(safeRate)
    }

    fun speak(text: String, flush: Boolean = true, onComplete: (() -> Unit)? = null) {
        if (!isInitialized || tts == null) {
            Log.w("TTSManager", "TTS not initialized yet")
            return
        }

        val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        val utteranceId = "memo_tts_${System.currentTimeMillis()}"

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        if (onComplete != null) {
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(id: String?) {
                    _isSpeaking.value = false
                    if (id == utteranceId) {
                        onComplete()
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(id: String?) {
                    _isSpeaking.value = false
                }
            })
        }

        _isSpeaking.value = true
        tts?.speak(text, queueMode, params, utteranceId)
    }

    /**
     * Speaks the 3-minute (or customized) event reminder message clearly and repeats it as requested.
     */
    fun speakEventReminder(event: CalendarEvent, minutesRemaining: Int = event.reminderMinutesBefore) {
        val textToSpeak = event.buildSpokenReminder(minutesRemaining)
        speak(textToSpeak, flush = true)
    }

    /**
     * Reads out loud today's agenda sequentially with friendly, clear Italian wording.
     */
    fun speakDaySchedule(dateLabel: String, events: List<CalendarEvent>) {
        if (events.isEmpty()) {
            speak("Oggi, $dateLabel, non ci sono eventi né promemoria in programma. Sei completamente libero.", flush = true)
            return
        }

        val timeFormat = SimpleDateFormat("HH:mm", Locale.ITALY)
        val sb = StringBuilder()
        sb.append("Ecco il tuo programma per $dateLabel. ")
        sb.append("Hai in programma ${events.size} attività. ")

        events.sortedBy { it.startEpochMillis }.forEachIndexed { index, event ->
            val timeStr = timeFormat.format(event.startEpochMillis)
            sb.append("Attività numero ${index + 1}: alle ore $timeStr, ${event.title}. ")
            if (event.description.isNotBlank()) {
                sb.append("Nota: ${event.description}. ")
            }
        }
        sb.append("Ti avviserò a voce prima di ogni appuntamento.")

        speak(sb.toString(), flush = true)
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    companion object {
        @Volatile
        private var INSTANCE: TextToSpeechManager? = null

        fun getInstance(context: Context): TextToSpeechManager {
            return INSTANCE ?: synchronized(this) {
                val instance = TextToSpeechManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
