import { deleteApi, getApi, postApi, putApi } from './client';

export function getNotices(tournamentId, token) {
  return getApi('/api/admin/notices', { tournamentId }, { token });
}

export function createNotice(notice, token) {
  return postApi('/api/admin/notices', notice, { token });
}

export function updateNotice(noticeId, notice, token) {
  return putApi(`/api/admin/notices/${noticeId}`, notice, { token });
}

export function deleteNotice(noticeId, token) {
  return deleteApi(`/api/admin/notices/${noticeId}`, { token });
}
