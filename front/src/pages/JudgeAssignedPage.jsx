import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { getBoutDetail } from '../api/audience';
import { login, logout } from '../api/auth';
import { getJudgeScores, submitRoundScore } from '../api/judge';
import { getAssignedBouts, getAssignedRings } from '../api/staffAssignments';
import { StatePanel } from '../components/StatePanel';
import { useBoutEventStream } from '../hooks/useBoutEventStream';
import { useEventRefresh } from '../hooks/useEventRefresh';
import { statusLabel, streamStateLabel } from '../utils';

const JUDGE_EVENT_TYPES = ['BOUT_STARTED', 'BOUT_STATUS_CHANGED', 'ROUND_STARTED', 'NEXT_BOUT_READY', 'RESULT_CONFIRMED'];
const MAX_SCORE = 10;
const SCORE_ERROR_MESSAGES = {
  BOUT_ACCESS_DENIED: '이 경기에 배정되지 않았습니다.',
  BOUT_NOT_STARTED: '경기가 시작되지 않았습니다.',
  INVALID_BOUT_STATE: '이 경기에는 점수를 제출할 수 없습니다.',
  INVALID_ROUND_NUMBER: '경기의 라운드 범위를 벗어났습니다.',
  INVALID_SCORE_VALUE: '점수는 0점부터 10점 사이의 정수로 입력해 주세요.',
  RING_ACCESS_DENIED: '링 배정이 비활성 상태입니다.',
  ROUND_NOT_STARTED: '해당 라운드가 아직 시작되지 않았습니다.',
  SCORE_ALREADY_SUBMITTED: '이미 제출된 점수입니다.',
};

function scoreErrorMessage(requestError) {
  if (requestError.status === 401) return '세션이 만료되었습니다. 다시 로그인해 주세요.';
  return SCORE_ERROR_MESSAGES[requestError.message] || '점수 제출에 실패했습니다.';
}

