import { useTranslation } from 'react-i18next';
import { translate as t, localizedMessage, resolveMessage } from '../i18n';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { login, logout } from '../api/auth';
import { moveToNextBout, startBout, startRound, updateBoutStatus } from '../api/ringManager';
import { getAssignedBouts, getAssignedRings } from '../api/staffAssignments';
import { StatePanel } from '../components/StatePanel';
import { useBoutEventStream } from '../hooks/useBoutEventStream';
import { useEventRefresh } from '../hooks/useEventRefresh';
import { statusLabel, streamStateLabel } from '../utils';

const SESSION_KEY = 'boxing.ring-manager.session';
const RING_MANAGER_EVENT_TYPES = ['BOUT_STARTED', 'BOUT_STATUS_CHANGED', 'ROUND_STARTED', 'NEXT_BOUT_READY', 'RESULT_CONFIRMED'];

const ERROR_MESSAGES = {
  BOUT_ALREADY_IN_PROGRESS: localizedMessage('errors:ring.anotherBoutActive'),
  BOUT_ALREADY_STARTED: localizedMessage('errors:bout.alreadyStarted'),
  BOUT_RESULT_REQUIRED: localizedMessage('errors:bout.resultRequired'),
  CONCURRENT_MODIFICATION: localizedMessage('errors:concurrentModification'),
  CURRENT_BOUT_NOT_FINISHED: localizedMessage('errors:ring.currentNotFinished'),
  INVALID_BOUT_STATE: localizedMessage('errors:bout.invalidState'),
  INVALID_BOUT_TRANSITION: localizedMessage('errors:bout.invalidTransition'),
  NEXT_BOUT_NOT_FOUND: localizedMessage('errors:ring.nextNotFound'),
  RING_ALREADY_HAS_CURRENT_BOUT: localizedMessage('errors:ring.currentMismatch'),
  ROUND_OUT_OF_RANGE: localizedMessage('errors:round.outOfRange'),
  ROUND_SEQUENCE_INVALID: localizedMessage('errors:round.sequence'),
};

function readSession() {
  try {
    const value = JSON.parse(window.sessionStorage.getItem(SESSION_KEY) || 'null');
    return value?.accessToken && value?.account?.role === 'RING_MANAGER' ? value : null;
  } catch {
    return null;
  }
}

function actionErrorMessage(error, fallback) {
  if (error?.status === 401) return localizedMessage('errors:session.expired');
  if (error?.status === 403) return localizedMessage('errors:assignment.denied');
  return ERROR_MESSAGES[error?.message] || fallback;
}

function nextRoundValue(bout) {
  return String(Math.max(1, (bout?.currentRound || 0) + 1));
}

function isActiveStatus(status) {
  return status === 'READY' || status === 'IN_PROGRESS' || status === 'SCORING';
}

