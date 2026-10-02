package com.example.ui.word

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.net.Entry
import com.example.net.Result
import com.example.repo.DictionaryRepository
import com.example.store.PrefsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface WordUiState {
    data object Loading : WordUiState
    data class Success(val entries: List<Entry>) : WordUiState
    data object Empty : WordUiState
    data object Offline : WordUiState
    data class Error(val message: String) : WordUiState
}

class WordViewModel(
    private val repository: DictionaryRepository = DictionaryRepository.instance,
    private val prefs: PrefsStore = PrefsStore.instance
) : ViewModel() {

    private val _uiState = MutableStateFlow<WordUiState>(WordUiState.Loading)
    val uiState: StateFlow<WordUiState> = _uiState.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    val crossRefEnabled: StateFlow<Boolean> = prefs.crossRefFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _slugsSet = MutableStateFlow<Set<String>>(emptySet())
    val slugsSet: StateFlow<Set<String>> = _slugsSet.asStateFlow()

    private var currentSlug: String = ""

    fun loadWord(slug: String) {
        currentSlug = slug
        viewModelScope.launch {
            _uiState.value = WordUiState.Loading

            // Check favorite status using first()
            try {
                val favs = prefs.favoritesFlow.first()
                _isFavorite.value = favs.any { it.slug.equals(slug, ignoreCase = true) }
            } catch (e: Exception) {
                _isFavorite.value = false
            }

            val result = repository.getWord(slug)
            when (result) {
                is Result.Ok -> {
                    if (result.data.isEmpty()) {
                        _uiState.value = WordUiState.Empty
                    } else {
                        _uiState.value = WordUiState.Success(result.data)
                    }
                }
                is Result.Empty -> _uiState.value = WordUiState.Empty
                is Result.Offline -> _uiState.value = WordUiState.Offline
                is Result.Http -> _uiState.value = WordUiState.Error("Gabim në server (${result.code})")
                is Result.Err -> _uiState.value = WordUiState.Error(result.msg)
            }

            // Lazy fetch slugs in background for cross-reference if needed
            launch {
                val set = repository.getSlugsSet()
                _slugsSet.value = set
            }
        }
    }

    fun toggleFavorite(term: String) {
        viewModelScope.launch {
            val isNowFav = prefs.toggleFavorite(currentSlug, term)
            _isFavorite.value = isNowFav
        }
    }

    fun toggleCrossRef() {
        viewModelScope.launch {
            val current = crossRefEnabled.value
            prefs.setCrossRefEnabled(!current)
            if (!current && _slugsSet.value.isEmpty()) {
                val set = repository.getSlugsSet()
                _slugsSet.value = set
            }
        }
    }
}
