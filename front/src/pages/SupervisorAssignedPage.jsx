import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { getBoutDetail } from '../api/audience';
import { login, logout } from '../api/auth';
import { confirmResult, createPenalty, getPenalties, getSupervisorScores } from '../api/supervisor';
import { getAssignedBouts, getAssignedRings } from '../api/staffAssignments';
import { StatePanel } from '../components/StatePanel';
import { useBoutEventStream } from '../hooks/useBoutEventStream';
import { useEventRefresh } from '../hooks/useEventRefresh';

const SESSION_KEY = 'boxing.supervisor.session';
const SUPERVISOR_EVENT_TYPES = ['BOUT_STARTED', 'BOUT_STATUS_CHANGED', 'ROUND_STARTED', 'NEXT_BOUT_READY', 'SCORE_SUBMITTED', 'RESULT_CONFIRMED'];
const DECISION_TYPES = [
  ['POINTS', 'Points'],
  ['KO', 'KO'],
  ['RSC', 'RSC'],
  ['ABD', 'Abandoned'],
  ['DSQ', 'Disqualification'],
  ['WALKOVER', 'Walkover'],
];

function readSession() {
  try {
    const value = JSON.parse(window.sessionStorage.getItem(SESSION_KEY) || 'null');
    return value?.accessToken && value?.account?.role === 'SUPERVISOR' ? value : null;
  } catch { return null; }
}

function requestMessage(error, fallback) {
  if (error?.status === 401) return 'Session expired.';
  if (error?.status === 403 || error?.message === 'RING_ACCESS_DENIED' || error?.message === 'BOUT_ACCESS_DENIED') return 'Assignment access denied.';
  const messages = {
    ACTOR_ID_MISMATCH: 'The authenticated account must submit this action.',
    BOUT_NOT_STARTED: 'The bout must start before this action.',
    INVALID_BOUT_STATE: 'This bout no longer accepts the action.',
    INVALID_PENALTY_VALUE: 'Penalty points must be a positive whole number.',
    INVALID_RESULT_DECISION: 'Select a valid result decision.',
    INVALID_WINNER_SELECTION: 'Select a valid winner.',
    PENALTY_NOT_ALLOWED: 'Penalties cannot be added after result confirmation.',
    RESULT_ALREADY_CONFIRMED: 'This result has already been confirmed.',
    SCORES_NOT_READY: 'All available judge scores must be submitted first.',
  };
  return messages[error?.message] || fallback;
}

function resultLabel(result) {
  if (!result) return '';
  const winner = result.winnerSide === 'RED' ? 'Red win' : result.winnerSide === 'BLUE' ? 'Blue win' : 'Draw';
  const decision = DECISION_TYPES.find(([value]) => value === result.decisionType)?.[1] || result.decisionType;
  return `${winner} · ${decision}`;
}

