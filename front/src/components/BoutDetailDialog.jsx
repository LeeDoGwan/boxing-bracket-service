import { useEffect, useState } from 'react';
import { getBoutDetail } from '../api/audience';
import { athleteLabel, statusLabel, winnerLabel } from '../utils';

export function BoutDetailDialog({ boutId, onClose }) {
  const [state, setState] = useState({ loading: false, detail: null, error: null });

  useEffect(() => {
    if (!boutId) {
      return undefined;
    }
    let active = true;
    setState({ loading: true, detail: null, error: null });
    getBoutDetail(boutId)
      .then((detail) => active && setState({ loading: false, detail, error: null }))
      .catch(() => active && setState({ loading: false, detail: null, error: true }));
    return () => {
      active = false;
    };
  }, [boutId]);

  if (!boutId) {
    return null;
  }

  const { detail, error, loading } = state;
  return (
    <div aria-modal="true" className="dialog-backdrop" role="dialog" aria-labelledby="bout-detail-title">
      <section className="bout-dialog">
        <button aria-label="경기 상세 닫기" className="icon-close" onClick={onClose} type="button">x</button>
        {loading && <p className="dialog-state">경기 정보를 불러오는 중입니다.</p>}
        {error && <p className="dialog-state">경기 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.</p>}
        {detail && (
          <>
            <p className="eyebrow">BOUT {detail.boutNumber}</p>
            <h2 id="bout-detail-title">{detail.matchType || '복싱 경기'}</h2>
            <p className="dialog-status">{statusLabel(detail.status)} · {detail.currentRound || 0}/{detail.totalRounds || '-'} 라운드</p>
            <div className="detail-fighters">
              <p><span className="red-label">홍</span>{athleteLabel(detail.redAthlete)}</p>
              <p><span className="blue-label">청</span>{athleteLabel(detail.blueAthlete)}</p>
            </div>
            {detail.resultConfirmed && (
              <div className="confirmed-result">
                <p>확정 결과</p>
                <strong>{winnerLabel(detail)} 승</strong>
                <span>{detail.result?.decisionType || '판정'}</span>
              </div>
            )}
          </>
        )}
      </section>
    </div>
  );
}