export function RingManagerAssignedPage({ session, onLogout, tournamentId }) {
  useTranslation();
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
      setError(actionErrorMessage(requestError, localizedMessage('errors:assignment.rings')));
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
      setError(actionErrorMessage(requestError, localizedMessage('errors:assignment.revoked')));
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
      setActionError(actionErrorMessage(requestError, localizedMessage('errors:ring.action')));
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

  if (loading) return <main className="page-shell"><StatePanel title={t('operations:assignment.loading')}>{t('common:waitBriefly')}</StatePanel></main>;

  return <main className="page-shell ring-manager-shell">
    <div className="judge-heading">
      <div>
        <p className="eyebrow">{t('common:role.ringManager')}</p>
        <h2>{t('operations:ring.title')}</h2>
        <p>{t('operations:assignment.operator', { value1: session.account.name, value2: tournamentId })}</p>
        <p aria-live="polite" className="stream-status" data-testid="stream-status">{t('operations:connection', { value1: streamStateLabel(streamState) })}</p>
      </div>
      <button className="secondary-button" onClick={onLogout} type="button">{t('common:signOut')}</button>
    </div>

    {error && <StatePanel action={<button className="command-button" onClick={loadRings} type="button">{t('common:retry')}</button>} title={resolveMessage(error)} tone="error">{t('operations:assignment.scope')}</StatePanel>}
    {!error && !rings.length && <StatePanel title={t('operations:assignment.noRings')}>{t('operations:assignment.request')}</StatePanel>}

    {!error && rings.length > 0 && <div className="ring-manager-layout">
      <aside>
        <label>{t('operations:assignment.ring')}<select aria-label={t('operations:assignment.ring')} onChange={(event) => { setRingId(Number(event.target.value)); setBoutId(null); }} value={ringId || ''}>{rings.map((ring) => <option key={ring.ringId} value={ring.ringId}>{ring.name} #{ring.ringId}</option>)}</select></label>
        {bouts.length ? bouts.map((item) => <button className={`judge-bout-option${boutId === item.boutId ? ' selected' : ''}`} key={item.boutId} onClick={() => { setBoutId(item.boutId); setRound(nextRoundValue(item)); }} type="button"><strong>{t('common:boutNumber', { value1: item.boutNumber })}</strong><small>{t('operations:ring.boutStatusRound', { value1: statusLabel(item.status), value2: item.currentRound || 0, value3: item.totalRounds ? `/${item.totalRounds}` : '' })}</small></button>) : <StatePanel title={t('operations:assignment.noBouts')}>{t('operations:assignment.emptyRing')}</StatePanel>}
      </aside>

      <section className="ring-manager-panel">
        <div className="ring-current-card">
          <p className="eyebrow">{t('common:currentBout')}</p>
          <h3>{currentBout ? t('common:boutNumber', { value1: currentBout.boutNumber }) : t('operations:ring.noActiveBout')}</h3>
          <p>{currentBout ? `${currentBout.matchType || t('common:officialBout')} · ${statusLabel(currentBout.status)}` : t('operations:ring.noPreparedBout')}</p>
        </div>

        {selected ? <>
          <div className="ring-selected-heading">
            <div>
              <p className="eyebrow">{t('operations:ring.selectedBout', { value1: selected.boutNumber })}</p>
              <h3>{selected.matchType || t('common:officialBout')}</h3>
              <p>{t('operations:ring.currentStatus', { value1: statusLabel(selected.status), value2: selected.currentRound || 0, value3: selected.totalRounds ? `/${selected.totalRounds}` : '' })}</p>
            </div>
            <span className="status-pill">{resolveMessage(statusLabel(selected.status))}</span>
          </div>

          {currentBout && currentBout.boutId !== selected.boutId && <p className="form-error" role="status">{t('operations:ring.selectionMismatch')}</p>}
          {actionError && <p aria-live="polite" className="form-error" role="alert">{resolveMessage(actionError)}</p>}
          {actionMessage && <p aria-live="polite" className="ring-success-message">{resolveMessage(actionMessage)}</p>}

          <div className="ring-command-grid">
            {selected.status === 'SCHEDULED' && <button className="secondary-button" disabled={busy} onClick={() => askForConfirmation(
              () => updateBoutStatus(selected.boutId, 'CANCELED', session.accessToken),
              localizedMessage('operations:bout.cancel'),
              localizedMessage('operations:bout.cancelDetails', { value1: selected.boutNumber }),
              localizedMessage('operations:bout.canceled')
            )} type="button">{t('operations:bout.cancel')}</button>}

            {selected.status === 'READY' && <button className="command-button" disabled={busy || !selectedIsCurrent} onClick={() => askForConfirmation(
              () => startBout(selected.boutId, session.accessToken),
              localizedMessage('operations:bout.start'),
              localizedMessage('operations:bout.startDetails', { value1: selected.boutNumber }),
              localizedMessage('operations:bout.started')
            )} type="button">{t('operations:bout.start')}</button>}

            {selected.status === 'IN_PROGRESS' && <>
              <label>{t('operations:round.next')}<input aria-label={t('operations:round.next')} disabled={busy || !selectedIsCurrent} max={selected.totalRounds || undefined} min="1" onChange={(event) => setRound(event.target.value)} type="number" value={round} /></label>
              <button className="command-button" disabled={busy || !canStartRound} onClick={() => run(
                () => startRound(selected.boutId, parsedRound, session.accessToken),
                localizedMessage('operations:round.started', { value1: parsedRound })
              )} type="button">{t('operations:round.start')}</button>
              <button className="secondary-button" disabled={busy || !canSendToScoring} onClick={() => askForConfirmation(
                () => updateBoutStatus(selected.boutId, 'SCORING', session.accessToken),
                localizedMessage('operations:bout.sendToScoring'),
                localizedMessage('operations:bout.scoringDetails', { value1: selected.boutNumber }),
                localizedMessage('operations:bout.sentToScoring')
              )} type="button">{t('operations:bout.sendToScoring')}</button>
            </>}

            {selected.status === 'SCORING' && <p className="dialog-state">{t('operations:ring.waitingResult')}</p>}
            {selected.status === 'FINISHED' && <p className="dialog-state">{t('operations:ring.resultConfirmed')}</p>}
            {selected.status === 'CANCELED' && <p className="dialog-state">{t('operations:ring.canceledReadOnly')}</p>}

            <button className="secondary-button" disabled={busy || !canPrepareNext} onClick={() => askForConfirmation(
              () => moveToNextBout(ringId, session.accessToken),
              localizedMessage('operations:bout.prepareNext'),
              localizedMessage('operations:bout.prepareDetails', { value1: selectedRing?.name || ringId }),
              localizedMessage('operations:bout.prepared')
            )} type="button">{t('operations:bout.prepareNext')}</button>
          </div>
        </> : <StatePanel title={t('operations:selectBout')}>{t('operations:ring.selectHint')}</StatePanel>}
      </section>
    </div>}

    {pendingAction && <div aria-modal="true" className="dialog-backdrop" role="dialog" aria-label={t('operations:ring.confirmOperation')}>
      <section className="bout-dialog">
        <p className="eyebrow">{t('operations:ring.confirmHeading')}</p>
        <h2>{resolveMessage(pendingAction.title)}</h2>
        <p className="dialog-status">{resolveMessage(pendingAction.details)}</p>
        <div className="score-confirmation">
          <div><button className="command-button" disabled={busy} onClick={confirmPendingAction} type="button">{t('common:confirm')}</button><button className="secondary-button" disabled={busy} onClick={() => setPendingAction(null)} type="button">{t('common:cancel')}</button></div>
        </div>
      </section>
    </div>}
  </main>;
}

export function AssignedRingManagerRoute({ tournamentId }) {
  useTranslation();
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
      setError(requestError.message === 'RING_MANAGER_ONLY' ? localizedMessage('errors:role.ringManager') : localizedMessage('errors:auth.login'));
    }
  }

  async function signOut() {
    await logout(session.accessToken).catch(() => undefined);
    window.sessionStorage.removeItem(SESSION_KEY);
    setSession(null);
  }

  if (!session) return <main className="page-shell auth-shell"><section className="auth-panel"><h2>{t('operations:ring.title')}</h2><form onSubmit={signIn}><label>{t('common:loginId')}<input required value={loginId} onChange={(event) => setLoginId(event.target.value)} /></label><label>{t('common:password')}<input required type="password" value={password} onChange={(event) => setPassword(event.target.value)} /></label>{error && <p className="form-error" role="alert">{resolveMessage(error)}</p>}<button className="command-button" type="submit">{t('common:signIn')}</button></form></section></main>;
  return <RingManagerAssignedPage onLogout={signOut} session={session} tournamentId={tournamentId} />;
}
