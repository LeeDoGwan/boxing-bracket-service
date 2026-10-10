import { decisionLabel, winnerText } from '../utils';
import { apiErrorMessage } from './errors';
import { formatVenueDateTime, getLanguagePreference, i18n, LANGUAGE_STORAGE_KEY, localizedMessage, readLanguagePreference, resolveLanguage, resolveMessage, setLanguagePreference } from './index';

afterEach(() => {
  vi.restoreAllMocks();
  localStorage.removeItem(LANGUAGE_STORAGE_KEY);
});

describe('language preference', () => {
  it.each([
    ['auto', ['ja-JP', 'en-US'], 'ja'],
    ['auto', ['fr-FR', 'en-GB', 'ko-KR'], 'en'],
    ['auto', ['KO_kr'], 'ko'],
    ['auto', ['de-DE'], 'ko'],
    ['auto', [], 'ko'],
    ['en', ['ja-JP'], 'en'],
    ['invalid', ['ja'], 'ja'],
  ])('resolves %s and %j to %s', (preference, languages, expected) => {
    expect(resolveLanguage(preference, languages)).toBe(expected);
  });

  it('stores explicit preferences and removes the override for auto', async () => {
    vi.spyOn(navigator, 'languages', 'get').mockReturnValue(['ja-JP']);
    await setLanguagePreference('en');
    expect(readLanguagePreference()).toBe('en');
    expect(localStorage.getItem(LANGUAGE_STORAGE_KEY)).toBe('en');
    await setLanguagePreference('auto');
    expect(getLanguagePreference()).toBe('auto');
    expect(i18n.resolvedLanguage).toBe('ja');
    expect(localStorage.getItem(LANGUAGE_STORAGE_KEY)).toBeNull();
  });

  it('ignores an invalid saved preference', () => {
    localStorage.setItem(LANGUAGE_STORAGE_KEY, 'fr');
    expect(readLanguagePreference()).toBe('auto');
  });

  it('keeps this tab usable when browser storage is blocked', async () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new Error('blocked'); });
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('blocked'); });
    vi.spyOn(Storage.prototype, 'removeItem').mockImplementation(() => { throw new Error('blocked'); });
    expect(readLanguagePreference()).toBe('auto');
    await expect(setLanguagePreference('ja')).resolves.toBeTypeOf('function');
    expect(i18n.resolvedLanguage).toBe('ja');
    await expect(setLanguagePreference('auto')).resolves.toBeTypeOf('function');
  });
});

describe('language-independent data', () => {
  it('retranslates a stored error without changing the API error', async () => {
    const error = Object.assign(new Error('ACCOUNT_DELETE_NOT_ALLOWED'), { status: 409 });
    const notice = apiErrorMessage(error);
    await setLanguagePreference('en', { persist: false });
    expect(resolveMessage(notice)).toBe('Accounts with assignments or records cannot be deleted.');
    await setLanguagePreference('ja', { persist: false });
    expect(resolveMessage(notice)).toContain('削除できません');
    expect(error.message).toBe('ACCOUNT_DELETE_NOT_ALLOWED');
  });

  it('keeps import row context and hides unknown raw server messages', async () => {
    await setLanguagePreference('en', { persist: false });
    expect(resolveMessage(apiErrorMessage({ message: 'row 3: ringId must be a number', status: 400 })))
      .toBe('File row 3: Check the value in ringId.');
    expect(resolveMessage(apiErrorMessage({ message: 'private diagnostic', status: 500 })))
      .toBe('A server error occurred. Please try again later.');
  });

  it.each(['ko', 'en', 'ja'])('uses enum identity, not a translated name, for winners in %s', async (language) => {
    await setLanguagePreference(language, { persist: false });
    expect(winnerText({ resultConfirmed: true, winnerSide: 'DRAW' })).toBe(i18n.t('common:draw'));
    expect(winnerText({ resultConfirmed: true, winnerSide: 'RED', redAthlete: { name: '무승부' } }))
      .toBe(i18n.t('common:winner', { value1: '무승부' }));
    expect(decisionLabel('RSC')).toBe('TKO');
    expect(resolveMessage('홍길동')).toBe('홍길동');
  });

  it('resolves nested references after switching languages', async () => {
    const notice = localizedMessage('common:roleTournament', {
      value1: localizedMessage('common:role.judge'), value2: 1,
    });
    await setLanguagePreference('en', { persist: false });
    expect(resolveMessage(notice)).toBe('Judge · Tournament 1');
  });

  it.each(['ko', 'en', 'ja'])('formats a venue timestamp in %s without shifting its time', async (language) => {
    await setLanguagePreference(language, { persist: false });
    const value = formatVenueDateTime('2026-10-08T09:00:00');
    expect(value).toBe(formatVenueDateTime('2026-10-08T00:00:00Z'));
    expect(value).not.toBe(formatVenueDateTime('2026-10-08T09:00:00Z'));
    expect(formatVenueDateTime('invalid')).toBe('-');
  });
});
