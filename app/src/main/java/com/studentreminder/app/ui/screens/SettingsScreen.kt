@file:OptIn(ExperimentalMaterial3Api::class)

package com.studentreminder.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import com.studentreminder.app.StudentReminderApp
import com.studentreminder.app.data.AccentColor
import com.studentreminder.app.data.ReminderOption
import com.studentreminder.app.data.ThemeMode
import com.studentreminder.app.reminders.NotificationHelper
import com.studentreminder.app.ui.AppNavigator
import com.studentreminder.app.ui.SettingsViewModel
import com.studentreminder.app.ui.components.ConfirmDialog
import com.studentreminder.app.ui.components.ReminderSelector
import com.studentreminder.app.ui.theme.AppColors
import com.studentreminder.app.ui.theme.LocalFormatters
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun SettingsScreen(vm: SettingsViewModel, navigator: AppNavigator) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val f = LocalFormatters.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun toast(msg: String) { scope.launch { snackbar.showSnackbar(msg) } }

    // Re-read system permission states whenever the screen resumes (e.g. back from system settings).
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val status = remember(lifecycleState) { SystemStatus.read(context) }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) openAppNotificationSettings(context)
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.export(uri) { r ->
            r.fold(
                { toast("Backup saved: ${it.exams} exams, ${it.assignments} assignments") },
                { toast("Backup failed: ${it.message}") },
            )
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.restore(uri) { r ->
            r.fold(
                { toast("Restored ${it.exams} exams and ${it.assignments} assignments") },
                { toast("Restore failed: ${it.message}") },
            )
        }
    }
    var confirmRestore by rememberSaveable { mutableStateOf(false) }
    var showExamDefaults by rememberSaveable { mutableStateOf(false) }
    var showAssignmentDefaults by rememberSaveable { mutableStateOf(false) }
    var showBatteryInfo by rememberSaveable { mutableStateOf(false) }
    var nameDraft by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ---------------- Profile
            SettingsGroup("Profile") {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    OutlinedTextField(
                        value = nameDraft ?: settings.userName,
                        onValueChange = { v ->
                            val clean = v.take(30)
                            nameDraft = clean
                            vm.update { it.copy(userName = clean) }
                        },
                        label = { Text("Your name") },
                        placeholder = { Text("Shown on the dashboard") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // ---------------- Appearance
            SettingsGroup("Appearance") {
                SettingLabel(Icons.Rounded.DarkMode, "Theme")
                SingleChoiceSegmentedButtonRow(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    ThemeMode.entries.forEachIndexed { i, mode ->
                        SegmentedButton(
                            selected = settings.themeMode == mode,
                            onClick = { vm.update { it.copy(themeMode = mode) } },
                            shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                        ) { Text(mode.label) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                SettingLabel(Icons.Rounded.ColorLens, "Accent color")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    AccentColor.entries.forEach { accent ->
                        val selected = settings.accent == accent && !settings.dynamicColor
                        Box(
                            Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(accent.argb))
                                .then(
                                    if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { vm.update { it.copy(accent = accent, dynamicColor = false) } },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) Icon(Icons.Rounded.Check, accent.label, tint = Color.White)
                        }
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SwitchRow(
                        icon = Icons.Rounded.WbSunny,
                        title = "Use wallpaper colors",
                        subtitle = "Material You dynamic colors (Android 12+)",
                        checked = settings.dynamicColor,
                        onChange = { v -> vm.update { it.copy(dynamicColor = v) } },
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // ---------------- Date & time
            SettingsGroup("Date & time") {
                SettingLabel(Icons.Rounded.Today, "First day of the week")
                val days = listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY)
                SingleChoiceSegmentedButtonRow(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    days.forEachIndexed { i, d ->
                        SegmentedButton(
                            selected = settings.firstDayOfWeek == d,
                            onClick = { vm.update { it.copy(firstDayOfWeek = d) } },
                            shape = SegmentedButtonDefaults.itemShape(i, days.size),
                        ) { Text(d.name.lowercase().replaceFirstChar { c -> c.uppercase() }) }
                    }
                }
                SwitchRow(
                    icon = Icons.Rounded.Schedule,
                    title = "24-hour time",
                    subtitle = "Example: " + f.time(LocalTime.of(14, 30)),
                    checked = settings.use24h,
                    onChange = { v -> vm.update { it.copy(use24h = v) } },
                )
            }

            // ---------------- Notifications
            SettingsGroup("Notifications & reminders") {
                SwitchRow(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "Reminder notifications",
                    subtitle = "Get notified before exams and deadlines",
                    checked = settings.notificationsEnabled,
                    onChange = { v -> vm.update { it.copy(notificationsEnabled = v) } },
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                StatusRow(
                    icon = Icons.Rounded.Notifications,
                    title = "Notification permission",
                    ok = status.notificationsAllowed,
                    okText = "Allowed",
                    badText = "Blocked — tap to allow",
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !status.notificationsAllowed) {
                            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else openAppNotificationSettings(context)
                    },
                )
                StatusRow(
                    icon = Icons.Rounded.Alarm,
                    title = "Exact alarms",
                    ok = status.exactAlarmsAllowed,
                    okText = "Allowed — reminders fire on time",
                    badText = "Not allowed — reminders may be late. Tap to fix",
                    onClick = { openExactAlarmSettings(context) },
                )
                StatusRow(
                    icon = Icons.Rounded.BatteryAlert,
                    title = "Battery optimization",
                    ok = status.ignoringBatteryOptimizations,
                    okText = "Unrestricted — best reliability",
                    badText = "Optimized — tap to allow background reminders",
                    onClick = { requestIgnoreBatteryOptimizations(context) },
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                // Same-day reminder time
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.WbSunny, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("\"On the same day\" reminder time", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Morning reminder on the day of the event",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { vm.update { it.copy(sameDayHour = (it.sameDayHour - 1).coerceAtLeast(0)) } }) { Text("−") }
                    Text(f.time(LocalTime.of(settings.sameDayHour, 0)), style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = { vm.update { it.copy(sameDayHour = (it.sameDayHour + 1).coerceAtMost(23)) } }) { Text("+") }
                }
                ExpandableRow(
                    icon = Icons.Rounded.School,
                    title = "Default exam reminders",
                    subtitle = summary(settings.defaultExamReminders),
                    expanded = showExamDefaults,
                    onToggle = { showExamDefaults = !showExamDefaults },
                ) {
                    ReminderSelector(
                        settings.defaultExamReminders.toSet(),
                        { set -> vm.update { it.copy(defaultExamReminders = set.toList()) } },
                        Modifier.padding(horizontal = 16.dp),
                    )
                }
                ExpandableRow(
                    icon = Icons.Rounded.Tune,
                    title = "Default assignment reminders",
                    subtitle = summary(settings.defaultAssignmentReminders),
                    expanded = showAssignmentDefaults,
                    onToggle = { showAssignmentDefaults = !showAssignmentDefaults },
                ) {
                    ReminderSelector(
                        settings.defaultAssignmentReminders.toSet(),
                        { set -> vm.update { it.copy(defaultAssignmentReminders = set.toList()) } },
                        Modifier.padding(horizontal = 16.dp),
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(
                        onClick = {
                            if (!NotificationHelper.canPostNotifications(context)) {
                                toast("Notifications are blocked. Allow them first.")
                            } else {
                                NotificationHelper.showTest(context)
                                toast("Test notification sent")
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("Send test") }
                    FilledTonalButton(onClick = { openAppNotificationSettings(context) }, modifier = Modifier.weight(1f)) {
                        Text("Sound & style")
                    }
                }
                ClickRow(Icons.Rounded.Info, "Why might reminders be late?", "Battery-saving tips for your phone") {
                    showBatteryInfo = !showBatteryInfo
                }
                AnimatedVisibility(showBatteryInfo) {
                    Text(
                        BATTERY_INFO,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 56.dp, end = 16.dp, bottom = 16.dp),
                    )
                }
            }

            // ---------------- Data
            SettingsGroup("Subjects & data") {
                ClickRow(Icons.Rounded.Category, "Manage subjects", "Rename, recolor or remove subjects") { navigator.subjects() }
                ClickRow(Icons.Rounded.FileUpload, "Back up data", "Save everything to a file (Drive, Downloads…)") {
                    exportLauncher.launch("student-reminder-backup-${LocalDate.now()}.json")
                }
                ClickRow(Icons.Rounded.FileDownload, "Restore from backup", "Replace current data with a backup file") {
                    confirmRestore = true
                }
                Text(
                    "All data is stored only on this phone. No account or internet connection is needed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 56.dp, end = 16.dp, bottom = 16.dp),
                )
            }

            Text(
                "Student Reminder 1.0 · Made for students who never want to miss a deadline.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp),
            )
        }
    }

    if (confirmRestore) {
        ConfirmDialog(
            title = "Restore from backup?",
            message = "This will replace ALL current exams, assignments, subjects and settings with the contents of the backup file.",
            confirmText = "Choose file",
            onConfirm = { restoreLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
            onDismiss = { confirmRestore = false },
        )
    }
}

private fun summary(list: List<Long>): String =
    if (list.isEmpty()) "None" else list.sortedByDescending { ReminderOption.sortKey(it) }.joinToString(", ") { ReminderOption.label(it) }

private const val BATTERY_INFO =
    "Reminders are scheduled on your phone with Android's alarm system, so they work offline. " +
        "However, some phones (Samsung, Xiaomi, Huawei, Oppo, Vivo, OnePlus…) aggressively stop apps to save battery, " +
        "which can delay or block notifications.\n\n" +
        "For the most reliable reminders:\n" +
        "• Set Battery optimization to \"Unrestricted\" (button above).\n" +
        "• Allow notifications and exact alarms.\n" +
        "• Don't swipe-close the app with \"Force stop\" — that cancels its alarms until you open it again.\n" +
        "• On Xiaomi/MIUI enable \"Autostart\"; on Samsung remove the app from \"Sleeping apps\".\n\n" +
        "Reminders are restored automatically after your phone restarts, and any reminder missed while the phone was off " +
        "is shown as soon as it's back on (if the event hasn't happened yet)."

// ---------------------------------------------------------------- System status helpers

private data class SystemStatus(
    val notificationsAllowed: Boolean,
    val exactAlarmsAllowed: Boolean,
    val ignoringBatteryOptimizations: Boolean,
) {
    companion object {
        fun read(context: Context): SystemStatus {
            val app = context.applicationContext as StudentReminderApp
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return SystemStatus(
                notificationsAllowed = NotificationHelper.canPostNotifications(context),
                exactAlarmsAllowed = app.container.scheduler.canScheduleExactAlarms(),
                ignoringBatteryOptimizations = pm.isIgnoringBatteryOptimizations(context.packageName),
            )
        }
    }
}

private fun Context.tryStart(intent: Intent, fallback: Intent? = null) {
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        if (fallback != null) runCatching { startActivity(fallback) }
    } catch (e: SecurityException) {
        if (fallback != null) runCatching { startActivity(fallback) }
    }
}

private fun appDetails(context: Context) =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))

private fun openAppNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    context.tryStart(intent, appDetails(context))
}

private fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.tryStart(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
            appDetails(context),
        )
    } else {
        context.tryStart(appDetails(context))
    }
}

@SuppressLint("BatteryLife")
private fun requestIgnoreBatteryOptimizations(context: Context) {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    if (pm.isIgnoringBatteryOptimizations(context.packageName)) {
        context.tryStart(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS), appDetails(context))
    } else {
        context.tryStart(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")),
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
        )
    }
}

// ---------------------------------------------------------------- Building blocks

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 8.dp),
        )
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(1.dp),
        ) {
            Column(Modifier.padding(vertical = 4.dp)) { content() }
        }
    }
}

@Composable
private fun SettingLabel(icon: ImageVector, title: String) {
    Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun StatusRow(icon: ImageVector, title: String, ok: Boolean, okText: String, badText: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                if (ok) okText else badText,
                style = MaterialTheme.typography.bodySmall,
                color = if (ok) AppColors.completed else AppColors.overdue,
            )
        }
        Icon(
            if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
            null,
            tint = if (ok) AppColors.completed else AppColors.overdue,
        )
    }
}

@Composable
private fun ClickRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ExpandableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
        }
        AnimatedVisibility(expanded) {
            Column(Modifier.padding(bottom = 12.dp)) { content() }
        }
    }
}
