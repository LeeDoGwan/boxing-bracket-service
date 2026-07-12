import { deleteApi, getApi, postApi, putApi } from './client';

export function getAccounts(token) {
  return getApi('/api/admin/accounts', undefined, { token });
}

export function createAccount(account, token) {
  return postApi('/api/admin/accounts', account, { token });
}

export function updateAccount(accountId, account, token) {
  return putApi(`/api/admin/accounts/${accountId}`, account, { token });
}

export function deleteAccount(accountId, token) {
  return deleteApi(`/api/admin/accounts/${accountId}`, { token });
}
