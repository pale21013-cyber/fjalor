package com.example.ui.wotd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.net.Entry
import com.example.net.Result
import com.example.repo.DictionaryRepository
import com.example.store.PrefsStore
import com.example.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface WotdUiState {
    data object Loading : WotdUiState
    data class Success(val entry: Entry) : WotdUiState
    data object Offline : WotdUiState
    data class Error(val message: String) : WotdUiState
}

class WotdViewModel(
    private val repository: DictionaryRepository = DictionaryRepository.instance,
    private val prefs: PrefsStore = PrefsStore.instance
) : ViewModel() {

    private val _uiState = MutableStateFlow<WotdUiState>(WotdUiState.Loading)
    val uiState: StateFlow<WotdUiState> = _uiState.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    val formattedDate: String = DateUtils.formatAlbanianLocalDate()

    init {
        loadWordOfTheDay()
    }

    fun loadWordOfTheDay() {
        viewModelScope.launch {
            _uiState.value = WotdUiState.Loading
            val result = repository.getWordOfTheDay()
            when (result) {
                is Result.Ok -> {
                    val entry = result.data
                    _uiState.value = WotdUiState.Success(entry)
                    // Check if favorite using first()
                    try {
                        val favs = prefs.favoritesFlow.first()
                        _isFavorite.value = favs.any { it.slug == entry.slug }
                    } catch (e: Exception) {
                        _isFavorite.value = false
                    }
                }
                is Result.Offline -> _uiState.value = WotdUiState.Offline
                is Result.Empty -> _uiState.value = WotdUiState.Error("Nuk u gjet fjala e ditës për momentin")
                is Result.Http -> _uiState.value = WotdUiState.Error("Gabim serveri (${result.code})")
                is Result.Err -> _uiState.value = WotdUiState.Error(result.msg)
            }
        }
    }

    fun toggleFavorite(slug: String, term: String) {
        viewModelScope.launch {
            val isNowFav = prefs.toggleFavorite(slug, term)
            _isFavorite.value = isNowFav
        }
    }
}
