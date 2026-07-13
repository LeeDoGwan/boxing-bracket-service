import { getApi, postApi } from './client';

export function getSupervisorScores(boutId, token) {
  return getApi(`/api/supervisor/bouts/${boutId}/scores`, undefined, { token });
}

export function getPenalties(boutId, token) {
  return getApi(`/api/supervisor/bouts/${boutId}/penalties`, undefined, { token });
}

export function createPenalty(boutId, penalty, token) {
  return postApi(`/api/supervisor/bouts/${boutId}/penalties`, penalty, { token });
}

export function confirmResult(boutId, result, token) {
  return postApi(`/api/supervisor/bouts/${boutId}/result`, result, { token });
}
