package com.example.ui.word

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AttrChips
import com.example.ui.components.DefinitionList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordScreen(
    slug: String,
    onBackClick: () -> Unit,
    onNavigateToWord: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WordViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val crossRefEnabled by viewModel.crossRefEnabled.collectAsStateWithLifecycle()
    val slugsSet by viewModel.slugsSet.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(slug) {
        viewModel.loadWord(slug)
        selectedTabIndex = 0
    }

    val currentEntries = (uiState as? WordUiState.Success)?.entries ?: emptyList()
    val activeEntry = currentEntries.getOrNull(selectedTabIndex) ?: currentEntries.firstOrNull()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("word_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kthehu"
                        )
                    }
                },
                actions = {
                    // Cross-reference toggle
                    IconButton(
                        onClick = {
                            viewModel.toggleCrossRef()
                            val msg = if (!crossRefEnabled) "Ndërlidhja e fjalëve u aktivizua" else "Ndërlidhja e fjalëve u çaktivizua"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("cross_reference_toggle")
                    ) {
                        Icon(
                            imageVector = if (crossRefEnabled) Icons.Default.Link else Icons.Default.LinkOff,
                            contentDescription = "Ndërlidhje fjalësh",
                            tint = if (crossRefEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Favorite button
                    activeEntry?.let { entry ->
                        IconButton(
                            onClick = { viewModel.toggleFavorite(entry.term) },
                            modifier = Modifier.testTag("favorite_button")
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = if (isFavorite) "Hiq nga të preferuarat" else "Ruaj në të preferuarat",
                                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Share button
                        IconButton(
                            onClick = {
                                val defs = entry.displayDefinitions.joinToString("\n• ")
                                val shareText = "${entry.term}\n\n• $defs\n\nNga Fjalor Shqip: https://fjalor.bashk.eu/f/${entry.slug}"
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Shpërndaj fjalën"))
                            },
                            modifier = Modifier.testTag("share_word_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Shpërndaj"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is WordUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("word_loading_indicator")
                        )
                    }
                }

                is WordUiState.Success -> {
                    val entries = state.entries
                    val currentEntry = entries.getOrNull(selectedTabIndex) ?: entries.first()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        // Multi-entry switcher if multiple rows returned for this slug
                        if (entries.size > 1) {
                            Text(
                                text = "Kjo fjalë ka ${entries.size} kuptime/pjesë ligjërate:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            ScrollableTabRow(
                                selectedTabIndex = selectedTabIndex,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                edgePadding = 0.dp
                            ) {
                                entries.forEachIndexed { idx, entry ->
                                    val attrLabel = entry.attributes.firstOrNull() ?: "#${idx + 1}"
                                    Tab(
                                        selected = selectedTabIndex == idx,
                                        onClick = { selectedTabIndex = idx },
                                        text = { Text("$attrLabel (${idx + 1})") }
                                    )
                                }
                            }
                        }

                        // Term Header Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = currentEntry.term,
                                        style = MaterialTheme.typography.headlineLarge.copy(
                                            fontFamily = FontFamily.Serif,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("word_term_title")
                                    )

                                    // Copy definition button
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val textToCopy = "${currentEntry.term}: ${currentEntry.displayDefinitions.joinToString("; ")}"
                                            val clip = ClipData.newPlainText("Përkufizimi", textToCopy)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "U kopjua në kujtesë", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.testTag("copy_definition_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Kopjo fjalën",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (currentEntry.attributes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    AttrChips(attributes = currentEntry.attributes)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Cross-reference status banner if active
                        if (crossRefEnabled) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Ndërlidhja aktive: prekni fjalët e nënvizuara për t'i parë",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // Section Title: Përkufizimi
                        Text(
                            text = if (currentEntry.displayDefinitions.size > 1) "Kuptimet" else "Kuptimi",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            thickness = 1.dp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Definitions List
                        DefinitionList(
                            definitions = currentEntry.displayDefinitions,
                            crossRefEnabled = crossRefEnabled,
                            slugsSet = slugsSet,
                            onWordClick = onNavigateToWord
                        )

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }

                is WordUiState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "Fjala nuk u gjet",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Nuk ka asnjë të dhënë për fjalën '$slug' në fjalor.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onBackClick,
                                modifier = Modifier.testTag("empty_back_button")
                            ) {
                                Text("Kthehu te kërkimi")
                            }
                        }
                    }
                }

                is WordUiState.Offline -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = null,
                                modifier = Modifier.size(52.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Nuk keni lidhje me internetin",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Button(
                                onClick = { viewModel.loadWord(slug) },
                                modifier = Modifier.testTag("retry_word_button")
                            ) {
                                Text("Provo përsëri")
                            }
                        }
                    }
                }

                is WordUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { viewModel.loadWord(slug) },
                                modifier = Modifier.testTag("retry_word_button")
                            ) {
                                Text("Riprovo")
                            }
                        }
                    }
                }
            }
        }
    }
}
