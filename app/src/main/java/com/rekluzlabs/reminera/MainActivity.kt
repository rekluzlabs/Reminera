package com.rekluzlabs.reminera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.rekluzlabs.reminera.data.repository.BiographyRepository
import com.rekluzlabs.reminera.data.repository.FamilyMemberRepository
import com.rekluzlabs.reminera.data.repository.MemoryEntryRepository
import com.rekluzlabs.reminera.ui.biography.BiographyScreen
import com.rekluzlabs.reminera.ui.biography.BiographyViewModel
import com.rekluzlabs.reminera.ui.biography.BiographyViewModelFactory
import com.rekluzlabs.reminera.ui.biography.StoryEntryScreen
import com.rekluzlabs.reminera.ui.familygroups.FamilyGroupsScreen
import com.rekluzlabs.reminera.ui.familygroups.FamilyGroupsViewModelFactory
import com.rekluzlabs.reminera.ui.home.FamilyMemberListScreen
import com.rekluzlabs.reminera.ui.home.RemineraHomeScreen
import com.rekluzlabs.reminera.ui.home.RemineraViewModel
import com.rekluzlabs.reminera.ui.home.RemineraViewModelFactory
import com.rekluzlabs.reminera.ui.settings.SettingsScreen
import com.rekluzlabs.reminera.ui.settings.ThemeManager
import com.rekluzlabs.reminera.ui.settings.ThemeMode
import com.rekluzlabs.reminera.ui.splash.RemineraSplashScreen
import com.rekluzlabs.reminera.ui.theme.RemineraTheme
import com.rekluzlabs.reminera.ui.tutorial.TutorialRepository
import com.rekluzlabs.reminera.ui.tutorial.TutorialViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: RemineraViewModel by viewModels {
        val database = RemineraDatabase.getInstance(applicationContext)
        val repository = MemoryEntryRepository(database.memoryEntryDao())
        val memberRepository = FamilyMemberRepository(database.familyMemberDao())
        RemineraViewModelFactory(repository, memberRepository)
    }

    private lateinit var themeManager: ThemeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        themeManager = ThemeManager(applicationContext)

        setContent {
            var showSplash by rememberSaveable { mutableStateOf(true) }
            var themeMode by rememberSaveable { mutableStateOf(themeManager.getThemeMode()) }
            var showSettings by remember { mutableStateOf(false) }

            RemineraTheme(themeMode = themeMode) {
                when {
                    showSplash -> {
                        RemineraSplashScreen(onBegin = { showSplash = false })
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

            val tutorialViewModel: TutorialViewModel = viewModel()
            val tutorialRepository = remember { TutorialRepository(context) }

            FamilyGroupsScreen(
                viewModel = familyViewModel,
                tutorialCoordinator = tutorialViewModel.coordinator,
                tutorialRepository = tutorialRepository,
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
            FamilyMemberListScreen(
                groupId = groupId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onMemberClick = { member ->
                    navController.navigate("biography/${groupId}/${member.id}")
                },
                onSettingsClick = onSettingsClick
            )
        }

        composable(
            route = "biography/{groupId}/{personId}",
            arguments = listOf(
                navArgument("groupId") { type = NavType.LongType },
                navArgument("personId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId") ?: return@composable
            val personId = backStackEntry.arguments?.getLong("personId") ?: return@composable
            val context = LocalContext.current
            val database = remember { RemineraDatabase.getInstance(context) }
            val bioRepo = remember { BiographyRepository(database.biographyDao(), database.biographySectionDao(), database.storyEntryDao()) }
            val membersList by viewModel.getMembersByGroupId(groupId).collectAsState(initial = emptyList())
            val member = remember(personId, membersList) { membersList.find { m -> m.id == personId } }
            val bioViewModel: BiographyViewModel = viewModel(
                factory = BiographyViewModelFactory(personId, member, bioRepo)
            )
            BiographyScreen(
                personId = personId,
                memberName = member?.name ?: "Family Member",
                viewModel = bioViewModel,
                remineraViewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSettingsClick = onSettingsClick,
                onNavigateToStory = { biographyId ->
                    navController.navigate("story/$biographyId")
                },
                onAddMemory = {
                    navController.navigate("member/${groupId}/${personId}")
                }
            )
        }

        composable(
            route = "story/{biographyId}",
            arguments = listOf(
                navArgument("biographyId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val biographyId = backStackEntry.arguments?.getString("biographyId") ?: return@composable
            val context = LocalContext.current
            val database = remember { RemineraDatabase.getInstance(context) }
            val bioRepo = remember { BiographyRepository(database.biographyDao(), database.biographySectionDao(), database.storyEntryDao()) }
            val bioViewModel: BiographyViewModel = viewModel(
                factory = BiographyViewModelFactory(-1L, null, bioRepo)
            )
            StoryEntryScreen(
                biographyId = biographyId,
                viewModel = bioViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