function ScoreReview({ scores }) {
  if (!scores.length) return <p className="empty-copy">No judge scores have been submitted.</p>;
  return <div className="supervisor-score-table" role="table">
    <div className="supervisor-score-row supervisor-score-header" role="row">
      <span>Round</span><span>Judge</span><span>Red</span><span>Blue</span><span>Status</span>
    </div>
    {scores.map((score) => <div className="supervisor-score-row" key={`${score.roundNo}-${score.judgeId}`} role="row">
      <span>{score.roundNo}R</span>
      <span>#{score.judgeId}</span>
      <strong className="red-score-text">{score.redScore ?? '-'}</strong>
      <strong className="blue-score-text">{score.blueScore ?? '-'}</strong>
      <span>{score.status === 'SUBMITTED' ? 'Submitted' : 'Draft'}</span>
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
  const [penalty, setPenalty] = useState({ penaltyPoint: '', reason: '', targetSide: 'RED' });
  const [resultForm, setResultForm] = useState({ decisionType: 'POINTS', winnerSide: 'RED' });
  const [confirmingResult, setConfirmingResult] = useState(false);
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
  const adjustedRed = scoreTotals.red - penaltyTotals.red;
  const adjustedBlue = scoreTotals.blue - penaltyTotals.blue;
  const expectedWinner = adjustedRed === adjustedBlue ? 'DRAW' : adjustedRed > adjustedBlue ? 'RED' : 'BLUE';
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
      setError(requestMessage(requestError, 'Assigned rings could not be loaded.'));
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
      setError(requestMessage(requestError, 'Assigned bouts could not be loaded.'));
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
      }
    } catch (requestError) {
      if (isActive()) setActionError(requestMessage(requestError, 'Review data could not be loaded.'));
    }
  }, [session.accessToken]);

  useEffect(() => { loadRings(); }, [loadRings]);
  useEffect(() => { loadBouts(); }, [loadBouts]);
  useEffect(() => {
    setPenalty({ penaltyPoint: '', reason: '', targetSide: 'RED' });
    setResultForm({ decisionType: 'POINTS', winnerSide: 'RED' });
    setConfirmingResult(false);
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
    if (!Number.isInteger(penaltyPoint) || penaltyPoint < 1) {
      setActionError('Penalty points must be a positive whole number.');
      return;
    }
    if (busyRef.current || confirmed) return;
    busyRef.current = true;
    setBusy(true);
    setActionError('');
    try {
      const saved = await createPenalty(boutId, { ...penalty, penaltyPoint }, session.accessToken);
      setPenalties((current) => [...current, saved]);
      setPenalty((current) => ({ ...current, penaltyPoint: '', reason: '' }));
    } catch (requestError) {
      setActionError(requestMessage(requestError, 'Penalty creation failed.'));
    } finally {
      busyRef.current = false;
      setBusy(false);
    }
  }

  function startResultConfirmation(event) {
    event.preventDefault();
    if (!canConfirm) {
      setActionError(boutStarted ? 'All available judge scores must be submitted first.' : 'The bout must start before result confirmation.');
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
      setActionError(requestMessage(requestError, 'Result confirmation failed.'));
    } finally {
      busyRef.current = false;
      setBusy(false);
    }
  }

  if (loading) return <main className="page-shell"><StatePanel title="Loading assigned rings">Please wait.</StatePanel></main>;
  return <main className="page-shell supervisor-shell">
    <div className="judge-heading">
      <div><p className="eyebrow">SUPERVISOR DESK</p><h2>Result review</h2><p>{session.account.name} | Tournament {tournamentId}</p><p aria-live="polite" className="stream-status" data-testid="stream-status">Live updates: {streamState}</p></div>
      <button className="secondary-button" onClick={onLogout} type="button">Sign out</button>
    </div>
    {error && <StatePanel action={<button className="command-button" onClick={loadRings} type="button">Retry</button>} title={error} tone="error">Access is controlled by active assignments.</StatePanel>}
    {!error && !rings.length && <StatePanel title="No assigned rings">Ask an administrator to assign a ring.</StatePanel>}
    {!error && rings.length > 0 && <div className="judge-layout">
      <aside>
        <label>Assigned ring<select aria-label="Assigned ring" onChange={(event) => { setRingId(Number(event.target.value)); setBoutId(null); }} value={ringId || ''}>{rings.map((ring) => <option key={ring.ringId} value={ring.ringId}>{ring.name} #{ring.ringId}</option>)}</select></label>
        <h3>Assigned bouts</h3>
        {bouts.length ? bouts.map((item) => <button className={`judge-bout-option${boutId === item.boutId ? ' selected' : ''}`} key={item.boutId} onClick={() => setBoutId(item.boutId)} type="button"><strong>Bout {item.boutNumber}</strong><small>{item.status}</small></button>) : <StatePanel title="No bouts">This ring has no official bouts.</StatePanel>}
      </aside>
      <section className="judge-score-panel">
        {bout ? <>
          <h3>Bout {bout.boutNumber}</h3>
          <p>{bout.redAthlete?.name || 'Red'} vs {bout.blueAthlete?.name || 'Blue'}</p>
          <h4>Scores</h4>
          <div className="supervisor-score-summary">
            <div className="supervisor-total red-total"><span>Red total</span><strong>{scoreTotals.red}</strong><small>Penalty -{penaltyTotals.red} · Adjusted {adjustedRed}</small></div>
            <div className="supervisor-total blue-total"><span>Blue total</span><strong>{scoreTotals.blue}</strong><small>Penalty -{penaltyTotals.blue} · Adjusted {adjustedBlue}</small></div>
          </div>
          <p aria-live="polite" className="score-readiness">Submitted {submittedScores.length} · Draft {draftScores.length}</p>
          <ScoreReview scores={scores} />
          <h4>Penalties</h4>
          <ul>{penalties.map((item) => <li key={item.penaltyId}>{item.targetSide}: -{item.penaltyPoint} {item.reason}</li>)}</ul>
          <form noValidate onSubmit={addPenalty}>
            <label>Side<select disabled={busy || confirmed} onChange={(event) => setPenalty({ ...penalty, targetSide: event.target.value })} value={penalty.targetSide}><option value="RED">Red</option><option value="BLUE">Blue</option></select></label>
            <label>Points<input disabled={busy || confirmed} inputMode="numeric" min="1" onChange={(event) => setPenalty({ ...penalty, penaltyPoint: event.target.value })} required type="number" value={penalty.penaltyPoint} /></label>
            <label>Reason<input disabled={busy || confirmed} onChange={(event) => setPenalty({ ...penalty, reason: event.target.value })} value={penalty.reason} /></label>
            <button className="command-button" disabled={busy || confirmed} type="submit">Add penalty</button>
          </form>
          <h4>Result</h4>
          {confirmed ? <div className="confirmed-result supervisor-confirmed"><strong>{resultLabel(result)}</strong><small>Result confirmed. Further changes are locked.</small></div> : <form noValidate onSubmit={startResultConfirmation}>
            <label>Winner<select disabled={busy} onChange={(event) => setResultForm({ ...resultForm, winnerSide: event.target.value })} value={resultForm.winnerSide}><option value="RED">Red</option><option value="BLUE">Blue</option><option value="DRAW">Draw</option></select></label>
            <label>Decision<select disabled={busy} onChange={(event) => setResultForm({ ...resultForm, decisionType: event.target.value })} value={resultForm.decisionType}>{DECISION_TYPES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
            <button className="command-button" disabled={busy || !canConfirm} type="submit">Review result</button>
          </form>}
          {confirmingResult && !confirmed && <div aria-label="Confirm result" className="score-confirmation" role="dialog">
            <strong>Confirm result</strong>
            <p>Bout {bout.boutNumber}: {bout.redAthlete?.name || 'Red'} vs {bout.blueAthlete?.name || 'Blue'}</p>
            <p>{resultForm.winnerSide} · {resultForm.decisionType} · scores {scoreTotals.red}-{scoreTotals.blue} · penalties {penaltyTotals.red}-{penaltyTotals.blue}</p>
            {winnerMismatch && <p className="form-error" role="alert">The selected winner differs from the adjusted score comparison.</p>}
            <div><button className="command-button" disabled={busy} onClick={submitResult} type="button">Confirm result</button><button className="secondary-button" disabled={busy} onClick={() => setConfirmingResult(false)} type="button">Cancel</button></div>
          </div>}
          {actionError && <p aria-live="polite" className="form-error" role="alert">{actionError}</p>}
        </> : <StatePanel title="Select a bout">Choose an assigned bout to review.</StatePanel>}
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
    } catch (requestError) { setError(requestError.message === 'SUPERVISOR_ONLY' ? 'Supervisor role is required.' : 'Login failed.'); }
  }
  async function signOut() { await logout(session.accessToken).catch(() => undefined); window.sessionStorage.removeItem(SESSION_KEY); setSession(null); }
  if (!session) return <main className="page-shell auth-shell"><section className="auth-panel"><h2>Supervisor review</h2><form onSubmit={signIn}><label>Login ID<input required value={loginId} onChange={(event) => setLoginId(event.target.value)} /></label><label>Password<input required type="password" value={password} onChange={(event) => setPassword(event.target.value)} /></label>{error && <p className="form-error" role="alert">{error}</p>}<button className="command-button" type="submit">Sign in</button></form></section></main>;
  return <SupervisorAssignedPage onLogout={signOut} session={session} tournamentId={tournamentId} />;
}
