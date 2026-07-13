import { statusLabel } from '../utils';

function BoutSummary({ bout, sideLabel }) {
  if (!bout) {
    return <p className="empty-copy">{sideLabel} 경기가 없습니다.</p>;
  }
  return (
    <div className="bout-summary">
      <p className="summary-label">{sideLabel}</p>
      <p className="bout-number">경기 {bout.boutNumber}</p>
      <p className="match-type">{bout.matchType || '일반 경기'}</p>
      <div className="fighter-pair">
        <span className="red-label">홍</span>
        <span>{bout.redAthleteName}</span>
        <small>{bout.redAthleteAffiliation}</small>
      </div>
      <div className="fighter-pair">
        <span className="blue-label">청</span>
        <span>{bout.blueAthleteName}</span>
        <small>{bout.blueAthleteAffiliation}</small>
      </div>
      <p className="round-copy">{statusLabel(bout.boutStatus)} · {bout.currentRound ? `${bout.currentRound}라운드` : '라운드 대기'}</p>
    </div>
  );
}

export function RingCard({ ring, onSelectBout }) {
  return (
    <article className="ring-card">
      <div className="ring-card-header">
        <div>
          <p className="eyebrow">RING</p>
          <h3>{ring.ringName}</h3>
        </div>
        <span className={`status-pill status-${ring.ringStatus?.toLowerCase()}`}>{statusLabel(ring.ringStatus)}</span>
      </div>
      <button
        className="current-bout-button"
        disabled={!ring.currentBout}
        onClick={() => onSelectBout(ring.currentBout?.boutId)}
        type="button"
      >
        <BoutSummary bout={ring.currentBout} sideLabel="현재 경기" />
      </button>
      <BoutSummary bout={ring.nextBout} sideLabel="다음 경기" />
    </article>
  );
}
