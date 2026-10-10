import { useTranslation } from 'react-i18next';
import { translate as t, localizedMessage, resolveMessage, formatVenueDateTime } from '../i18n';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { getOperationStatus } from '../api/operations';
import { useStaffAuth } from '../auth/StaffAuthContext';
import { StatePanel } from '../components/StatePanel';

const STATUS_LABELS = {
  CANCELED: localizedMessage('common:status.canceled'),
  FINISHED: localizedMessage('common:finished'),
  IN_PROGRESS: localizedMessage('common:inProgress'),
  READY: localizedMessage('common:ready'),
  SCHEDULED: localizedMessage('common:scheduled'),
  SCORING: localizedMessage('common:scoring'),
};
const RING_STATUS_LABELS = {
  CLOSED: localizedMessage('common:ringClosed'),
  IN_PROGRESS: localizedMessage('common:inProgress'),
  READY: localizedMessage('common:ready'),
};
const STATUS_ORDER = ['IN_PROGRESS', 'SCORING', 'READY', 'SCHEDULED', 'FINISHED', 'CANCELED'];

function statusLabel(status) {
  return STATUS_LABELS[status] || status || localizedMessage('common:statusUnknown');
}

function ringStatusLabel(status) {
  return RING_STATUS_LABELS[status] || status || localizedMessage('common:statusUnknown');
}

function formatJudgeIds(ids) {
  return ids.length ? ids.map((id) => `#${id}`).join(', ') : localizedMessage('common:none');
}

function formatUpdatedAt(value) {
  return value ? formatVenueDateTime(value, { year: undefined, month: undefined, day: undefined }) : localizedMessage('operations:notUpdated');
}

function Metric({ label, value, tone = '' }) {
  useTranslation();
  return <article className={`operation-stat${tone ? ` operation-stat-${tone}` : ''}`}><span>{resolveMessage(label)}</span><strong>{value}</strong></article>;
}

function BoutSummary({ bout, emptyLabel = t('common:none') }) {
  useTranslation();
  if (!bout) {
    return <p className="operation-empty">{emptyLabel}</p>;
  }
  return (
    <div className="operation-bout-summary">
      <strong>{t('common:boutNumber', { value1: bout.boutNumber })}</strong>
      <span>{t('operations:boutProgress', { value1: statusLabel(bout.status), value2: bout.currentRound || 0, value3: bout.totalRounds || '?' })}</span>
      <small>{t('operations:resultStatus', { value1: bout.matchType || t('common:standardBout'), value2: bout.resultConfirmed ? t('operations:result.confirmed') : t('operations:result.pending') })}</small>
    </div>
  );
}

function RingStatusCard({ ring }) {
  useTranslation();
  return (
    <article className="operation-ring-card">
      <div className="operation-ring-heading">
        <div><p className="eyebrow">{t('common:ringNumber', { value1: ring.ringId })}</p><h3>{ring.ringName}</h3></div>
        <span className="status-pill">{resolveMessage(ringStatusLabel(ring.ringStatus))}</span>
      </div>
      <div className="operation-bout-block"><span>{t('common:currentBout')}</span><BoutSummary bout={ring.currentBout} emptyLabel={t('operations:noCurrentBout')} /></div>
      <div className="operation-bout-block"><span>{t('common:nextBout')}</span><BoutSummary bout={ring.nextBout} emptyLabel={t('operations:noNextBout')} /></div>
    </article>
  );
}

function JudgeProgress({ progress }) {
  useTranslation();
  const submitted = progress.submittedJudgeIds || [];
  const unsubmitted = progress.unsubmittedJudgeIds || [];
  const total = submitted.length + unsubmitted.length;
  const percentage = total ? Math.round((submitted.length / total) * 100) : 0;
  return (
    <article className="operation-progress-row">
      <div className="operation-progress-heading"><strong>{t('common:boutNumber', { value1: progress.boutNumber })}</strong><span>{t('operations:roundSubmissions', { value1: progress.roundNo, value2: submitted.length, value3: total || 0 })}</span></div>
      <div aria-label={t('operations:judgeProgressLabel', { value1: progress.boutNumber })} aria-valuemax="100" aria-valuemin="0" aria-valuenow={percentage} className="operation-progress-track" role="progressbar"><span style={{ width: `${percentage}%` }} /></div>
      <div className="operation-progress-detail"><span>{t('operations:submittedJudges', { value1: formatJudgeIds(submitted) })}</span><span>{t('operations:waitingJudges', { value1: formatJudgeIds(unsubmitted) })}</span></div>
    </article>
  );
}

function BoutAlertList({ bouts, title, emptyLabel }) {
  useTranslation();
  return (
    <section className="operation-panel operation-alert-panel">
      <div className="operation-panel-heading"><div><p className="eyebrow">{t('operations:attention')}</p><h3>{resolveMessage(title)}</h3></div><span>{t('common:recordCount', { value1: bouts.length })}</span></div>
      {bouts.length ? <ul className="operation-alert-list">{bouts.map((bout) => <li key={bout.boutId}><strong>{t('common:boutNumber', { value1: bout.boutNumber })}</strong><span>{t('operations:boutRing', { value1: statusLabel(bout.status), value2: bout.ringId || '-' })}</span></li>)}</ul> : <p className="operation-empty">{emptyLabel}</p>}
    </section>
  );
}

