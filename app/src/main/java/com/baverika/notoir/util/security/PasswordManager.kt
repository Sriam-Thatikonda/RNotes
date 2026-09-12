package com.baverika.notoir.util.security

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

interface VaultStorage {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun getBoolean(key: String): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun contains(key: String): Boolean
    fun remove(key: String)
}

class SharedPrefsVaultStorage(context: Context) : VaultStorage {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("notoir_vault_security", Context.MODE_PRIVATE)

    override fun getString(key: String): String? = prefs.getString(key, null)
    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).commit()
    }
    override fun getBoolean(key: String): Boolean = prefs.getBoolean(key, false)
    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).commit()
    }
    override fun contains(key: String): Boolean = prefs.contains(key)
    override fun remove(key: String) {
        prefs.edit().remove(key).commit()
    }
}

class PasswordManager(private val storage: VaultStorage) {
    constructor(context: Context) : this(SharedPrefsVaultStorage(context))

    companion object {
        private const val KEY_SALT = "vault_salt_hex"
        private const val KEY_HASH = "vault_hash_hex"
        private const val KEY_ACKNOWLEDGED = "vault_warning_acknowledged"
        private const val KEY_LOCK_ENABLED = "vault_lock_enabled"
        private const val KEY_SETUP_DISMISSED = "vault_setup_dismissed"

        private const val ITERATIONS = 20_000
        private const val KEY_LENGTH = 256
        private const val SALT_BYTES = 16
    }

    fun isPasswordConfigured(): Boolean {
        val hasSalt = !storage.getString(KEY_SALT).isNullOrBlank()
        val hasHash = !storage.getString(KEY_HASH).isNullOrBlank()
        val hasAck = storage.getBoolean(KEY_ACKNOWLEDGED)
        return hasSalt && hasHash && hasAck
    }

    fun isSetupDismissed(): Boolean {
        return storage.getBoolean(KEY_SETUP_DISMISSED)
    }

    fun isLockEnabled(): Boolean {
        return if (storage.contains(KEY_LOCK_ENABLED)) {
            storage.getBoolean(KEY_LOCK_ENABLED) && isPasswordConfigured()
        } else {
            isPasswordConfigured()
        }
    }

    fun setLockEnabled(enabled: Boolean) {
        storage.putBoolean(KEY_LOCK_ENABLED, enabled)
    }

    fun skipInitialSetup() {
        storage.putBoolean(KEY_SETUP_DISMISSED, true)
        storage.putBoolean(KEY_LOCK_ENABLED, false)
    }

    fun disableLock(currentPassword: String): Boolean {
        if (!verifyPassword(currentPassword)) {
            return false
        }
        setLockEnabled(false)
        return true
    }

    fun enableLock(): Boolean {
        if (!isPasswordConfigured()) {
            return false
        }
        setLockEnabled(true)
        return true
    }

    fun removePassword(currentPassword: String): Boolean {
        if (!verifyPassword(currentPassword)) {
            return false
        }
        storage.remove(KEY_SALT)
        storage.remove(KEY_HASH)
        storage.remove(KEY_ACKNOWLEDGED)
        storage.putBoolean(KEY_LOCK_ENABLED, false)
        storage.putBoolean(KEY_SETUP_DISMISSED, true)
        return true
    }

    fun changePassword(oldPassword: String, newPassword: String, acknowledgedWarning: Boolean): Boolean {
        if (!verifyPassword(oldPassword)) {
            return false
        }
        return setPassword(newPassword, acknowledgedWarning)
    }

    fun setPassword(password: String, acknowledgedWarning: Boolean): Boolean {
        if (!acknowledgedWarning || password.isBlank()) {
            return false
        }

        val salt = ByteArray(SALT_BYTES).apply {
            SecureRandom().nextBytes(this)
        }
        val hash = hashPassword(password.toCharArray(), salt)

        storage.putString(KEY_SALT, bytesToHex(salt))
        storage.putString(KEY_HASH, bytesToHex(hash))
        storage.putBoolean(KEY_ACKNOWLEDGED, true)
        storage.putBoolean(KEY_LOCK_ENABLED, true)
        storage.putBoolean(KEY_SETUP_DISMISSED, true)
        return true
    }

    fun verifyPassword(password: String): Boolean {
        val saltHex = storage.getString(KEY_SALT) ?: return false
        val hashHex = storage.getString(KEY_HASH) ?: return false

        val salt = hexToBytes(saltHex)
        val expectedHash = hexToBytes(hashHex)
        val computedHash = hashPassword(password.toCharArray(), salt)

        return MessageDigest.isEqual(expectedHash, computedHash)
    }

    private fun hashPassword(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return try {
            factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        val hexArray = "0123456789ABCDEF".toCharArray()
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = hexArray[v ushr 4]
            hexChars[i * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) +
                    Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
