import React, { useState } from 'react';
import { Shield, Lock, Key, Trash2, X, Eye, EyeOff, AlertTriangle, CheckCircle2 } from 'lucide-react';
import { PasswordService } from '../services/passwordService';

interface SecuritySettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
  onLockNow: () => void;
  onOpenSetup: () => void;
}

export const SecuritySettingsModal: React.FC<SecuritySettingsModalProps> = ({
  isOpen,
  onClose,
  onLockNow,
  onOpenSetup
}) => {
  const [activeSubView, setActiveSubView] = useState<'main' | 'change' | 'remove' | 'disable'>('main');

  // Form states for subviews
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [acknowledged, setAcknowledged] = useState(false);
  const [showCurrent, setShowCurrent] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!isOpen) return null;

  const isConfigured = PasswordService.isPasswordConfigured();
  const isLockEnabled = PasswordService.isLockEnabled();

  const resetForm = () => {
    setCurrentPassword('');
    setNewPassword('');
    setConfirmPassword('');
    setAcknowledged(false);
    setError(null);
    setSuccessMsg(null);
    setActiveSubView('main');
  };

  const handleToggleLock = () => {
    if (!isConfigured) {
      onClose();
      onOpenSetup();
      return;
    }

    if (isLockEnabled) {
      // Prompt password to disable
      setActiveSubView('disable');
    } else {
      PasswordService.enableLock();
      setSuccessMsg('Vault lock enabled');
      setTimeout(() => setSuccessMsg(null), 2000);
    }
  };

  const handleDisableLock = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    setError(null);

    const success = await PasswordService.disableLock(currentPassword);
    setIsSubmitting(false);

    if (success) {
      resetForm();
      setSuccessMsg('Vault lock turned off');
      setTimeout(() => setSuccessMsg(null), 2000);
    } else {
      setError('Incorrect current password');
    }
  };

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newPassword.trim()) {
      setError('New password cannot be empty');
      return;
    }
    if (newPassword.length < 4) {
      setError('New password must be at least 4 characters long');
      return;
    }
    if (newPassword !== confirmPassword) {
      setError('New passwords do not match');
      return;
    }
    if (!acknowledged) {
      setError('You must acknowledge the irrecoverable password warning');
      return;
    }

    setIsSubmitting(true);
    setError(null);

    const result = await PasswordService.changePassword(currentPassword, newPassword, acknowledged);
    setIsSubmitting(false);

    if (result.success) {
      resetForm();
      setSuccessMsg('Master password updated successfully');
      setTimeout(() => setSuccessMsg(null), 2500);
    } else {
      setError(result.error || 'Failed to change password');
    }
  };

  const handleRemovePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    setError(null);

    const success = await PasswordService.removePassword(currentPassword);
    setIsSubmitting(false);

    if (success) {
      resetForm();
      setSuccessMsg('Password protection removed');
      setTimeout(() => setSuccessMsg(null), 2000);
    } else {
      setError('Incorrect current password');
    }
  };

  return (
    <div className="dialog-overlay" onClick={() => { resetForm(); onClose(); }}>
      <div className="dialog-content security-modal-card" onClick={e => e.stopPropagation()}>
        {/* Header */}
        <div className="dialog-header">
          <div className="setup-pw-icon-badge">
            <Shield size={22} className="shield-icon" />
          </div>
          <h3 className="dialog-title">Password & Security</h3>
          <button
            className="icon-btn close-btn"
            onClick={() => { resetForm(); onClose(); }}
            title="Close"
          >
            <X size={18} />
          </button>
        </div>

        {successMsg && (
          <div className="security-success-banner">
            <CheckCircle2 size={16} />
            <span>{successMsg}</span>
          </div>
        )}

        {activeSubView === 'main' && (
          <div className="security-main-view">
            {/* Status Section */}
            <div className="security-row">
              <div className="security-row-info">
                <span className="security-row-title">Vault Password Lock</span>
                <span className="security-row-desc">
                  {isLockEnabled
                    ? 'App locks when launched, closed, or manually locked'
                    : isConfigured
                    ? 'Disabled — opens directly without password prompt'
                    : 'Not configured — notes are unencrypted'}
                </span>
              </div>
              <button
                className={`switch-toggle ${isLockEnabled ? 'switch-on' : ''}`}
                onClick={handleToggleLock}
                title={isLockEnabled ? 'Disable Lock' : 'Enable Lock'}
              >
                <span className="switch-handle" />
              </button>
            </div>

            {isConfigured ? (
              <div className="security-actions-group">
                <div className="security-section-title">Password Options</div>

                <button
                  type="button"
                  className="security-btn"
                  onClick={() => {
                    onClose();
                    onLockNow();
                  }}
                >
                  <div className="security-btn-icon-wrapper">
                    <Lock size={18} />
                  </div>
                  <div className="security-btn-text">
                    <span className="security-btn-title">Lock Vault Now</span>
                    <span className="security-btn-desc">Immediately lock the app (or press Ctrl + L)</span>
                  </div>
                  <span className="shortcut-tag">Ctrl+L</span>
                </button>

                <button
                  type="button"
                  className="security-btn"
                  onClick={() => setActiveSubView('change')}
                >
                  <div className="security-btn-icon-wrapper">
                    <Key size={18} />
                  </div>
                  <div className="security-btn-text">
                    <span className="security-btn-title">Change Master Password</span>
                    <span className="security-btn-desc">Update your current vault password</span>
                  </div>
                </button>

                <button
                  type="button"
                  className="security-btn danger-btn-hover"
                  onClick={() => setActiveSubView('remove')}
                >
                  <div className="security-btn-icon-wrapper text-danger">
                    <Trash2 size={18} />
                  </div>
                  <div className="security-btn-text">
                    <span className="security-btn-title text-danger">Remove Password Completely</span>
                    <span className="security-btn-desc">Removes password protection from R-Notes</span>
                  </div>
                </button>
              </div>
            ) : (
              <div className="security-unconfigured-box">
                <p>No master password has been configured yet.</p>
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={() => {
                    onClose();
                    onOpenSetup();
                  }}
                >
                  <Shield size={16} />
                  <span>Create Master Password</span>
                </button>
              </div>
            )}
          </div>
        )}

        {/* Change Password Subview */}
        {activeSubView === 'change' && (
          <form onSubmit={handleChangePassword} className="security-subview-form">
            <h4 className="subview-title">Change Master Password</h4>

            <div className="form-group">
              <label className="form-label">Current Password</label>
              <div className="input-with-eye">
                <input
                  type={showCurrent ? 'text' : 'password'}
                  placeholder="Enter current password"
                  value={currentPassword}
                  onChange={e => {
                    setCurrentPassword(e.target.value);
                    if (error) setError(null);
                  }}
                  className="text-input"
                  autoFocus
                />
                <button
                  type="button"
                  className="toggle-pw-btn"
                  onClick={() => setShowCurrent(!showCurrent)}
                  tabIndex={-1}
                >
                  {showCurrent ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">New Password</label>
              <div className="input-with-eye">
                <input
                  type={showNew ? 'text' : 'password'}
                  placeholder="Enter new password"
                  value={newPassword}
                  onChange={e => {
                    setNewPassword(e.target.value);
                    if (error) setError(null);
                  }}
                  className="text-input"
                />
                <button
                  type="button"
                  className="toggle-pw-btn"
                  onClick={() => setShowNew(!showNew)}
                  tabIndex={-1}
                >
                  {showNew ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Confirm New Password</label>
              <input
                type="password"
                placeholder="Confirm new password"
                value={confirmPassword}
                onChange={e => {
                  setConfirmPassword(e.target.value);
                  if (error) setError(null);
                }}
                className="text-input"
              />
            </div>

            <label className="ack-checkbox-label">
              <input
                type="checkbox"
                checked={acknowledged}
                onChange={e => {
                  setAcknowledged(e.target.checked);
                  if (error) setError(null);
                }}
                className="ack-checkbox"
              />
              <span className="ack-text">
                I understand there is no recovery if I forget this password.
              </span>
            </label>

            {error && <div className="dialog-error-msg">{error}</div>}

            <div className="dialog-actions">
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => resetForm()}
                disabled={isSubmitting}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={isSubmitting || !currentPassword || !newPassword || !confirmPassword || !acknowledged}
              >
                {isSubmitting ? 'Updating...' : 'Update Password'}
              </button>
            </div>
          </form>
        )}

        {/* Remove Password Subview */}
        {activeSubView === 'remove' && (
          <form onSubmit={handleRemovePassword} className="security-subview-form">
            <h4 className="subview-title text-danger">Remove Password Completely</h4>
            <p className="dialog-message">
              Enter your current password to remove protection. The app will open directly without asking for any password.
            </p>

            <div className="form-group">
              <label className="form-label">Current Password</label>
              <input
                type="password"
                placeholder="Enter current password"
                value={currentPassword}
                onChange={e => {
                  setCurrentPassword(e.target.value);
                  if (error) setError(null);
                }}
                className="text-input"
                autoFocus
              />
            </div>

            {error && <div className="dialog-error-msg">{error}</div>}

            <div className="dialog-actions">
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => resetForm()}
                disabled={isSubmitting}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-danger"
                disabled={isSubmitting || !currentPassword}
              >
                {isSubmitting ? 'Removing...' : 'Remove Password'}
              </button>
            </div>
          </form>
        )}

        {/* Disable Lock Subview */}
        {activeSubView === 'disable' && (
          <form onSubmit={handleDisableLock} className="security-subview-form">
            <h4 className="subview-title">Turn Off Vault Lock</h4>
            <p className="dialog-message">
              Enter your current password to turn off lock screen. Your password will remain saved for future use.
            </p>

            <div className="form-group">
              <label className="form-label">Current Password</label>
              <input
                type="password"
                placeholder="Enter current password"
                value={currentPassword}
                onChange={e => {
                  setCurrentPassword(e.target.value);
                  if (error) setError(null);
                }}
                className="text-input"
                autoFocus
              />
            </div>

            {error && <div className="dialog-error-msg">{error}</div>}

            <div className="dialog-actions">
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => resetForm()}
                disabled={isSubmitting}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={isSubmitting || !currentPassword}
              >
                {isSubmitting ? 'Verifying...' : 'Turn Off Lock'}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};
