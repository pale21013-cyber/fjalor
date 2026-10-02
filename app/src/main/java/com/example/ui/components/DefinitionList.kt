package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.Slug

@Composable
fun DefinitionList(
    definitions: List<String>,
    modifier: Modifier = Modifier,
    crossRefEnabled: Boolean = false,
    slugsSet: Set<String> = emptySet(),
    onWordClick: (String) -> Unit = {}
) {
    if (definitions.isEmpty()) {
        Text(
            text = "Nuk ka përkufizime të disponueshme.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(vertical = 8.dp)
        )
        return
    }

    val multiple = definitions.size > 1

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        definitions.forEachIndexed { index, defText ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                if (multiple) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 10.dp, top = 2.dp)
                    )
                }

                if (crossRefEnabled && slugsSet.isNotEmpty()) {
                    val annotated = remember(defText, slugsSet) {
                        buildCrossRefString(defText, slugsSet, onWordClick)
                    }

                    Text(
                        text = annotated,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 26.sp,
                            fontSize = 17.sp
                        ),
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Text(
                        text = defText,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 26.sp,
                            fontSize = 17.sp
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun buildCrossRefString(
    text: String,
    slugsSet: Set<String>,
    onWordClick: (String) -> Unit
): AnnotatedString {
    return buildAnnotatedString {
        val wordRegex = Regex("([a-zA-ZëËçÇ]+)|([^a-zA-ZëËçÇ]+)")
        val matches = wordRegex.findAll(text)

        for (match in matches) {
            val token = match.value
            val isWord = match.groups[1] != null

            if (isWord) {
                val candidateSlug = Slug.of(token)
                val matchedSlug = Slug.matchSlug(candidateSlug, slugsSet)

                if (matchedSlug != null) {
                    val link = LinkAnnotation.Clickable(
                        tag = matchedSlug,
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = androidx.compose.ui.graphics.Color(0xFFB31920),
                                fontWeight = FontWeight.Medium,
                                textDecoration = TextDecoration.Underline
                            )
                        ),
                        linkInteractionListener = {
                            onWordClick(matchedSlug)
                        }
                    )
                    pushLink(link)
                    append(token)
                    pop()
                } else {
                    append(token)
                }
            } else {
                append(token)
            }
        }
    }
}
