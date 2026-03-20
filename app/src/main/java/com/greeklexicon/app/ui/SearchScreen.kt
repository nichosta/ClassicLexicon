package com.greeklexicon.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.greeklexicon.app.data.LexiconEntry

@Composable
fun SearchScreen(
    viewModel: LexiconViewModel,
    onEntryClick: (LexiconEntry) -> Unit,
) {
    val query by viewModel.query.collectAsState()
    val searchMode by viewModel.searchMode.collectAsState()
    val results by viewModel.results.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.onQueryChanged(it) },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        if (searchMode == SearchMode.GREEK) {
                            "Search L&S headwords..."
                        } else {
                            "Search S&H headwords..."
                        }
                    )
                },
                singleLine = true,
            )
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .wrapContentWidth(),
            ) {
                OutlinedButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.widthIn(min = 88.dp),
                ) {
                    Text(
                        if (searchMode == SearchMode.GREEK) {
                            "L&S ▼"
                        } else {
                            "S&H ▼"
                        }
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("L&S") },
                        onClick = {
                            menuExpanded = false
                            viewModel.onSearchModeChanged(SearchMode.GREEK)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("S&H") },
                        onClick = {
                            menuExpanded = false
                            viewModel.onSearchModeChanged(SearchMode.LATIN)
                        },
                    )
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        if (query.isNotBlank() && results.isEmpty() && !isLoading) {
            Text(
                text = "No results found",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(results, key = { it.id }) { entry ->
                EntryListItem(entry = entry, onClick = { onEntryClick(entry) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun EntryListItem(entry: LexiconEntry, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = entry.headword,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        if (!entry.shortDef.isNullOrBlank()) {
            Text(
                text = entry.shortDef,
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
