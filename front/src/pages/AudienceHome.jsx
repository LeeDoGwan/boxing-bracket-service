import { useTranslation } from 'react-i18next';
import { translate as t, resolveMessage } from '../i18n';
import { useState } from 'react';
import { BoutDetailDialog } from '../components/BoutDetailDialog';
import { NoticeCarousel } from '../components/NoticeCarousel';
import { RingCard } from '../components/RingCard';
import { ScheduleList } from '../components/ScheduleList';
import { StatePanel } from '../components/StatePanel';
import { useAudienceData } from '../hooks/useAudienceData';
import { useBoutEventStream } from '../hooks/useBoutEventStream';
import { decisionLabel, winnerText } from '../utils';

export function AudienceHome({ tournamentId }) {
  useTranslation();
  const { bouts, boutsError, dataTournamentId, error, home, loading, reload } = useAudienceData(tournamentId);
  const [selectedBoutId, setSelectedBoutId] = useState(null);
  const streamState = useBoutEventStream(tournamentId, reload);
  const hasCurrentData = dataTournamentId === tournamentId;

  if (loading && !hasCurrentData) {
    return (
      <main className="page-shell live-board-page">
        <StatePanel title={t('audience:home.loading')}>{t('common:wait')}</StatePanel>
      </main>
    );
  }
  if (error && !hasCurrentData) {
    return (
      <main className="page-shell live-board-page">
        <StatePanel
          action={<button className="command-button" onClick={reload} type="button">{t('common:retry')}</button>}
          title={t('errors:bout.detail')}
          tone="error"
        >{t('errors:home.retryHint')}</StatePanel>
      </main>
    );
  }

  const currentNotices = home?.notices || [];
  const currentRings = home?.ringStatuses || [];
  const results = home?.confirmedResults || [];
  const schedules = home?.schedules || [];
  const laterBoutsByRing = currentRings.reduce((groups, ring) => {
    const excludedIds = new Set([ring.currentBout?.boutId, ring.nextBout?.boutId]);
    const laterBouts = bouts
      .filter((bout) => bout.ringId === ring.ringId && !excludedIds.has(bout.boutId))
      .filter((bout) => !['FINISHED', 'CANCELED'].includes(bout.status))
      .sort((left, right) => (left.scheduledOrder ?? left.boutNumber ?? 0) - (right.scheduledOrder ?? right.boutNumber ?? 0))
      .slice(0, 3);
    groups.set(ring.ringId, laterBouts);
    return groups;
  }, new Map());
  const activeRingCount = currentRings.filter((ring) => ring.ringStatus === 'IN_PROGRESS').length;
  const upcomingBoutCount = boutsError ? null : bouts.filter((bout) => ['SCHEDULED', 'READY'].includes(bout.status)).length;
  const orderedRings = [...currentRings].sort((left, right) => {
    const priority = { IN_PROGRESS: 0, READY: 1, SCHEDULED: 2, FINISHED: 3, CLOSED: 4 };
    return (priority[left.ringStatus] ?? 5) - (priority[right.ringStatus] ?? 5);
  });

  return (
    <main className="page-shell live-board-page">
      <NoticeCarousel notices={currentNotices} />

      <section aria-label={t('audience:home.status')} className="board-toolbar">
        <div className="board-title">
          <p className="eyebrow">{t('audience:home.tournamentBoard', { value1: tournamentId })}</p>
          <h2>{t('audience:home.ringBoard')}</h2>
          <p>{t('audience:home.description')}</p>
        </div>
        <div className={`board-connection board-connection-${streamState}`} aria-live="polite">
          <span className="connection-dot" aria-hidden="true" />
          <span>{streamState === 'connected' ? t('common:liveUpdates') : streamState === 'reconnecting' ? t('common:retryingConnection') : t('common:snapshotMode')}</span>
        </div>
      </section>

      <section aria-label={t('audience:home.summary')} className="board-summary">
        <div className="board-metric board-metric-total">
          <span>{t('audience:home.totalRings')}</span>
          <strong>{currentRings.length}</strong>
        </div>
        <div className="board-metric board-metric-live">
          <span>{t('common:inProgress')}</span>
          <strong>{activeRingCount}</strong>
        </div>
        <div className="board-metric board-metric-next">
          <span>{t('audience:home.waitingBouts')}</span>
          <strong>{upcomingBoutCount ?? '-'}</strong>
        </div>
      </section>

      {(error || boutsError) && hasCurrentData && (
        <div className="stale-banner" role="alert">
          <span>{boutsError ? t('errors:home.boutsStale') : t('errors:home.stale')}</span>
          <button className="secondary-button" onClick={reload} type="button">{t('common:retry')}</button>
        </div>
      )}

      {currentRings.length ? (
        <section aria-labelledby="live-rings-title" className="live-rings-section">
          <div className="board-section-heading">
            <div>
              <p className="eyebrow">{t('audience:home.ringStatus')}</p>
              <h2 id="live-rings-title">{t('common:currentBout')}</h2>
            </div>
            <span>{activeRingCount ? t('audience:home.activeRingCount', { value1: activeRingCount }) : t('audience:home.noActiveRings')}</span>
          </div>
          <div className="ring-grid">
            {orderedRings.map((ring) => (
              <RingCard laterBouts={laterBoutsByRing.get(ring.ringId)} key={ring.ringId} onSelectBout={setSelectedBoutId} ring={ring} />
            ))}
          </div>
        </section>
      ) : (
        <StatePanel title={t('audience:home.noRings')}>{t('audience:home.notStarted')}</StatePanel>
      )}

      <div className="live-board-secondary">
        <section aria-labelledby="confirmed-results-title" className="result-section">
          <div className="board-section-heading">
            <div>
              <p className="eyebrow">{t('common:confirmedResult')}</p>
              <h2 id="confirmed-results-title">{t('audience:home.recentResults')}</h2>
            </div>
            <span>{t('common:boutCountShort', { value1: results.length })}</span>
          </div>
          {results.length ? (
            <div className="result-list">
              {results.map((bout) => (
                <button className="result-row" key={bout.boutId} onClick={() => setSelectedBoutId(bout.boutId)} type="button">
                  <span>{t('common:boutNumber', { value1: bout.boutNumber })}</span>
                  <strong>{resolveMessage(winnerText(bout))}</strong>
                  <span>{resolveMessage(decisionLabel(bout.result?.decisionType))}</span>
                </button>
              ))}
            </div>
          ) : (
            <p className="empty-copy">{t('audience:home.noResults')}</p>
          )}
        </section>

        <section aria-labelledby="schedule-title" className="schedule-section">
          <div className="board-section-heading">
            <div>
              <p className="eyebrow">{t('common:schedule')}</p>
              <h2 id="schedule-title">{t('audience:home.todaySchedule')}</h2>
            </div>
          </div>
          <ScheduleList schedules={schedules} />
        </section>
      </div>
      <BoutDetailDialog boutId={selectedBoutId} onClose={() => setSelectedBoutId(null)} />
    </main>
  );
}
