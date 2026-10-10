import { useTranslation } from 'react-i18next';
import { resolveMessage } from '../i18n';
export function StatePanel({ action, children, title, tone = 'neutral' }) {
  useTranslation();
  return (
    <section aria-live="polite" className={`state-panel state-${tone}`} role={tone === 'error' ? 'alert' : 'status'}>
      <h2>{resolveMessage(title)}</h2>
      <p>{resolveMessage(children)}</p>
      {action}
    </section>
  );
}
