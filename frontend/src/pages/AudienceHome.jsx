import { useState } from 'react';
import { BoutDetailDialog } from '../components/BoutDetailDialog';
import { NoticeCarousel } from '../components/NoticeCarousel';
import { RingCard } from '../components/RingCard';
import { StatePanel } from '../components/StatePanel';
import { useAudienceData } from '../hooks/useAudienceData';
import { useBoutEventStream } from '../hooks/useBoutEventStream';
import { winnerLabel } from '../utils';

export function AudienceHome({ tournamentId }) {
  const { error, home, loading, notices, reload, ringStatuses } = useAudienceData(tournamentId);
  const [selectedBoutId, setSelectedBoutId] = useState(null);
  const streamState = useBoutEventStream(tournamentId, reload);

  if (loading && !home) {
    return <StatePanel title="대회 현황을 불러오는 중입니다.">잠시만 기다려 주세요.</StatePanel>;
  }
  if (error && !home) {
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

  const currentNotices = notices.length ? notices : home?.notices || [];
  const currentRings = ringStatuses.length ? ringStatuses : home?.ringStatuses || [];
  const results = home?.confirmedResults || [];

  return (
    <main className="page-shell">
      <NoticeCarousel notices={currentNotices} />

      <section className="page-heading">
        <div>
          <p className="eyebrow">TOURNAMENT {tournamentId}</p>
          <h2>링별 진행 현황</h2>
        </div>
        <p aria-live="polite" className={`stream-state stream-${streamState}`}>
          {streamState === 'connected' ? '실시간 반영 중' : streamState === 'reconnecting' ? '연결 재시도 중' : '현황 조회 모드'}
        </p>
      </section>

      {currentRings.length ? (
        <section aria-label="링별 진행 현황" className="ring-grid">
          {currentRings.map((ring) => (
            <RingCard key={ring.ringId} onSelectBout={setSelectedBoutId} ring={ring} />
          ))}
        </section>
      ) : (
        <StatePanel title="표시할 링이 없습니다.">대회 운영이 시작되면 링별 경기 현황이 표시됩니다.</StatePanel>
      )}

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
                <strong>{winnerLabel(bout)} 승</strong>
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
        <p>등록된 일정 정보가 없습니다.</p>
      </section>
      <BoutDetailDialog boutId={selectedBoutId} onClose={() => setSelectedBoutId(null)} />
    </main>
  );
}
