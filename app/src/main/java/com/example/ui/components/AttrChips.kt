package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.Abbrev

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttrChips(
    attributes: List<String>,
    modifier: Modifier = Modifier
) {
    if (attributes.isEmpty()) return

    var selectedAttr by remember { mutableStateOf<String?>(null) }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        attributes.forEach { rawAttr ->
            val expanded = Abbrev.expand(rawAttr)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                    .clickable { selectedAttr = rawAttr }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = rawAttr,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }

    selectedAttr?.let { attr ->
        val full = Abbrev.expand(attr)
        AlertDialog(
            onDismissRequest = { selectedAttr = null },
            title = {
                Text(
                    text = "Shpjegimi i shkurtesës",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = "$attr = $full",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                TextButton(onClick = { selectedAttr = null }) {
                    Text("Në rregull")
                }
            }
        )
    }
}
