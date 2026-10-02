package com.example.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.net.Entry
import com.example.net.Result
import com.example.repo.DictionaryRepository
import com.example.store.HistoryEntry
import com.example.store.PrefsStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val entries: List<Entry>) : SearchUiState
    data object Empty : SearchUiState
    data object Offline : SearchUiState
    data class Error(val message: String) : SearchUiState
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val repository: DictionaryRepository = DictionaryRepository.instance,
    private val prefs: PrefsStore = PrefsStore.instance
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val history: StateFlow<List<HistoryEntry>> = prefs.historyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            _query
                .debounce(200)
                .distinctUntilChanged()
                .flatMapLatest { q ->
                    val trimmed = q.trim()
                    if (trimmed.length < 2) {
                        flowOf(SearchUiState.Idle)
                    } else {
                        _uiState.value = SearchUiState.Loading
                        prefs.setLastSearchQuery(trimmed)
                        val res = repository.search(trimmed)
                        flowOf(mapResultToState(res))
                    }
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
    }

    fun appendChar(char: String) {
        _query.value += char
    }

    fun clearQuery() {
        _query.value = ""
        _uiState.value = SearchUiState.Idle
    }

    fun retrySearch() {
        val q = _query.value.trim()
        if (q.length >= 2) {
            viewModelScope.launch {
                _uiState.value = SearchUiState.Loading
                val res = repository.search(q)
                _uiState.value = mapResultToState(res)
            }
        }
    }

    private fun mapResultToState(result: Result<List<Entry>>): SearchUiState {
        return when (result) {
            is Result.Ok -> {
                if (result.data.isEmpty()) SearchUiState.Empty else SearchUiState.Success(result.data)
            }
            is Result.Empty -> SearchUiState.Empty
            is Result.Offline -> SearchUiState.Offline
            is Result.Http -> SearchUiState.Error("Gabim serveri (${result.code})")
            is Result.Err -> SearchUiState.Error(result.msg)
        }
    }
}
