export function StatePanel({ action, children, title, tone = 'neutral' }) {
  return (
    <section aria-live="polite" className={`state-panel state-${tone}`} role={tone === 'error' ? 'alert' : 'status'}>
      <h2>{title}</h2>
      <p>{children}</p>
      {action}
    </section>
  );
}
