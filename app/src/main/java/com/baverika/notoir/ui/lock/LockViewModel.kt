package com.baverika.notoir.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baverika.notoir.util.security.PasswordManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LockState {
    data object Checking : LockState
    data object NeedsSetup : LockState
    data object Locked : LockState
    data object Unlocked : LockState
}

class LockViewModel(
    private val passwordManager: PasswordManager
) : ViewModel() {

    private val _lockState = MutableStateFlow<LockState>(LockState.Checking)
    val lockState: StateFlow<LockState> = _lockState.asStateFlow()

    private val _unlockError = MutableStateFlow<String?>(null)
    val unlockError: StateFlow<String?> = _unlockError.asStateFlow()

    private val _setupError = MutableStateFlow<String?>(null)
    val setupError: StateFlow<String?> = _setupError.asStateFlow()

    private val _isLockEnabled = MutableStateFlow(passwordManager.isLockEnabled())
    val isLockEnabled: StateFlow<Boolean> = _isLockEnabled.asStateFlow()

    private val _isPasswordConfigured = MutableStateFlow(passwordManager.isPasswordConfigured())
    val isPasswordConfigured: StateFlow<Boolean> = _isPasswordConfigured.asStateFlow()

    init {
        checkLockStatus()
    }

    fun checkLockStatus() {
        viewModelScope.launch {
            _isLockEnabled.value = passwordManager.isLockEnabled()
            _isPasswordConfigured.value = passwordManager.isPasswordConfigured()

            if (!passwordManager.isPasswordConfigured()) {
                if (passwordManager.isSetupDismissed()) {
                    _lockState.value = LockState.Unlocked
                } else {
                    _lockState.value = LockState.NeedsSetup
                }
            } else if (!passwordManager.isLockEnabled()) {
                _lockState.value = LockState.Unlocked
            } else if (_lockState.value != LockState.Unlocked) {
                _lockState.value = LockState.Locked
            }
        }
    }

    fun skipSetup() {
        passwordManager.skipInitialSetup()
        _isLockEnabled.value = false
        _isPasswordConfigured.value = false
        _lockState.value = LockState.Unlocked
    }

    fun setupPassword(password: String, confirm: String, acknowledged: Boolean): Boolean {
        _setupError.value = null
        if (password.length < 4) {
            _setupError.value = "Password must be at least 4 characters long."
            return false
        }
        if (password != confirm) {
            _setupError.value = "Passwords do not match."
            return false
        }
        if (!acknowledged) {
            _setupError.value = "You must acknowledge that passwords cannot be recovered."
            return false
        }

        val success = passwordManager.setPassword(password, acknowledged)
        if (success) {
            _isLockEnabled.value = true
            _isPasswordConfigured.value = true
            _lockState.value = LockState.Unlocked
            return true
        } else {
            _setupError.value = "Failed to securely save password. Please try again."
            return false
        }
    }

    fun disableLock(currentPassword: String): Boolean {
        val success = passwordManager.disableLock(currentPassword)
        if (success) {
            _isLockEnabled.value = false
        }
        return success
    }

    fun enableLock(): Boolean {
        val success = passwordManager.enableLock()
        if (success) {
            _isLockEnabled.value = true
        }
        return success
    }

    fun removePassword(currentPassword: String): Boolean {
        val success = passwordManager.removePassword(currentPassword)
        if (success) {
            _isLockEnabled.value = false
            _isPasswordConfigured.value = false
        }
        return success
    }

    fun changePassword(oldPassword: String, newPassword: String, confirmNew: String, acknowledged: Boolean): Pair<Boolean, String?> {
        if (newPassword.length < 4) {
            return Pair(false, "New password must be at least 4 characters long.")
        }
        if (newPassword != confirmNew) {
            return Pair(false, "New passwords do not match.")
        }
        if (!acknowledged) {
            return Pair(false, "You must acknowledge the warning.")
        }
        val success = passwordManager.changePassword(oldPassword, newPassword, acknowledged)
        if (success) {
            _isLockEnabled.value = true
            _isPasswordConfigured.value = true
            return Pair(true, null)
        } else {
            return Pair(false, "Incorrect current password.")
        }
    }

    fun unlock(password: String): Boolean {
        _unlockError.value = null
        if (password.isBlank()) {
            _unlockError.value = "Please enter your password."
            return false
        }

        val isValid = passwordManager.verifyPassword(password)
        if (isValid) {
            _lockState.value = LockState.Unlocked
            return true
        } else {
            _unlockError.value = "Incorrect password. Access denied."
            return false
        }
    }

    fun lockNow() {
        if (passwordManager.isPasswordConfigured() && passwordManager.isLockEnabled()) {
            _lockState.value = LockState.Locked
            _unlockError.value = null
        }
    }

    fun onAppBackgrounded() {
        if (passwordManager.isPasswordConfigured() && passwordManager.isLockEnabled()) {
            _lockState.value = LockState.Locked
            _unlockError.value = null
        }
    }

    class Factory(private val passwordManager: PasswordManager) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LockViewModel(passwordManager) as T
        }
    }
}
