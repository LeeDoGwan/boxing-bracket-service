import { getApi } from './client';

export function getOperationStatus(tournamentId, token) {
  return getApi('/api/admin/operations/status', { tournamentId }, { token });
}
