package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.store.AppThemeMode
import com.example.store.PrefsStore
import com.example.ui.favorites.FavoritesScreen
import com.example.ui.game.GameScreen
import com.example.ui.search.SearchScreen
import com.example.ui.theme.FjalorTheme
import com.example.ui.word.WordScreen
import com.example.ui.wotd.WotdScreen
import com.example.util.Slug
import com.example.widget.FjalorWidgetProvider
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val incomingWordSlug = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIncomingIntent(intent)

        // Trigger widget update
        FjalorWidgetProvider.updateAllWidgets(this)

        setContent {
            val prefs = remember { PrefsStore.instance }
            // Default to SYSTEM theme so the app adapts dynamically with the system light/dark mode
            val themeMode by prefs.themeFlow.collectAsStateWithLifecycle(initialValue = AppThemeMode.SYSTEM)
            val scope = rememberCoroutineScope()
            val slugFromIntent by incomingWordSlug

            FjalorTheme(themeMode = themeMode) {
                MainAppScaffold(
                    initialSlug = slugFromIntent,
                    onSlugConsumed = { incomingWordSlug.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        // 1. Process Text from text selection in any external app
        if (intent.action == Intent.ACTION_PROCESS_TEXT) {
            val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
                ?: intent.getCharSequenceExtra("android.intent.extra.PROCESS_TEXT_READONLY")?.toString()
            if (!text.isNullOrBlank()) {
                val slug = Slug.of(text.trim())
                if (slug.isNotEmpty()) {
                    incomingWordSlug.value = slug
                    return
                }
            }
        }

        // 2. Share text from external app
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                val firstWord = sharedText.trim().split(Regex("\\s+")).firstOrNull() ?: sharedText.trim()
                val slug = Slug.of(firstWord)
                if (slug.isNotEmpty()) {
                    incomingWordSlug.value = slug
                    return
                }
            }
        }

        // 3. Widget direct word slug extra
        val widgetSlug = intent.getStringExtra("EXTRA_WORD_SLUG")
        if (!widgetSlug.isNullOrBlank()) {
            incomingWordSlug.value = Slug.of(widgetSlug.trim())
            return
        }

        // 4. Web deep link (/f/{slug})
        val deepLinkSlug = extractSlugFromDeepLink(intent)
        if (!deepLinkSlug.isNullOrBlank()) {
            incomingWordSlug.value = deepLinkSlug
        }
    }

    private fun extractSlugFromDeepLink(intent: Intent?): String? {
        val data: Uri? = intent?.data
        if (data != null && data.scheme == "https" && data.host == "fjalor.bashk.eu") {
            val path = data.path ?: ""
            if (path.startsWith("/f/")) {
                return path.removePrefix("/f/").trim().takeIf { it.isNotEmpty() }
            }
        }
        return null
    }
}

@Composable
fun MainAppScaffold(
    initialSlug: String? = null,
    onSlugConsumed: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val wordStack = remember { mutableStateListOf<String>() }

    LaunchedEffect(initialSlug) {
        if (!initialSlug.isNullOrEmpty()) {
            wordStack.add(initialSlug)
            onSlugConsumed()
        }
    }

    // Handle back button when viewing words
    BackHandler(enabled = wordStack.isNotEmpty()) {
        wordStack.removeAt(wordStack.size - 1)
    }

    val currentWordSlug = wordStack.lastOrNull()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentWordSlug == null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    // Tab 0: Kërko
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 0) Icons.Filled.Search else Icons.Outlined.Search,
                                contentDescription = "Kërko"
                            )
                        },
                        label = { Text("Kërko") },
                        modifier = Modifier.testTag("nav_tab_search"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )

                    // Tab 1: Fjala e Ditës
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 1) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                                contentDescription = "Fjala e ditës"
                            )
                        },
                        label = { Text("Fjala e ditës") },
                        modifier = Modifier.testTag("nav_tab_wotd"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )

                    // Tab 2: Gjeje
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 2) Icons.Filled.Extension else Icons.Outlined.Extension,
                                contentDescription = "Gjeje"
                            )
                        },
                        label = { Text("Gjeje") },
                        modifier = Modifier.testTag("nav_tab_game"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )

                    // Tab 3: Të preferuarat
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 3) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Të miat"
                            )
                        },
                        label = { Text("Të miat") },
                        modifier = Modifier.testTag("nav_tab_favorites"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (currentWordSlug != null) {
                // Word Detail Screen with sub-navigation
                WordScreen(
                    slug = currentWordSlug,
                    onBackClick = {
                        if (wordStack.isNotEmpty()) {
                            wordStack.removeAt(wordStack.size - 1)
                        }
                    },
                    onNavigateToWord = { nextSlug ->
                        wordStack.add(nextSlug)
                    }
                )
            } else {
                when (selectedTab) {
                    0 -> SearchScreen(
                        onNavigateToWord = { slug -> wordStack.add(slug) }
                    )
                    1 -> WotdScreen(
                        onNavigateToWord = { slug -> wordStack.add(slug) }
                    )
                    2 -> GameScreen(
                        onNavigateToWord = { slug -> wordStack.add(slug) }
                    )
                    3 -> FavoritesScreen(
                        onNavigateToWord = { slug -> wordStack.add(slug) }
                    )
                }
            }
        }
    }
}
