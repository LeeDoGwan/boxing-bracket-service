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
  const { bouts, boutsError, dataTournamentId, error, home, loading, reload } = useAudienceData(tournamentId);
  const [selectedBoutId, setSelectedBoutId] = useState(null);
  const streamState = useBoutEventStream(tournamentId, reload);
  const hasCurrentData = dataTournamentId === tournamentId;

  if (loading && !hasCurrentData) {
    return (
      <main className="page-shell live-board-page">
        <StatePanel title="대회 현황을 불러오는 중입니다.">잠시만 기다려 주세요.</StatePanel>
      </main>
    );
  }
  if (error && !hasCurrentData) {
    return (
      <main className="page-shell live-board-page">
        <StatePanel
          action={<button className="command-button" onClick={reload} type="button">다시 시도</button>}
          title="경기 정보를 불러오지 못했습니다."
          tone="error"
        >
          대회 ID를 확인하거나 잠시 후 다시 시도해 주세요.
        </StatePanel>
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

      <section aria-label="대회 현황" className="board-toolbar">
        <div className="board-title">
          <p className="eyebrow">TOURNAMENT {tournamentId} · PUBLIC BOARD</p>
          <h2>링별 경기 현황</h2>
          <p>현재 진행 중인 경기와 다음 순서를 한눈에 확인하세요.</p>
        </div>
        <div className={`board-connection board-connection-${streamState}`} aria-live="polite">
          <span className="connection-dot" aria-hidden="true" />
          <span>{streamState === 'connected' ? '실시간 반영 중' : streamState === 'reconnecting' ? '연결 재시도 중' : '현황 조회 모드'}</span>
        </div>
      </section>

      <section aria-label="대회 요약" className="board-summary">
        <div className="board-metric board-metric-total">
          <span>전체 링</span>
          <strong>{currentRings.length}</strong>
        </div>
        <div className="board-metric board-metric-live">
          <span>진행 중</span>
          <strong>{activeRingCount}</strong>
        </div>
        <div className="board-metric board-metric-next">
          <span>대기 경기</span>
          <strong>{upcomingBoutCount ?? '-'}</strong>
        </div>
      </section>

      {(error || boutsError) && hasCurrentData && (
        <div className="stale-banner" role="alert">
          <span>{boutsError ? '경기 목록을 불러오지 못해 대기 경기 수와 이후 경기를 표시할 수 없습니다.' : '최신 현황을 갱신하지 못했습니다. 현재 표시된 데이터는 마지막으로 확인된 상태입니다.'}</span>
          <button className="secondary-button" onClick={reload} type="button">다시 시도</button>
        </div>
      )}

      {currentRings.length ? (
        <section aria-labelledby="live-rings-title" className="live-rings-section">
          <div className="board-section-heading">
            <div>
              <p className="eyebrow">LIVE RINGS</p>
              <h2 id="live-rings-title">현재 경기</h2>
            </div>
            <span>{activeRingCount ? `${activeRingCount}개 링 진행 중` : '진행 중인 링 없음'}</span>
          </div>
          <div className="ring-grid">
            {orderedRings.map((ring) => (
              <RingCard laterBouts={laterBoutsByRing.get(ring.ringId)} key={ring.ringId} onSelectBout={setSelectedBoutId} ring={ring} />
            ))}
          </div>
        </section>
      ) : (
        <StatePanel title="표시할 링이 없습니다.">대회 운영이 시작되면 링별 경기 현황이 표시됩니다.</StatePanel>
      )}

      <div className="live-board-secondary">
        <section aria-labelledby="confirmed-results-title" className="result-section">
          <div className="board-section-heading">
            <div>
              <p className="eyebrow">CONFIRMED RESULTS</p>
              <h2 id="confirmed-results-title">최근 확정 결과</h2>
            </div>
            <span>{results.length}경기</span>
          </div>
          {results.length ? (
            <div className="result-list">
              {results.map((bout) => (
                <button className="result-row" key={bout.boutId} onClick={() => setSelectedBoutId(bout.boutId)} type="button">
                  <span>경기 {bout.boutNumber}</span>
                  <strong>{winnerText(bout)}</strong>
                  <span>{decisionLabel(bout.result?.decisionType)}</span>
                </button>
              ))}
            </div>
          ) : (
            <p className="empty-copy">아직 확정된 경기 결과가 없습니다.</p>
          )}
        </section>

        <section aria-labelledby="schedule-title" className="schedule-section">
          <div className="board-section-heading">
            <div>
              <p className="eyebrow">SCHEDULE</p>
              <h2 id="schedule-title">오늘의 일정</h2>
            </div>
          </div>
          <ScheduleList schedules={schedules} />
        </section>
      </div>
      <BoutDetailDialog boutId={selectedBoutId} onClose={() => setSelectedBoutId(null)} />
    </main>
  );
}
