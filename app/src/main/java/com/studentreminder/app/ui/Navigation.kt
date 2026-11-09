package com.studentreminder.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.studentreminder.app.AppContainer
import com.studentreminder.app.data.AppSettings
import com.studentreminder.app.ui.screens.AssignmentEditScreen
import com.studentreminder.app.ui.screens.AssignmentsScreen
import com.studentreminder.app.ui.screens.CalendarScreen
import com.studentreminder.app.ui.screens.ExamDetailScreen
import com.studentreminder.app.ui.screens.ExamEditScreen
import com.studentreminder.app.ui.screens.ExamsScreen
import com.studentreminder.app.ui.screens.HomeScreen
import com.studentreminder.app.ui.screens.SearchScreen
import com.studentreminder.app.ui.screens.SettingsScreen
import com.studentreminder.app.ui.screens.SubjectsScreen
import java.time.LocalDate

object Routes {
    const val HOME = "home"
    const val EXAMS = "exams"
    const val ASSIGNMENTS = "assignments"
    const val CALENDAR = "calendar"
    const val SETTINGS = "settings"
    const val SEARCH = "search"
    const val SUBJECTS = "subjects"
    const val EXAM_DETAIL = "exam/{id}"
    const val EXAM_EDIT = "exam_edit?id={id}&date={date}"
    const val ASSIGNMENT_EDIT = "assignment_edit?id={id}&date={date}"

    fun examDetail(id: Long) = "exam/$id"
    fun examEdit(id: Long? = null, date: LocalDate? = null) = "exam_edit?id=${id ?: -1}&date=${date ?: ""}"
    fun assignmentEdit(id: Long? = null, date: LocalDate? = null) = "assignment_edit?id=${id ?: -1}&date=${date ?: ""}"
}

private data class TopLevel(val route: String, val label: String, val selected: ImageVector, val unselected: ImageVector)

private val topLevels = listOf(
    TopLevel(Routes.HOME, "Home", Icons.Rounded.Home, Icons.Outlined.Home),
    TopLevel(Routes.EXAMS, "Exams", Icons.Rounded.School, Icons.Outlined.School),
    TopLevel(Routes.ASSIGNMENTS, "Assignments", Icons.AutoMirrored.Rounded.Assignment, Icons.AutoMirrored.Outlined.Assignment),
    TopLevel(Routes.CALENDAR, "Calendar", Icons.Rounded.CalendarMonth, Icons.Outlined.CalendarMonth),
    TopLevel(Routes.SETTINGS, "Settings", Icons.Rounded.Settings, Icons.Outlined.Settings),
)

/** Navigation actions shared by all screens. */
class AppNavigator(private val nav: NavHostController) {
    fun back() { nav.popBackStack() }
    fun closeExamDetail() { nav.popBackStack(Routes.EXAM_DETAIL, inclusive = true) }
    fun openExam(id: Long) = nav.navigate(Routes.examDetail(id))
    fun editExam(id: Long) = nav.navigate(Routes.examEdit(id))
    fun addExam(date: LocalDate? = null) = nav.navigate(Routes.examEdit(date = date))
    fun openAssignment(id: Long) = nav.navigate(Routes.assignmentEdit(id))
    fun addAssignment(date: LocalDate? = null) = nav.navigate(Routes.assignmentEdit(date = date))
    fun search() = nav.navigate(Routes.SEARCH)
    fun subjects() = nav.navigate(Routes.SUBJECTS)
    fun open(item: AgendaItem) = when (item) {
        is AgendaItem.Exam -> openExam(item.data.exam.id)
        is AgendaItem.Assignment -> openAssignment(item.data.assignment.id)
    }
    fun topLevel(route: String) = nav.navigate(route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun StudentReminderRoot(
    container: AppContainer,
    settings: AppSettings,
    pendingRoute: String?,
    onRouteConsumed: () -> Unit,
) {
    val navController = rememberNavController()
    val navigator = remember(navController) { AppNavigator(navController) }
    val eventsVm: EventsViewModel = viewModel(
        factory = viewModelFactory { initializer { EventsViewModel(container.repository) } }
    )
    val settingsVmFactory = viewModelFactory {
        initializer { SettingsViewModel(container.settingsRepository, container.repository, container.backupManager) }
    }

    // Ask for notification permission once on Android 13+.
    val context = LocalContext.current
    var askedPermission by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (!askedPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            askedPermission = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Open the screen requested by a tapped notification.
    LaunchedEffect(pendingRoute) {
        if (pendingRoute != null) {
            if (pendingRoute != Routes.HOME) navController.navigate(pendingRoute)
            onRouteConsumed()
        }
    }

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = topLevels.any { it.route == currentRoute }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                    topLevels.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigator.topLevel(item.route) },
                            icon = { Icon(if (selected) item.selected else item.unselected, contentDescription = item.label) },
                            label = { Text(item.label, maxLines = 1) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        val state by eventsVm.state.collectAsStateWithLifecycle()
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn() + scaleIn(initialScale = 0.97f) },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { fadeOut() + scaleOut(targetScale = 0.97f) },
        ) {
            composable(Routes.HOME) {
                HomeScreen(state = state, settings = settings, navigator = navigator, events = eventsVm)
            }
            composable(Routes.EXAMS) {
                ExamsScreen(state = state, navigator = navigator)
            }
            composable(Routes.ASSIGNMENTS) {
                AssignmentsScreen(state = state, navigator = navigator, events = eventsVm)
            }
            composable(Routes.CALENDAR) {
                CalendarScreen(state = state, navigator = navigator)
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(vm = viewModel(factory = settingsVmFactory), navigator = navigator)
            }
            composable(Routes.SEARCH) {
                SearchScreen(state = state, navigator = navigator)
            }
            composable(Routes.SUBJECTS) {
                SubjectsScreen(events = eventsVm, navigator = navigator)
            }
            composable(
                Routes.EXAM_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: -1L
                ExamDetailScreen(examId = id, state = state, navigator = navigator, events = eventsVm)
            }
            composable(
                Routes.EXAM_EDIT,
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("date") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                val id = entry.arguments?.getLong("id")?.takeIf { it > 0 }
                val date = entry.arguments?.getString("date")?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                val vm: ExamEditorViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { ExamEditorViewModel(id, date, container.repository, container.settingsRepository) }
                    }
                )
                ExamEditScreen(vm = vm, subjects = state.subjects, navigator = navigator, events = eventsVm, examId = id)
            }
            composable(
                Routes.ASSIGNMENT_EDIT,
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("date") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                val id = entry.arguments?.getLong("id")?.takeIf { it > 0 }
                val date = entry.arguments?.getString("date")?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                val vm: AssignmentEditorViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { AssignmentEditorViewModel(id, date, container.repository, container.settingsRepository) }
                    }
                )
                AssignmentEditScreen(vm = vm, subjects = state.subjects, navigator = navigator, events = eventsVm, assignmentId = id)
            }
        }
    }
}
