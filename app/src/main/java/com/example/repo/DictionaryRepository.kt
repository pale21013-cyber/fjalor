package com.example.repo

import com.example.net.Entry
import com.example.net.FjalorApi
import com.example.net.Result
import com.example.store.PrefsStore
import com.example.util.DateUtils
import com.example.util.Slug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class DictionaryRepository(
    private val api: FjalorApi = FjalorApi(),
    private val prefs: PrefsStore = PrefsStore.instance
) {
    companion object {
        val instance: DictionaryRepository by lazy { DictionaryRepository() }
        private const val SLUGS_CACHE_TTL_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    private var cachedSlugsSet: Set<String>? = null
    private var lastSlugsFetchTimestamp: Long = 0L
    private val slugsMutex = Mutex()

    /**
     * Search words with query
     */
    suspend fun search(query: String): Result<List<Entry>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return Result.Ok(emptyList())
        return api.search(trimmed)
    }

    /**
     * Get word by slug, with fallback to search if word endpoint returns empty or error
     * as implemented in the upstream web app (WordPage.tsx:20-47)
     */
    suspend fun getWord(slug: String): Result<List<Entry>> = withContext(Dispatchers.IO) {
        val normalizedSlug = Slug.of(slug)
        val wordResult = api.word(normalizedSlug)

        val finalResult = when (wordResult) {
            is Result.Ok -> {
                if (wordResult.data.isNotEmpty()) {
                    wordResult
                } else {
                    fallbackSearch(normalizedSlug)
                }
            }
            is Result.Empty, is Result.Http -> {
                fallbackSearch(normalizedSlug)
            }
            is Result.Offline -> wordResult
            is Result.Err -> fallbackSearch(normalizedSlug)
        }

        // If success, save to history
        if (finalResult is Result.Ok && finalResult.data.isNotEmpty()) {
            val first = finalResult.data.first()
            prefs.addToHistory(first.slug, first.term)
        }

        finalResult
    }

    private suspend fun fallbackSearch(slug: String): Result<List<Entry>> {
        val searchRes = api.search(slug)
        return when (searchRes) {
            is Result.Ok -> {
                val matched = searchRes.data.filter { it.slug.equals(slug, ignoreCase = true) }
                if (matched.isNotEmpty()) {
                    Result.Ok(matched)
                } else if (searchRes.data.isNotEmpty()) {
                    Result.Ok(searchRes.data)
                } else {
                    Result.Empty
                }
            }
            else -> searchRes
        }
    }

    /**
     * Lazy fetch of 37,000+ slugs set. Cached in-memory with 24h TTL.
     */
    suspend fun getSlugsSet(): Set<String> = slugsMutex.withLock {
        val now = System.currentTimeMillis()
        val currentSet = cachedSlugsSet
        if (currentSet != null && (now - lastSlugsFetchTimestamp) < SLUGS_CACHE_TTL_MS) {
            return@withLock currentSet
        }

        val result = api.slugs()
        if (result is Result.Ok) {
            val set = result.data.toHashSet()
            cachedSlugsSet = set
            lastSlugsFetchTimestamp = now
            return@withLock set
        }

        return@withLock cachedSlugsSet ?: emptySet()
    }

    /**
     * Word of the day implementation from word-of-the-day.ts with reliable fallbacks
     */
    suspend fun getWordOfTheDay(): Result<Entry> = withContext(Dispatchers.IO) {
        val todayUtc = DateUtils.todayUtc()

        // 1. Check local cache
        try {
            val cachedSlug = prefs.getWotdCache(todayUtc)
            if (!cachedSlug.isNullOrEmpty()) {
                val wordRes = api.word(cachedSlug)
                if (wordRes is Result.Ok) {
                    val valid = wordRes.data.firstOrNull { it.displayDefinitions.isNotEmpty() }
                    if (valid != null) {
                        return@withContext Result.Ok(valid)
                    }
                }
            }
        } catch (e: Exception) {
            // continue to fetch
        }

        // 2. Fetch slugs and calculate pick
        val slugs = try {
            getSlugsSet()
        } catch (e: Exception) {
            emptySet()
        }

        if (slugs.isNotEmpty()) {
            val seen = try { prefs.getWotdSeen() } catch (e: Exception) { emptySet() }
            val availableList = slugs.filter { it !in seen }.ifEmpty { slugs.toList() }

            var hash = DateUtils.jsHash(todayUtc)
            var attempts = 0
            val maxAttempts = 15

            while (attempts < maxAttempts && availableList.isNotEmpty()) {
                val pickIndex = (hash.toLong() % availableList.size).toInt().coerceAtLeast(0)
                val pickedSlug = availableList[pickIndex]

                val wordRes = api.word(pickedSlug)
                if (wordRes is Result.Ok) {
                    val valid = wordRes.data.firstOrNull { it.displayDefinitions.isNotEmpty() }
                    if (valid != null) {
                        try { prefs.saveWotdCache(todayUtc, pickedSlug) } catch (_: Exception) {}
                        return@withContext Result.Ok(valid)
                    }
                }

                // Retry step with new hash
                hash = DateUtils.jsHash("$todayUtc:$attempts")
                attempts++
            }
        }

        // 3. Fallback to random if slugs not available or loop didn't yield entry
        val randomFallback = api.random(10)
        if (randomFallback is Result.Ok) {
            val candidate = randomFallback.data.firstOrNull { it.displayDefinitions.isNotEmpty() }
            if (candidate != null) {
                try { prefs.saveWotdCache(todayUtc, candidate.slug) } catch (_: Exception) {}
                return@withContext Result.Ok(candidate)
            }
        }

        if (randomFallback is Result.Offline) {
            return@withContext Result.Offline
        }

        Result.Empty
    }

    /**
     * Get random words with non-empty definitions for Word Game
     */
    suspend fun getRandomGameWords(count: Int = 20): Result<List<Entry>> = withContext(Dispatchers.IO) {
        val result = api.random(count)
        if (result is Result.Ok) {
            val valid = result.data.filter { it.displayDefinitions.isNotEmpty() }
            if (valid.isNotEmpty()) Result.Ok(valid) else Result.Empty
        } else {
            result
        }
    }
}
