import { localizedMessage } from './index';

const ERROR_KEYS = {
  INVALID_RESPONSE: 'request.invalidResponse',
  INVALID_CREDENTIALS: 'auth.credentials',
  AUTHENTICATION_REQUIRED: 'session.expired',
  ACCESS_DENIED: 'auth.denied',
  CONCURRENT_MODIFICATION: 'concurrentModification',
  DATA_INTEGRITY_VIOLATION: 'request.conflict',
  ACCOUNT_DELETE_NOT_ALLOWED: 'account.deleteBlocked',
  ATHLETE_DELETE_NOT_ALLOWED: 'athlete.deleteBlocked',
  BOUT_DELETE_NOT_ALLOWED: 'bout.deleteBlocked',
  BOUT_SCHEDULE_LOCKED: 'bout.scheduleLocked',
  RING_DELETE_NOT_ALLOWED: 'ring.deleteBlocked',
  TOURNAMENT_DELETE_NOT_ALLOWED: 'tournament.deleteBlocked',
  ASSIGNMENT_ALREADY_EXISTS: 'assignment.duplicate',
  ACCOUNT_ROLE_MISMATCH: 'assignment.roleMismatch',
  RING_TOURNAMENT_MISMATCH: 'assignment.tournamentMismatch',
  IDEMPOTENCY_KEY_CONFLICT: 'bout.importKeyConflict',
  'loginId already exists': 'account.duplicateLogin',
  'account must be ACTIVE': 'assignment.accountInactive',
  'redAthleteId and blueAthleteId must be different': 'bout.sameAthlete',
  'ring does not belong to tournament': 'assignment.tournamentMismatch',
  'bout does not belong to tournament': 'schedule.boutTournamentMismatch',
  'Only CSV or Excel bout import is supported': 'bout.importFormat',
  'bout import file cannot be read': 'bout.importUnreadable',
  'bout import file has no rows': 'bout.importEmpty',
  'bout import workbook has no sheets': 'bout.importEmpty',
};

// Keep API messages intact for diagnostics; translate only recognized contracts.
export function apiErrorMessage(error, fallback = localizedMessage('errors:request.failed')) {
  const key = ERROR_KEYS[error?.message];
  if (key) return localizedMessage(`errors:${key}`);
  const row = /^row (\d+): (.+)$/.exec(error?.message || '');
  if (row) return localizedMessage('errors:bout.importRow', {
    row: row[1], detail: apiErrorMessage({ message: row[2], status: 400 }),
  });
  const field = /^(\w+) (is required|column is required|must be a number|must be positive)$/.exec(error?.message || '');
  if (field) return localizedMessage('errors:request.field', { field: field[1] });
  const statusKeys = {
    400: 'request.invalid', 401: 'session.expired', 403: 'auth.denied',
    404: 'request.notFound', 409: 'request.conflict',
  };
  if (error?.status >= 500) return localizedMessage('errors:request.server');
  return statusKeys[error?.status] ? localizedMessage(`errors:${statusKeys[error.status]}`) : fallback;
}
