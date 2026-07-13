import { useCallback, useEffect, useMemo, useState } from 'react';
import { getBoutDetail, getBouts } from '../api/audience';
import { login, logout } from '../api/auth';
import { confirmResult, createPenalty, getPenalties, getSupervisorScores } from '../api/supervisor';
import { StatePanel } from '../components/StatePanel';

const SESSION_KEY = 'boxing.supervisor.session';

const DECISION_LABELS = {
  ABD: '기권',
  DSQ: '실격',
  KO: 'KO',
  POINTS: '판정',
  RSC: 'RSC',
  UNKNOWN: '기타',
  WALKOVER: '부전승',
};

function readSession() {
  try {
    const stored = window.sessionStorage.getItem(SESSION_KEY);
    const session = stored ? JSON.parse(stored) : null;
    return session?.accessToken && session?.account?.role === 'SUPERVISOR' ? session : null;
  } catch {
    return null;
  }
}

function athleteName(athlete) {
  return athlete?.name || '선수 미정';
}

function boutStatus(status) {
  const labels = { FINISHED: '종료', IN_PROGRESS: '진행 중', READY: '준비', SCHEDULED: '예정' };
  return labels[status] || status || '상태 미정';
}

function sortBouts(bouts) {
  const priority = { IN_PROGRESS: 0, READY: 1, SCHEDULED: 2, FINISHED: 3 };
  return [...bouts].sort((left, right) => {
    const statusDifference = (priority[left.status] ?? 4) - (priority[right.status] ?? 4);
    return statusDifference || ((left.scheduledOrder ?? left.boutNumber ?? 0) - (right.scheduledOrder ?? right.boutNumber ?? 0));
  });
}

function LoginForm({ onLogin }) {
  const [form, setForm] = useState({ loginId: '', password: '' });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const session = await login(form.loginId, form.password);
      if (session.account?.role !== 'SUPERVISOR') {
        throw new Error('SUPERVISOR_ONLY');
      }
      onLogin(session);
    } catch (requestError) {
      setError(requestError.message === 'SUPERVISOR_ONLY' ? '감독자 계정으로 로그인해 주세요.' : '로그인 정보를 확인해 주세요.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="page-shell auth-shell">
      <section className="auth-panel">
        <p className="eyebrow">SUPERVISOR DESK</p>
        <h2>감독자 로그인</h2>
        <p>심판 점수와 결과를 검토하려면 감독자 계정으로 로그인하세요.</p>
        <form onSubmit={handleSubmit}>
          <label>
            아이디
            <input autoComplete="username" onChange={(event) => setForm((current) => ({ ...current, loginId: event.target.value }))} required value={form.loginId} />
          </label>
          <label>
            비밀번호
            <input autoComplete="current-password" onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))} required type="password" value={form.password} />
          </label>
          {error && <p aria-live="polite" className="form-error" role="alert">{error}</p>}
          <button className="command-button" disabled={submitting} type="submit">{submitting ? '로그인 중...' : '로그인'}</button>
        </form>
      </section>
    </main>
  );
}

function BoutPicker({ bouts, onSelect, selectedBoutId }) {
  if (!bouts.length) {
    return <StatePanel title="검토할 경기가 없습니다.">게임 매니저가 등록한 공식 경기가 나타납니다.</StatePanel>;
  }

  return (
    <div aria-label="경기 목록" className="judge-bout-list">
      {bouts.map((bout) => (
        <button className={`judge-bout-option${selectedBoutId === bout.boutId ? ' selected' : ''}`} key={bout.boutId} onClick={() => onSelect(bout.boutId)} type="button">
          <span>경기 {bout.boutNumber}</span>
          <strong>{athleteName(bout.redAthlete)} <b>vs</b> {athleteName(bout.blueAthlete)}</strong>
          <small>{bout.matchType || '일반 경기'} · {boutStatus(bout.status)}</small>
        </button>
      ))}
    </div>
  );
}

