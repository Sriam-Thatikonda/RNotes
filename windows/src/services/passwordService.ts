/**
 * Password Manager & Security Service for RNotes Windows Desktop
 * Mirrors Android's PasswordManager.kt using PBKDF2-SHA256 with 20,000 iterations.
 */

const KEY_SALT = 'rnotes_vault_salt_hex';
const KEY_HASH = 'rnotes_vault_hash_hex';
const KEY_ACKNOWLEDGED = 'rnotes_vault_warning_acknowledged';
const KEY_LOCK_ENABLED = 'rnotes_vault_lock_enabled';
const KEY_SETUP_DISMISSED = 'rnotes_vault_setup_dismissed';

const ITERATIONS = 20000;
const KEY_LENGTH_BITS = 256;
const SALT_BYTES = 16;

function arrayBufferToHex(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  return Array.from(bytes)
    .map(b => b.toString(16).padStart(2, '0'))
    .join('');
}

function hexToArrayBuffer(hex: string): ArrayBuffer {
  const bytes = new Uint8Array(hex.length / 2);
  for (let i = 0; i < hex.length; i += 2) {
    bytes[i / 2] = parseInt(hex.substring(i, i + 2), 16);
  }
  return bytes.buffer;
}

async function hashPassword(password: string, saltBytes: Uint8Array): Promise<string> {
  const encoder = new TextEncoder();
  const passwordKey = await crypto.subtle.importKey(
    'raw',
    encoder.encode(password),
    { name: 'PBKDF2' },
    false,
    ['deriveBits', 'deriveKey']
  );

  const derivedBits = await crypto.subtle.deriveBits(
    {
      name: 'PBKDF2',
      salt: saltBytes.buffer as ArrayBuffer,
      iterations: ITERATIONS,
      hash: 'SHA-256'
    },
    passwordKey,
    KEY_LENGTH_BITS
  );

  return arrayBufferToHex(derivedBits);
}

export const PasswordService = {
  isPasswordConfigured(): boolean {
    const hasSalt = !!localStorage.getItem(KEY_SALT);
    const hasHash = !!localStorage.getItem(KEY_HASH);
    const hasAck = localStorage.getItem(KEY_ACKNOWLEDGED) === 'true';
    return hasSalt && hasHash && hasAck;
  },

  isSetupDismissed(): boolean {
    return localStorage.getItem(KEY_SETUP_DISMISSED) === 'true';
  },

  isLockEnabled(): boolean {
    const isConfigured = this.isPasswordConfigured();
    const stored = localStorage.getItem(KEY_LOCK_ENABLED);
    if (stored !== null) {
      return stored === 'true' && isConfigured;
    }
    return isConfigured;
  },

  setLockEnabled(enabled: boolean): void {
    localStorage.setItem(KEY_LOCK_ENABLED, enabled ? 'true' : 'false');
  },

  skipInitialSetup(): void {
    localStorage.setItem(KEY_SETUP_DISMISSED, 'true');
    localStorage.setItem(KEY_LOCK_ENABLED, 'false');
  },

  async verifyPassword(password: string): Promise<boolean> {
    const storedSaltHex = localStorage.getItem(KEY_SALT);
    const storedHashHex = localStorage.getItem(KEY_HASH);
    if (!storedSaltHex || !storedHashHex) return false;

    try {
      const saltBytes = new Uint8Array(hexToArrayBuffer(storedSaltHex));
      const calculatedHashHex = await hashPassword(password, saltBytes);
      return calculatedHashHex.toLowerCase() === storedHashHex.toLowerCase();
    } catch (e) {
      console.error('Password verification error', e);
      return false;
    }
  },

  async setPassword(password: string, acknowledgedWarning: boolean): Promise<boolean> {
    if (!acknowledgedWarning || !password.trim()) {
      return false;
    }

    try {
      const saltBytes = new Uint8Array(SALT_BYTES);
      crypto.getRandomValues(saltBytes);

      const hashHex = await hashPassword(password, saltBytes);
      const saltHex = arrayBufferToHex(saltBytes.buffer);

      localStorage.setItem(KEY_SALT, saltHex);
      localStorage.setItem(KEY_HASH, hashHex);
      localStorage.setItem(KEY_ACKNOWLEDGED, 'true');
      localStorage.setItem(KEY_LOCK_ENABLED, 'true');
      localStorage.setItem(KEY_SETUP_DISMISSED, 'true');
      return true;
    } catch (e) {
      console.error('Failed to set password', e);
      return false;
    }
  },

  async changePassword(
    oldPassword: string,
    newPassword: string,
    acknowledgedWarning: boolean
  ): Promise<{ success: boolean; error?: string }> {
    const isOldValid = await this.verifyPassword(oldPassword);
    if (!isOldValid) {
      return { success: false, error: 'Current password is incorrect' };
    }

    if (!newPassword.trim()) {
      return { success: false, error: 'New password cannot be blank' };
    }

    const success = await this.setPassword(newPassword, acknowledgedWarning);
    return success ? { success: true } : { success: false, error: 'Failed to update password' };
  },

  async removePassword(currentPassword: string): Promise<boolean> {
    const isCurrentValid = await this.verifyPassword(currentPassword);
    if (!isCurrentValid) return false;

    localStorage.removeItem(KEY_SALT);
    localStorage.removeItem(KEY_HASH);
    localStorage.removeItem(KEY_ACKNOWLEDGED);
    localStorage.setItem(KEY_LOCK_ENABLED, 'false');
    localStorage.setItem(KEY_SETUP_DISMISSED, 'true');
    return true;
  },

  async disableLock(currentPassword: string): Promise<boolean> {
    const isCurrentValid = await this.verifyPassword(currentPassword);
    if (!isCurrentValid) return false;

    this.setLockEnabled(false);
    return true;
  },

  enableLock(): boolean {
    if (!this.isPasswordConfigured()) return false;
    this.setLockEnabled(true);
    return true;
  }
};
