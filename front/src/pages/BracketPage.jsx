import { useTranslation } from 'react-i18next';
import { translate as t, resolveMessage } from '../i18n';
import { useCallback, useEffect, useRef, useState } from 'react';
import { getBouts, searchBouts } from '../api/audience';
import { StatePanel } from '../components/StatePanel';
import { useBoutEventStream } from '../hooks/useBoutEventStream';
import { useEventRefresh } from '../hooks/useEventRefresh';
import { athleteLabel, statusLabel, winnerText } from '../utils';

export function BracketPage({ tournamentId }) {
  useTranslation();
  const [bouts, setBouts] = useState([]);
  const [keyword, setKeyword] = useState('');
  const [state, setState] = useState({ dataTournamentId: null, loading: true, error: null });
  const [selectedBoutId, setSelectedBoutId] = useState(null);
  const requestRef = useRef({ controller: null, id: 0 });
  const appliedKeywordRef = useRef('');

  const loadBouts = useCallback(async (searchKeyword = '') => {
    requestRef.current.controller?.abort();
    const requestId = requestRef.current.id + 1;
    const controller = new AbortController();
    requestRef.current = { controller, id: requestId };
    setState((current) => ({ ...current, loading: true, error: null }));
    try {
      const result = searchKeyword.trim()
        ? await searchBouts(tournamentId, searchKeyword.trim(), { signal: controller.signal })
        : await getBouts(tournamentId, { signal: controller.signal });
      if (requestRef.current.id !== requestId) {
        return;
      }
      setBouts(result);
      setState({ dataTournamentId: tournamentId, loading: false, error: null });
    } catch (error) {
      if (error?.name === 'AbortError' || requestRef.current.id !== requestId) {
        return;
      }
      setState((current) => ({ ...current, loading: false, error: true }));
    }
  }, [tournamentId]);

  useEffect(() => {
    appliedKeywordRef.current = '';
    setKeyword('');
    setSelectedBoutId(null);
    loadBouts();
    return () => requestRef.current.controller?.abort();
  }, [loadBouts]);

  const refreshLiveData = useEventRefresh(() => loadBouts(appliedKeywordRef.current));
  const streamState = useBoutEventStream(tournamentId, refreshLiveData);
  const hasCurrentData = state.dataTournamentId === tournamentId;

  const handleSearch = (event) => {
    event.preventDefault();
    const nextKeyword = keyword.trim();
    appliedKeywordRef.current = nextKeyword;
    loadBouts(nextKeyword);
  };

  const focusBout = (boutId) => {
    setSelectedBoutId(boutId);
    window.setTimeout(() => {
      document.getElementById(`bout-${boutId}`)?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }, 0);
  };

  return (
    <main className="page-shell bracket-page">
      <section className="page-heading bracket-heading">
        <div>
          <p className="eyebrow">{t('audience:bracket.official')}</p>
          <h2>{t('audience:bracket.title')}</h2>
        </div>
        <form className="bracket-search" onSubmit={handleSearch}>
          <label htmlFor="bout-keyword">{t('audience:bracket.searchLabel')}</label>
          <div>
            <input
              id="bout-keyword"
              onChange={(event) => setKeyword(event.target.value)}
              placeholder={t('audience:bracket.search')}
              value={keyword}
            />
            <button type="submit">{t('common:search')}</button>
          </div>
        </form>
        <div className="bracket-heading-actions">
          <p aria-live="polite" className={`stream-state stream-${streamState}`}>
            {streamState === 'connected' ? t('common:liveUpdates') : streamState === 'reconnecting' ? t('common:retryingConnection') : t('common:snapshotMode')}
          </p>
          <button className="secondary-button" onClick={() => loadBouts(appliedKeywordRef.current)} type="button">{t('common:refresh')}</button>
        </div>
      </section>

      {state.loading && !hasCurrentData && <StatePanel title={t('audience:bracket.loading')}>{t('common:wait')}</StatePanel>}
      {state.error && !hasCurrentData && (
        <StatePanel
          action={<button className="command-button" onClick={() => loadBouts(keyword)} type="button">{t('common:retry')}</button>}
          title={t('errors:bracket.load')}
          tone="error"
        >{t('common:tryAgainLater')}</StatePanel>
      )}
      {state.error && hasCurrentData && (
        <div className="stale-banner" role="alert">
          <span>{t('errors:bracket.stale')}</span>
          <button className="secondary-button" onClick={() => loadBouts(appliedKeywordRef.current)} type="button">{t('common:retry')}</button>
        </div>
      )}
      {hasCurrentData && !bouts.length && (
        <StatePanel title={t('common:noSearchResults')}>{t('audience:bracket.emptyHint')}</StatePanel>
      )}
      {hasCurrentData && bouts.length > 0 && (
        <section aria-label={t('audience:bracket.officialBouts')} className="bracket-table-wrap">
          <p className="result-count">{t('common:boutCount', { value1: bouts.length })}</p>
          <div className="bracket-table">
            <div className="bracket-row bracket-row-header" role="row">
              <span>{t('common:bout')}</span><span>{t('audience:redAthlete')}</span><span>{t('audience:blueAthlete')}</span><span>{t('common:status')}</span><span>{t('common:result')}</span>
            </div>
            {bouts.map((bout) => (
              <button
                aria-pressed={selectedBoutId === bout.boutId}
                className={`bracket-row ${selectedBoutId === bout.boutId ? 'selected' : ''}`}
                id={`bout-${bout.boutId}`}
                key={bout.boutId}
                onClick={() => focusBout(bout.boutId)}
                type="button"
              >
                <span><strong>{bout.boutNumber}</strong><small>{bout.matchType || t('common:standardBout')}</small></span>
                <span><b className="red-label">{t('common:redCorner')}</b>{resolveMessage(athleteLabel(bout.redAthlete))}</span>
                <span><b className="blue-label">{t('common:blueCorner')}</b>{resolveMessage(athleteLabel(bout.blueAthlete))}</span>
                <span><em className={`status-pill status-${bout.status?.toLowerCase()}`}>{resolveMessage(statusLabel(bout.status))}</em></span>
                <span>{resolveMessage(winnerText(bout))}</span>
              </button>
            ))}
          </div>
        </section>
      )}
    </main>
  );
}
