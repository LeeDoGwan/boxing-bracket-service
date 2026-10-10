import { useTranslation } from 'react-i18next';
import { translate as t, resolveMessage } from '../i18n';
import { useEffect, useRef } from 'react';

export function ConfirmDialog({
  cancelLabel = t('common:cancel'),
  confirmLabel = t('common:confirm'),
  description,
  onCancel,
  onConfirm,
  title,
  busy = false,
}) {
  useTranslation();
  const dialogRef = useRef(null);
  const confirmRef = useRef(null);
  const onCancelRef = useRef(onCancel);

  useEffect(() => {
    onCancelRef.current = onCancel;
  }, [onCancel]);

  useEffect(() => {
    const previousActiveElement = document.activeElement;
    confirmRef.current?.focus();
    function handleKeyDown(event) {
      if (event.key === 'Escape' && !busy) {
        onCancelRef.current();
      }
    }
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      previousActiveElement?.focus?.();
    };
  }, [busy]);

  return (
    <div className="dialog-backdrop" role="presentation">
      <section
        aria-labelledby="confirm-dialog-title"
        aria-modal="true"
        className="confirm-dialog"
        ref={dialogRef}
        role="dialog"
      >
        <h2 id="confirm-dialog-title">{resolveMessage(title)}</h2>
        <p>{resolveMessage(description)}</p>
        <div className="confirm-dialog-actions">
          <button className="secondary-button" disabled={busy} onClick={onCancel} type="button">
            {resolveMessage(cancelLabel)}
          </button>
          <button className="danger-button" disabled={busy} onClick={onConfirm} ref={confirmRef} type="button">
            {busy ? t('common:processing') : resolveMessage(confirmLabel)}
          </button>
        </div>
      </section>
    </div>
  );
}
