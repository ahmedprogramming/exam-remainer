package com.studentreminder.app.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentreminder.app.data.AppSettings
import com.studentreminder.app.data.BackupManager
import com.studentreminder.app.data.BackupSummary
import com.studentreminder.app.data.SettingsRepository
import com.studentreminder.app.data.StudentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val repository: StudentRepository,
    private val backupManager: BackupManager,
) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun update(transform: (AppSettings) -> AppSettings) = viewModelScope.launch {
        val before = settingsRepository.current()
        settingsRepository.update(transform)
        val after = settingsRepository.current()
        // "Same day" reminder time changed → recompute all alarm times.
        if (before.sameDayHour != after.sameDayHour) repository.rescheduleAll()
    }

    fun export(uri: Uri, onResult: (Result<BackupSummary>) -> Unit) = viewModelScope.launch {
        onResult(backupManager.export(uri))
    }

    fun restore(uri: Uri, onResult: (Result<BackupSummary>) -> Unit) = viewModelScope.launch {
        onResult(backupManager.restore(uri))
    }

    fun rescheduleAll() = viewModelScope.launch { repository.rescheduleAll() }
}
