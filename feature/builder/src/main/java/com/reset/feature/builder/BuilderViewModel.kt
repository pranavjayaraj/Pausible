package com.reset.feature.builder

import androidx.lifecycle.SavedStateHandle
import com.reset.core.mvi.BaseViewModel
import com.reset.feature.builder.navigation.BuilderIntent
import com.reset.feature.builder.navigation.BuilderSideEffect
import com.reset.feature.sessions.api.SessionDestination
import com.reset.model.domain.preferences.PreferencesRepository
import com.reset.model.domain.presets.PresetsRepository
import com.reset.model.domain.TimeProvider
import com.reset.model.domain.model.SessionPreset
import com.reset.navigation.Navigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.reduce
import javax.inject.Inject

@HiltViewModel
class BuilderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val preferencesRepository: PreferencesRepository,
    private val presetsRepository: PresetsRepository,
    private val navigator: Navigator,
    private val timeProvider: TimeProvider,
) : BaseViewModel<BuilderState, BuilderSideEffect>(savedStateHandle) {

    override fun initialState() = BuilderState.getDefault()

    override fun initData() {
        intent {
            presetsRepository.presets.collectLatest { presets ->
                reduce { state.copy(presets = presets) }
            }
        }
    }

    fun handleIntent(intent: BuilderIntent) = when (intent) {
        is BuilderIntent.SelectIntentTag -> reduceBuilder { copy(intentKey = intent.key) }
        is BuilderIntent.SelectDuration -> reduceBuilder { copy(durationMin = intent.minutes) }
        is BuilderIntent.ToggleSound -> toggleSound(intent.key)
        is BuilderIntent.ChangeMix -> reduceBuilder {
            copy(mixPercent = intent.percent.coerceIn(0, BuilderConstants.MIX_MAX_PERCENT))
        }
        is BuilderIntent.SelectPace -> reduceBuilder { copy(paceSec = intent.seconds) }
        is BuilderIntent.SelectBell -> reduceBuilder { copy(bellMin = intent.minutes) }
        BuilderIntent.ToggleGuidedVoice -> reduceBuilder { copy(guidedVoice = !guidedVoice) }
        BuilderIntent.ToggleWarmup -> reduceBuilder { copy(warmup = !warmup) }
        BuilderIntent.ToggleGong -> reduceBuilder { copy(gong = !gong) }
        is BuilderIntent.SelectReminder -> selectReminder(intent.key)
        is BuilderIntent.SetSessionName -> reduceBuilder {
            copy(sessionName = intent.name.take(BuilderConstants.SESSION_NAME_MAX_CHARS))
        }
        BuilderIntent.SavePreset -> savePreset()
        is BuilderIntent.ApplyPreset -> applyPreset(intent.name)
        BuilderIntent.ApplySuggestion -> applySuggestion()
        BuilderIntent.BeginCustomSession -> beginCustomSession()
        BuilderIntent.HandleBackPress -> intent { navigator.pop() }
    }

    private fun reduceBuilder(transform: BuilderState.() -> BuilderState) = intent {
        reduce { state.transform() }
    }

    private fun toggleSound(key: String) = reduceBuilder {
        when {
            key == soundKey -> copy(soundKey = layerSoundKey ?: BuilderConstants.SOUND_SILENCE, layerSoundKey = null)
            key == layerSoundKey -> copy(layerSoundKey = null)
            key == BuilderConstants.SOUND_SILENCE -> copy(soundKey = key, layerSoundKey = null)
            soundKey == BuilderConstants.SOUND_SILENCE -> copy(soundKey = key)
            else -> copy(layerSoundKey = key)
        }
    }

    private fun selectReminder(key: String) = intent {
        reduce { state.copy(remindKey = key) }
        preferencesRepository.setRemindersEnabled(key != BuilderConstants.REMIND_OFF)
    }

    private fun savePreset() = intent {
        presetsRepository.savePreset(state.toPreset())
    }

    private fun applyPreset(name: String) = intent {
        val preset = state.presets.firstOrNull { it.name == name } ?: return@intent
        reduce {
            state.copy(
                intentKey = preset.intentKey,
                durationMin = preset.durationMin,
                soundKey = preset.soundKey,
                layerSoundKey = preset.layerSoundKey,
                mixPercent = preset.mixPercent,
                paceSec = preset.paceSec,
                bellMin = preset.bellMin,
                guidedVoice = preset.guidedVoice,
                warmup = preset.warmup,
                gong = preset.gong,
                remindKey = preset.remindKey,
                sessionName = preset.name,
            )
        }
    }

    private fun applySuggestion() = intent {
        val suggestion = currentSuggestion()
        reduce {
            state.copy(
                intentKey = suggestion.intentKey,
                durationMin = suggestion.durationMin,
                soundKey = suggestion.soundKey,
                layerSoundKey = null,
                paceSec = suggestion.paceSec,
            )
        }
    }

    private fun beginCustomSession() = intent {
        navigator.navigate(
            SessionDestination(
                mode = SessionDestination.MODE_FOCUS,
                durationMin = state.durationMin,
                paceSec = state.paceSec,
                soundKey = state.soundKey,
                warmup = state.warmup,
                gong = state.gong,
            ),
        )
    }

    fun currentSuggestion(): BuilderSuggestion {
        val hour = timeProvider.hourOfDay()
        return when {
            hour < BuilderConstants.SUGGEST_MORNING_UNTIL -> BuilderSuggestion(
                BuilderConstants.INTENT_ENERGY, 10, BuilderConstants.SOUND_FOREST, BuilderConstants.PACE_QUICK_SEC,
            )
            hour < BuilderConstants.SUGGEST_AFTERNOON_UNTIL -> BuilderSuggestion(
                BuilderConstants.INTENT_FOCUS, 20, BuilderConstants.SOUND_RAIN, BuilderConstants.PACE_MEDIUM_SEC,
            )
            hour < BuilderConstants.SUGGEST_EVENING_UNTIL -> BuilderSuggestion(
                BuilderConstants.INTENT_UNWIND, 15, BuilderConstants.SOUND_OCEAN, BuilderConstants.PACE_SLOW_SEC,
            )
            else -> BuilderSuggestion(
                BuilderConstants.INTENT_SLEEP, 20, BuilderConstants.SOUND_SILENCE, BuilderConstants.PACE_SLOW_SEC,
            )
        }
    }
}

private fun BuilderState.toPreset() = SessionPreset(
    name = sessionName.trim().ifEmpty { "$soundKey · $durationMin min" },
    durationMin = durationMin,
    soundKey = soundKey,
    layerSoundKey = layerSoundKey,
    mixPercent = mixPercent,
    paceSec = paceSec,
    bellMin = bellMin,
    gong = gong,
    guidedVoice = guidedVoice,
    warmup = warmup,
    intentKey = intentKey,
    remindKey = remindKey,
)
