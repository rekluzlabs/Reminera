package com.example.reminera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.reminera.data.MemoryEntryEntity
import com.example.reminera.data.RemineraDatabase
import com.example.reminera.data.repository.MemoryEntryRepository
import com.example.reminera.ui.detail.MemoryDetailScreen
import com.example.reminera.ui.home.RemineraHomeScreen
import com.example.reminera.ui.home.RemineraViewModel
import com.example.reminera.ui.home.RemineraViewModelFactory
import com.example.reminera.ui.settings.SettingsScreen
import com.example.reminera.ui.settings.ThemeManager
import com.example.reminera.ui.settings.ThemeMode
import com.example.reminera.ui.splash.RemineraSplashScreen
import com.example.reminera.ui.theme.RemineraTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val viewModel: RemineraViewModel by viewModels {
        val database = RemineraDatabase.getInstance(applicationContext)
        val repository = MemoryEntryRepository(database.memoryEntryDao())
        RemineraViewModelFactory(repository)
    }

    private lateinit var themeManager: ThemeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        themeManager = ThemeManager(applicationContext)

        setContent {
            var showSplash by rememberSaveable { mutableStateOf(true) }
            var themeMode by rememberSaveable { mutableStateOf(themeManager.getThemeMode()) }
            var showSettings by rememberSaveable { mutableStateOf(false) }
            var selectedEntry by rememberSaveable { mutableStateOf<MemoryEntryEntity?>(null) }

            LaunchedEffect(Unit) {
                delay(2000)
                showSplash = false
            }

            RemineraTheme(themeMode = themeMode) {
                val currentEntry = selectedEntry

                when {
                    showSplash -> {
                        BackHandler { showSplash = false }
                        RemineraSplashScreen()
                    }
                    showSettings -> {
                        BackHandler { showSettings = false }
                        SettingsScreen(
                            currentTheme = themeMode,
                            onThemeSelected = { mode ->
                                themeMode = mode
                                themeManager.setThemeMode(mode)
                            },
                            onBack = { showSettings = false }
                        )
                    }
                    currentEntry != null -> {
                        BackHandler { selectedEntry = null }
                        MemoryDetailScreen(
                            entry = currentEntry,
                            onBack = { selectedEntry = null },
                            onSave = { id, title, notes, personTag, type, localFilePath ->
                                viewModel.updateEntryDetails(id, title, notes, personTag, type, localFilePath)
                                selectedEntry = null
                            },
                            onDelete = { id ->
                                viewModel.deleteEntry(id)
                                selectedEntry = null
                            }
                        )
                    }
                    else -> RemineraHomeScreen(
                        viewModel = viewModel,
                        onSettingsClick = { showSettings = true },
                        onEntryClick = { entry -> selectedEntry = entry }
                    )
                }
            }
        }
    }
}
