import { deleteApi, getApi, postApi, postFormDataApi, putApi } from './client';

export function getBouts(tournamentId, token) {
  return getApi('/api/admin/bouts', { tournamentId }, { token });
}

export function createBout(bout, token) {
  return postApi('/api/admin/bouts', bout, { token });
}

export function updateBout(boutId, bout, token) {
  return putApi(`/api/admin/bouts/${boutId}`, bout, { token });
}

export function deleteBout(boutId, token) {
  return deleteApi(`/api/admin/bouts/${boutId}`, { token });
}

export function importBouts(file, token) {
  const formData = new FormData();
  formData.append('file', file);
  return postFormDataApi('/api/admin/bouts/import', formData, { token });
}