export function JudgeAssignedPage({ session, onLogout, tournamentId }) {
  const [rings, setRings] = useState([]);
  const [ringId, setRingId] = useState(null);
  const [bouts, setBouts] = useState([]);
  const [boutId, setBoutId] = useState(null);
  const [bout, setBout] = useState(null);
  const [scores, setScores] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [detailError, setDetailError] = useState('');
  const [busy, setBusy] = useState(false);
  const submitLockRef = useRef(false);

  const loadRings = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const nextRings = await getAssignedRings(tournamentId, session.accessToken) || [];
      setRings(nextRings);
      setRingId((current) => current && nextRings.some((ring) => ring.ringId === current) ? current : nextRings[0]?.ringId || null);
    } catch (requestError) {
      setRings([]);
      setRingId(null);
      setError(requestError.status === 401 ? '세션이 만료되었습니다. 다시 로그인해 주세요.' : requestError.status === 403 ? '배정 권한이 없습니다. 관리자에게 확인해 주세요.' : '배정된 링을 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  }, [session.accessToken, tournamentId]);

  const loadBouts = useCallback(async () => {
    if (!ringId) {
      setBouts([]);
      setBoutId(null);
      return [];
    }
    try {
      const nextBouts = await getAssignedBouts(ringId, session.accessToken) || [];
      setBouts(nextBouts);
      setBoutId((current) => current && nextBouts.some((item) => item.boutId === current) ? current : nextBouts[0]?.boutId || null);
      return nextBouts;
    } catch (requestError) {
      setBouts([]);
      setBoutId(null);
      setError(requestError.status === 401 ? '세션이 만료되었습니다. 다시 로그인해 주세요.' : '선택한 링에 접근할 수 없습니다. 배정을 확인해 주세요.');
      return [];
    }
  }, [ringId, session.accessToken]);

  const loadDetail = useCallback(async (targetBoutId, isActive = () => true) => {
    if (!targetBoutId) {
      if (isActive()) {
        setBout(null);
        setScores([]);
      }
      return;
    }
    setDetailError('');
    try {
      const [nextBout, nextScores] = await Promise.all([
        getBoutDetail(targetBoutId),
        getJudgeScores(targetBoutId, session.accessToken),
      ]);
      if (isActive()) {
        setBout(nextBout);
        setScores(nextScores || []);
      }
    } catch (requestError) {
      if (isActive()) {
        setDetailError(requestError.status === 403 ? '이 경기에 접근할 권한이 없습니다.' : '경기 정보를 불러오지 못했습니다.');
      }
      throw requestError;
    }
  }, [session.accessToken]);

  useEffect(() => { loadRings(); }, [loadRings]);
  useEffect(() => { loadBouts(); }, [loadBouts]);
  useEffect(() => {
    let active = true;
    loadDetail(boutId, () => active).catch(() => undefined);
    return () => { active = false; };
  }, [boutId, loadDetail]);

  const refreshLiveData = useEventRefresh(async () => {
    const nextBouts = await loadBouts();
    if (boutId && nextBouts.some((item) => item.boutId === boutId)) {
      await loadDetail(boutId);
    }
  });
  const handleStreamEvent = useCallback(() => refreshLiveData(), [refreshLiveData]);
  const streamState = useBoutEventStream(tournamentId, {
    enabled: Boolean(ringId),
    eventTypes: JUDGE_EVENT_TYPES,
    onEvent: handleStreamEvent,
    ringId,
  });

  async function submit(roundNo, score) {
    if (submitLockRef.current) return;
    submitLockRef.current = true;
    setBusy(true);
    try {
      const saved = await submitRoundScore(boutId, roundNo, score, session.accessToken);
      setScores((current) => [...current.filter((item) => item.roundNo !== roundNo), saved]);
    } catch (requestError) {
      setDetailError(scoreErrorMessage(requestError));
    } finally {
      setBusy(false);
      submitLockRef.current = false;
    }
  }

  const rounds = useMemo(() => bout ? Array.from({ length: Math.max(1, bout.totalRounds || bout.currentRound || 1) }, (_, index) => index + 1) : [], [bout]);
  if (loading) return <main className="page-shell"><StatePanel title="배정된 링을 불러오는 중">잠시 기다려 주세요.</StatePanel></main>;
  return <main className="page-shell judge-shell">
    <div className="judge-heading">
      <div><p className="eyebrow">심판</p><h2>심판 작업실</h2><p>{session.account.name} | 대회 {tournamentId}</p><p aria-live="polite" className="stream-status" data-testid="stream-status">실시간 연결: {streamStateLabel(streamState)}</p></div>
      <button className="secondary-button" onClick={onLogout} type="button">로그아웃</button>
    </div>
    {error && <StatePanel action={<button className="command-button" onClick={loadRings} type="button">다시 시도</button>} title={error} tone="error">활성 상태로 배정된 링에만 접근할 수 있습니다.</StatePanel>}
    {!error && !rings.length && <StatePanel title="배정된 링이 없습니다">관리자에게 링 배정을 요청해 주세요.</StatePanel>}
    {!error && rings.length > 0 && <div className="judge-layout">
      <aside>
        <label>배정된 링<select aria-label="배정된 링" onChange={(event) => { setRingId(Number(event.target.value)); setBoutId(null); }} value={ringId || ''}>{rings.map((ring) => <option key={ring.ringId} value={ring.ringId}>{ring.name} #{ring.ringId}</option>)}</select></label>
        <h3>배정된 경기</h3>
        {bouts.length ? <div className="judge-bout-list">{bouts.map((item) => <button className={`judge-bout-option${boutId === item.boutId ? ' selected' : ''}`} key={item.boutId} onClick={() => setBoutId(item.boutId)} type="button"><span>경기 {item.boutNumber}</span><strong>{item.matchType || '공식 경기'}</strong><small>{statusLabel(item.status)}</small></button>)}</div> : <StatePanel title="경기가 없습니다">이 링에는 공식 경기가 없습니다.</StatePanel>}
      </aside>
      <section className="judge-score-panel" aria-label="점수 입력">
        {detailError && <StatePanel title={detailError} tone="error">화면을 새로고침한 뒤 다시 시도해 주세요.</StatePanel>}
        {bout && <><h3>경기 {bout.boutNumber}</h3><p>{bout.redAthlete?.name || '빨강'} vs {bout.blueAthlete?.name || '파랑'}</p>{rounds.map((roundNo) => <ScoreCard key={`${bout.boutId}-${roundNo}`} busy={busy} bout={bout} onSubmit={submit} roundNo={roundNo} score={scores.find((score) => score.roundNo === roundNo)} />)}</>}
      </section>
    </div>}
  </main>;
}

const SESSION_KEY = 'boxing.judge.session';

export function AssignedJudgeRoute({ tournamentId }) {
  const [session, setSession] = useState(() => {
    try {
      const value = JSON.parse(window.sessionStorage.getItem(SESSION_KEY) || 'null');
      return value?.accessToken && value?.account?.role === 'JUDGE' ? value : null;
    } catch { return null; }
  });
  const [loginId, setLoginId] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  async function signIn(event) {
    event.preventDefault();
    try {
      const next = await login(loginId, password);
      if (next.account?.role !== 'JUDGE') throw new Error('JUDGE_ONLY');
      window.sessionStorage.setItem(SESSION_KEY, JSON.stringify(next));
      setSession(next);
    } catch (requestError) { setError(requestError.message === 'JUDGE_ONLY' ? '심판 권한이 필요합니다.' : '로그인에 실패했습니다.'); }
  }
  async function signOut() { await logout(session.accessToken).catch(() => undefined); window.sessionStorage.removeItem(SESSION_KEY); setSession(null); }
  if (!session) return <main className="page-shell auth-shell"><section className="auth-panel"><h2>심판 작업실</h2><form onSubmit={signIn}><label>로그인 ID<input required value={loginId} onChange={(event) => setLoginId(event.target.value)} /></label><label>비밀번호<input required type="password" value={password} onChange={(event) => setPassword(event.target.value)} /></label>{error && <p className="form-error" role="alert">{error}</p>}<button className="command-button" type="submit">로그인</button></form></section></main>;
  return <JudgeAssignedPage onLogout={signOut} session={session} tournamentId={tournamentId} />;
}

function ScoreCard({ bout, busy, onSubmit, roundNo, score }) {
  const [redScore, setRedScore] = useState(score?.redScore ?? '');
  const [blueScore, setBlueScore] = useState(score?.blueScore ?? '');
  const [dirty, setDirty] = useState(false);
  const [validationError, setValidationError] = useState('');
  const [confirming, setConfirming] = useState(false);
  const submittingRef = useRef(false);
  const submitted = score?.status === 'SUBMITTED';
  const closed = bout.resultConfirmed || bout.status === 'FINISHED';
  const currentRound = Number(bout.currentRound || 0);
  const futureRound = currentRound < roundNo;
  const canSubmit = !busy && !submitted && !closed && !futureRound && currentRound > 0;
  const roundStatus = submitted ? '제출 완료' : closed ? '종료' : futureRound ? '시작 전' : roundNo === currentRound ? '현재 라운드' : '이전 라운드';
  useEffect(() => {
    if (!dirty || score?.status === 'SUBMITTED') {
      setRedScore(score?.redScore ?? '');
      setBlueScore(score?.blueScore ?? '');
      setDirty(false);
    }
  }, [dirty, score?.blueScore, score?.redScore, score?.status]);
  function submit(event) {
    event.preventDefault();
    if (!canSubmit || confirming || submittingRef.current) return;
    if (redScore === '' || blueScore === '') {
      setValidationError('두 선수의 점수를 모두 입력해 주세요.');
      return;
    }
    if (!/^\d+$/.test(String(redScore)) || !/^\d+$/.test(String(blueScore))
      || Number(redScore) > MAX_SCORE || Number(blueScore) > MAX_SCORE) {
      setValidationError('점수는 0점부터 10점 사이의 정수로 입력해 주세요.');
      return;
    }
    setValidationError('');
    setConfirming(true);
  }
  async function confirmSubmission() {
    if (!canSubmit || submittingRef.current) return;
    submittingRef.current = true;
    setConfirming(false);
    try {
      await onSubmit(roundNo, { blueScore: Number(blueScore), redScore: Number(redScore) });
    } finally {
      submittingRef.current = false;
    }
  }
  return <form className={`judge-score-card${roundNo === currentRound ? ' current-round' : ''}`} noValidate onSubmit={submit}>
    <h4>라운드 {roundNo}</h4><p className="round-status">{roundStatus}</p>
    <label><span>빨강 점수 <small className="score-range-hint">0~10점</small></span><input aria-describedby={validationError ? `round-${roundNo}-error` : undefined} aria-label="빨강 점수" disabled={busy || submitted || closed || futureRound} inputMode="numeric" max={MAX_SCORE} min="0" pattern="[0-9]*" required type="number" value={redScore} onChange={(event) => { setDirty(true); setValidationError(''); setRedScore(event.target.value); }} /></label>
    <label><span>파랑 점수 <small className="score-range-hint">0~10점</small></span><input aria-describedby={validationError ? `round-${roundNo}-error` : undefined} aria-label="파랑 점수" disabled={busy || submitted || closed || futureRound} inputMode="numeric" max={MAX_SCORE} min="0" pattern="[0-9]*" required type="number" value={blueScore} onChange={(event) => { setDirty(true); setValidationError(''); setBlueScore(event.target.value); }} /></label>
    {validationError && <p className="form-error" id={`round-${roundNo}-error`} role="alert">{validationError}</p>}
    {!submitted && !closed && <button className="command-button" disabled={!canSubmit} type="submit">라운드 제출</button>}
    {confirming && <div aria-label={`라운드 ${roundNo} 점수 확인`} className="score-confirmation" role="dialog"><p>라운드 {roundNo}: 빨강 {redScore}, 파랑 {blueScore}</p><div><button className="command-button" disabled={busy} onClick={confirmSubmission} type="button">점수 제출 확정</button><button className="secondary-button" onClick={() => setConfirming(false)} type="button">취소</button></div></div>}
  </form>;
}
