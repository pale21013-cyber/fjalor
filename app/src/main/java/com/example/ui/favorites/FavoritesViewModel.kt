package com.example.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.store.FavoriteEntry
import com.example.store.HistoryEntry
import com.example.store.PrefsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val prefs: PrefsStore = PrefsStore.instance
) : ViewModel() {

    val favorites: StateFlow<List<FavoriteEntry>> = prefs.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryEntry>> = prefs.historyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeFavorite(slug: String) {
        viewModelScope.launch {
            prefs.removeFavorite(slug)
        }
    }

    fun removeHistoryItem(slug: String) {
        viewModelScope.launch {
            prefs.removeHistoryItem(slug)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            prefs.clearHistory()
        }
    }
}
