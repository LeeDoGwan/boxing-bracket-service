import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { getBoutDetail } from '../api/audience';
import { login, logout } from '../api/auth';
import { getJudgeScores, submitRoundScore } from '../api/judge';
import { getAssignedBouts, getAssignedRings } from '../api/staffAssignments';
import { StatePanel } from '../components/StatePanel';
import { useBoutEventStream } from '../hooks/useBoutEventStream';
import { useEventRefresh } from '../hooks/useEventRefresh';

const JUDGE_EVENT_TYPES = ['BOUT_STARTED', 'BOUT_STATUS_CHANGED', 'ROUND_STARTED', 'NEXT_BOUT_READY', 'RESULT_CONFIRMED'];
const SCORE_ERROR_MESSAGES = {
  BOUT_ACCESS_DENIED: 'You are not assigned to this bout.',
  BOUT_NOT_STARTED: 'The bout must be started before scores can be submitted.',
  INVALID_BOUT_STATE: 'Scores cannot be submitted for this bout.',
  INVALID_ROUND_NUMBER: 'This round is outside the bout round range.',
  INVALID_SCORE_VALUE: 'Scores must be non-negative whole numbers.',
  RING_ACCESS_DENIED: 'Your ring assignment is no longer active.',
  ROUND_NOT_STARTED: 'This round has not started yet.',
  SCORE_ALREADY_SUBMITTED: 'This round score was already submitted.',
};

function scoreErrorMessage(requestError) {
  if (requestError.status === 401) return 'Session expired.';
  return SCORE_ERROR_MESSAGES[requestError.message] || requestError.message || 'Score submission failed.';
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
      setError(requestError.status === 401 ? 'Session expired.' : requestError.status === 403 ? 'Assignment access denied.' : 'Assigned rings could not be loaded.');
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
      setError(requestError.status === 401 ? 'Session expired.' : 'Selected ring access was revoked.');
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
        setDetailError(requestError.status === 403 ? 'Bout access denied.' : 'Bout data could not be loaded.');
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
  if (loading) return <main className="page-shell"><StatePanel title="Loading assigned rings">Please wait.</StatePanel></main>;
  return <main className="page-shell judge-shell">
    <div className="judge-heading">
      <div><p className="eyebrow">JUDGE DESK</p><h2>Judge scoring</h2><p>{session.account.name} | Tournament {tournamentId}</p><p aria-live="polite" className="stream-status" data-testid="stream-status">Live updates: {streamState}</p></div>
      <button className="secondary-button" onClick={onLogout} type="button">Sign out</button>
    </div>
    {error && <StatePanel action={<button className="command-button" onClick={loadRings} type="button">Retry</button>} title={error} tone="error">Access is controlled by active assignments.</StatePanel>}
    {!error && !rings.length && <StatePanel title="No assigned rings">Ask an administrator to assign a ring before scoring.</StatePanel>}
    {!error && rings.length > 0 && <div className="judge-layout">
      <aside>
        <label>Assigned ring<select aria-label="Assigned ring" onChange={(event) => { setRingId(Number(event.target.value)); setBoutId(null); }} value={ringId || ''}>{rings.map((ring) => <option key={ring.ringId} value={ring.ringId}>{ring.name} #{ring.ringId}</option>)}</select></label>
        <h3>Assigned bouts</h3>
        {bouts.length ? <div className="judge-bout-list">{bouts.map((item) => <button className={`judge-bout-option${boutId === item.boutId ? ' selected' : ''}`} key={item.boutId} onClick={() => setBoutId(item.boutId)} type="button"><span>Bout {item.boutNumber}</span><strong>{item.matchType || 'Official bout'}</strong><small>{item.status}</small></button>)}</div> : <StatePanel title="No bouts">This ring has no official bouts.</StatePanel>}
      </aside>
      <section className="judge-score-panel" aria-label="Score entry">
        {detailError && <StatePanel title={detailError} tone="error">Refresh the assigned ring before retrying.</StatePanel>}
        {bout && <><h3>Bout {bout.boutNumber}</h3><p>{bout.redAthlete?.name || 'Red'} vs {bout.blueAthlete?.name || 'Blue'}</p>{rounds.map((roundNo) => <ScoreCard key={`${bout.boutId}-${roundNo}`} busy={busy} bout={bout} onSubmit={submit} roundNo={roundNo} score={scores.find((score) => score.roundNo === roundNo)} />)}</>}
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
    } catch (requestError) { setError(requestError.message === 'JUDGE_ONLY' ? 'Judge role is required.' : 'Login failed.'); }
  }
  async function signOut() { await logout(session.accessToken).catch(() => undefined); window.sessionStorage.removeItem(SESSION_KEY); setSession(null); }
  if (!session) return <main className="page-shell auth-shell"><section className="auth-panel"><h2>Judge scoring</h2><form onSubmit={signIn}><label>Login ID<input required value={loginId} onChange={(event) => setLoginId(event.target.value)} /></label><label>Password<input required type="password" value={password} onChange={(event) => setPassword(event.target.value)} /></label>{error && <p className="form-error" role="alert">{error}</p>}<button className="command-button" type="submit">Sign in</button></form></section></main>;
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
  const roundStatus = submitted ? 'Submitted' : closed ? 'Closed' : futureRound ? 'Not started' : roundNo === currentRound ? 'Current round' : 'Previous round';
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
      setValidationError('Both scores are required.');
      return;
    }
    if (!/^\d+$/.test(String(redScore)) || !/^\d+$/.test(String(blueScore))) {
      setValidationError('Scores must be non-negative whole numbers.');
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
    <h4>Round {roundNo}</h4><p className="round-status">{roundStatus}</p>
    <label>Red score<input aria-describedby={validationError ? `round-${roundNo}-error` : undefined} disabled={busy || submitted || closed || futureRound} inputMode="numeric" min="0" pattern="[0-9]*" required type="number" value={redScore} onChange={(event) => { setDirty(true); setValidationError(''); setRedScore(event.target.value); }} /></label>
    <label>Blue score<input aria-describedby={validationError ? `round-${roundNo}-error` : undefined} disabled={busy || submitted || closed || futureRound} inputMode="numeric" min="0" pattern="[0-9]*" required type="number" value={blueScore} onChange={(event) => { setDirty(true); setValidationError(''); setBlueScore(event.target.value); }} /></label>
    {validationError && <p className="form-error" id={`round-${roundNo}-error`} role="alert">{validationError}</p>}
    {!submitted && !closed && <button className="command-button" disabled={!canSubmit} type="submit">Submit round</button>}
    {confirming && <div aria-label={`Confirm round ${roundNo} score`} className="score-confirmation" role="dialog"><p>Round {roundNo}: Red {redScore}, Blue {blueScore}</p><div><button className="command-button" disabled={busy} onClick={confirmSubmission} type="button">Confirm score</button><button className="secondary-button" onClick={() => setConfirming(false)} type="button">Cancel</button></div></div>}
  </form>;
}
