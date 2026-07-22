package com.rekluzlabs.reminera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rekluzlabs.reminera.data.RemineraDatabase
import com.rekluzlabs.reminera.data.repository.MemoryEntryRepository
import com.rekluzlabs.reminera.ui.familygroups.FamilyGroupsScreen
import com.rekluzlabs.reminera.ui.familygroups.FamilyGroupsViewModelFactory
import com.rekluzlabs.reminera.ui.home.RemineraHomeScreen
import com.rekluzlabs.reminera.ui.home.RemineraViewModel
import com.rekluzlabs.reminera.ui.home.RemineraViewModelFactory
import com.rekluzlabs.reminera.ui.settings.SettingsScreen
import com.rekluzlabs.reminera.ui.settings.ThemeManager
import com.rekluzlabs.reminera.ui.settings.ThemeMode
import com.rekluzlabs.reminera.ui.splash.RemineraSplashScreen
import com.rekluzlabs.reminera.ui.theme.RemineraTheme
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
            var showSettings by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                delay(2000)
                showSplash = false
            }

            RemineraTheme(themeMode = themeMode) {
                when {
                    showSplash -> {
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
                    else -> {
                        RemineraNavHost(
                            viewModel = viewModel,
                            themeManager = themeManager,
                            themeMode = themeMode,
                            onSettingsClick = { showSettings = true }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RemineraNavHost(
    viewModel: RemineraViewModel,
    themeManager: ThemeManager,
    themeMode: ThemeMode,
    onSettingsClick: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "family_groups"
    ) {
        composable("family_groups") {
            val context = LocalContext.current
            val database = remember { RemineraDatabase.getInstance(context) }
            val familyViewModel: com.rekluzlabs.reminera.ui.familygroups.FamilyGroupsViewModel = viewModel(
                factory = FamilyGroupsViewModelFactory(database.familyGroupDao())
            )
            FamilyGroupsScreen(
                viewModel = familyViewModel,
                onGroupClick = { groupId ->
                    navController.navigate("home/$groupId")
                },
                onSettingsClick = onSettingsClick
            )
        }

        composable(
            route = "home/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.LongType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId") ?: return@composable
            RemineraHomeScreen(
                groupId = groupId,
                viewModel = viewModel,
                themeManager = themeManager,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
