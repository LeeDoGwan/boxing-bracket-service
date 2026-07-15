import { useCallback, useEffect, useState } from 'react';
import { getBoutDetail } from '../api/audience';
import { login, logout } from '../api/auth';
import { confirmResult, createPenalty, getPenalties, getSupervisorScores } from '../api/supervisor';
import { getAssignedBouts, getAssignedRings } from '../api/staffAssignments';
import { StatePanel } from '../components/StatePanel';
import { useBoutEventStream } from '../hooks/useBoutEventStream';
import { useEventRefresh } from '../hooks/useEventRefresh';

const SESSION_KEY = 'boxing.supervisor.session';
const SUPERVISOR_EVENT_TYPES = ['BOUT_STARTED', 'BOUT_STATUS_CHANGED', 'ROUND_STARTED', 'NEXT_BOUT_READY', 'SCORE_SUBMITTED', 'RESULT_CONFIRMED'];

function readSession() {
  try {
    const value = JSON.parse(window.sessionStorage.getItem(SESSION_KEY) || 'null');
    return value?.accessToken && value?.account?.role === 'SUPERVISOR' ? value : null;
  } catch { return null; }
}

export function SupervisorAssignedPage({ session, onLogout, tournamentId }) {
  const [rings, setRings] = useState([]);
  const [ringId, setRingId] = useState(null);
  const [bouts, setBouts] = useState([]);
  const [boutId, setBoutId] = useState(null);
  const [bout, setBout] = useState(null);
  const [scores, setScores] = useState([]);
  const [penalties, setPenalties] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');
  const [penalty, setPenalty] = useState({ penaltyPoint: '', reason: '', targetSide: 'RED' });
  const [busy, setBusy] = useState(false);

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
      const next = await getAssignedBouts(ringId, session.accessToken) || [];
      setBouts(next);
      setBoutId((current) => current && next.some((item) => item.boutId === current) ? current : next[0]?.boutId || null);
      return next;
    } catch {
      setBouts([]);
      setBoutId(null);
      setError('Selected ring access was revoked.');
      return [];
    }
  }, [ringId, session.accessToken]);

  const loadDetail = useCallback(async (targetBoutId, isActive = () => true) => {
    if (!targetBoutId) {
      if (isActive()) {
        setBout(null);
        setScores([]);
        setPenalties([]);
      }
      return;
    }
    setActionError('');
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
      }
    } catch (requestError) {
      if (isActive()) setActionError(requestError.status === 403 ? 'Bout access denied.' : 'Review data could not be loaded.');
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
    eventTypes: SUPERVISOR_EVENT_TYPES,
    onEvent: handleStreamEvent,
    ringId,
  });

  async function addPenalty(event) {
    event.preventDefault();
    setBusy(true);
    setActionError('');
    try {
      const saved = await createPenalty(boutId, { ...penalty, createdBy: session.account.accountId, penaltyPoint: Number(penalty.penaltyPoint) }, session.accessToken);
      setPenalties((current) => [...current, saved]);
      setPenalty({ penaltyPoint: '', reason: '', targetSide: 'RED' });
    } catch (requestError) {
      setActionError(requestError.status === 403 ? 'Assignment access denied.' : requestError.message || 'Penalty creation failed.');
    } finally {
      setBusy(false);
    }
  }

  async function confirm() {
    setBusy(true);
    setActionError('');
    try {
      await confirmResult(boutId, { confirmedBy: session.account.accountId, decisionType: 'POINTS', winnerSide: 'RED' }, session.accessToken);
      setBout((current) => current ? { ...current, resultConfirmed: true, status: 'FINISHED' } : current);
    } catch (requestError) {
      setActionError(requestError.status === 403 ? 'Assignment access denied.' : requestError.message || 'Result confirmation failed.');
    } finally {
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
        {bout ? <><h3>Bout {bout.boutNumber}</h3><p>{bout.redAthlete?.name || 'Red'} vs {bout.blueAthlete?.name || 'Blue'}</p><h4>Scores</h4><ul>{scores.map((score) => <li key={`${score.roundNo}-${score.judgeId}`}>Round {score.roundNo}: {score.redScore} - {score.blueScore}</li>)}</ul><h4>Penalties</h4><ul>{penalties.map((item) => <li key={item.penaltyId}>{item.targetSide}: -{item.penaltyPoint} {item.reason}</li>)}</ul><form onSubmit={addPenalty}><label>Side<select onChange={(event) => setPenalty({ ...penalty, targetSide: event.target.value })} value={penalty.targetSide}><option value="RED">Red</option><option value="BLUE">Blue</option></select></label><label>Points<input min="0" onChange={(event) => setPenalty({ ...penalty, penaltyPoint: event.target.value })} required type="number" value={penalty.penaltyPoint} /></label><label>Reason<input onChange={(event) => setPenalty({ ...penalty, reason: event.target.value })} value={penalty.reason} /></label><button className="command-button" disabled={busy || bout.resultConfirmed} type="submit">Add penalty</button></form><button className="command-button" disabled={busy || bout.resultConfirmed} onClick={confirm} type="button">Confirm result</button>{actionError && <p className="form-error" role="alert">{actionError}</p>}</> : <StatePanel title="Select a bout">Choose an assigned bout to review.</StatePanel>}
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
