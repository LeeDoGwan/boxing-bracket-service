import { useCallback, useEffect, useRef, useState } from 'react';
import { getBoutDetail } from '../api/audience';
import { athleteLabel, statusLabel, winnerText } from '../utils';

export function BoutDetailDialog({ boutId, onClose }) {
  const [state, setState] = useState({ loading: false, detail: null, error: null });
  const [retryKey, setRetryKey] = useState(0);
  const closeButtonRef = useRef(null);
  const activeRef = useRef(false);
  const onCloseRef = useRef(onClose);
  const previousActiveElementRef = useRef(null);

  useEffect(() => {
    onCloseRef.current = onClose;
  }, [onClose]);

  const loadDetail = useCallback(async (signal) => {
    setState({ loading: true, detail: null, error: null });
    try {
      const detail = await getBoutDetail(boutId, { signal });
      if (activeRef.current) {
        setState({ loading: false, detail, error: null });
      }
    } catch (error) {
      if (error?.name !== 'AbortError' && activeRef.current) {
        setState({ loading: false, detail: null, error: true });
      }
    }
  }, [boutId]);

  useEffect(() => {
    if (!boutId) {
      return undefined;
    }
    const controller = new AbortController();
    activeRef.current = true;
    previousActiveElementRef.current = document.activeElement;
    loadDetail(controller.signal);
    closeButtonRef.current?.focus();
    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        onCloseRef.current();
      }
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      activeRef.current = false;
      controller.abort();
      document.removeEventListener('keydown', handleKeyDown);
      previousActiveElementRef.current?.focus?.();
    };
  }, [boutId, loadDetail, retryKey]);

  if (!boutId) {
    return null;
  }

  const { detail, error, loading } = state;
  return (
    <div aria-modal="true" className="dialog-backdrop" role="presentation">
      <section aria-labelledby="bout-detail-title" aria-modal="true" className="bout-dialog" role="dialog">
        <button aria-label="경기 상세 닫기" className="icon-close" onClick={onClose} ref={closeButtonRef} type="button">x</button>
        <h2 className={detail ? 'visually-hidden' : undefined} id="bout-detail-title">{detail ? detail.matchType || '복싱 경기' : '경기 상세'}</h2>
        {loading && <p className="dialog-state">경기 정보를 불러오는 중입니다.</p>}
        {error && (
          <div className="dialog-state">
            <p>경기 정보를 불러오지 못했습니다.</p>
            <button className="secondary-button" onClick={() => setRetryKey((current) => current + 1)} type="button">다시 시도</button>
          </div>
        )}
        {detail && (
          <>
            <p className="eyebrow">BOUT {detail.boutNumber}</p>
            <p className="dialog-status">{statusLabel(detail.status)} · {detail.currentRound || 0}/{detail.totalRounds || '-'} 라운드</p>
            <div className="detail-fighters">
              <p><span className="red-label">홍</span>{athleteLabel(detail.redAthlete)}</p>
              <p><span className="blue-label">청</span>{athleteLabel(detail.blueAthlete)}</p>
            </div>
            {detail.resultConfirmed && (
              <div className="confirmed-result">
                <p>확정 결과</p>
                <strong>{winnerText(detail)}</strong>
                <span className="confirmed-score-summary">총점 {detail.result?.redTotalScore ?? '-'} : {detail.result?.blueTotalScore ?? '-'}</span>
                <span>{detail.result?.decisionType || '판정'}</span>
              </div>
            )}
          </>
        )}
      </section>
    </div>
  );
}
