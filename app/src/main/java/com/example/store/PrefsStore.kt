package com.example.store

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.FjalorApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "fjalor_prefs")

data class HistoryEntry(
    val slug: String,
    val term: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class FavoriteEntry(
    val slug: String,
    val term: String
)

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    SEPIA,
    DARK;

    fun next(): AppThemeMode = when (this) {
        SYSTEM -> LIGHT
        LIGHT -> SEPIA
        SEPIA -> DARK
        DARK -> SYSTEM
    }
}

class PrefsStore(private val context: Context = FjalorApp.instance) {

    companion object {
        private val KEY_FAVORITES = stringPreferencesKey("fj_favorites")
        private val KEY_HISTORY = stringPreferencesKey("fj_history")
        private val KEY_LAST_SEARCH_QUERY = stringPreferencesKey("fj_last_search_query")
        private val KEY_CROSSREF = booleanPreferencesKey("fj_crossref")
        private val KEY_WOTD_DATE = stringPreferencesKey("wotd_date")
        private val KEY_WOTD_SLUG = stringPreferencesKey("wotd_slug")
        private val KEY_WOTD_SEEN = stringSetPreferencesKey("wotd_seen")
        private val KEY_THEME = stringPreferencesKey("app_theme")

        val instance: PrefsStore by lazy { PrefsStore() }
    }

    val favoritesFlow: Flow<List<FavoriteEntry>> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_FAVORITES] ?: "[]"
        parseFavorites(jsonStr)
    }

    val historyFlow: Flow<List<HistoryEntry>> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_HISTORY] ?: "[]"
        parseHistory(jsonStr)
    }

    val lastSearchQueryFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_SEARCH_QUERY] ?: ""
    }

    val crossRefFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_CROSSREF] ?: false
    }

    val themeFlow: Flow<AppThemeMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_THEME] ?: AppThemeMode.SYSTEM.name
        try {
            AppThemeMode.valueOf(raw)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    suspend fun toggleFavorite(slug: String, term: String): Boolean {
        var isNowFav = false
        context.dataStore.edit { prefs ->
            val list = parseFavorites(prefs[KEY_FAVORITES] ?: "[]").toMutableList()
            val existingIndex = list.indexOfFirst { it.slug == slug }
            if (existingIndex >= 0) {
                list.removeAt(existingIndex)
                isNowFav = false
            } else {
                list.add(0, FavoriteEntry(slug, term))
                isNowFav = true
            }
            prefs[KEY_FAVORITES] = serializeFavorites(list)
        }
        return isNowFav
    }

    suspend fun removeFavorite(slug: String) {
        context.dataStore.edit { prefs ->
            val list = parseFavorites(prefs[KEY_FAVORITES] ?: "[]").toMutableList()
            list.removeAll { it.slug == slug }
            prefs[KEY_FAVORITES] = serializeFavorites(list)
        }
    }

    suspend fun addToHistory(slug: String, term: String) {
        context.dataStore.edit { prefs ->
            val list = parseHistory(prefs[KEY_HISTORY] ?: "[]").toMutableList()
            // Dedupe by slug
            list.removeAll { it.slug == slug }
            // Add to front
            list.add(0, HistoryEntry(slug = slug, term = term, timestamp = System.currentTimeMillis()))
            // Cap at 50 items
            val capped = if (list.size > 50) list.take(50) else list
            prefs[KEY_HISTORY] = serializeHistory(capped)
        }
    }

    suspend fun clearHistory() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_HISTORY)
        }
    }

    suspend fun removeHistoryItem(slug: String) {
        context.dataStore.edit { prefs ->
            val list = parseHistory(prefs[KEY_HISTORY] ?: "[]").toMutableList()
            list.removeAll { it.slug == slug }
            prefs[KEY_HISTORY] = serializeHistory(list)
        }
    }

    suspend fun setLastSearchQuery(query: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_SEARCH_QUERY] = query
        }
    }

    suspend fun setCrossRefEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CROSSREF] = enabled
        }
    }

    suspend fun setTheme(theme: AppThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME] = theme.name
        }
    }

    suspend fun getWotdCache(todayUtc: String): String? {
        val prefs = context.dataStore.data.first()
        return if (prefs[KEY_WOTD_DATE] == todayUtc) {
            prefs[KEY_WOTD_SLUG]
        } else null
    }

    suspend fun saveWotdCache(todayUtc: String, slug: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WOTD_DATE] = todayUtc
            prefs[KEY_WOTD_SLUG] = slug
            val seen = prefs[KEY_WOTD_SEEN]?.toMutableSet() ?: mutableSetOf()
            seen.add(slug)
            prefs[KEY_WOTD_SEEN] = seen
        }
    }

    suspend fun getWotdSeen(): Set<String> {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_WOTD_SEEN] ?: emptySet()
    }

    private fun parseFavorites(jsonStr: String): List<FavoriteEntry> {
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<FavoriteEntry>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(FavoriteEntry(obj.getString("slug"), obj.getString("term")))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeFavorites(list: List<FavoriteEntry>): String {
        val arr = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("slug", item.slug)
            obj.put("term", item.term)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun parseHistory(jsonStr: String): List<HistoryEntry> {
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<HistoryEntry>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    HistoryEntry(
                        slug = obj.getString("slug"),
                        term = obj.getString("term"),
                        timestamp = obj.optLong("ts", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeHistory(list: List<HistoryEntry>): String {
        val arr = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("slug", item.slug)
            obj.put("term", item.term)
            arr.put(obj)
        }
        return arr.toString()
    }
}
