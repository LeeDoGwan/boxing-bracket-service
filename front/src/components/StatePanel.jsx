export function StatePanel({ action, children, title, tone = 'neutral' }) {
  return (
    <section className={`state-panel state-${tone}`}>
      <h2>{title}</h2>
      <p>{children}</p>
      {action}
    </section>
  );
}
