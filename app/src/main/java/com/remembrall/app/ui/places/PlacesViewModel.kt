package com.remembrall.app.ui.places

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.remembrall.app.data.model.Place
import com.remembrall.app.data.repo.ReminderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlacesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ReminderRepository(app)

    val places: StateFlow<List<Place>> =
        repo.places.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(place: Place) {
        viewModelScope.launch { repo.deletePlace(place) }
    }
}
