import { resources, SUPPORTED_LANGUAGES } from './index';

const sources = import.meta.glob('../**/*.{js,jsx}', { query: '?raw', import: 'default', eager: true });
const variables = (value) => [...value.matchAll(/{{\s*([^}]+)\s*}}/g)].map((match) => match[1].trim()).sort();

describe('translation resources', () => {
  it.each(SUPPORTED_LANGUAGES)('%s has all namespaces, keys and interpolation variables', (language) => {
    expect(Object.keys(resources[language]).sort()).toEqual(Object.keys(resources.ko).sort());
    for (const [namespace, base] of Object.entries(resources.ko)) {
      const translated = resources[language][namespace];
      expect(Object.keys(translated).sort(), namespace).toEqual(Object.keys(base).sort());
      for (const [key, value] of Object.entries(translated)) {
        expect(typeof value, `${namespace}:${key}`).toBe('string');
        expect(value.trim(), `${namespace}:${key}`).not.toBe('');
        expect(variables(value), `${namespace}:${key}`).toEqual(variables(base[key]));
        if (language !== 'ko') expect(value, `${namespace}:${key}`).not.toMatch(/[가-힣]/);
      }
    }
  });

  it('resolves every literal translation key used by the UI', () => {
    for (const [file, source] of Object.entries(sources)) {
      if (file.includes('.test.') || file.startsWith('./') || file.includes('/i18n/')) continue;
      for (const [, namespace, key] of source.matchAll(/['"](common|admin|audience|operations|errors):([^'"\s]+)['"]/g)) {
        expect(resources.ko[namespace][key], `${file}: ${namespace}:${key}`).toBeTypeOf('string');
      }
      expect(source, file).not.toMatch(/[가-힣]/);
    }
  });
});
