package com.reset.feature.checkin

import com.reset.model.domain.breakprefs.BreakPreferences
import com.reset.model.domain.breakprefs.BreakPreferencesRepository
import com.reset.model.domain.checkin.NeedState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeBreakPreferencesRepository(
    preferences: BreakPreferences = BreakPreferences(),
) : BreakPreferencesRepository {

    val preferencesFlow = MutableStateFlow(preferences)
    override val preferences: Flow<BreakPreferences> = preferencesFlow

    val recordedEchoes = mutableListOf<NeedState>()

    override suspend fun setAudioAvailable(available: Boolean) {
        preferencesFlow.value = preferencesFlow.value.copy(audioAvailable = available)
    }

    override suspend fun setStairsAvailable(available: Boolean) {
        preferencesFlow.value = preferencesFlow.value.copy(stairsAvailable = available)
    }

    override suspend fun setMoveSpaceAvailable(available: Boolean) {
        preferencesFlow.value = preferencesFlow.value.copy(moveSpaceAvailable = available)
    }

    override suspend fun setWaterAccessAvailable(available: Boolean) {
        preferencesFlow.value = preferencesFlow.value.copy(waterAccessAvailable = available)
    }

    override suspend fun recordNeedStateEcho(needState: NeedState) {
        recordedEchoes += needState
    }
}
