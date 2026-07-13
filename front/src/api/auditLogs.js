import { getApi } from './client';

export function getAuditLogs(filters, token) {
  return getApi('/api/admin/audit-logs', filters, { token });
}
