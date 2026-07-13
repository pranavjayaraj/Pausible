package com.reset.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reset.feature.home.api.HomeDestination
import com.reset.feature.sessions.api.SessionDestination
import com.reset.model.domain.preferences.PreferencesRepository
import com.reset.navigation.Navigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * App-level ViewModel for [MainActivity]'s launch actions. Owns the data reads the host
 * needs (reminder duration preference) so the Activity stays a pure intent-parsing shell;
 * navigation still flows through the injected [Navigator] seam.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val navigator: Navigator,
) : ViewModel() {

    /**
     * Handles the reminder notification's "Reset" action: lands on the Home tab, then
     * starts a focus session with the user's saved duration preference.
     */
    fun startReminderReset() {
        viewModelScope.launch {
            // Guarantee the underlying tab is Home before pushing a full screen on top
            navigator.switchTab(HomeDestination)
            val prefs = preferencesRepository.preferences.first()
            navigator.navigate(
                SessionDestination(
                    mode = SessionDestination.MODE_FOCUS,
                    durationMin = prefs.durationMin,
                )
            )
        }
    }
}
