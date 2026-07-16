import { getApi, postApi, putApi } from './client';

export function getAssignedRings(tournamentId, token) {
  return getApi('/api/staff/assignments/rings', { tournamentId }, { token });
}

export function getAssignedBouts(ringId, token) {
  return getApi(`/api/staff/assignments/rings/${ringId}/bouts`, undefined, { token });
}

export function getAssignments(filters, token) {
  return getApi('/api/admin/assignments', filters, { token });
}

export function createAssignment(assignment, token) {
  return postApi('/api/admin/assignments', assignment, { token });
}

export function changeAssignmentActive(assignmentId, active, token) {
  return putApi(`/api/admin/assignments/${assignmentId}/active`, { active }, { token });
}
