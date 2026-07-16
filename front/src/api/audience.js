import { buildApiUrl, getApi } from './client';

export function getHome(tournamentId, options = {}) {
  return getApi('/api/home', { tournamentId }, options);
}

export function getNotices(tournamentId, options = {}) {
  return getApi('/api/notices', { tournamentId }, options);
}

export function getSchedules(tournamentId, options = {}) {
  return getApi('/api/schedules', { tournamentId }, options);
}

export function getRingStatuses(tournamentId, options = {}) {
  return getApi('/api/rings/status', { tournamentId }, options);
}

export function getBouts(tournamentId, options = {}) {
  return getApi('/api/bouts', { tournamentId }, options);
}

export function searchBouts(tournamentId, keyword, options = {}) {
  return getApi('/api/bouts/search', { tournamentId, keyword }, options);
}

export function getBoutDetail(boutId, options = {}) {
  return getApi(`/api/bouts/${boutId}`, {}, options);
}

export function eventStreamUrl(tournamentId, ringId) {
  return buildApiUrl('/api/events/stream', { tournamentId, ringId });
}
