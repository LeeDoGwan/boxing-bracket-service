import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { getBoutDetail } from '../api/audience';
import { login, logout } from '../api/auth';
import { confirmResult, correctResult, createPenalty, getPenalties, getSupervisorScores } from '../api/supervisor';
import { getAssignedBouts, getAssignedRings } from '../api/staffAssignments';
import { StatePanel } from '../components/StatePanel';
import { useBoutEventStream } from '../hooks/useBoutEventStream';
import { useEventRefresh } from '../hooks/useEventRefresh';
import { decisionLabel, statusLabel, streamStateLabel } from '../utils';

const SESSION_KEY = 'boxing.supervisor.session';
const SUPERVISOR_EVENT_TYPES = ['BOUT_STARTED', 'BOUT_STATUS_CHANGED', 'ROUND_STARTED', 'NEXT_BOUT_READY', 'SCORE_SUBMITTED', 'RESULT_CONFIRMED', 'RESULT_CORRECTED'];
const DECISION_TYPES = ['POINTS', 'KO', 'RSC', 'ABD', 'DSQ', 'WALKOVER'];

function readSession() {
  try {
    const value = JSON.parse(window.sessionStorage.getItem(SESSION_KEY) || 'null');
    return value?.accessToken && value?.account?.role === 'SUPERVISOR' ? value : null;
  } catch { return null; }
}

function requestMessage(error, fallback) {
  if (error?.status === 401) return '세션이 만료되었습니다. 다시 로그인해 주세요.';
  if (error?.status === 403 || error?.message === 'RING_ACCESS_DENIED' || error?.message === 'BOUT_ACCESS_DENIED') return '배정 권한이 없습니다. 관리자에게 확인해 주세요.';
  const messages = {
    ACTOR_ID_MISMATCH: '로그인한 계정으로 처리해야 합니다.',
    BOUT_NOT_STARTED: '경기가 시작되어야 처리할 수 있습니다.',
    INVALID_BOUT_STATE: '현재 경기 상태에서는 처리할 수 없습니다.',
    INVALID_ROUND_NUMBER: '이 경기의 라운드를 선택해 주세요.',
    INVALID_PENALTY_VALUE: '패널티 점수는 1 이상의 정수로 입력해 주세요.',
    INVALID_RESULT_DECISION: '올바른 결과 방식을 선택해 주세요.',
    INVALID_WINNER_SELECTION: '승자와 결과 방식의 조합을 확인해 주세요.',
    PENALTY_NOT_ALLOWED: '결과 확정 후에는 패널티를 추가할 수 없습니다.',
    RESULT_ALREADY_CONFIRMED: '이미 확정된 결과입니다.',
    RESULT_NOT_FOUND: '확정된 결과가 없습니다.',
    RESULT_NOT_CONFIRMED: '확정된 결과만 수정할 수 있습니다.',
    reason: '수정 사유를 입력해 주세요.',
    SCORES_NOT_READY: '시작된 모든 라운드의 심판 점수가 제출되어야 합니다.',
  };
  return messages[error?.message] || fallback;
}

function resultLabel(result) {
  if (!result) return '';
  const winner = result.winnerSide === 'RED' ? '빨강 승' : result.winnerSide === 'BLUE' ? '파랑 승' : '무승부';
  const decision = decisionLabel(result.decisionType);
  return `${winner} · ${decision}`;
}

function ScoreReview({ scores }) {
  if (!scores.length) return <p className="empty-copy">등록된 심판 점수가 없습니다.</p>;
  return <div className="supervisor-score-table" role="table">
    <div className="supervisor-score-row supervisor-score-header" role="row">
      <span>라운드</span><span>심판</span><span>빨강</span><span>파랑</span><span>상태</span>
    </div>
    {scores.map((score) => <div className="supervisor-score-row" key={`${score.roundNo}-${score.judgeId}`} role="row">
      <span>{score.roundNo}R</span>
      <span>#{score.judgeId}</span>
      <strong className="red-score-text">{score.redScore ?? '-'}</strong>
      <strong className="blue-score-text">{score.blueScore ?? '-'}</strong>
      <span>{score.status === 'SUBMITTED' ? '제출 완료' : '작성 중'}</span>
    </div>)}
  </div>;
}

