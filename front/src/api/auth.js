import { getApi, postApi } from './client';

export function login(loginId, password) {
  return postApi('/api/auth/login', { loginId, password });
}

export function getCurrentAccount(token) {
  return getApi('/api/auth/me', undefined, { token });
}

export function logout(token) {
  if (typeof window !== 'undefined') window.dispatchEvent(new Event('boxing:staff-logout'));
  return postApi('/api/auth/logout', null, { token });
}