function OperationsWorkspace({ onLogout, session, tournamentId }) {
  useTranslation();
  const [status, setStatus] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [lastUpdatedAt, setLastUpdatedAt] = useState(null);

  const loadStatus = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setStatus(await getOperationStatus(tournamentId, session.accessToken));
      setLastUpdatedAt(new Date());
    } catch {
      setError(localizedMessage('errors:operations.loadPermission'));
    } finally {
      setLoading(false);
    }
  }, [session.accessToken, tournamentId]);

  useEffect(() => {
    loadStatus();
  }, [loadStatus]);

  useEffect(() => {
    const refreshTimer = window.setInterval(loadStatus, 15000);
    return () => window.clearInterval(refreshTimer);
  }, [loadStatus]);

  const statusMetrics = useMemo(() => STATUS_ORDER.map((statusKey) => ({ label: statusLabel(statusKey), count: status?.boutStatusCounts?.[statusKey] || 0, statusKey })), [status]);
  const incompleteProgress = useMemo(() => (status?.judgeScoreSubmissionStatuses || []).filter((progress) => !progress.complete), [status]);

  return (
    <main className="page-shell operations-shell">
      <div className="judge-heading">
        <div><p className="eyebrow">{t('operations:desk')}</p><h2>{t('operations:title')}</h2><p>{t('common:operatorTournament', { value1: session.account.name, value2: tournamentId })}</p></div>
        <div className="operations-heading-actions"><button className="command-button" disabled={loading} onClick={loadStatus} type="button">{t('common:refresh')}</button><button className="secondary-button" onClick={onLogout} type="button">{t('common:signOut')}</button></div>
      </div>

      {loading && !status ? <StatePanel title={t('operations:loading')}>{t('common:wait')}</StatePanel> : null}
      {error && !loading && !status ? <StatePanel action={<button className="command-button" onClick={loadStatus} type="button">{t('common:retry')}</button>} title={t('errors:operations.load')} tone="error">{t('errors:tournamentPermissionRetry')}</StatePanel> : null}
      {error && status ? <div className="stale-banner" role="alert"><span>{t('errors:operations.stale')}</span><button className="secondary-button" onClick={loadStatus} type="button">{t('common:retry')}</button></div> : null}
      {status ? (
        <>
          <section className="operation-panel operation-summary-panel">
            <div className="operation-panel-heading"><div><p className="eyebrow">{t('common:tournamentNumber', { value1: status.tournamentId })}</p><h3>{t('operations:summary')}</h3></div><span className="operation-refresh-state" aria-live="polite">{t('operations:autoRefresh', { value1: formatUpdatedAt(lastUpdatedAt), value2: loading ? t('operations:refreshingSuffix') : '' })}</span></div>
            <div className="operation-stat-grid">{statusMetrics.map((metric) => <Metric key={metric.statusKey} label={metric.label} tone={metric.statusKey === 'IN_PROGRESS' ? 'active' : ''} value={metric.count} />)}</div>
          </section>

          <section className="operation-panel operation-rings-panel">
            <div className="operation-panel-heading"><div><p className="eyebrow">{t('admin:ring.venue')}</p><h3>{t('operations:ringProgress')}</h3></div><span>{t('common:ringCount', { value1: status.rings?.length || 0 })}</span></div>
            {status.rings?.length ? <div className="operation-ring-grid">{status.rings.map((ring) => <RingStatusCard key={ring.ringId} ring={ring} />)}</div> : <p className="operation-empty">{t('admin:ring.empty')}</p>}
          </section>

          <div className="operations-two-column">
            <section className="operation-panel operation-progress-panel">
              <div className="operation-panel-heading"><div><p className="eyebrow">{t('operations:roundSubmissionHeading')}</p><h3>{t('operations:judgeSubmissions')}</h3></div><span>{t('operations:waitingCount', { value1: incompleteProgress.length })}</span></div>
              {incompleteProgress.length ? <div className="operation-progress-list">{incompleteProgress.map((progress) => <JudgeProgress key={`${progress.boutId}-${progress.roundNo}`} progress={progress} />)}</div> : <p className="operation-empty">{t('operations:noMissingSubmissions')}</p>}
            </section>
            <BoutAlertList bouts={status.pendingResultBouts || []} emptyLabel={t('operations:noPendingResults')} title={t('operations:pendingResults')} />
          </div>

          <BoutAlertList bouts={status.stalledBouts || []} emptyLabel={t('operations:noDelayedBouts')} title={t('operations:delayedBouts')} />
        </>
      ) : null}
    </main>
  );
}

export function OperationsPage({ tournamentId }) {
  useTranslation();
  const { session, signOut } = useStaffAuth();
  if (!session || !['GAME_MANAGER', 'SERVICE_MANAGER'].includes(session.account.role)) return null;
  return <OperationsWorkspace onLogout={signOut} session={session} tournamentId={tournamentId} />;
}