export function SupervisorAssignedPage({ session, onLogout, tournamentId }) {
  const [rings, setRings] = useState([]);
  const [ringId, setRingId] = useState(null);
  const [bouts, setBouts] = useState([]);
  const [boutId, setBoutId] = useState(null);
  const [bout, setBout] = useState(null);
  const [scores, setScores] = useState([]);
  const [penalties, setPenalties] = useState([]);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');
  const [penalty, setPenalty] = useState({ penaltyPoint: '', reason: '', roundNo: '1', targetSide: 'RED' });
  const [resultForm, setResultForm] = useState({ decisionType: 'POINTS', winnerSide: 'RED' });
  const [correctionForm, setCorrectionForm] = useState({ decisionType: 'POINTS', reason: '', winnerSide: 'RED' });
  const [confirmingResult, setConfirmingResult] = useState(false);
  const [confirmingCorrection, setConfirmingCorrection] = useState(false);
  const [busy, setBusy] = useState(false);
  const busyRef = useRef(false);

  const submittedScores = useMemo(() => scores.filter((score) => score.status === 'SUBMITTED'), [scores]);
  const draftScores = useMemo(() => scores.filter((score) => score.status !== 'SUBMITTED'), [scores]);
  const scoreTotals = useMemo(() => submittedScores.reduce((summary, score) => ({
    blue: summary.blue + (score.blueScore || 0),
    red: summary.red + (score.redScore || 0),
  }), { blue: 0, red: 0 }), [submittedScores]);
  const penaltyTotals = useMemo(() => penalties.reduce((summary, item) => ({
    blue: summary.blue + (item.targetSide === 'BLUE' ? item.penaltyPoint || 0 : 0),
    red: summary.red + (item.targetSide === 'RED' ? item.penaltyPoint || 0 : 0),
  }), { blue: 0, red: 0 }), [penalties]);
  const confirmed = Boolean(result || bout?.resultConfirmed);
  const scoreReady = submittedScores.length > 0 && draftScores.length === 0;
  const boutStarted = bout?.status === 'IN_PROGRESS' || bout?.status === 'SCORING';
  const canConfirm = !confirmed && boutStarted && scoreReady;
  const effectiveRed = scoreTotals.red + penaltyTotals.blue;
  const effectiveBlue = scoreTotals.blue + penaltyTotals.red;
  const penaltyRounds = useMemo(() => bout
    ? Array.from({ length: Math.max(1, bout.totalRounds || bout.currentRound || 1) }, (_, index) => index + 1)
    : [], [bout]);
  const expectedWinner = effectiveRed === effectiveBlue ? 'DRAW' : effectiveRed > effectiveBlue ? 'RED' : 'BLUE';
  const totalsTied = effectiveRed === effectiveBlue;
  const winnerMismatch = resultForm.decisionType === 'POINTS'
    && expectedWinner !== 'DRAW'
    && resultForm.winnerSide !== expectedWinner;

  const loadRings = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const next = await getAssignedRings(tournamentId, session.accessToken) || [];
      setRings(next);
      setRingId((current) => current && next.some((item) => item.ringId === current) ? current : next[0]?.ringId || null);
    } catch (requestError) {
      setRings([]);
      setRingId(null);
      setError(requestMessage(requestError, '배정된 링을 불러오지 못했습니다.'));
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
      const next = await getAssignedBouts(ringId, session.accessToken) || [];
      setBouts(next);
      setBoutId((current) => current && next.some((item) => item.boutId === current) ? current : next[0]?.boutId || null);
      return next;
    } catch (requestError) {
      setBouts([]);
      setBoutId(null);
      setError(requestMessage(requestError, '배정된 경기를 불러오지 못했습니다.'));
      return [];
    }
  }, [ringId, session.accessToken]);

  const loadDetail = useCallback(async (targetBoutId, isActive = () => true) => {
    if (!targetBoutId) {
      if (isActive()) {
        setBout(null);
        setScores([]);
        setPenalties([]);
        setResult(null);
      }
      return;
    }
    try {
      const [nextBout, nextScores, nextPenalties] = await Promise.all([
        getBoutDetail(targetBoutId),
        getSupervisorScores(targetBoutId, session.accessToken),
        getPenalties(targetBoutId, session.accessToken),
      ]);
      if (isActive()) {
        setBout(nextBout);
        setScores(nextScores || []);
        setPenalties(nextPenalties || []);
        setResult(nextBout.result || null);
        setCorrectionForm({
          decisionType: nextBout.result?.decisionType || 'POINTS',
          reason: '',
          winnerSide: nextBout.result?.winnerSide || 'RED',
        });
      }
    } catch (requestError) {
      if (isActive()) setActionError(requestMessage(requestError, '검토할 정보를 불러오지 못했습니다.'));
    }
  }, [session.accessToken]);

  useEffect(() => { loadRings(); }, [loadRings]);
  useEffect(() => { loadBouts(); }, [loadBouts]);
  useEffect(() => {
    setPenalty({ penaltyPoint: '', reason: '', roundNo: '1', targetSide: 'RED' });
    setResultForm({ decisionType: 'POINTS', winnerSide: 'RED' });
    setCorrectionForm({ decisionType: 'POINTS', reason: '', winnerSide: 'RED' });
    setConfirmingResult(false);
    setConfirmingCorrection(false);
    setActionError('');
    let active = true;
    loadDetail(boutId, () => active);
    return () => { active = false; };
  }, [boutId, loadDetail]);

  const refreshLiveData = useEventRefresh(async () => {
    const nextBouts = await loadBouts();
    if (boutId && nextBouts.some((item) => item.boutId === boutId)) await loadDetail(boutId);
  });
  const handleStreamEvent = useCallback(() => refreshLiveData(), [refreshLiveData]);
  const streamState = useBoutEventStream(tournamentId, {
    enabled: Boolean(ringId),
    eventTypes: SUPERVISOR_EVENT_TYPES,
    onEvent: handleStreamEvent,
    ringId,
  });

  async function addPenalty(event) {
    event.preventDefault();
    const penaltyPoint = Number(penalty.penaltyPoint);
    const roundNo = Number(penalty.roundNo);
    if (!Number.isInteger(roundNo) || roundNo < 1 || (bout?.totalRounds && roundNo > bout.totalRounds)) {
      setActionError('패널티를 적용할 라운드를 선택해 주세요.');
      return;
    }
    if (!Number.isInteger(penaltyPoint) || penaltyPoint < 1) {
      setActionError('패널티 점수는 1 이상의 정수로 입력해 주세요.');
      return;
    }
    if (busyRef.current || confirmed) return;
    busyRef.current = true;
    setBusy(true);
    setActionError('');
    try {
      const saved = await createPenalty(boutId, { ...penalty, penaltyPoint, roundNo }, session.accessToken);
      setPenalties((current) => [...current, saved]);
      setPenalty((current) => ({ ...current, penaltyPoint: '', reason: '' }));
    } catch (requestError) {
      setActionError(requestMessage(requestError, '패널티 등록에 실패했습니다.'));
    } finally {
      busyRef.current = false;
      setBusy(false);
    }
  }

  function startResultConfirmation(event) {
    event.preventDefault();
    if (!canConfirm) {
      setActionError(boutStarted ? '시작된 모든 라운드의 심판 점수가 제출되어야 합니다.' : '경기가 시작되어야 결과를 확정할 수 있습니다.');
      return;
    }
    setActionError('');
    setConfirmingResult(true);
  }

  async function submitResult() {
    if (busyRef.current || !canConfirm) return;
    busyRef.current = true;
    setBusy(true);
    setActionError('');
    try {
      const saved = await confirmResult(boutId, resultForm, session.accessToken);
      const confirmedResult = saved || resultForm;
      setResult(confirmedResult);
      setBout((current) => current ? { ...current, result: confirmedResult, resultConfirmed: true, status: 'FINISHED' } : current);
      setConfirmingResult(false);
    } catch (requestError) {
      setActionError(requestMessage(requestError, '결과 확정에 실패했습니다.'));
    } finally {
      busyRef.current = false;
      setBusy(false);
    }
  }

  function startResultCorrection(event) {
    event.preventDefault();
    if (!correctionForm.reason.trim()) {
      setActionError('수정 사유를 입력해 주세요.');
      return;
    }
    setActionError('');
    setConfirmingCorrection(true);
  }

  async function submitResultCorrection() {
    if (busyRef.current || !correctionForm.reason.trim()) return;
    busyRef.current = true;
    setBusy(true);
    setActionError('');
    try {
      const saved = await correctResult(boutId, {
        decisionType: correctionForm.decisionType,
        reason: correctionForm.reason.trim(),
        winnerSide: correctionForm.winnerSide,
      }, session.accessToken);
      const correctedResult = saved || { ...result, ...correctionForm };
      setResult(correctedResult);
      setBout((current) => current ? { ...current, result: correctedResult, resultConfirmed: true, status: 'FINISHED', winnerSide: correctedResult.winnerSide } : current);
      setCorrectionForm((current) => ({ ...current, reason: '' }));
      setConfirmingCorrection(false);
    } catch (requestError) {
      setActionError(requestMessage(requestError, '결과 수정에 실패했습니다.'));
    } finally {
      busyRef.current = false;
      setBusy(false);
    }
  }

  if (loading) return <main className="page-shell"><StatePanel title="배정된 링을 불러오는 중">잠시 기다려 주세요.</StatePanel></main>;
  return <main className="page-shell supervisor-shell">
    <div className="judge-heading">
      <div><p className="eyebrow">감독관</p><h2>감독관 작업실</h2><p>{session.account.name} | 대회 {tournamentId}</p><p aria-live="polite" className="stream-status" data-testid="stream-status">실시간 연결: {streamStateLabel(streamState)}</p></div>
      <button className="secondary-button" onClick={onLogout} type="button">로그아웃</button>
    </div>
    {error && <StatePanel action={<button className="command-button" onClick={loadRings} type="button">다시 시도</button>} title={error} tone="error">활성 상태로 배정된 링에만 접근할 수 있습니다.</StatePanel>}
    {!error && !rings.length && <StatePanel title="배정된 링이 없습니다">관리자에게 링 배정을 요청해 주세요.</StatePanel>}
    {!error && rings.length > 0 && <div className="judge-layout">
      <aside>
        <label>배정된 링<select aria-label="배정된 링" onChange={(event) => { setRingId(Number(event.target.value)); setBoutId(null); }} value={ringId || ''}>{rings.map((ring) => <option key={ring.ringId} value={ring.ringId}>{ring.name} #{ring.ringId}</option>)}</select></label>
        <h3>배정된 경기</h3>
        {bouts.length ? bouts.map((item) => <button className={`judge-bout-option${boutId === item.boutId ? ' selected' : ''}`} key={item.boutId} onClick={() => setBoutId(item.boutId)} type="button"><strong>경기 {item.boutNumber}</strong><small>{statusLabel(item.status)}</small></button>) : <StatePanel title="경기가 없습니다">이 링에는 공식 경기가 없습니다.</StatePanel>}
      </aside>
      <section className="judge-score-panel">
        {bout ? <>
          <h3>경기 {bout.boutNumber}</h3>
          <p>{bout.redAthlete?.name || '빨강'} vs {bout.blueAthlete?.name || '파랑'}</p>
          <h4>심판 점수</h4>
          <div className="supervisor-score-summary">
            <div className="supervisor-total red-total"><span>빨강 합계</span><strong>{scoreTotals.red}</strong><small>파랑 패널티 +{penaltyTotals.blue} | 유효 점수 {effectiveRed}</small></div>
            <div className="supervisor-total blue-total"><span>파랑 합계</span><strong>{scoreTotals.blue}</strong><small>빨강 패널티 +{penaltyTotals.red} | 유효 점수 {effectiveBlue}</small></div>
          </div>
          <p aria-live="polite" className="score-readiness">제출 완료 {submittedScores.length}건 · 작성 중 {draftScores.length}건</p>
          <ScoreReview scores={scores} />
          <h4>패널티</h4>
          <ul>{penalties.map((item) => <li key={item.penaltyId}>라운드 {item.roundNo || '?'} · {item.targetSide === 'RED' ? '빨강 패널티 → 파랑' : '파랑 패널티 → 빨강'} +{item.penaltyPoint}점 {item.reason}</li>)}</ul>
          <form noValidate onSubmit={addPenalty}>
            <label>라운드<select disabled={busy || confirmed} onChange={(event) => setPenalty({ ...penalty, roundNo: event.target.value })} required value={penalty.roundNo}>{penaltyRounds.map((roundNo) => <option key={roundNo} value={roundNo}>{roundNo}라운드</option>)}</select></label>
            <label>패널티 대상<select disabled={busy || confirmed} onChange={(event) => setPenalty({ ...penalty, targetSide: event.target.value })} value={penalty.targetSide}><option value="RED">빨강</option><option value="BLUE">파랑</option></select></label>
            <label>패널티 점수<input disabled={busy || confirmed} inputMode="numeric" min="1" onChange={(event) => setPenalty({ ...penalty, penaltyPoint: event.target.value })} required type="number" value={penalty.penaltyPoint} /></label>
            <label>패널티 사유<input disabled={busy || confirmed} onChange={(event) => setPenalty({ ...penalty, reason: event.target.value })} value={penalty.reason} /></label>
            <button className="command-button" disabled={busy || confirmed} type="submit">패널티 추가</button>
          </form>
          <h4>경기 결과</h4>
          {confirmed ? <>
            <div className="confirmed-result supervisor-confirmed"><strong>{resultLabel(result)}</strong><small>결과가 확정되었습니다. 감독관은 사유를 입력하여 결과를 수정할 수 있습니다.</small></div>
            <form noValidate onSubmit={startResultCorrection}>
              <label>승자<select disabled={busy} onChange={(event) => setCorrectionForm({ ...correctionForm, winnerSide: event.target.value })} value={correctionForm.winnerSide}><option value="RED">빨강</option><option value="BLUE">파랑</option><option value="DRAW">무승부</option></select></label>
              <label>결과 방식<select disabled={busy} onChange={(event) => setCorrectionForm({ ...correctionForm, decisionType: event.target.value })} value={correctionForm.decisionType}>{DECISION_TYPES.map((value) => <option key={value} value={value}>{decisionLabel(value)}</option>)}</select></label>
              <label>수정 사유<textarea disabled={busy} maxLength="500" onChange={(event) => setCorrectionForm({ ...correctionForm, reason: event.target.value })} required value={correctionForm.reason} /></label>
              <button className="command-button" disabled={busy} type="submit">수정 내용 검토</button>
            </form>
          </> : <form noValidate onSubmit={startResultConfirmation}>
            <label>승자<select disabled={busy} onChange={(event) => setResultForm({ ...resultForm, winnerSide: event.target.value })} value={resultForm.winnerSide}><option value="RED">빨강</option><option value="BLUE">파랑</option><option value="DRAW">무승부</option></select></label>
            <label>결과 방식<select disabled={busy} onChange={(event) => setResultForm({ ...resultForm, decisionType: event.target.value })} value={resultForm.decisionType}>{DECISION_TYPES.map((value) => <option key={value} value={value}>{decisionLabel(value)}</option>)}</select></label>
            <button className="command-button" disabled={busy || !canConfirm} type="submit">결과 검토</button>
          </form>}
          {confirmingResult && !confirmed && <div aria-label="결과 확정" className="score-confirmation" role="dialog">
            <strong>결과 확정</strong>
            <p>경기 {bout.boutNumber}: {bout.redAthlete?.name || '빨강'} vs {bout.blueAthlete?.name || '파랑'}</p>
            <p>{resultLabel(resultForm)} | 유효 점수 {effectiveRed}-{effectiveBlue}</p>
            {totalsTied && <p>유효 점수가 동점입니다. 감독관이 승자 또는 무승부를 결정합니다.</p>}
            {winnerMismatch && <p className="form-error" role="alert">선택한 승자가 유효 점수 비교 결과와 다릅니다.</p>}
            <div><button className="command-button" disabled={busy} onClick={submitResult} type="button">결과 확정</button><button className="secondary-button" disabled={busy} onClick={() => setConfirmingResult(false)} type="button">취소</button></div>
          </div>}
          {confirmingCorrection && confirmed && <div aria-label="결과 수정 확인" className="score-confirmation" role="dialog">
            <strong>결과 수정 확인</strong>
            <p>경기 {bout.boutNumber}: {resultLabel(correctionForm)}</p>
            <p>수정 사유: {correctionForm.reason}</p>
            <div><button className="command-button" disabled={busy} onClick={submitResultCorrection} type="button">수정 확정</button><button className="secondary-button" disabled={busy} onClick={() => setConfirmingCorrection(false)} type="button">취소</button></div>
          </div>}
          {actionError && <p aria-live="polite" className="form-error" role="alert">{actionError}</p>}
        </> : <StatePanel title="경기를 선택하세요">배정된 경기 중 검토할 경기를 선택해 주세요.</StatePanel>}
      </section>
    </div>}
  </main>;
}

export function AssignedSupervisorRoute({ tournamentId }) {
  const [session, setSession] = useState(readSession);
  const [loginId, setLoginId] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  async function signIn(event) {
    event.preventDefault();
    try {
      const next = await login(loginId, password);
      if (next.account?.role !== 'SUPERVISOR') throw new Error('SUPERVISOR_ONLY');
      window.sessionStorage.setItem(SESSION_KEY, JSON.stringify(next));
      setSession(next);
    } catch (requestError) { setError(requestError.message === 'SUPERVISOR_ONLY' ? '감독관 권한이 필요합니다.' : '로그인에 실패했습니다.'); }
  }
  async function signOut() { await logout(session.accessToken).catch(() => undefined); window.sessionStorage.removeItem(SESSION_KEY); setSession(null); }
  if (!session) return <main className="page-shell auth-shell"><section className="auth-panel"><h2>감독관 작업실</h2><form onSubmit={signIn}><label>로그인 ID<input required value={loginId} onChange={(event) => setLoginId(event.target.value)} /></label><label>비밀번호<input required type="password" value={password} onChange={(event) => setPassword(event.target.value)} /></label>{error && <p className="form-error" role="alert">{error}</p>}<button className="command-button" type="submit">로그인</button></form></section></main>;
  return <SupervisorAssignedPage onLogout={signOut} session={session} tournamentId={tournamentId} />;
}
