import { buildApiUrl, getApi } from './client';

export function getHome(tournamentId) {
  return getApi('/api/home', { tournamentId });
}

export function getNotices(tournamentId) {
  return getApi('/api/notices', { tournamentId });
}

export function getRingStatuses(tournamentId) {
  return getApi('/api/rings/status', { tournamentId });
}

export function getBouts(tournamentId) {
  return getApi('/api/bouts', { tournamentId });
}

export function searchBouts(tournamentId, keyword) {
  return getApi('/api/bouts/search', { tournamentId, keyword });
}

export function getBoutDetail(boutId) {
  return getApi(`/api/bouts/${boutId}`);
}

export function eventStreamUrl(tournamentId) {
  return buildApiUrl('/api/events/stream', { tournamentId });
}
