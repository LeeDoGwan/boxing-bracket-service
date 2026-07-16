import { useEffect, useRef } from 'react';

export function ConfirmDialog({
  cancelLabel = '취소',
  confirmLabel = '확인',
  description,
  onCancel,
  onConfirm,
  title,
  busy = false,
}) {
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
        <h2 id="confirm-dialog-title">{title}</h2>
        <p>{description}</p>
        <div className="confirm-dialog-actions">
          <button className="secondary-button" disabled={busy} onClick={onCancel} type="button">
            {cancelLabel}
          </button>
          <button className="danger-button" disabled={busy} onClick={onConfirm} ref={confirmRef} type="button">
            {busy ? '처리 중...' : confirmLabel}
          </button>
        </div>
      </section>
    </div>
  );
}
