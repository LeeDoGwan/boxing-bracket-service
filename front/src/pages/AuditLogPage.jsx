import { useTranslation } from 'react-i18next';
import { translate as t, localizedMessage, resolveMessage, formatVenueDateTime } from '../i18n';
import { useCallback, useEffect, useState } from 'react';
import { getAuditLogs } from '../api/auditLogs';
import { useStaffAuth } from '../auth/StaffAuthContext';
import { StatePanel } from '../components/StatePanel';

const ACTION_LABELS = {
  ACCOUNT_CREATED: localizedMessage('admin:account.create'),
  ACCOUNT_DELETED: localizedMessage('admin:account.delete'),
  ACCOUNT_UPDATED: localizedMessage('admin:audit.accountUpdated'),
  BOUT_CREATED: localizedMessage('admin:bout.create'),
  BOUT_DELETED: localizedMessage('admin:bout.delete'),
  BOUT_IMPORTED: localizedMessage('admin:audit.boutImported'),
  BOUT_STARTED: localizedMessage('operations:bout.start'),
  BOUT_STATUS_CHANGED: localizedMessage('admin:audit.boutStatusChanged'),
  BOUT_UPDATED: localizedMessage('admin:audit.boutUpdated'),
  LOGIN_FAILED: localizedMessage('admin:audit.loginFailed'),
  LOGIN_SUCCEEDED: localizedMessage('admin:audit.loginSucceeded'),
  LOGOUT: localizedMessage('common:signOut'),
  NEXT_BOUT_READY: localizedMessage('operations:bout.prepareNext'),
  NOTICE_CREATED: localizedMessage('admin:notice.create'),
  NOTICE_DELETED: localizedMessage('admin:notice.delete'),
  NOTICE_UPDATED: localizedMessage('admin:audit.noticeUpdated'),
  PENALTY_CREATED: localizedMessage('admin:audit.penaltyCreated'),
  RESULT_CONFIRMED: localizedMessage('operations:result.confirm'),
  RESULT_CORRECTED: localizedMessage('admin:audit.resultCorrected'),
  ROUND_STARTED: localizedMessage('operations:round.start'),
  SCORE_SUBMITTED: localizedMessage('admin:audit.scoreSubmitted'),
};
const TARGET_LABELS = {
  ACCOUNT: localizedMessage('common:account'),
  AUTH: localizedMessage('admin:audit.authentication'),
  BOUT: localizedMessage('common:bout'),
  BOUT_RESULT: localizedMessage('operations:result.title'),
  NOTICE: localizedMessage('common:notice'),
  PENALTY: localizedMessage('admin:audit.penalty'),
  RING: localizedMessage('common:ring'),
  ROUND_SCORE: localizedMessage('admin:audit.roundScore'),
};
const ROLE_LABELS = {
  GAME_MANAGER: localizedMessage('common:role.gameManager'),
  JUDGE: localizedMessage('common:role.judge'),
  RING_MANAGER: localizedMessage('common:role.ringManager'),
  SERVICE_MANAGER: localizedMessage('common:role.serviceManager'),
  SUPERVISOR: localizedMessage('common:role.supervisor'),
};
const INITIAL_FILTERS = {
  actionType: '',
  actorAccountId: '',
  actorRole: '',
  boutId: '',
  from: '',
  page: 0,
  ringId: '',
  size: 20,
  success: '',
  targetType: '',
  to: '',
  tournamentId: '',
};

function initialFilters(tournamentId) {
  return { ...INITIAL_FILTERS, tournamentId: String(tournamentId) };
}

function labelFor(labels, value) {
  return labels[value] || value || '-';
}

function formatDateTime(value) {
  if (!value) {
    return '-';
  }
  return formatVenueDateTime(value);
}

