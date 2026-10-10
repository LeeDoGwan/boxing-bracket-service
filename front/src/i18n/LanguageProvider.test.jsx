import { act, fireEvent, render, screen } from '@testing-library/react';
import { LanguageSelector } from '../components/LanguageSelector';
import { getLanguagePreference, i18n, LANGUAGE_STORAGE_KEY, setLanguagePreference } from './index';
import { LanguageProvider } from './LanguageProvider';

afterEach(() => {
  vi.restoreAllMocks();
  localStorage.removeItem(LANGUAGE_STORAGE_KEY);
});

describe('LanguageProvider', () => {
  it('updates the selector, document language and title without remounting inputs', () => {
    render(<LanguageProvider><LanguageSelector /><input aria-label="draft" defaultValue="" /></LanguageProvider>);
    const input = screen.getByLabelText('draft');
    fireEvent.change(input, { target: { value: '작성 중 / draft / 下書き' } });
    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'en' } });
    expect(document.documentElement.lang).toBe('en');
    expect(document.title).toBe('Boxing Tournament');
    expect(screen.getByRole('combobox', { name: 'Language' })).toHaveValue('en');
    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'ja' } });
    expect(document.documentElement.lang).toBe('ja');
    expect(screen.getByLabelText('draft')).toBe(input);
    expect(input).toHaveValue('작성 중 / draft / 下書き');
  });

  it('follows device changes only in auto mode and synchronizes another tab', async () => {
    const languages = vi.spyOn(navigator, 'languages', 'get').mockReturnValue(['en-US']);
    await setLanguagePreference('auto', { persist: false });
    const view = render(<LanguageProvider><LanguageSelector /></LanguageProvider>);
    expect(document.documentElement.lang).toBe('en');
    languages.mockReturnValue(['ja-JP']);
    act(() => window.dispatchEvent(new Event('languagechange')));
    expect(document.documentElement.lang).toBe('ja');
    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'ko' } });
    act(() => window.dispatchEvent(new Event('languagechange')));
    expect(document.documentElement.lang).toBe('ko');
    localStorage.setItem(LANGUAGE_STORAGE_KEY, 'en');
    act(() => window.dispatchEvent(new StorageEvent('storage', { key: LANGUAGE_STORAGE_KEY })));
    expect(screen.getByRole('combobox')).toHaveValue('en');
    expect(document.documentElement.lang).toBe('en');
    localStorage.removeItem(LANGUAGE_STORAGE_KEY);
    act(() => window.dispatchEvent(new StorageEvent('storage', { key: LANGUAGE_STORAGE_KEY })));
    expect(getLanguagePreference()).toBe('auto');
    expect(i18n.resolvedLanguage).toBe('ja');
    view.unmount();
    languages.mockReturnValue(['ko-KR']);
    act(() => window.dispatchEvent(new Event('languagechange')));
    expect(i18n.resolvedLanguage).toBe('ja');
  });
});
