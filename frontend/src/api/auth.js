import { getApi, postApi } from './client';

export function login(loginId, password) {
  return postApi('/api/auth/login', { loginId, password });
}

export function getCurrentAccount(token) {
  return getApi('/api/auth/me', undefined, { token });
}

export function logout(token) {
  return postApi('/api/auth/logout', null, { token });
}
