import React from 'react';

interface SnackbarProps {
  message: string | null;
  actionLabel?: string | null;
  onAction?: () => void;
  onClose?: () => void;
}

export const Snackbar: React.FC<SnackbarProps> = ({
  message,
  actionLabel,
  onAction,
}) => {
  if (!message) return null;

  return (
    <div className="snackbar-container">
      <span>{message}</span>
      {actionLabel && (
        <button className="snackbar-undo-btn" onClick={onAction}>
          {actionLabel}
        </button>
      )}
    </div>
  );
};
