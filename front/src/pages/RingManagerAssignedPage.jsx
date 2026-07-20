import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { login, logout } from '../api/auth';
import { moveToNextBout, startBout, startRound, updateBoutStatus } from '../api/ringManager';
import { getAssignedBouts, getAssignedRings } from '../api/staffAssignments';
import { StatePanel } from '../components/StatePanel';
import { useBoutEventStream } from '../hooks/useBoutEventStream';
import { useEventRefresh } from '../hooks/useEventRefresh';

const SESSION_KEY = 'boxing.ring-manager.session';
const RING_MANAGER_EVENT_TYPES = ['BOUT_STARTED', 'BOUT_STATUS_CHANGED', 'ROUND_STARTED', 'NEXT_BOUT_READY', 'RESULT_CONFIRMED'];
const STATUS_LABELS = {
  SCHEDULED: 'Scheduled',
  READY: 'Ready',
  IN_PROGRESS: 'In progress',
  SCORING: 'Awaiting result',
  FINISHED: 'Finished',
  CANCELED: 'Canceled',
};

const ERROR_MESSAGES = {
  BOUT_ALREADY_IN_PROGRESS: 'Another bout is already active on this ring.',
  BOUT_ALREADY_STARTED: 'This bout is already active on another ring context.',
  BOUT_RESULT_REQUIRED: 'The Supervisor must confirm the result before the bout can finish.',
  CONCURRENT_MODIFICATION: 'Another operator changed this bout. Refresh and review the current state.',
  CURRENT_BOUT_NOT_FINISHED: 'The current bout must finish before the next bout can be prepared.',
  INVALID_BOUT_STATE: 'This bout is no longer available for this operation.',
  INVALID_BOUT_TRANSITION: 'That state transition is not allowed from the current bout state.',
  NEXT_BOUT_NOT_FOUND: 'There is no next official bout on this ring.',
  RING_ALREADY_HAS_CURRENT_BOUT: 'Prepare the current ring bout before starting another bout.',
  ROUND_OUT_OF_RANGE: 'The selected round is outside this bout range.',
  ROUND_SEQUENCE_INVALID: 'Start the next round in sequence.',
};

function readSession() {
  try {
    const value = JSON.parse(window.sessionStorage.getItem(SESSION_KEY) || 'null');
    return value?.accessToken && value?.account?.role === 'RING_MANAGER' ? value : null;
  } catch {
    return null;
  }
}

function statusLabel(status) {
  return STATUS_LABELS[status] || status || 'Unknown';
}

function actionErrorMessage(error, fallback) {
  if (error?.status === 401) return 'Session expired.';
  if (error?.status === 403) return 'Assignment access denied.';
  return ERROR_MESSAGES[error?.message] || fallback;
}

function nextRoundValue(bout) {
  return String(Math.max(1, (bout?.currentRound || 0) + 1));
}

function isActiveStatus(status) {
  return status === 'READY' || status === 'IN_PROGRESS' || status === 'SCORING';
}

