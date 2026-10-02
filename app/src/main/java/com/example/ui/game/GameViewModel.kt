package com.example.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.net.Entry
import com.example.net.Result
import com.example.repo.DictionaryRepository
import com.example.util.Slug
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface GameState {
    data object Playing : GameState
    data object Correct : GameState
    data object Revealed : GameState
}

class GameViewModel(
    private val repository: DictionaryRepository = DictionaryRepository.instance
) : ViewModel() {

    private val _wordsList = mutableListOf<Entry>()
    private var currentIndex = 0

    private val _currentEntry = MutableStateFlow<Entry?>(null)
    val currentEntry: StateFlow<Entry?> = _currentEntry.asStateFlow()

    private val _userGuess = MutableStateFlow("")
    val userGuess: StateFlow<String> = _userGuess.asStateFlow()

    private val _wrongAttempts = MutableStateFlow(0)
    val wrongAttempts: StateFlow<Int> = _wrongAttempts.asStateFlow()

    private val _gameState = MutableStateFlow<GameState>(GameState.Playing)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _totalRounds = MutableStateFlow(0)
    val totalRounds: StateFlow<Int> = _totalRounds.asStateFlow()

    private val _streak = MutableStateFlow(0)
    val streak: StateFlow<Int> = _streak.asStateFlow()

    private val _hintLetters = MutableStateFlow<String?>(null)
    val hintLetters: StateFlow<String?> = _hintLetters.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val maxWrong = 3

    init {
        startNewGame()
    }

    fun startNewGame() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val res = repository.getRandomGameWords(20)
            when (res) {
                is Result.Ok -> {
                    _wordsList.clear()
                    _wordsList.addAll(res.data)
                    currentIndex = 0
                    if (_wordsList.isNotEmpty()) {
                        loadWordAt(0)
                    } else {
                        _errorMessage.value = "Nuk u gjetën fjalë për lojën"
                    }
                    _isLoading.value = false
                }
                is Result.Offline -> {
                    _errorMessage.value = "Nuk keni lidhje me internetin për të ngarkuar lojën"
                    _isLoading.value = false
                }
                is Result.Empty, is Result.Http, is Result.Err -> {
                    _errorMessage.value = "Gabim gjatë ngarkimit të fjalëve"
                    _isLoading.value = false
                }
            }
        }
    }

    private fun loadWordAt(index: Int) {
        val entry = _wordsList.getOrNull(index) ?: return
        _currentEntry.value = entry
        _userGuess.value = ""
        _wrongAttempts.value = 0
        _gameState.value = GameState.Playing
        _hintLetters.value = null
    }

    fun onGuessChanged(guess: String) {
        if (_gameState.value == GameState.Playing) {
            _userGuess.value = guess
        }
    }

    fun appendChar(char: String) {
        if (_gameState.value == GameState.Playing) {
            _userGuess.value += char
        }
    }

    fun submitGuess() {
        val entry = _currentEntry.value ?: return
        if (_gameState.value != GameState.Playing) return

        val guessTrimmed = _userGuess.value.trim()
        if (guessTrimmed.isEmpty()) return

        // Exact match validation (no diacritic folding) as per WordGame.tsx:109
        if (guessTrimmed.equals(entry.term, ignoreCase = true)) {
            _gameState.value = GameState.Correct
            _score.value += 1
            _totalRounds.value += 1
            _streak.value += 1
        } else {
            val newWrong = _wrongAttempts.value + 1
            _wrongAttempts.value = newWrong

            if (newWrong >= maxWrong) {
                // Auto reveal hint shuffle and reveal word
                _hintLetters.value = shuffleTerm(entry.term)
                _gameState.value = GameState.Revealed
                _totalRounds.value += 1
                _streak.value = 0
            }
        }
    }

    fun revealWord() {
        val entry = _currentEntry.value ?: return
        if (_gameState.value == GameState.Playing) {
            _hintLetters.value = shuffleTerm(entry.term)
            _gameState.value = GameState.Revealed
            _totalRounds.value += 1
            _streak.value = 0
        }
    }

    fun nextWord() {
        currentIndex++
        if (currentIndex < _wordsList.size) {
            loadWordAt(currentIndex)
        } else {
            // Fetch next batch of 20 random words
            startNewGame()
        }
    }

    /**
     * Mask logic:
     * Index 0 always shown, plus indices matching characters typed by the user,
     * rest '_', suffix " (${term.length})"
     */
    fun computeMaskedTerm(term: String, guess: String): String {
        if (term.isEmpty()) return ""
        val typedFolded = Slug.foldDiacritic(guess).toSet()

        val sb = StringBuilder()
        for (i in term.indices) {
            val char = term[i]
            val folded = Slug.foldDiacritic(char.toString()).firstOrNull() ?: char.lowercaseChar()
            if (i == 0 || char == ' ' || char == '-' || folded in typedFolded) {
                sb.append(char)
            } else {
                sb.append('_')
            }
            if (i < term.length - 1) {
                sb.append(' ')
            }
        }
        sb.append(" (${term.length})")
        return sb.toString()
    }

    /**
     * Blur the target term inside the definition text so it does not give away the answer
     */
    fun getMaskedDefinition(definition: String, term: String): String {
        if (term.isEmpty() || definition.isEmpty()) return definition

        val termFolded = Slug.foldDiacritic(term)
        val defFolded = Slug.foldDiacritic(definition)

        val idx = defFolded.indexOf(termFolded)
        return if (idx >= 0) {
            val originalMatch = definition.substring(idx, (idx + term.length).coerceAtMost(definition.length))
            definition.replace(originalMatch, "[ · · · ]")
        } else {
            // Replace any uppercase run equal to term
            val regex = Regex("\\b${Regex.escape(term)}\\b", RegexOption.IGNORE_CASE)
            regex.replace(definition, "[ · · · ]")
        }
    }

    private fun shuffleTerm(term: String): String {
        val cleanChars = term.filter { it.isLetter() }.toList()
        return cleanChars.shuffled().joinToString(" ")
    }
}
