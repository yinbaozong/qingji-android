package com.dreamjournal.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dreamjournal.app.data.local.DreamEntryEntity
import com.dreamjournal.app.data.repository.AudioRecorderManager
import com.dreamjournal.app.data.repository.DreamRepository
import com.dreamjournal.app.data.repository.WeatherRepository
import com.dreamjournal.app.domain.ai.AiService
import com.dreamjournal.app.domain.model.RecordType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalTime

data class HomeUiState(
    val entries: List<DreamEntryEntity> = emptyList(),
    val isRecording: Boolean = false,
    val isTranscribing: Boolean = false,
    val recordingSeconds: Int = 0,
    val waveformLevels: List<Float> = List(18) { 0.08f },
    val activeRecordType: RecordType = RecordType.DREAM,
    val weatherText: String? = null,
    val locationText: String? = null,
    val isLoadingWeather: Boolean = true,
    val errorMessage: String? = null
)

class HomeViewModel(
    private val dreamRepository: DreamRepository,
    private val audioRecorderManager: AudioRecorderManager,
    private val aiService: AiService,
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private val _newEntryEvent = MutableSharedFlow<Long>()
    val newEntryEvent = _newEntryEvent.asSharedFlow()
    private var amplitudeJob: Job? = null

    private val entriesFlow = dreamRepository.observeEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            entriesFlow.collect { entries ->
                _uiState.update { it.copy(entries = entries) }
            }
        }
        refreshWeather()
    }

    fun startRecording(recordType: RecordType) {
        val result = audioRecorderManager.start()
        result.onSuccess {
            _uiState.update {
                it.copy(
                    isRecording = true,
                    recordingSeconds = 0,
                    waveformLevels = List(18) { 0.08f },
                    activeRecordType = recordType,
                    errorMessage = null
                )
            }
            startAmplitudeMonitoring()
        }.onFailure { error ->
            _uiState.update { it.copy(errorMessage = error.message ?: "启动录音失败") }
        }
    }

    fun stopRecordingAndCreateEntry() {
        amplitudeJob?.cancel()
        amplitudeJob = null
        _uiState.update {
                it.copy(
                    isRecording = false,
                    recordingSeconds = 0,
                    waveformLevels = List(18) { 0.08f }
                )
        }

        audioRecorderManager.stop()
            .onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "停止录音失败") }
            }
            .onSuccess { path ->
                viewModelScope.launch {
                    val entryId = dreamRepository.createEntry(
                        path,
                        uiState.value.activeRecordType,
                        uiState.value.weatherText,
                        uiState.value.locationText
                    )
                    _newEntryEvent.emit(entryId)
                    enrichWeatherIfNeeded(entryId)
                    if (!path.isNullOrBlank()) {
                        transcribeAudio(entryId, path)
                    }
                }
            }
    }

    fun createEntryFromSystemRecognition(text: String, recordType: RecordType) {
        viewModelScope.launch {
            val cleaned = text.trim()
            if (cleaned.isBlank()) {
                _uiState.update { it.copy(errorMessage = "未识别到有效语音内容") }
                return@launch
            }
            val entryId = dreamRepository.createEntryWithContent(
                cleaned,
                recordType,
                uiState.value.weatherText,
                uiState.value.locationText
            )
            _newEntryEvent.emit(entryId)
            enrichWeatherIfNeeded(entryId)
        }
    }

    fun createTextEntry() {
        viewModelScope.launch {
            val recordType = if (LocalTime.now().hour in 6..17) RecordType.DAY else RecordType.DREAM
            val entryId = dreamRepository.createEntry(
                null,
                recordType,
                uiState.value.weatherText,
                uiState.value.locationText
            )
            _newEntryEvent.emit(entryId)
            enrichWeatherIfNeeded(entryId)
        }
    }

    fun refreshWeather() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingWeather = true) }
            weatherRepository.currentWeather()
                .onSuccess { snapshot ->
                    _uiState.update {
                        it.copy(
                            weatherText = snapshot.weatherText,
                            locationText = snapshot.locationText,
                            isLoadingWeather = false
                        )
                    }
                }
                .onFailure { _uiState.update { it.copy(isLoadingWeather = false) } }
        }
    }

    private fun enrichWeatherIfNeeded(entryId: Long) {
        if (!uiState.value.weatherText.isNullOrBlank()) return
        viewModelScope.launch {
            weatherRepository.currentWeather().onSuccess { snapshot ->
                _uiState.update {
                    it.copy(
                        weatherText = snapshot.weatherText,
                        locationText = snapshot.locationText,
                        isLoadingWeather = false
                    )
                }
                dreamRepository.attachContext(entryId, snapshot.weatherText, snapshot.locationText)
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun showRecordingMode(recordType: RecordType) {
        _uiState.update { it.copy(activeRecordType = recordType) }
    }

    fun showError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    private suspend fun transcribeAudio(entryId: Long, path: String) {
        _uiState.update { it.copy(isTranscribing = true) }

        aiService.transcribe(File(path))
            .onSuccess { transcript ->
                if (transcript.isNotBlank()) {
                    dreamRepository.attachTranscript(entryId, transcript)
                } else {
                    _uiState.update {
                        it.copy(errorMessage = "当前为演示模式，未启用真实语音转写。可在设置中配置 API。")
                    }
                }
            }
            .onFailure { error ->
                val detail = buildString {
                    append(error.message ?: error.toString())
                    error.cause?.message?.takeIf { it.isNotBlank() }?.let {
                        append("；原因：")
                        append(it)
                    }
                }
                _uiState.update {
                    it.copy(errorMessage = "语音转文字失败：$detail")
                }
            }

        _uiState.update { it.copy(isTranscribing = false) }
    }

    override fun onCleared() {
        super.onCleared()
        amplitudeJob?.cancel()
        audioRecorderManager.cancel()
    }

    private fun startAmplitudeMonitoring() {
        amplitudeJob?.cancel()
        amplitudeJob = viewModelScope.launch {
            while (uiState.value.isRecording) {
                val amplitude = audioRecorderManager.currentAmplitude()
                val normalized = ((amplitude.coerceAtLeast(0).toFloat() / 32767f).coerceIn(0f, 1f))
                val shaped = (0.12f + normalized * 0.88f).coerceIn(0.08f, 1f)
                _uiState.update { current ->
                    current.copy(
                        recordingSeconds = audioRecorderManager.currentDurationSeconds(),
                        waveformLevels = (current.waveformLevels + shaped).takeLast(18)
                    )
                }
                delay(120)
            }
        }
    }

    companion object {
        fun factory(
            dreamRepository: DreamRepository,
            audioRecorderManager: AudioRecorderManager,
            aiService: AiService,
            weatherRepository: WeatherRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(
                        dreamRepository = dreamRepository,
                        audioRecorderManager = audioRecorderManager,
                        aiService = aiService,
                        weatherRepository = weatherRepository
                    ) as T
                }
            }
        }
    }
}
