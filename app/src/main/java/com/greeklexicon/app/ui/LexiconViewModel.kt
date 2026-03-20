package com.greeklexicon.app.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.greeklexicon.app.data.LexiconDatabase
import com.greeklexicon.app.data.LexiconEntry
import com.greeklexicon.app.data.normalizeGreekForLookup
import com.greeklexicon.app.data.normalizeLatinForLookup
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class SearchMode {
    GREEK,
    LATIN,
}

class LexiconViewModel(app: Application) : AndroidViewModel(app) {
    companion object {
        private const val TAG = "LexiconViewModel"
    }

    private val dao = LexiconDatabase.getInstance(app).lexiconDao()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _searchMode = MutableStateFlow(SearchMode.GREEK)
    val searchMode: StateFlow<SearchMode> = _searchMode

    private val _results = MutableStateFlow<List<LexiconEntry>>(emptyList())
    val results: StateFlow<List<LexiconEntry>> = _results

    private val _selectedEntry = MutableStateFlow<LexiconEntry?>(null)
    val selectedEntry: StateFlow<LexiconEntry?> = _selectedEntry

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var searchJob: Job? = null

    private fun mergeResults(
        primary: List<LexiconEntry>,
        secondary: List<LexiconEntry>,
        limit: Int = 50,
    ): List<LexiconEntry> {
        return (primary + secondary)
            .distinctBy { it.id }
            .take(limit)
    }

    private fun isLatinScriptQuery(query: String): Boolean {
        val hasLetters = query.any { it.isLetter() }
        if (!hasLetters) {
            return false
        }

        return query.none { ch ->
            Character.UnicodeScript.of(ch.code) == Character.UnicodeScript.GREEK
        }
    }

    fun onSearchModeChanged(newMode: SearchMode) {
        if (_searchMode.value == newMode) {
            return
        }

        _searchMode.value = newMode
        rerunSearch()
    }

    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
        rerunSearch()
    }

    private fun rerunSearch() {
        searchJob?.cancel()

        val pendingQuery = _query.value
        if (pendingQuery.isBlank()) {
            _results.value = emptyList()
            return
        }

        searchJob = viewModelScope.launch {
            delay(200)
            _isLoading.value = true
            try {
                val trimmed = pendingQuery.trim().lowercase()
                val results = when (_searchMode.value) {
                    SearchMode.GREEK -> {
                        val normalizedGreek = normalizeGreekForLookup(trimmed)
                        if (isLatinScriptQuery(trimmed)) {
                            dao.searchGreekByLatinPrefix(trimmed)
                        } else if (normalizedGreek.length <= 2) {
                            dao.searchGreekByPrefix(normalizedGreek)
                        } else {
                            val ftsQuery = normalizedGreek + "*"
                            val ftsResults =
                                try {
                                    dao.searchGreek(ftsQuery)
                                } catch (_: Exception) {
                                    emptyList()
                                }
                            val prefixResults = dao.searchGreekByPrefix(normalizedGreek)
                            mergeResults(ftsResults, prefixResults)
                        }
                    }

                    SearchMode.LATIN -> {
                        val normalizedLatin = normalizeLatinForLookup(trimmed)
                        if (normalizedLatin.isBlank()) {
                            emptyList()
                        } else {
                            dao.searchLatinByPrefix(normalizedLatin)
                        }
                    }
                }
                _results.value = results
            } catch (_: CancellationException) {
            } catch (e: Exception) {
                Log.e(TAG, "Search failed for query='${pendingQuery.trim()}'", e)
                _results.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectEntry(entry: LexiconEntry) {
        _selectedEntry.value = entry
    }

    fun clearSelection() {
        _selectedEntry.value = null
    }

    fun selectEntryById(id: Long) {
        viewModelScope.launch {
            _selectedEntry.value = dao.getById(id)
        }
    }
}
