package com.motocare.app.ui.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motocare.app.backup.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupUiState(val working: Boolean = false, val message: String? = null)

@HiltViewModel
class BackupViewModel @Inject constructor(private val repository: BackupRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(BackupUiState())
    val uiState = mutableState.asStateFlow()

    fun backup(uri: Uri) = runOperation("Backup created") { repository.writeJson(uri) }
    fun restore(uri: Uri) = runOperation("Backup restored") { repository.restoreJson(uri) }
    fun export(uri: Uri, table: String) = runOperation("CSV exported") { repository.writeCsv(uri, table) }

    private fun runOperation(success: String, block: suspend () -> Unit) = viewModelScope.launch {
        mutableState.value = BackupUiState(working = true)
        mutableState.value = runCatching { block(); BackupUiState(message = success) }
            .getOrElse { BackupUiState(message = it.message ?: "Operation failed") }
    }
}
