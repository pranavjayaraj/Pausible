package com.reset.feature.sessions

import androidx.lifecycle.SavedStateHandle
import com.reset.core.mvi.BaseViewModel
import com.reset.feature.home.api.HomeDestination
import com.reset.feature.sessions.api.SessionDestination
import com.reset.feature.sessions.navigation.SessionsIntent
import com.reset.feature.sessions.navigation.SessionsSideEffect
import com.reset.navigation.Navigator
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.syntax.simple.intent
import javax.inject.Inject

@HiltViewModel
class SessionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigator: Navigator,
) : BaseViewModel<SessionsState, SessionsSideEffect>(savedStateHandle) {

    override fun initialState() = SessionsState.getDefault()

    fun handleSessionsIntent(intent: SessionsIntent) = when (intent) {
        is SessionsIntent.PickScript -> pickScript(intent.scriptId)
        SessionsIntent.SkipBreak -> skipBreak()
        SessionsIntent.HandleBackPress -> close()
    }

    /** Hands the picked script to the full-screen session experience. */
    private fun pickScript(scriptId: String) = intent {
        navigator.navigate(
            SessionDestination(mode = SessionDestination.MODE_BREAK, scriptId = scriptId),
        )
    }

    /** "SKIP FOR NOW" returns to the Home tab, like the design. */
    private fun skipBreak() = intent {
        navigator.switchTab(HomeDestination)
    }

    /** Back on a non-start tab returns to Home (the start destination), per dashboard
     *  semantics — the host pops the stack that SwitchTab left one level deep. */
    private fun close() = intent {
        navigator.pop()
    }
}
