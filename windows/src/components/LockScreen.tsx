import React, { useState, useRef, useEffect } from 'react';
import { Lock, LockOpen, Eye, EyeOff, AlertCircle } from 'lucide-react';
import { PasswordService } from '../services/passwordService';

interface LockScreenProps {
  onUnlock: () => void;
}

export const LockScreen: React.FC<LockScreenProps> = ({ onUnlock }) => {
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isShaking, setIsShaking] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  const handleUnlock = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!password) {
      setError('Please enter your password');
      triggerShake();
      return;
    }

    setIsSubmitting(true);
    setError(null);

    const isValid = await PasswordService.verifyPassword(password);
    setIsSubmitting(false);

    if (isValid) {
      setPassword('');
      setError(null);
      onUnlock();
    } else {
      setError('Incorrect password. Try again.');
      triggerShake();
      inputRef.current?.focus();
    }
  };

  const triggerShake = () => {
    setIsShaking(true);
    setTimeout(() => setIsShaking(false), 500);
  };

  return (
    <div className="lock-screen-container">
      <div className={`lock-card ${isShaking ? 'shake-anim' : ''}`}>
        <div className="lock-icon-wrapper">
          <Lock size={36} className="lock-badge-icon" />
        </div>

        <h1 className="lock-title">R-Notes</h1>
        <p className="lock-subtitle">Enter your vault password to access your notes</p>

        <form onSubmit={handleUnlock} className="lock-form">
          <div className="lock-input-group">
            <input
              ref={inputRef}
              type={showPassword ? 'text' : 'password'}
              placeholder="Vault Password"
              value={password}
              onChange={e => {
                setPassword(e.target.value);
                if (error) setError(null);
              }}
              className={`lock-input ${error ? 'input-error' : ''}`}
              autoComplete="current-password"
            />
            <button
              type="button"
              className="toggle-pw-btn"
              onClick={() => setShowPassword(!showPassword)}
              title={showPassword ? 'Hide password' : 'Show password'}
              tabIndex={-1}
            >
              {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
            </button>
          </div>

          {error && (
            <div className="lock-error-msg">
              <AlertCircle size={15} />
              <span>{error}</span>
            </div>
          )}

          <button
            type="submit"
            className="btn btn-primary lock-submit-btn"
            disabled={isSubmitting || !password.trim()}
          >
            {isSubmitting ? 'Verifying...' : (
              <>
                <LockOpen size={18} />
                <span>Unlock Vault</span>
              </>
            )}
          </button>
        </form>
      </div>
    </div>
  );
};
