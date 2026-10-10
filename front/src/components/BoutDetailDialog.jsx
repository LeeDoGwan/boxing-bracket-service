import { useTranslation } from 'react-i18next';
import { translate as t, resolveMessage } from '../i18n';
import { useCallback, useEffect, useRef, useState } from 'react';
import { getBoutDetail } from '../api/audience';
import { athleteLabel, decisionLabel, effectiveResultScores, statusLabel, winnerText } from '../utils';

export function BoutDetailDialog({ boutId, onClose }) {
  useTranslation();
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
  const effectiveScores = effectiveResultScores(detail?.result);
  return (
    <div aria-modal="true" className="dialog-backdrop" role="presentation">
      <section aria-labelledby="bout-detail-title" aria-modal="true" className="bout-dialog" role="dialog">
        <button aria-label={t('audience:detail.close')} className="icon-close" onClick={onClose} ref={closeButtonRef} type="button">x</button>
        <h2 className={detail ? 'visually-hidden' : undefined} id="bout-detail-title">{detail ? detail.matchType || t('audience:detail.boxingBout') : t('audience:detail.title')}</h2>
        {loading && <p className="dialog-state">{t('audience:detail.loading')}</p>}
        {error && (
          <div className="dialog-state">
            <p>{t('errors:bout.detail')}</p>
            <button className="secondary-button" onClick={() => setRetryKey((current) => current + 1)} type="button">{t('common:retry')}</button>
          </div>
        )}
        {detail && (
          <>
            <p className="eyebrow">{t('common:boutNumber', { value1: detail.boutNumber })}</p>
            <p className="dialog-status">{t('operations:boutProgress', { value1: statusLabel(detail.status), value2: detail.currentRound || 0, value3: detail.totalRounds || '-' })}</p>
            <div className="detail-fighters">
              <p><span className="red-label">{t('common:redCorner')}</span>{resolveMessage(athleteLabel(detail.redAthlete))}</p>
              <p><span className="blue-label">{t('common:blueCorner')}</span>{resolveMessage(athleteLabel(detail.blueAthlete))}</p>
            </div>
            {detail.roundScores?.length > 0 && (
              <section aria-label={t('audience:detail.roundScores')} className="public-round-scores">
                <h3>{t('audience:detail.roundScores')}</h3>
                <div className="public-round-score-table" role="table">
                  <div className="public-round-score-row public-round-score-header" role="row">
                    <span>{t('common:round')}</span>
                    <span>{t('common:role.judge')}</span>
                    <span className="red-score-text">{t('common:red')}</span>
                    <span className="blue-score-text">{t('common:blue')}</span>
                  </div>
                  {detail.roundScores.map((score) => (
                    <div className="public-round-score-row" key={`${score.roundNo}-${score.judgeNo}`} role="row">
                      <span>R{score.roundNo}</span>
                      <span>{t('common:judgeNumber', { value1: score.judgeNo })}</span>
                      <strong className="red-score-text">{score.redScore}</strong>
                      <strong className="blue-score-text">{score.blueScore}</strong>
                    </div>
                  ))}
                </div>
              </section>
            )}
            {detail.resultConfirmed && (
              <div className="confirmed-result">
                <p>{t('common:confirmedResult')}</p>
                <strong>{resolveMessage(winnerText(detail))}</strong>
                <span className="confirmed-score-summary">{t('audience:detail.effectiveScore', { value1: effectiveScores?.red ?? '-', value2: effectiveScores?.blue ?? '-' })}</span>
                <span>{resolveMessage(decisionLabel(detail.result?.decisionType))}</span>
              </div>
            )}
          </>
        )}
      </section>
    </div>
  );
}
