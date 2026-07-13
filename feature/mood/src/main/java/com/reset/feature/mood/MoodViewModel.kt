package com.reset.feature.mood

import androidx.lifecycle.SavedStateHandle
import com.reset.core.mvi.BaseViewModel
import com.reset.feature.mood.navigation.MoodIntent
import com.reset.feature.mood.navigation.MoodSideEffect
import com.reset.model.domain.mood.MoodRepository
import com.reset.model.domain.model.ChimeKind
import com.reset.navigation.Navigator
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import javax.inject.Inject

@HiltViewModel
class MoodViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val moodRepository: MoodRepository,
    private val navigator: Navigator,
) : BaseViewModel<MoodState, MoodSideEffect>(savedStateHandle) {

    override fun initialState() = MoodState.getDefault()

    fun handleMoodIntent(intent: MoodIntent) = when (intent) {
        is MoodIntent.LevelChanged -> setLevel(intent.level)
        MoodIntent.SaveMood -> saveMood()
        MoodIntent.Dismiss -> dismiss()
    }

    /** Slider drag: the only in-place state mutation. */
    private fun setLevel(level: Int) = intent {
        val clamped = level.coerceIn(MoodConstants.LEVEL_MIN, MoodConstants.LEVEL_MAX)
        if (clamped != state.level) reduce { state.copy(level = clamped) }
    }

    /** Persists the reported mood, chimes, then pops back to whatever launched the log. */
    private fun saveMood() = intent {
        moodRepository.recordMood(state.level)
        postSideEffect(MoodSideEffect.PlayChime(ChimeKind.End))
        navigator.pop()
    }

    /** Skip / close / back — leave without recording anything. */
    private fun dismiss() = intent {
        navigator.pop()
    }
}
