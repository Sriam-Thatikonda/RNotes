import React, { useState } from 'react';
import { Shield, AlertTriangle, Eye, EyeOff, X } from 'lucide-react';
import { PasswordService } from '../services/passwordService';

interface SetupPasswordModalProps {
  isOpen: boolean;
  isInitialSetup?: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export const SetupPasswordModal: React.FC<SetupPasswordModalProps> = ({
  isOpen,
  isInitialSetup = false,
  onClose,
  onSuccess
}) => {
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [acknowledged, setAcknowledged] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!password.trim()) {
      setError('Password cannot be empty');
      return;
    }

    if (password.length < 4) {
      setError('Password must be at least 4 characters long');
      return;
    }

    if (password !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    if (!acknowledged) {
      setError('You must acknowledge the irrecoverable password warning');
      return;
    }

    setIsSubmitting(true);
    setError(null);

    const success = await PasswordService.setPassword(password, acknowledged);
    setIsSubmitting(false);

    if (success) {
      setPassword('');
      setConfirmPassword('');
      setAcknowledged(false);
      onSuccess();
    } else {
      setError('Failed to set password. Please try again.');
    }
  };

  const handleSkip = () => {
    if (isInitialSetup) {
      PasswordService.skipInitialSetup();
    }
    onClose();
  };

  return (
    <div className="dialog-overlay" onClick={handleSkip}>
      <div className="dialog-content setup-password-card" onClick={e => e.stopPropagation()}>
        <div className="dialog-header">
          <div className="setup-pw-icon-badge">
            <Shield size={24} className="shield-icon" />
          </div>
          <h3 className="dialog-title">Set Master Password</h3>
          <button className="icon-btn close-btn" onClick={handleSkip} title="Close">
            <X size={18} />
          </button>
        </div>

        <p className="dialog-message">
          Set a master password to encrypt and protect your private notes.
        </p>

        {/* Irrecoverable Warning Card */}
        <div className="warning-card">
          <div className="warning-card-header">
            <AlertTriangle size={18} className="warning-icon" />
            <span className="warning-title">NO PASSWORD RECOVERY</span>
          </div>
          <p className="warning-body">
            Your password cannot be recovered. If you forget it, your protected notes cannot be accessed.
            There is no reset email, security question, or backdoor.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="setup-password-form">
          {/* Checkbox */}
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
              I understand that if I lose my password, my notes cannot be recovered.
            </span>
          </label>

          {/* Password Input */}
          <div className="form-group">
            <label className="form-label">Master Password</label>
            <div className="input-with-eye">
              <input
                type={showPassword ? 'text' : 'password'}
                placeholder="Enter password"
                value={password}
                onChange={e => {
                  setPassword(e.target.value);
                  if (error) setError(null);
                }}
                className="text-input"
                autoComplete="new-password"
              />
              <button
                type="button"
                className="toggle-pw-btn"
                onClick={() => setShowPassword(!showPassword)}
                tabIndex={-1}
              >
                {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
          </div>

          {/* Confirm Password Input */}
          <div className="form-group">
            <label className="form-label">Confirm Password</label>
            <div className="input-with-eye">
              <input
                type={showConfirm ? 'text' : 'password'}
                placeholder="Re-enter password"
                value={confirmPassword}
                onChange={e => {
                  setConfirmPassword(e.target.value);
                  if (error) setError(null);
                }}
                className="text-input"
                autoComplete="new-password"
              />
              <button
                type="button"
                className="toggle-pw-btn"
                onClick={() => setShowConfirm(!showConfirm)}
                tabIndex={-1}
              >
                {showConfirm ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
          </div>

          {error && <div className="dialog-error-msg">{error}</div>}

          <div className="dialog-actions">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={handleSkip}
              disabled={isSubmitting}
            >
              {isInitialSetup ? 'Skip for Now' : 'Cancel'}
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={isSubmitting || !password || !confirmPassword || !acknowledged}
            >
              {isSubmitting ? 'Setting...' : 'Set Password'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
