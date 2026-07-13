import { deleteApi, getApi, postApi, putApi } from './client';

export function getTournaments(token) {
  return getApi('/api/admin/tournaments', undefined, { token });
}

export function createTournament(tournament, token) {
  return postApi('/api/admin/tournaments', tournament, { token });
}

export function updateTournament(tournamentId, tournament, token) {
  return putApi(`/api/admin/tournaments/${tournamentId}`, tournament, { token });
}

export function deleteTournament(tournamentId, token) {
  return deleteApi(`/api/admin/tournaments/${tournamentId}`, { token });
}