export function RingManagerAssignedPage({ session, onLogout, tournamentId }) {
  const [rings, setRings] = useState([]);
  const [ringId, setRingId] = useState(null);
  const [bouts, setBouts] = useState([]);
  const [boutId, setBoutId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');
  const [actionMessage, setActionMessage] = useState('');
  const [busy, setBusy] = useState(false);
  const [round, setRound] = useState('1');
  const [pendingAction, setPendingAction] = useState(null);
  const busyRef = useRef(false);

  const selected = useMemo(() => bouts.find((item) => item.boutId === boutId) || null, [bouts, boutId]);
  const selectedRing = useMemo(() => rings.find((item) => item.ringId === ringId) || null, [rings, ringId]);
  const currentBout = useMemo(() => {
    const assignedCurrent = selectedRing?.currentBoutId
      ? bouts.find((item) => item.boutId === selectedRing.currentBoutId)
      : null;
    return assignedCurrent || bouts.find((item) => isActiveStatus(item.status)) || null;
  }, [bouts, selectedRing]);
  const selectedIsCurrent = Boolean(selected && (!currentBout || currentBout.boutId === selected.boutId));
  const currentRound = selected?.currentRound || 0;
  const totalRounds = selected?.totalRounds || null;
  const expectedNextRound = currentRound + 1;
  const parsedRound = Number.parseInt(round, 10);
  const roundIsValid = Number.isInteger(parsedRound) && parsedRound >= 1
    && parsedRound === expectedNextRound
    && (!totalRounds || parsedRound <= totalRounds);
  const canStartRound = selected?.status === 'IN_PROGRESS'
    && selectedIsCurrent
    && roundIsValid;
  const canSendToScoring = selected?.status === 'IN_PROGRESS'
    && selectedIsCurrent
    && currentRound >= 1
    && (!totalRounds || currentRound >= totalRounds);
  const canPrepareNext = !currentBout || currentBout.status === 'FINISHED';

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
      setError(actionErrorMessage(requestError, 'Assigned rings could not be loaded.'));
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
      setBoutId((current) => current && next.some((item) => item.boutId === current)
        ? current
        : next.find((item) => item.boutId === selectedRing?.currentBoutId)?.boutId || next[0]?.boutId || null);
      return next;
    } catch (requestError) {
      setBouts([]);
      setBoutId(null);
      setError(actionErrorMessage(requestError, 'Selected ring access was revoked.'));
      return [];
    }
  }, [ringId, selectedRing?.currentBoutId, session.accessToken]);

  useEffect(() => { loadRings(); }, [loadRings]);
  useEffect(() => { loadBouts(); }, [loadBouts]);
  useEffect(() => {
    if (selected) {
      setRound(nextRoundValue(selected));
    }
  }, [selected]);

  const refreshLiveData = useEventRefresh(async () => {
    await Promise.all([loadRings(), loadBouts()]);
  });
  const handleStreamEvent = useCallback(() => refreshLiveData(), [refreshLiveData]);
  const streamState = useBoutEventStream(tournamentId, {
    enabled: Boolean(ringId),
    eventTypes: RING_MANAGER_EVENT_TYPES,
    onEvent: handleStreamEvent,
    ringId,
  });

  async function run(action, successMessage) {
    if (busyRef.current) return;
    busyRef.current = true;
    setBusy(true);
    setActionError('');
    setActionMessage('');
    try {
      await action();
      await Promise.all([loadRings(), loadBouts()]);
      setActionMessage(successMessage);
    } catch (requestError) {
      setActionError(actionErrorMessage(requestError, 'Ring action failed.'));
    } finally {
      busyRef.current = false;
      setBusy(false);
    }
  }

  function askForConfirmation(action, title, details, successMessage) {
    if (busyRef.current) return;
    setActionError('');
    setPendingAction({ action, details, successMessage, title });
  }

  async function confirmPendingAction() {
    if (!pendingAction || busyRef.current) return;
    const action = pendingAction;
    setPendingAction(null);
    await run(action.action, action.successMessage);
  }

  if (loading) return <main className="page-shell"><StatePanel title="Loading assigned rings">Please wait.</StatePanel></main>;

  return <main className="page-shell ring-manager-shell">
    <div className="judge-heading">
      <div>
        <p className="eyebrow">RING MANAGER DESK</p>
        <h2>Ring operations</h2>
        <p>{session.account.name} | Tournament {tournamentId}</p>
        <p aria-live="polite" className="stream-status" data-testid="stream-status">Live updates: {streamState}</p>
      </div>
      <button className="secondary-button" onClick={onLogout} type="button">Sign out</button>
    </div>

    {error && <StatePanel action={<button className="command-button" onClick={loadRings} type="button">Retry</button>} title={error} tone="error">Access is controlled by active assignments.</StatePanel>}
    {!error && !rings.length && <StatePanel title="No assigned rings">Ask an administrator to assign a ring.</StatePanel>}

    {!error && rings.length > 0 && <div className="ring-manager-layout">
      <aside>
        <label>Assigned ring<select aria-label="Assigned ring" onChange={(event) => { setRingId(Number(event.target.value)); setBoutId(null); }} value={ringId || ''}>{rings.map((ring) => <option key={ring.ringId} value={ring.ringId}>{ring.name} #{ring.ringId}</option>)}</select></label>
        {bouts.length ? bouts.map((item) => <button className={`judge-bout-option${boutId === item.boutId ? ' selected' : ''}`} key={item.boutId} onClick={() => { setBoutId(item.boutId); setRound(nextRoundValue(item)); }} type="button"><strong>Bout {item.boutNumber}</strong><small>{statusLabel(item.status)} · Round {item.currentRound || 0}{item.totalRounds ? `/${item.totalRounds}` : ''}</small></button>) : <StatePanel title="No bouts">This ring has no official bouts.</StatePanel>}
      </aside>

      <section className="ring-manager-panel">
        <div className="ring-current-card">
          <p className="eyebrow">CURRENT BOUT</p>
          <h3>{currentBout ? `Bout ${currentBout.boutNumber}` : 'No active bout'}</h3>
          <p>{currentBout ? `${currentBout.matchType || 'Official bout'} · ${statusLabel(currentBout.status)}` : 'Prepare the next official bout to begin ring operations.'}</p>
        </div>

        {selected ? <>
          <div className="ring-selected-heading">
            <div>
              <p className="eyebrow">SELECTED BOUT {selected.boutNumber}</p>
              <h3>{selected.matchType || 'Official bout'}</h3>
              <p>Current state: {statusLabel(selected.status)} · Round {selected.currentRound || 0}{selected.totalRounds ? `/${selected.totalRounds}` : ''}</p>
            </div>
            <span className="status-pill">{statusLabel(selected.status)}</span>
          </div>

          {currentBout && currentBout.boutId !== selected.boutId && <p className="form-error" role="status">Selected bout differs from the current ring bout. Start and round commands are disabled until it becomes current.</p>}
          {actionError && <p aria-live="polite" className="form-error" role="alert">{actionError}</p>}
          {actionMessage && <p aria-live="polite" className="ring-success-message">{actionMessage}</p>}

          <div className="ring-command-grid">
            {selected.status === 'SCHEDULED' && <button className="secondary-button" disabled={busy} onClick={() => askForConfirmation(
              () => updateBoutStatus(selected.boutId, 'CANCELED', session.accessToken),
              'Cancel bout',
              `Bout ${selected.boutNumber} · Scheduled → Canceled`,
              'Bout canceled.'
            )} type="button">Cancel bout</button>}

            {selected.status === 'READY' && <button className="command-button" disabled={busy || !selectedIsCurrent} onClick={() => askForConfirmation(
              () => startBout(selected.boutId, session.accessToken),
              'Start bout',
              `Bout ${selected.boutNumber} · Ready → In progress`,
              'Bout started.'
            )} type="button">Start bout</button>}

            {selected.status === 'IN_PROGRESS' && <>
              <label>Next round<input aria-label="Next round" disabled={busy || !selectedIsCurrent} max={selected.totalRounds || undefined} min="1" onChange={(event) => setRound(event.target.value)} type="number" value={round} /></label>
              <button className="command-button" disabled={busy || !canStartRound} onClick={() => run(
                () => startRound(selected.boutId, parsedRound, session.accessToken),
                `Round ${parsedRound} started.`
              )} type="button">Start round</button>
              <button className="secondary-button" disabled={busy || !canSendToScoring} onClick={() => askForConfirmation(
                () => updateBoutStatus(selected.boutId, 'SCORING', session.accessToken),
                'Send bout to scoring',
                `Bout ${selected.boutNumber} · In progress → Awaiting result`,
                'Bout sent to scoring.'
              )} type="button">Send to scoring</button>
            </>}

            {selected.status === 'SCORING' && <p className="dialog-state">Waiting for Supervisor result confirmation. Ring Manager commands are locked.</p>}
            {selected.status === 'FINISHED' && <p className="dialog-state">Result confirmed. Prepare the next bout when the ring is ready.</p>}
            {selected.status === 'CANCELED' && <p className="dialog-state">Canceled bouts are read-only.</p>}

            <button className="secondary-button" disabled={busy || !canPrepareNext} onClick={() => askForConfirmation(
              () => moveToNextBout(ringId, session.accessToken),
              'Prepare next bout',
              `Ring ${selectedRing?.name || ringId} · Server-selected next official bout`,
              'Next bout prepared.'
            )} type="button">Prepare next bout</button>
          </div>
        </> : <StatePanel title="Select a bout">Choose an assigned bout to operate.</StatePanel>}
      </section>
    </div>}

    {pendingAction && <div aria-modal="true" className="dialog-backdrop" role="dialog" aria-label="Confirm ring operation">
      <section className="bout-dialog">
        <p className="eyebrow">CONFIRM OPERATION</p>
        <h2>{pendingAction.title}</h2>
        <p className="dialog-status">{pendingAction.details}</p>
        <div className="score-confirmation">
          <div><button className="command-button" disabled={busy} onClick={confirmPendingAction} type="button">Confirm</button><button className="secondary-button" disabled={busy} onClick={() => setPendingAction(null)} type="button">Cancel</button></div>
        </div>
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
    setError('');
    try {
      const next = await login(loginId, password);
      if (next.account?.role !== 'RING_MANAGER') throw new Error('RING_MANAGER_ONLY');
      window.sessionStorage.setItem(SESSION_KEY, JSON.stringify(next));
      setSession(next);
    } catch (requestError) {
      setError(requestError.message === 'RING_MANAGER_ONLY' ? 'Ring Manager role is required.' : 'Login failed.');
    }
  }

  async function signOut() {
    await logout(session.accessToken).catch(() => undefined);
    window.sessionStorage.removeItem(SESSION_KEY);
    setSession(null);
  }

  if (!session) return <main className="page-shell auth-shell"><section className="auth-panel"><h2>Ring operations</h2><form onSubmit={signIn}><label>Login ID<input required value={loginId} onChange={(event) => setLoginId(event.target.value)} /></label><label>Password<input required type="password" value={password} onChange={(event) => setPassword(event.target.value)} /></label>{error && <p className="form-error" role="alert">{error}</p>}<button className="command-button" type="submit">Sign in</button></form></section></main>;
  return <RingManagerAssignedPage onLogout={signOut} session={session} tournamentId={tournamentId} />;
}
