import { getApi, postApi } from './client';

export function getRingBouts(ringId, token) {
  return getApi(`/api/ring-manager/rings/${ringId}/bouts`, undefined, { token });
}

export function startBout(boutId, token) {
  return postApi(`/api/ring-manager/bouts/${boutId}/start`, null, { token });
}

export function updateBoutStatus(boutId, status, token) {
  return postApi(`/api/ring-manager/bouts/${boutId}/status`, { status }, { token });
}

export function startRound(boutId, roundNo, token) {
  return postApi(`/api/ring-manager/bouts/${boutId}/rounds/${roundNo}/start`, null, { token });
}

export function moveToNextBout(ringId, token) {
  return postApi(`/api/ring-manager/rings/${ringId}/next`, null, { token });
}
