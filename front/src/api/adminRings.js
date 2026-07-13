import { deleteApi, getApi, postApi, putApi } from './client';

export function getRings(tournamentId, token) {
  return getApi('/api/admin/rings', { tournamentId }, { token });
}

export function createRing(ring, token) {
  return postApi('/api/admin/rings', ring, { token });
}

export function updateRing(ringId, ring, token) {
  return putApi(`/api/admin/rings/${ringId}`, ring, { token });
}

export function deleteRing(ringId, token) {
  return deleteApi(`/api/admin/rings/${ringId}`, { token });
}
