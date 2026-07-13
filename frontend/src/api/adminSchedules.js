import { deleteApi, getApi, postApi, putApi } from './client';

export function getSchedules(tournamentId, token) {
  return getApi('/api/admin/schedules', { tournamentId }, { token });
}

export function createSchedule(schedule, token) {
  return postApi('/api/admin/schedules', schedule, { token });
}

export function updateSchedule(scheduleId, schedule, token) {
  return putApi(`/api/admin/schedules/${scheduleId}`, schedule, { token });
}

export function deleteSchedule(scheduleId, token) {
  return deleteApi(`/api/admin/schedules/${scheduleId}`, { token });
}
