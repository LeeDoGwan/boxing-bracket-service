import { useCallback, useEffect, useState } from 'react';
import { getBouts, searchBouts } from '../api/audience';
import { StatePanel } from '../components/StatePanel';
import { athleteLabel, statusLabel, winnerLabel } from '../utils';

export function BracketPage({ tournamentId }) {
  const [bouts, setBouts] = useState([]);
  const [keyword, setKeyword] = useState('');
  const [state, setState] = useState({ loading: true, error: null });
  const [selectedBoutId, setSelectedBoutId] = useState(null);

  const loadBouts = useCallback(async (searchKeyword = '') => {
    setState({ loading: true, error: null });
    try {
      const result = searchKeyword.trim()
        ? await searchBouts(tournamentId, searchKeyword.trim())
        : await getBouts(tournamentId);
      setBouts(result);
      setState({ loading: false, error: null });
    } catch {
      setState({ loading: false, error: true });
    }
  }, [tournamentId]);

  useEffect(() => {
    setKeyword('');
    setSelectedBoutId(null);
    loadBouts();
  }, [loadBouts]);

  const handleSearch = (event) => {
    event.preventDefault();
    loadBouts(keyword);
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
      </section>

      {state.loading && <StatePanel title="대진표를 불러오는 중입니다.">잠시만 기다려 주세요.</StatePanel>}
      {state.error && (
        <StatePanel
          action={<button className="command-button" onClick={() => loadBouts(keyword)} type="button">다시 시도</button>}
          title="대진표를 불러오지 못했습니다."
          tone="error"
        >
          잠시 후 다시 시도해 주세요.
        </StatePanel>
      )}
      {!state.loading && !state.error && !bouts.length && (
        <StatePanel title="검색 결과가 없습니다.">다른 검색어를 입력하거나 대회 ID를 확인해 주세요.</StatePanel>
      )}
      {!state.loading && !state.error && bouts.length > 0 && (
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
                <span>{winnerLabel(bout) ? `${winnerLabel(bout)} 승` : '-'}</span>
              </button>
            ))}
          </div>
        </section>
      )}
    </main>
  );
}
