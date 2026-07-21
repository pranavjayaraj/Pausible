package com.reset.feature.checkin

import com.reset.model.domain.closeperson.ClosePerson
import com.reset.model.domain.closeperson.ClosePersonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeClosePersonRepository(
    closePerson: ClosePerson? = null,
) : ClosePersonRepository {

    val closePersonFlow = MutableStateFlow(closePerson)
    override val closePerson: Flow<ClosePerson?> = closePersonFlow

    override suspend fun setClosePerson(closePerson: ClosePerson) {
        closePersonFlow.value = closePerson
    }

    override suspend fun clearClosePerson() {
        closePersonFlow.value = null
    }
}
