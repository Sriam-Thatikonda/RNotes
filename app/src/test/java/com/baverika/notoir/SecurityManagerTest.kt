package com.baverika.notoir

import com.baverika.notoir.util.security.PasswordManager
import com.baverika.notoir.util.security.VaultStorage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeVaultStorage : VaultStorage {
    private val stringMap = mutableMapOf<String, String>()
    private val boolMap = mutableMapOf<String, Boolean>()

    override fun getString(key: String): String? = stringMap[key]
    override fun putString(key: String, value: String) { stringMap[key] = value }
    override fun getBoolean(key: String): Boolean = boolMap[key] ?: false
    override fun putBoolean(key: String, value: Boolean) { boolMap[key] = value }
    override fun contains(key: String): Boolean = stringMap.containsKey(key) || boolMap.containsKey(key)
    override fun remove(key: String) {
        stringMap.remove(key)
        boolMap.remove(key)
    }

    fun containsValue(value: String): Boolean = stringMap.values.contains(value)
}

class SecurityManagerTest {

    private lateinit var storage: FakeVaultStorage
    private lateinit var passwordManager: PasswordManager

    @Before
    fun setUp() {
        storage = FakeVaultStorage()
        passwordManager = PasswordManager(storage)
    }

    @Test
    fun `isPasswordConfigured returns false when not setup`() {
        assertFalse(passwordManager.isPasswordConfigured())
    }

    @Test
    fun `setPassword fails if warning not acknowledged`() {
        val result = passwordManager.setPassword("SecretPass123", acknowledgedWarning = false)
        assertFalse(result)
        assertFalse(passwordManager.isPasswordConfigured())
    }

    @Test
    fun `setPassword succeeds when warning is acknowledged and authenticates correctly`() {
        val result = passwordManager.setPassword("MySuperSecretVaultPass", acknowledgedWarning = true)
        assertTrue(result)
        assertTrue(passwordManager.isPasswordConfigured())
        assertTrue(passwordManager.isLockEnabled())

        // Correct password authentication
        assertTrue(passwordManager.verifyPassword("MySuperSecretVaultPass"))

        // Incorrect password rejection
        assertFalse(passwordManager.verifyPassword("WrongPassword"))
        assertFalse(passwordManager.verifyPassword("mysupersecretvaultpass"))
        assertFalse(passwordManager.verifyPassword(""))
    }

    @Test
    fun `plain text password is never stored in vault storage`() {
        val rawPassword = "SensitiveRawPasswordToNeverStore"
        passwordManager.setPassword(rawPassword, acknowledgedWarning = true)

        assertFalse("Storage must not contain plain text password", storage.containsValue(rawPassword))
    }

    @Test
    fun `skipInitialSetup marks setup dismissed and lock disabled`() {
        passwordManager.skipInitialSetup()
        assertTrue(passwordManager.isSetupDismissed())
        assertFalse(passwordManager.isLockEnabled())
        assertFalse(passwordManager.isPasswordConfigured())
    }

    @Test
    fun `disableLock with correct password disables lock but preserves configured credentials`() {
        passwordManager.setPassword("Secret123", acknowledgedWarning = true)
        assertTrue(passwordManager.isLockEnabled())

        val result = passwordManager.disableLock("Secret123")
        assertTrue(result)
        assertFalse(passwordManager.isLockEnabled())
        assertTrue(passwordManager.isPasswordConfigured())
    }

    @Test
    fun `disableLock with wrong password fails and keeps lock on`() {
        passwordManager.setPassword("Secret123", acknowledgedWarning = true)
        val result = passwordManager.disableLock("WrongPass")
        assertFalse(result)
        assertTrue(passwordManager.isLockEnabled())
    }

    @Test
    fun `enableLock restores lock state when password is configured`() {
        passwordManager.setPassword("Secret123", acknowledgedWarning = true)
        passwordManager.disableLock("Secret123")
        assertFalse(passwordManager.isLockEnabled())

        val result = passwordManager.enableLock()
        assertTrue(result)
        assertTrue(passwordManager.isLockEnabled())
    }

    @Test
    fun `removePassword with correct password wipes stored credentials completely`() {
        passwordManager.setPassword("Secret123", acknowledgedWarning = true)
        val result = passwordManager.removePassword("Secret123")
        assertTrue(result)
        assertFalse(passwordManager.isPasswordConfigured())
        assertFalse(passwordManager.isLockEnabled())
        assertFalse(passwordManager.verifyPassword("Secret123"))
    }

    @Test
    fun `changePassword updates credentials correctly`() {
        passwordManager.setPassword("OldSecret1", acknowledgedWarning = true)
        val result = passwordManager.changePassword("OldSecret1", "NewSecret2", acknowledgedWarning = true)
        assertTrue(result)
        assertFalse(passwordManager.verifyPassword("OldSecret1"))
        assertTrue(passwordManager.verifyPassword("NewSecret2"))
    }
}
