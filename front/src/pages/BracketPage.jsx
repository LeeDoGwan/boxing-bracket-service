import { useCallback, useEffect, useRef, useState } from 'react';
import { getBouts, searchBouts } from '../api/audience';
import { StatePanel } from '../components/StatePanel';
import { useBoutEventStream } from '../hooks/useBoutEventStream';
import { useEventRefresh } from '../hooks/useEventRefresh';
import { athleteLabel, statusLabel, winnerText } from '../utils';

export function BracketPage({ tournamentId }) {
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
          <p className="eyebrow">OFFICIAL BRACKET</p>
          <h2>전체 대진표</h2>
        </div>
        <form className="bracket-search" onSubmit={handleSearch}>
          <label htmlFor="bout-keyword">선수, 소속, 경기 타입 또는 경기번호</label>
          <div>
            <input
              id="bout-keyword"
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="대진표 검색"
              value={keyword}
            />
            <button type="submit">검색</button>
          </div>
        </form>
        <div className="bracket-heading-actions">
          <p aria-live="polite" className={`stream-state stream-${streamState}`}>
            {streamState === 'connected' ? '실시간 반영 중' : streamState === 'reconnecting' ? '연결 재시도 중' : '현황 조회 모드'}
          </p>
          <button className="secondary-button" onClick={() => loadBouts(appliedKeywordRef.current)} type="button">새로고침</button>
        </div>
      </section>

      {state.loading && !hasCurrentData && <StatePanel title="대진표를 불러오는 중입니다.">잠시만 기다려 주세요.</StatePanel>}
      {state.error && !hasCurrentData && (
        <StatePanel
          action={<button className="command-button" onClick={() => loadBouts(keyword)} type="button">다시 시도</button>}
          title="대진표를 불러오지 못했습니다."
          tone="error"
        >
          잠시 후 다시 시도해 주세요.
        </StatePanel>
      )}
      {state.error && hasCurrentData && (
        <div className="stale-banner" role="alert">
          <span>최신 대진표를 갱신하지 못했습니다. 마지막으로 불러온 목록을 표시하고 있습니다.</span>
          <button className="secondary-button" onClick={() => loadBouts(appliedKeywordRef.current)} type="button">다시 시도</button>
        </div>
      )}
      {hasCurrentData && !bouts.length && (
        <StatePanel title="검색 결과가 없습니다.">다른 검색어를 입력하거나 대회 ID를 확인해 주세요.</StatePanel>
      )}
      {hasCurrentData && bouts.length > 0 && (
        <section aria-label="공식 경기 목록" className="bracket-table-wrap">
          <p className="result-count">{bouts.length}개 경기</p>
          <div className="bracket-table">
            <div className="bracket-row bracket-row-header" role="row">
              <span>경기</span><span>홍 선수</span><span>청 선수</span><span>상태</span><span>결과</span>
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
                <span><strong>{bout.boutNumber}</strong><small>{bout.matchType || '일반 경기'}</small></span>
                <span><b className="red-label">홍</b>{athleteLabel(bout.redAthlete)}</span>
                <span><b className="blue-label">청</b>{athleteLabel(bout.blueAthlete)}</span>
                <span><em className={`status-pill status-${bout.status?.toLowerCase()}`}>{statusLabel(bout.status)}</em></span>
                <span>{winnerText(bout)}</span>
              </button>
            ))}
          </div>
        </section>
      )}
    </main>
  );
}
