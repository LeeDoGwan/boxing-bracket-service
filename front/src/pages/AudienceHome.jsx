import { useState } from 'react';
import { BoutDetailDialog } from '../components/BoutDetailDialog';
import { NoticeCarousel } from '../components/NoticeCarousel';
import { RingCard } from '../components/RingCard';
import { ScheduleList } from '../components/ScheduleList';
import { StatePanel } from '../components/StatePanel';
import { useAudienceData } from '../hooks/useAudienceData';
import { useBoutEventStream } from '../hooks/useBoutEventStream';
import { winnerText } from '../utils';

export function AudienceHome({ tournamentId }) {
  const { bouts, dataTournamentId, error, home, loading, reload } = useAudienceData(tournamentId);
  const [selectedBoutId, setSelectedBoutId] = useState(null);
  const streamState = useBoutEventStream(tournamentId, reload);
  const hasCurrentData = dataTournamentId === tournamentId;

  if (loading && !hasCurrentData) {
    return <StatePanel title="대회 현황을 불러오는 중입니다.">잠시만 기다려 주세요.</StatePanel>;
  }
  if (error && !hasCurrentData) {
    return (
      <StatePanel
        action={<button className="command-button" onClick={reload} type="button">다시 시도</button>}
        title="경기 정보를 불러오지 못했습니다."
        tone="error"
      >
        대회 ID를 확인하거나 잠시 후 다시 시도해 주세요.
      </StatePanel>
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
      .sort((left, right) => (left.boutNumber || 0) - (right.boutNumber || 0))
      .slice(0, 3);
    groups.set(ring.ringId, laterBouts);
    return groups;
  }, new Map());
  const activeRingCount = currentRings.filter((ring) => ring.ringStatus === 'IN_PROGRESS').length;
  const upcomingBoutCount = bouts.filter((bout) => ['SCHEDULED', 'READY'].includes(bout.status)).length;

  return (
    <main className="page-shell">
      <NoticeCarousel notices={currentNotices} />

      <section aria-label="대회 라이브보드" className="home-hero">
        <div className="home-hero-copy">
          <p className="eyebrow">FIGHT NIGHT · LIVE BOARD</p>
          <h2>오늘의 링 현황</h2>
          <p>확정된 경기와 현재 진행 상태를 한 화면에서 확인하세요.</p>
        </div>
        <div className="home-hero-stats">
          <div><strong>{currentRings.length}</strong><span>전체 링</span></div>
          <div><strong>{activeRingCount}</strong><span>진행 중</span></div>
          <div><strong>{upcomingBoutCount}</strong><span>대기 경기</span></div>
        </div>
      </section>

      <section className="page-heading">
        <div>
          <p className="eyebrow">TOURNAMENT {tournamentId}</p>
          <h2>링별 진행 현황</h2>
        </div>
        <p aria-live="polite" className={`stream-state stream-${streamState}`}>
          {streamState === 'connected' ? '실시간 반영 중' : streamState === 'reconnecting' ? '연결 재시도 중' : '현황 조회 모드'}
        </p>
      </section>

      {error && hasCurrentData && (
        <div className="stale-banner" role="alert">
          <span>최신 현황을 갱신하지 못했습니다. 현재 표시된 데이터는 마지막 확정 상태입니다.</span>
          <button className="secondary-button" onClick={reload} type="button">다시 시도</button>
        </div>
      )}

      {currentRings.length ? (
        <section aria-label="링별 진행 현황" className="ring-grid">
          {currentRings.map((ring) => (
            <RingCard laterBouts={laterBoutsByRing.get(ring.ringId)} key={ring.ringId} onSelectBout={setSelectedBoutId} ring={ring} />
          ))}
        </section>
      ) : (
        <StatePanel title="표시할 링이 없습니다.">대회 운영이 시작되면 링별 경기 현황이 표시됩니다.</StatePanel>
      )}

      <div className="home-secondary-grid">
        <section className="result-section">
          <div className="section-title-row">
            <div>
              <p className="eyebrow">CONFIRMED</p>
              <h2>확정 결과</h2>
            </div>
          </div>
          {results.length ? (
            <div className="result-list">
              {results.map((bout) => (
                <button className="result-row" key={bout.boutId} onClick={() => setSelectedBoutId(bout.boutId)} type="button">
                  <span>경기 {bout.boutNumber}</span>
                  <strong>{winnerText(bout)}</strong>
                  <span>{bout.result?.decisionType || '결과 확정'}</span>
                </button>
              ))}
            </div>
          ) : (
            <p className="empty-copy">아직 확정된 경기 결과가 없습니다.</p>
          )}
        </section>

        <section className="schedule-section">
          <p className="eyebrow">SCHEDULE</p>
          <h2>대회 일정</h2>
          <ScheduleList schedules={schedules} />
        </section>
      </div>
      <BoutDetailDialog boutId={selectedBoutId} onClose={() => setSelectedBoutId(null)} />
    </main>
  );
}
