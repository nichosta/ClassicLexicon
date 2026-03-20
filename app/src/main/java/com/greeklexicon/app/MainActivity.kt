package com.greeklexicon.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.greeklexicon.app.ui.EntryDetailScreen
import com.greeklexicon.app.ui.LexiconViewModel
import com.greeklexicon.app.ui.SearchScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val viewModel: LexiconViewModel = viewModel()
                    val selectedEntry by viewModel.selectedEntry.collectAsState()

                    val entry = selectedEntry
                    if (entry != null) {
                        EntryDetailScreen(
                            entry = entry,
                            onBack = { viewModel.clearSelection() },
                        )
                    } else {
                        SearchScreen(
                            viewModel = viewModel,
                            onEntryClick = { viewModel.selectEntry(it) },
                        )
                    }
                }
            }
        }
    }
}
