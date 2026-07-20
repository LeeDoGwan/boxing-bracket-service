import { deleteApi, getApi, postApi, putApi } from './client';

export function getAccounts(filters = {}, token) {
  return getApi('/api/admin/accounts', filters, { token });
}

export function createAccount(account, token) {
  return postApi('/api/admin/accounts', normalizeAccountPayload(account), { token });
}

export function updateAccount(accountId, account, token) {
  return putApi(`/api/admin/accounts/${accountId}`, normalizeAccountPayload(account), { token });
}

function normalizeAccountPayload(account) {
  const { password, ...rest } = account;
  return { ...rest, passwordHash: account.passwordHash ?? password };
}

export function deleteAccount(accountId, token) {
  return deleteApi(`/api/admin/accounts/${accountId}`, { token });
}
