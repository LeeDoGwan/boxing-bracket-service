import '@testing-library/jest-dom/vitest';
import { beforeEach } from 'vitest';
import { setLanguagePreference } from '../i18n';

beforeEach(() => setLanguagePreference('ko', { persist: false }));
