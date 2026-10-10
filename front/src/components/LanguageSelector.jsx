import { useSyncExternalStore } from 'react';
import { useTranslation } from 'react-i18next';
import { getLanguagePreference, LANGUAGE_NAMES, setLanguagePreference, subscribeLanguagePreference } from '../i18n';

export function LanguageSelector() {
  const { t } = useTranslation();
  const preference = useSyncExternalStore(subscribeLanguagePreference, getLanguagePreference);
  return <label className="language-selector">
    <span>{t('common:language.label')}</span>
    <select onChange={(event) => setLanguagePreference(event.target.value)} value={preference}>
      <option value="auto">{t('common:language.auto')}</option>
      {Object.entries(LANGUAGE_NAMES).map(([value, label]) => <option key={value} lang={value} value={value}>{label}</option>)}
    </select>
  </label>;
}