function ScoreReview({ scores }) {
  if (!scores.length) {
    return <p className="empty-copy">아직 제출된 심판 점수가 없습니다.</p>;
  }

  return (
    <div className="supervisor-score-table" role="table">
      <div className="supervisor-score-row supervisor-score-header" role="row">
        <span>라운드</span><span>심판</span><span>레드</span><span>블루</span><span>상태</span>
      </div>
      {scores.map((score) => (
        <div className="supervisor-score-row" key={`${score.roundNo}-${score.judgeId}`} role="row">
          <span>{score.roundNo}R</span>
          <span>#{score.judgeId}</span>
          <strong className="red-score-text">{score.redScore ?? '-'}</strong>
          <strong className="blue-score-text">{score.blueScore ?? '-'}</strong>
          <span>{score.status === 'SUBMITTED' ? '제출됨' : '초안'}</span>
        </div>
      ))}
    </div>
  );
}

function SupervisorWorkspace({ onLogout, session, tournamentId }) {
  const [bouts, setBouts] = useState([]);
  const [listLoading, setListLoading] = useState(true);
  const [listError, setListError] = useState('');
  const [selectedBoutId, setSelectedBoutId] = useState(null);
  const [bout, setBout] = useState(null);
  const [scores, setScores] = useState([]);
  const [penalties, setPenalties] = useState([]);
  const [result, setResult] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState('');
  const [actionError, setActionError] = useState('');
  const [actionLoading, setActionLoading] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);
  const [penaltyForm, setPenaltyForm] = useState({ penaltyPoint: '', reason: '', targetSide: 'RED' });
  const [resultForm, setResultForm] = useState({ decisionType: 'POINTS', winnerSide: 'RED' });

  const sortedBouts = useMemo(() => sortBouts(bouts), [bouts]);
  const scoreSummary = useMemo(() => scores
    .filter((score) => score.status === 'SUBMITTED')
    .reduce((summary, score) => ({
      blue: summary.blue + (score.blueScore || 0),
      red: summary.red + (score.redScore || 0),
    }), { blue: 0, red: 0 }), [scores]);
  const penaltySummary = useMemo(() => penalties.reduce((summary, penalty) => ({
    blue: summary.blue + (penalty.targetSide === 'BLUE' ? penalty.penaltyPoint || 0 : 0),
    red: summary.red + (penalty.targetSide === 'RED' ? penalty.penaltyPoint || 0 : 0),
  }), { blue: 0, red: 0 }), [penalties]);

  const loadBouts = useCallback(async () => {
    setListLoading(true);
    setListError('');
    try {
      const nextBouts = (await getBouts(tournamentId)) || [];
      setBouts(nextBouts);
      setSelectedBoutId((current) => current && nextBouts.some((item) => item.boutId === current)
        ? current
        : sortBouts(nextBouts)[0]?.boutId || null);
    } catch {
      setListError('경기 목록을 불러오지 못했습니다.');
    } finally {
      setListLoading(false);
    }
  }, [tournamentId]);

  useEffect(() => {
    loadBouts();
  }, [loadBouts]);

  useEffect(() => {
    if (!selectedBoutId) {
      setBout(null);
      setScores([]);
      setResult(null);
      setPenalties([]);
      return undefined;
    }
    let cancelled = false;
    setDetailLoading(true);
    setDetailError('');
    setActionError('');
    Promise.all([
      getBoutDetail(selectedBoutId),
      getSupervisorScores(selectedBoutId, session.accessToken),
      getPenalties(selectedBoutId, session.accessToken),
    ])
      .then(([nextBout, nextScores, nextPenalties]) => {
        if (!cancelled) {
          setBout(nextBout);
          setScores(nextScores || []);
          setResult(nextBout.result || null);
          setPenalties(nextPenalties || []);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setDetailError('경기와 심판 점수를 불러오지 못했습니다.');
        }
      })
      .finally(() => {
        if (!cancelled) {
          setDetailLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [reloadKey, selectedBoutId, session.accessToken]);

  async function handlePenalty(event) {
    event.preventDefault();
    const penaltyPoint = Number(penaltyForm.penaltyPoint);
    if (!Number.isInteger(penaltyPoint) || penaltyPoint < 0) {
      setActionError('패널티 점수는 0 이상의 정수로 입력하세요.');
      return;
    }
    setActionLoading(true);
    setActionError('');
    try {
      const created = await createPenalty(selectedBoutId, { ...penaltyForm, createdBy: session.account.accountId, penaltyPoint }, session.accessToken);
      setPenalties((current) => [...current, created]);
      setPenaltyForm({ penaltyPoint: '', reason: '', targetSide: penaltyForm.targetSide });
    } catch {
      setActionError('패널티 등록에 실패했습니다.');
    } finally {
      setActionLoading(false);
    }
  }

  async function handleResult(event) {
    event.preventDefault();
    setActionLoading(true);
    setActionError('');
    try {
      const confirmed = await confirmResult(selectedBoutId, { ...resultForm, confirmedBy: session.account.accountId }, session.accessToken);
      setResult(confirmed);
      setBout((current) => current ? { ...current, resultConfirmed: true, status: 'FINISHED' } : current);
    } catch (error) {
      setActionError(error.message === 'RESULT_ALREADY_CONFIRMED' ? '이미 확정된 결과입니다.' : '결과 확정에 실패했습니다.');
    } finally {
      setActionLoading(false);
    }
  }

  const confirmed = Boolean(result || bout?.resultConfirmed);

  return (
    <main className="page-shell supervisor-shell">
      <div className="judge-heading">
        <div>
          <p className="eyebrow">SUPERVISOR DESK</p>
          <h2>결과 검토 및 확정</h2>
          <p>로그인 계정: {session.account.name} · 감독자 ID {session.account.accountId}</p>
        </div>
        <button className="secondary-button" onClick={onLogout} type="button">로그아웃</button>
      </div>

      {listLoading ? <StatePanel title="경기 목록을 불러오는 중입니다.">잠시만 기다려 주세요.</StatePanel> : null}
      {listError ? <StatePanel action={<button className="command-button" onClick={loadBouts} type="button">다시 시도</button>} title="경기 목록을 불러오지 못했습니다." tone="error">대회 ID를 확인하거나 잠시 후 다시 시도해 주세요.</StatePanel> : null}
      {!listLoading && !listError ? (
        <div className="judge-layout">
          <aside>
            <div className="judge-section-heading">
              <div><p className="eyebrow">TOURNAMENT {tournamentId}</p><h3>경기 선택</h3></div>
              <span>{sortedBouts.length}경기</span>
            </div>
            <BoutPicker bouts={sortedBouts} onSelect={setSelectedBoutId} selectedBoutId={selectedBoutId} />
          </aside>

          <section aria-label="결과 검토" className="judge-score-panel supervisor-panel">
            {detailLoading ? <StatePanel title="검토 자료를 불러오는 중입니다.">심판 점수와 경기 정보를 준비하고 있습니다.</StatePanel> : null}
            {detailError && !detailLoading ? <StatePanel action={<button className="command-button" onClick={() => setReloadKey((current) => current + 1)} type="button">다시 시도</button>} title="검토 자료를 불러오지 못했습니다." tone="error">선택한 경기의 상태를 확인해 주세요.</StatePanel> : null}
            {!detailLoading && !detailError && !bout && !sortedBouts.length ? <StatePanel title="검토할 경기가 없습니다.">대회에 등록된 공식 경기를 선택하면 결과를 검토할 수 있습니다.</StatePanel> : null}
            {!detailLoading && !detailError && bout ? (
              <>
                <div className="judge-bout-heading">
                  <div><p className="eyebrow">BOUT {bout.boutNumber}</p><h3>{bout.matchType || '일반 경기'}</h3><p>{boutStatus(bout.status)} · {bout.currentRound || 0}/{bout.totalRounds || '?'} 라운드</p></div>
                  <span className="status-pill">{confirmed ? '결과 확정' : boutStatus(bout.status)}</span>
                </div>
                <div className="judge-athletes">
                  <div className="judge-athlete red-side"><span>R</span><strong>{athleteName(bout.redAthlete)}</strong><small>{bout.redAthlete?.affiliation || '소속 미정'}</small></div>
                  <div className="versus">VS</div>
                  <div className="judge-athlete blue-side"><span>B</span><strong>{athleteName(bout.blueAthlete)}</strong><small>{bout.blueAthlete?.affiliation || '소속 미정'}</small></div>
                </div>

                <section className="supervisor-section">
                  <div className="supervisor-section-heading"><div><p className="eyebrow">SCORES</p><h3>심판 점수</h3></div><span>{scores.length}건 제출</span></div>
                  <div className="supervisor-score-summary">
                    <div className="supervisor-total red-total"><span>레드 합계</span><strong>{scoreSummary.red}</strong><small>패널티 -{penaltySummary.red}</small></div>
                    <div className="supervisor-total blue-total"><span>블루 합계</span><strong>{scoreSummary.blue}</strong><small>패널티 -{penaltySummary.blue}</small></div>
                  </div>
                  <ScoreReview scores={scores} />
                </section>

                <section className="supervisor-section">
                  <div className="supervisor-section-heading"><div><p className="eyebrow">PENALTIES</p><h3>심판 패널티</h3></div><span>{penalties.length}건 등록</span></div>
                  <form className="supervisor-action-form" onSubmit={handlePenalty}>
                    <label>대상<select onChange={(event) => setPenaltyForm((current) => ({ ...current, targetSide: event.target.value }))} value={penaltyForm.targetSide}><option value="RED">레드</option><option value="BLUE">블루</option></select></label>
                    <label>점수<input min="0" onChange={(event) => setPenaltyForm((current) => ({ ...current, penaltyPoint: event.target.value }))} required type="number" value={penaltyForm.penaltyPoint} /></label>
                    <label className="wide-field">사유<input onChange={(event) => setPenaltyForm((current) => ({ ...current, reason: event.target.value }))} placeholder="선택 입력" value={penaltyForm.reason} /></label>
                    <button className="command-button" disabled={actionLoading || confirmed} type="submit">패널티 등록</button>
                  </form>
                  {penalties.length ? <ul className="penalty-list">{penalties.map((penalty) => <li key={penalty.penaltyId}><strong>{penalty.targetSide === 'RED' ? '레드' : '블루'} -{penalty.penaltyPoint}</strong><span>{penalty.reason || '사유 없음'}</span></li>)}</ul> : <p className="empty-copy">현재 화면에서 등록한 패널티가 없습니다.</p>}
                </section>

                <section className="supervisor-section result-confirm-section">
                  <div className="supervisor-section-heading"><div><p className="eyebrow">RESULT</p><h3>결과 확정</h3></div><span>{confirmed ? '공개 가능' : '검토 필요'}</span></div>
                  {confirmed ? <div className="confirmed-result supervisor-confirmed"><strong>{result?.winnerSide === 'RED' ? '레드 승' : result?.winnerSide === 'BLUE' ? '블루 승' : '무승부'}</strong><span>{DECISION_LABELS[result?.decisionType] || result?.decisionType}</span><small>결과가 확정되어 관객 화면에 반영됩니다.</small></div> : (
                    <form className="supervisor-action-form result-form" onSubmit={handleResult}>
                      <label>승자<select onChange={(event) => setResultForm((current) => ({ ...current, winnerSide: event.target.value }))} value={resultForm.winnerSide}><option value="RED">레드</option><option value="BLUE">블루</option><option value="DRAW">무승부</option></select></label>
                      <label>판정<select onChange={(event) => setResultForm((current) => ({ ...current, decisionType: event.target.value }))} value={resultForm.decisionType}>{Object.entries(DECISION_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
                      <button className="command-button" disabled={actionLoading} type="submit">결과 확정</button>
                    </form>
                  )}
                  {actionError && <p aria-live="polite" className="form-error" role="alert">{actionError}</p>}
                </section>
              </>
            ) : null}
          </section>
        </div>
      ) : null}
    </main>
  );
}

export function SupervisorPage({ tournamentId }) {
  const [session, setSession] = useState(readSession);

  function handleLogin(nextSession) {
    window.sessionStorage.setItem(SESSION_KEY, JSON.stringify(nextSession));
    setSession(nextSession);
  }

  async function handleLogout() {
    if (session?.accessToken) {
      await logout(session.accessToken).catch(() => undefined);
    }
    window.sessionStorage.removeItem(SESSION_KEY);
    setSession(null);
  }

  return session ? <SupervisorWorkspace onLogout={handleLogout} session={session} tournamentId={tournamentId} /> : <LoginForm onLogin={handleLogin} />;
}
