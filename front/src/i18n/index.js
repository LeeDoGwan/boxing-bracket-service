import i18next from 'i18next';
import { initReactI18next } from 'react-i18next';

const localeFiles = import.meta.glob('./locales/*/*.json', { eager: true, import: 'default' });
export const resources = {};
for (const [path, content] of Object.entries(localeFiles)) {
  const [, language, namespace] = path.match(/locales\/([^/]+)\/([^/]+)\.json$/);
  (resources[language] ||= {})[namespace] = content;
}

export const LANGUAGE_STORAGE_KEY = 'boxing.languagePreference';
export const SUPPORTED_LANGUAGES = ['ko', 'en', 'ja'];
export const LANGUAGE_NAMES = { ko: '한국어', en: 'English', ja: '日本語' };
const preferenceListeners = new Set();

export function readLanguagePreference() {
  try {
    const value = window.localStorage.getItem(LANGUAGE_STORAGE_KEY);
    return SUPPORTED_LANGUAGES.includes(value) ? value : 'auto';
  } catch {
    return 'auto';
  }
}

export function resolveLanguage(preference, languages = globalThis.navigator?.languages?.length
  ? globalThis.navigator.languages : [globalThis.navigator?.language]) {
  if (SUPPORTED_LANGUAGES.includes(preference)) return preference;
  for (const language of languages) {
    const base = typeof language === 'string' ? language.toLowerCase().split(/[-_]/)[0] : '';
    if (SUPPORTED_LANGUAGES.includes(base)) return base;
  }
  return 'ko';
}

let preference = readLanguagePreference();
export const i18n = i18next.createInstance();
i18n.use(initReactI18next).init({
  resources,
  lng: resolveLanguage(preference),
  fallbackLng: 'ko',
  supportedLngs: SUPPORTED_LANGUAGES,
  load: 'languageOnly',
  ns: ['common', 'admin', 'audience', 'operations', 'errors'],
  defaultNS: 'common',
  keySeparator: false,
  initAsync: false,
  interpolation: { escapeValue: false },
  returnNull: false,
});

export function getLanguagePreference() {
  return preference;
}

export function subscribeLanguagePreference(listener) {
  preferenceListeners.add(listener);
  return () => preferenceListeners.delete(listener);
}

export function setLanguagePreference(value, { persist = true } = {}) {
  preference = SUPPORTED_LANGUAGES.includes(value) ? value : 'auto';
  if (persist) {
    try {
      if (preference === 'auto') window.localStorage.removeItem(LANGUAGE_STORAGE_KEY);
      else window.localStorage.setItem(LANGUAGE_STORAGE_KEY, preference);
    } catch { /* The current tab still works when storage is unavailable. */ }
  }
  preferenceListeners.forEach((listener) => listener());
  return i18n.changeLanguage(resolveLanguage(preference));
}

// Store references, not rendered sentences, in notifications and pending actions.
export function localizedMessage(key, values = {}) {
  return { type: 'localized-message', key, values };
}

export function resolveMessage(value) {
  if (value?.type !== 'localized-message') return value;
  return translate(value.key, value.values);
}

export function translate(key, values = {}) {
  const resolved = Object.fromEntries(Object.entries(values).map(([name, value]) => [name, resolveMessage(value)]));
  return i18n.t(key, resolved);
}

export function formatVenueDateTime(value, options = {}) {
  if (!value) return '-';
  // Existing offset-free API timestamps describe the Korean venue's local time.
  const normalized = typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/.test(value)
    && !/(Z|[+-]\d{2}:?\d{2})$/i.test(value) ? `${value}+09:00` : value;
  const date = value instanceof Date ? value : new Date(normalized);
  if (Number.isNaN(date.getTime())) return '-';
  return new Intl.DateTimeFormat(i18n.resolvedLanguage || 'ko', {
    timeZone: 'Asia/Seoul', year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit', ...options,
  }).format(date);
}
