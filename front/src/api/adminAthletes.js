import { deleteApi, getApi, postApi, putApi } from './client';

export function getAthletes(tournamentId, keyword, token) {
  return getApi('/api/admin/athletes', { tournamentId, keyword }, { token });
}

export function createAthlete(athlete, token) {
  return postApi('/api/admin/athletes', athlete, { token });
}

export function updateAthlete(athleteId, athlete, token) {
  return putApi(`/api/admin/athletes/${athleteId}`, athlete, { token });
}

export function deleteAthlete(athleteId, tournamentId, token) {
  return deleteApi(`/api/admin/athletes/${athleteId}`, { token, params: { tournamentId } });
}