function formatSnapshot(value) {
  if (!value) {
    return '';
  }
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

function FilterForm({ filters, onChange, onClear, onSubmit }) {
  useTranslation();
  return (
    <form className="audit-filter-panel" onSubmit={onSubmit}>
      <div className="operation-panel-heading"><div><p className="eyebrow">{t('common:searchFilters')}</p><h3>{t('admin:audit.filters')}</h3></div><span>{t('admin:audit.limit')}</span></div>
      <div className="audit-filter-grid">
        <label>{t('common:tournamentId')}<input min="1" onChange={(event) => onChange('tournamentId', event.target.value)} type="number" value={filters.tournamentId} /></label>
        <label>{t('admin:audit.actorId')}<input min="1" onChange={(event) => onChange('actorAccountId', event.target.value)} type="number" value={filters.actorAccountId} /></label>
        <label>{t('admin:audit.actorRole')}<select aria-label={t('admin:audit.actorRole')} onChange={(event) => onChange('actorRole', event.target.value)} value={filters.actorRole}><option value="">{t('common:allRoles')}</option>{Object.entries(ROLE_LABELS).map(([value, label]) => <option key={value} value={value}>{resolveMessage(label)}</option>)}</select></label>
        <label>{t('admin:audit.actionType')}<select aria-label={t('admin:audit.actionType')} onChange={(event) => onChange('actionType', event.target.value)} value={filters.actionType}><option value="">{t('admin:audit.allActions')}</option>{Object.entries(ACTION_LABELS).map(([value, label]) => <option key={value} value={value}>{resolveMessage(label)}</option>)}</select></label>
        <label>{t('admin:audit.targetType')}<select aria-label={t('admin:audit.targetType')} onChange={(event) => onChange('targetType', event.target.value)} value={filters.targetType}><option value="">{t('admin:audit.allTargets')}</option>{Object.entries(TARGET_LABELS).map(([value, label]) => <option key={value} value={value}>{resolveMessage(label)}</option>)}</select></label>
        <label>{t('common:ringId')}<input min="1" onChange={(event) => onChange('ringId', event.target.value)} type="number" value={filters.ringId} /></label>
        <label>{t('common:boutId')}<input min="1" onChange={(event) => onChange('boutId', event.target.value)} type="number" value={filters.boutId} /></label>
        <label>{t('admin:audit.outcome')}<select aria-label={t('admin:audit.outcome')} onChange={(event) => onChange('success', event.target.value)} value={filters.success}><option value="">{t('admin:audit.allOutcomes')}</option><option value="true">{t('common:success')}</option><option value="false">{t('common:failure')}</option></select></label>
        <label>{t('common:fromDateTime')}<input onChange={(event) => onChange('from', event.target.value)} type="datetime-local" value={filters.from} /></label>
        <label>{t('common:toDateTime')}<input onChange={(event) => onChange('to', event.target.value)} type="datetime-local" value={filters.to} /></label>
      </div>
      <div className="audit-filter-actions"><button className="command-button" type="submit">{t('common:applyFilters')}</button><button className="secondary-button" onClick={onClear} type="button">{t('common:reset')}</button></div>
    </form>
  );
}

function AuditLogRow({ log }) {
  useTranslation();
  const beforeData = formatSnapshot(log.beforeData);
  const afterData = formatSnapshot(log.afterData);
  return (
    <article className={`audit-log-row${log.success ? '' : ' audit-log-failed'}`} role="row">
      <div className="audit-log-cell audit-log-time"><time dateTime={log.createdAt}>{resolveMessage(formatDateTime(log.createdAt))}</time><small>#{log.id}</small></div>
      <div className="audit-log-cell"><strong>{log.actorUsername || t('common:system')}</strong><span>{resolveMessage(labelFor(ROLE_LABELS, log.actorRole))} · ID {log.actorAccountId || '-'}</span></div>
      <div className="audit-log-cell"><strong>{resolveMessage(labelFor(ACTION_LABELS, log.actionType))}</strong><span>{resolveMessage(labelFor(TARGET_LABELS, log.targetType))} #{log.targetId || '-'}</span></div>
      <div className="audit-log-cell"><strong className={log.success ? 'audit-success' : 'audit-failure'}>{log.success ? t('common:success') : t('common:failure')}</strong><span>{log.failureReason || (log.boutId ? t('common:boutNumber', { value1: log.boutId }) : t('admin:audit.noTarget'))}</span></div>
      <div className="audit-log-cell audit-log-data">{beforeData || afterData ? <details><summary>{t('admin:audit.changedData')}</summary><div className="audit-snapshot-grid">{beforeData && <div><span>{t('admin:audit.before')}</span><pre>{beforeData}</pre></div>}{afterData && <div><span>{t('admin:audit.after')}</span><pre>{afterData}</pre></div>}</div></details> : <span className="operation-empty">{t('common:noData')}</span>}</div>
    </article>
  );
}

function AuditLogWorkspace({ onLogout, session, tournamentId }) {
  useTranslation();
  const [formFilters, setFormFilters] = useState(() => initialFilters(tournamentId));
  const [filters, setFilters] = useState(() => initialFilters(tournamentId));
  const [response, setResponse] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadLogs = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setResponse(await getAuditLogs(filters, session.accessToken));
    } catch {
      setError(localizedMessage('errors:audit.listPermission'));
    } finally {
      setLoading(false);
    }
  }, [filters, session.accessToken]);

  useEffect(() => {
    loadLogs();
  }, [loadLogs]);

  useEffect(() => {
    const nextTournamentId = String(tournamentId);
    setFormFilters((current) => current.tournamentId === nextTournamentId ? current : ({ ...current, tournamentId: nextTournamentId }));
    setFilters((current) => current.tournamentId === nextTournamentId ? current : ({ ...current, page: 0, tournamentId: nextTournamentId }));
  }, [tournamentId]);

  function updateFilter(key, value) {
    setFormFilters((current) => ({ ...current, [key]: value }));
  }

  function applyFilters(event) {
    event.preventDefault();
    setFilters({ ...formFilters, page: 0 });
  }

  function clearFilters() {
    const cleared = initialFilters(tournamentId);
    setFormFilters(cleared);
    setFilters(cleared);
  }

  function movePage(nextPage) {
    setFilters((current) => ({ ...current, page: nextPage }));
  }

  const logs = response?.content || [];
  const currentPage = response?.page || 0;
  const totalPages = response?.totalPages || 0;

  return (
    <main className="page-shell audit-shell">
      <div className="judge-heading">
        <div><p className="eyebrow">{t('admin:audit.desk')}</p><h2>{t('admin:audit.title')}</h2><p>{t('common:operatorTournament', { value1: session.account.name, value2: tournamentId })}</p></div>
        <button className="secondary-button" onClick={onLogout} type="button">{t('common:signOut')}</button>
      </div>
      <FilterForm filters={formFilters} onChange={updateFilter} onClear={clearFilters} onSubmit={applyFilters} />
      {loading ? <StatePanel title={t('admin:audit.loading')}>{t('common:wait')}</StatePanel> : null}
      {error && !loading ? <StatePanel action={<button className="command-button" onClick={loadLogs} type="button">{t('common:retry')}</button>} title={t('errors:audit.list')} tone="error">{t('errors:tournamentPermissionRetry')}</StatePanel> : null}
      {!loading && !error ? (
        <section className="operation-panel audit-results-panel">
          <div className="operation-panel-heading"><div><p className="eyebrow">{t('admin:audit.searchResults')}</p><h3>{t('admin:audit.changes')}</h3></div><span>{t('common:recordCount', { value1: response?.totalElements || 0 })}</span></div>
          {logs.length ? <div aria-label={t('admin:audit.list')} className="audit-log-table" role="table"><div className="audit-log-header" role="row"><span>{t('common:dateTime')}</span><span>{t('admin:audit.actor')}</span><span>{t('admin:audit.actionTarget')}</span><span>{t('common:result')}</span><span>{t('common:details')}</span></div>{logs.map((log) => <AuditLogRow key={log.id} log={log} />)}</div> : <p className="operation-empty audit-empty">{t('admin:audit.empty')}</p>}
          {totalPages > 1 ? <div className="audit-pagination"><button className="secondary-button" disabled={currentPage <= 0 || loading} onClick={() => movePage(currentPage - 1)} type="button">{t('common:previous')}</button><span>{currentPage + 1} / {totalPages}</span><button className="secondary-button" disabled={currentPage >= totalPages - 1 || loading} onClick={() => movePage(currentPage + 1)} type="button">{t('common:next')}</button></div> : null}
        </section>
      ) : null}
    </main>
  );
}

export function AuditLogPage({ tournamentId }) {
  useTranslation();
  const { session, signOut } = useStaffAuth();
  if (!session || !['GAME_MANAGER', 'SERVICE_MANAGER'].includes(session.account.role)) return null;
  return <AuditLogWorkspace onLogout={signOut} session={session} tournamentId={tournamentId} />;
}
