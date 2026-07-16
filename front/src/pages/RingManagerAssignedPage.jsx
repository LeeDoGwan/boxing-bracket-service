import { useCallback, useEffect, useState } from 'react';
import { login, logout } from '../api/auth';
import { moveToNextBout, startBout, startRound, updateBoutStatus } from '../api/ringManager';
import { getAssignedBouts, getAssignedRings } from '../api/staffAssignments';
import { StatePanel } from '../components/StatePanel';
import { useBoutEventStream } from '../hooks/useBoutEventStream';
import { useEventRefresh } from '../hooks/useEventRefresh';

const SESSION_KEY = 'boxing.ring-manager.session';
const STATUSES = ['READY', 'IN_PROGRESS', 'SCORING', 'FINISHED', 'CANCELED'];
const RING_MANAGER_EVENT_TYPES = ['BOUT_STARTED', 'BOUT_STATUS_CHANGED', 'ROUND_STARTED', 'NEXT_BOUT_READY', 'RESULT_CONFIRMED'];

function readSession() {
  try {
    const value = JSON.parse(window.sessionStorage.getItem(SESSION_KEY) || 'null');
    return value?.accessToken && value?.account?.role === 'RING_MANAGER' ? value : null;
  } catch { return null; }
}

export function RingManagerAssignedPage({ session, onLogout, tournamentId }) {
  const [rings, setRings] = useState([]);
  const [ringId, setRingId] = useState(null);
  const [bouts, setBouts] = useState([]);
  const [boutId, setBoutId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');
  const [busy, setBusy] = useState(false);
  const [round, setRound] = useState('1');
  const [status, setStatus] = useState('READY');

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

  useEffect(() => { loadRings(); }, [loadRings]);
  useEffect(() => { loadBouts(); }, [loadBouts]);

  const refreshLiveData = useEventRefresh(async () => {
    const nextBouts = await loadBouts();
    const latest = nextBouts.find((item) => item.boutId === boutId);
    if (latest) {
      setStatus(latest.status || 'READY');
      setRound(String((latest.currentRound || 0) + 1));
    }
  });
  const handleStreamEvent = useCallback(() => refreshLiveData(), [refreshLiveData]);
  const streamState = useBoutEventStream(tournamentId, {
    enabled: Boolean(ringId),
    eventTypes: RING_MANAGER_EVENT_TYPES,
    onEvent: handleStreamEvent,
    ringId,
  });

  async function run(action) {
    setBusy(true);
    setActionError('');
    try {
      await action();
      await loadBouts();
    } catch (requestError) {
      setActionError(requestError.status === 403 ? 'Assignment access denied.' : requestError.message || 'Ring action failed.');
    } finally {
      setBusy(false);
    }
  }

  if (loading) return <main className="page-shell"><StatePanel title="Loading assigned rings">Please wait.</StatePanel></main>;
  const selected = bouts.find((item) => item.boutId === boutId);
  return <main className="page-shell ring-manager-shell">
    <div className="judge-heading">
      <div><p className="eyebrow">RING MANAGER DESK</p><h2>Ring operations</h2><p>{session.account.name} | Tournament {tournamentId}</p><p aria-live="polite" className="stream-status" data-testid="stream-status">Live updates: {streamState}</p></div>
      <button className="secondary-button" onClick={onLogout} type="button">Sign out</button>
    </div>
    {error && <StatePanel action={<button className="command-button" onClick={loadRings} type="button">Retry</button>} title={error} tone="error">Access is controlled by active assignments.</StatePanel>}
    {!error && !rings.length && <StatePanel title="No assigned rings">Ask an administrator to assign a ring.</StatePanel>}
    {!error && rings.length > 0 && <div className="ring-manager-layout">
      <aside>
        <label>Assigned ring<select aria-label="Assigned ring" onChange={(event) => { setRingId(Number(event.target.value)); setBoutId(null); }} value={ringId || ''}>{rings.map((ring) => <option key={ring.ringId} value={ring.ringId}>{ring.name} #{ring.ringId}</option>)}</select></label>
        {bouts.length ? bouts.map((item) => <button className={`judge-bout-option${boutId === item.boutId ? ' selected' : ''}`} key={item.boutId} onClick={() => { setBoutId(item.boutId); setStatus(item.status || 'READY'); setRound(String((item.currentRound || 0) + 1)); }} type="button"><strong>Bout {item.boutNumber}</strong><small>{item.status}</small></button>) : <StatePanel title="No bouts">This ring has no official bouts.</StatePanel>}
      </aside>
      <section className="ring-manager-panel">
        {selected ? <><h3>Bout {selected.boutNumber}</h3><p>{selected.matchType || 'Official bout'} | {selected.status}</p><div className="ring-command-grid"><button className="command-button" disabled={busy || selected.status !== 'READY'} onClick={() => run(() => startBout(selected.boutId, session.accessToken))} type="button">Start bout</button><label>Round<input min="1" onChange={(event) => setRound(event.target.value)} type="number" value={round} /></label><button className="command-button" disabled={busy || selected.status === 'FINISHED'} onClick={() => run(() => startRound(selected.boutId, Number(round), session.accessToken))} type="button">Start round</button><label>Status<select onChange={(event) => setStatus(event.target.value)} value={status}>{STATUSES.map((item) => <option key={item} value={item}>{item}</option>)}</select></label><button className="command-button" disabled={busy || selected.status === 'FINISHED'} onClick={() => run(() => updateBoutStatus(selected.boutId, status, session.accessToken))} type="button">Update status</button><button className="secondary-button" disabled={busy} onClick={() => run(() => moveToNextBout(ringId, session.accessToken))} type="button">Prepare next bout</button></div>{actionError && <p className="form-error" role="alert">{actionError}</p>}</> : <StatePanel title="Select a bout">Choose an assigned bout to operate.</StatePanel>}
      </section>
    </div>}
  </main>;
}

export function AssignedRingManagerRoute({ tournamentId }) {
  const [session, setSession] = useState(readSession);
  const [loginId, setLoginId] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  async function signIn(event) {
    event.preventDefault();
    try {
      const next = await login(loginId, password);
      if (next.account?.role !== 'RING_MANAGER') throw new Error('RING_MANAGER_ONLY');
      window.sessionStorage.setItem(SESSION_KEY, JSON.stringify(next));
      setSession(next);
    } catch (requestError) { setError(requestError.message === 'RING_MANAGER_ONLY' ? 'Ring Manager role is required.' : 'Login failed.'); }
  }
  async function signOut() { await logout(session.accessToken).catch(() => undefined); window.sessionStorage.removeItem(SESSION_KEY); setSession(null); }
  if (!session) return <main className="page-shell auth-shell"><section className="auth-panel"><h2>Ring operations</h2><form onSubmit={signIn}><label>Login ID<input required value={loginId} onChange={(event) => setLoginId(event.target.value)} /></label><label>Password<input required type="password" value={password} onChange={(event) => setPassword(event.target.value)} /></label>{error && <p className="form-error" role="alert">{error}</p>}<button className="command-button" type="submit">Sign in</button></form></section></main>;
  return <RingManagerAssignedPage onLogout={signOut} session={session} tournamentId={tournamentId} />;
}
