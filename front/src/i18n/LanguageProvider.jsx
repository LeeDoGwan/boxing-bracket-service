import { useEffect } from 'react';
import { I18nextProvider } from 'react-i18next';
import { getLanguagePreference, i18n, LANGUAGE_STORAGE_KEY, readLanguagePreference, setLanguagePreference, translate } from './index';

export function LanguageProvider({ children }) {
  useEffect(() => {
    const syncDocument = () => {
      document.documentElement.lang = i18n.resolvedLanguage || 'ko';
      document.title = translate('common:brand.title');
    };
    const followDevice = () => {
      if (getLanguagePreference() === 'auto') setLanguagePreference('auto', { persist: false });
    };
    const followStorage = (event) => {
      if (event.key === LANGUAGE_STORAGE_KEY || event.key === null) {
        setLanguagePreference(readLanguagePreference(), { persist: false });
      }
    };
    syncDocument();
    i18n.on('languageChanged', syncDocument);
    window.addEventListener('languagechange', followDevice);
    window.addEventListener('storage', followStorage);
    return () => {
      i18n.off('languageChanged', syncDocument);
      window.removeEventListener('languagechange', followDevice);
      window.removeEventListener('storage', followStorage);
    };
  }, []);
  return <I18nextProvider i18n={i18n}>{children}</I18nextProvider>;
}
