import { useTranslation } from 'react-i18next';
import { translate as t, localizedMessage, resolveMessage } from '../i18n';
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
  if (error?.status === 401) return localizedMessage('errors:session.expired');
  if (error?.status === 403 || error?.message === 'RING_ACCESS_DENIED' || error?.message === 'BOUT_ACCESS_DENIED') return localizedMessage('errors:assignment.denied');
  const messages = {
    ACTOR_ID_MISMATCH: localizedMessage('errors:auth.actorMismatch'),
    BOUT_NOT_STARTED: localizedMessage('errors:bout.startRequired'),
    INVALID_BOUT_STATE: localizedMessage('errors:bout.invalidState'),
    INVALID_ROUND_NUMBER: localizedMessage('errors:penalty.round'),
    INVALID_PENALTY_VALUE: localizedMessage('errors:penalty.points'),
    INVALID_RESULT_DECISION: localizedMessage('errors:result.decision'),
    INVALID_WINNER_SELECTION: localizedMessage('errors:result.winner'),
    PENALTY_NOT_ALLOWED: localizedMessage('errors:penalty.confirmed'),
    RESULT_ALREADY_CONFIRMED: localizedMessage('errors:result.alreadyConfirmed'),
    RESULT_NOT_FOUND: localizedMessage('errors:result.notFound'),
    RESULT_NOT_CONFIRMED: localizedMessage('errors:result.notConfirmed'),
    reason: localizedMessage('errors:result.reasonRequired'),
    SCORES_NOT_READY: localizedMessage('errors:result.scoresNotReady'),
  };
  return messages[error?.message] || fallback;
}

function resultLabel(result) {
  if (!result) return '';
  const winner = result.winnerSide === 'RED' ? localizedMessage('operations:result.redWins') : result.winnerSide === 'BLUE' ? localizedMessage('operations:result.blueWins') : localizedMessage('common:draw');
  const decision = decisionLabel(result.decisionType);
  return `${resolveMessage(winner)} · ${decision}`;
}

function ScoreReview({ scores }) {
  useTranslation();
  if (!scores.length) return <p className="empty-copy">{t('operations:score.noRecords')}</p>;
  return <div className="supervisor-score-table" role="table">
    <div className="supervisor-score-row supervisor-score-header" role="row">
      <span>{t('common:round')}</span><span>{t('common:role.judge')}</span><span>{t('common:red')}</span><span>{t('common:blue')}</span><span>{t('common:status')}</span>
    </div>
    {scores.map((score) => <div className="supervisor-score-row" key={`${score.roundNo}-${score.judgeId}`} role="row">
      <span>{score.roundNo}R</span>
      <span>#{score.judgeId}</span>
      <strong className="red-score-text">{score.redScore ?? '-'}</strong>
      <strong className="blue-score-text">{score.blueScore ?? '-'}</strong>
      <span>{score.status === 'SUBMITTED' ? t('operations:score.submitted') : t('operations:score.draft')}</span>
    </div>)}
  </div>;
}

export function SupervisorAssignedPage({ session, onLogout, tournamentId }) {
  useTranslation();
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
      setError(requestMessage(requestError, localizedMessage('errors:assignment.rings')));
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
      setError(requestMessage(requestError, localizedMessage('errors:assignment.bouts')));
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
      if (isActive()) setActionError(requestMessage(requestError, localizedMessage('errors:result.reviewData')));
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
      setActionError(localizedMessage('errors:penalty.selectRound'));
      return;
    }
    if (!Number.isInteger(penaltyPoint) || penaltyPoint < 1) {
      setActionError(localizedMessage('errors:penalty.points'));
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
      setActionError(requestMessage(requestError, localizedMessage('errors:penalty.create')));
    } finally {
      busyRef.current = false;
      setBusy(false);
    }
  }

  function startResultConfirmation(event) {
    event.preventDefault();
    if (!canConfirm) {
      setActionError(boutStarted ? localizedMessage('errors:result.scoresNotReady') : localizedMessage('errors:result.startRequired'));
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
      setActionError(requestMessage(requestError, localizedMessage('errors:result.confirm')));
    } finally {
      busyRef.current = false;
      setBusy(false);
    }
  }

  function startResultCorrection(event) {
    event.preventDefault();
    if (!correctionForm.reason.trim()) {
      setActionError(localizedMessage('errors:result.reasonRequired'));
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
      setActionError(requestMessage(requestError, localizedMessage('errors:result.correct')));
    } finally {
      busyRef.current = false;
      setBusy(false);
    }
  }

  if (loading) return <main className="page-shell"><StatePanel title={t('operations:assignment.loading')}>{t('common:waitBriefly')}</StatePanel></main>;
  return <main className="page-shell supervisor-shell">
    <div className="judge-heading">
      <div><p className="eyebrow">{t('common:role.supervisor')}</p><h2>{t('operations:supervisor.title')}</h2><p>{t('operations:assignment.operator', { value1: session.account.name, value2: tournamentId })}</p><p aria-live="polite" className="stream-status" data-testid="stream-status">{t('operations:connection', { value1: streamStateLabel(streamState) })}</p></div>
      <button className="secondary-button" onClick={onLogout} type="button">{t('common:signOut')}</button>
    </div>
    {error && <StatePanel action={<button className="command-button" onClick={loadRings} type="button">{t('common:retry')}</button>} title={resolveMessage(error)} tone="error">{t('operations:assignment.scope')}</StatePanel>}
    {!error && !rings.length && <StatePanel title={t('operations:assignment.noRings')}>{t('operations:assignment.request')}</StatePanel>}
    {!error && rings.length > 0 && <div className="judge-layout">
      <aside>
        <label>{t('operations:assignment.ring')}<select aria-label={t('operations:assignment.ring')} onChange={(event) => { setRingId(Number(event.target.value)); setBoutId(null); }} value={ringId || ''}>{rings.map((ring) => <option key={ring.ringId} value={ring.ringId}>{ring.name} #{ring.ringId}</option>)}</select></label>
        <h3>{t('operations:assignment.bouts')}</h3>
        {bouts.length ? bouts.map((item) => <button className={`judge-bout-option${boutId === item.boutId ? ' selected' : ''}`} key={item.boutId} onClick={() => setBoutId(item.boutId)} type="button"><strong>{t('common:boutNumber', { value1: item.boutNumber })}</strong><small>{resolveMessage(statusLabel(item.status))}</small></button>) : <StatePanel title={t('operations:assignment.noBouts')}>{t('operations:assignment.emptyRing')}</StatePanel>}
      </aside>
      <section className="judge-score-panel">
        {bout ? <>
          <h3>{t('common:boutNumber', { value1: bout.boutNumber })}</h3>
          <p>{bout.redAthlete?.name || t('common:red')} vs {bout.blueAthlete?.name || t('common:blue')}</p>
          <h4>{t('operations:score.title')}</h4>
          <div className="supervisor-score-summary">
            <div className="supervisor-total red-total"><span>{t('operations:score.redTotal')}</span><strong>{scoreTotals.red}</strong><small>{t('operations:score.redEffective', { value1: penaltyTotals.blue, value2: effectiveRed })}</small></div>
            <div className="supervisor-total blue-total"><span>{t('operations:score.blueTotal')}</span><strong>{scoreTotals.blue}</strong><small>{t('operations:score.blueEffective', { value1: penaltyTotals.red, value2: effectiveBlue })}</small></div>
          </div>
          <p aria-live="polite" className="score-readiness">{t('operations:score.readiness', { value1: submittedScores.length, value2: draftScores.length })}</p>
          <ScoreReview scores={scores} />
          <h4>{t('operations:penalty.title')}</h4>
          <ul>{penalties.map((item) => <li key={item.penaltyId}>{t('operations:penalty.history', { value1: item.roundNo || '?', value2: item.targetSide === 'RED' ? t('operations:penalty.redToBlue') : t('operations:penalty.blueToRed'), value3: item.penaltyPoint, value4: item.reason })}</li>)}</ul>
          <form noValidate onSubmit={addPenalty}>
            <label>{t('common:round')}<select disabled={busy || confirmed} onChange={(event) => setPenalty({ ...penalty, roundNo: event.target.value })} required value={penalty.roundNo}>{penaltyRounds.map((roundNo) => <option key={roundNo} value={roundNo}>{t('common:roundOrdinal', { value1: roundNo })}</option>)}</select></label>
            <label>{t('operations:penalty.target')}<select disabled={busy || confirmed} onChange={(event) => setPenalty({ ...penalty, targetSide: event.target.value })} value={penalty.targetSide}><option value="RED">{t('common:red')}</option><option value="BLUE">{t('common:blue')}</option></select></label>
            <label>{t('operations:penalty.points')}<input disabled={busy || confirmed} inputMode="numeric" min="1" onChange={(event) => setPenalty({ ...penalty, penaltyPoint: event.target.value })} required type="number" value={penalty.penaltyPoint} /></label>
            <label>{t('operations:penalty.reason')}<input disabled={busy || confirmed} onChange={(event) => setPenalty({ ...penalty, reason: event.target.value })} value={penalty.reason} /></label>
            <button className="command-button" disabled={busy || confirmed} type="submit">{t('operations:penalty.add')}</button>
          </form>
          <h4>{t('operations:result.title')}</h4>
          {confirmed ? <>
            <div className="confirmed-result supervisor-confirmed"><strong>{resolveMessage(resultLabel(result))}</strong><small>{t('operations:result.correctionAvailable')}</small></div>
            <form noValidate onSubmit={startResultCorrection}>
              <label>{t('operations:result.winner')}<select disabled={busy} onChange={(event) => setCorrectionForm({ ...correctionForm, winnerSide: event.target.value })} value={correctionForm.winnerSide}><option value="RED">{t('common:red')}</option><option value="BLUE">{t('common:blue')}</option><option value="DRAW">{t('common:draw')}</option></select></label>
              <label>{t('operations:result.decision')}<select disabled={busy} onChange={(event) => setCorrectionForm({ ...correctionForm, decisionType: event.target.value })} value={correctionForm.decisionType}>{DECISION_TYPES.map((value) => <option key={value} value={value}>{resolveMessage(decisionLabel(value))}</option>)}</select></label>
              <label>{t('operations:result.correctionReason')}<textarea disabled={busy} maxLength="500" onChange={(event) => setCorrectionForm({ ...correctionForm, reason: event.target.value })} required value={correctionForm.reason} /></label>
              <button className="command-button" disabled={busy} type="submit">{t('operations:result.reviewCorrection')}</button>
            </form>
          </> : <form noValidate onSubmit={startResultConfirmation}>
            <label>{t('operations:result.winner')}<select disabled={busy} onChange={(event) => setResultForm({ ...resultForm, winnerSide: event.target.value })} value={resultForm.winnerSide}><option value="RED">{t('common:red')}</option><option value="BLUE">{t('common:blue')}</option><option value="DRAW">{t('common:draw')}</option></select></label>
            <label>{t('operations:result.decision')}<select disabled={busy} onChange={(event) => setResultForm({ ...resultForm, decisionType: event.target.value })} value={resultForm.decisionType}>{DECISION_TYPES.map((value) => <option key={value} value={value}>{resolveMessage(decisionLabel(value))}</option>)}</select></label>
            <button className="command-button" disabled={busy || !canConfirm} type="submit">{t('operations:result.review')}</button>
          </form>}
          {confirmingResult && !confirmed && <div aria-label={t('operations:result.confirm')} className="score-confirmation" role="dialog">
            <strong>{t('operations:result.confirm')}</strong>
            <p>{t('operations:result.boutSummary', { value1: bout.boutNumber, value2: bout.redAthlete?.name || t('common:red'), value3: bout.blueAthlete?.name || t('common:blue') })}</p>
            <p>{t('operations:result.effectiveSummary', { value1: resultLabel(resultForm), value2: effectiveRed, value3: effectiveBlue })}</p>
            {totalsTied && <p>{t('operations:result.tied')}</p>}
            {winnerMismatch && <p className="form-error" role="alert">{t('operations:result.mismatch')}</p>}
            <div><button className="command-button" disabled={busy} onClick={submitResult} type="button">{t('operations:result.confirm')}</button><button className="secondary-button" disabled={busy} onClick={() => setConfirmingResult(false)} type="button">{t('common:cancel')}</button></div>
          </div>}
          {confirmingCorrection && confirmed && <div aria-label={t('operations:result.confirmCorrection')} className="score-confirmation" role="dialog">
            <strong>{t('operations:result.confirmCorrection')}</strong>
            <p>{t('operations:result.correctionSummary', { value1: bout.boutNumber, value2: resultLabel(correctionForm) })}</p>
            <p>{t('operations:result.reasonSummary', { value1: correctionForm.reason })}</p>
            <div><button className="command-button" disabled={busy} onClick={submitResultCorrection} type="button">{t('operations:result.submitCorrection')}</button><button className="secondary-button" disabled={busy} onClick={() => setConfirmingCorrection(false)} type="button">{t('common:cancel')}</button></div>
          </div>}
          {actionError && <p aria-live="polite" className="form-error" role="alert">{resolveMessage(actionError)}</p>}
        </> : <StatePanel title={t('operations:selectBout')}>{t('operations:result.selectHint')}</StatePanel>}
      </section>
    </div>}
  </main>;
}

export function AssignedSupervisorRoute({ tournamentId }) {
  useTranslation();
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
    } catch (requestError) { setError(requestError.message === 'SUPERVISOR_ONLY' ? localizedMessage('errors:role.supervisor') : localizedMessage('errors:auth.login')); }
  }
  async function signOut() { await logout(session.accessToken).catch(() => undefined); window.sessionStorage.removeItem(SESSION_KEY); setSession(null); }
  if (!session) return <main className="page-shell auth-shell"><section className="auth-panel"><h2>{t('operations:supervisor.title')}</h2><form onSubmit={signIn}><label>{t('common:loginId')}<input required value={loginId} onChange={(event) => setLoginId(event.target.value)} /></label><label>{t('common:password')}<input required type="password" value={password} onChange={(event) => setPassword(event.target.value)} /></label>{error && <p className="form-error" role="alert">{resolveMessage(error)}</p>}<button className="command-button" type="submit">{t('common:signIn')}</button></form></section></main>;
  return <SupervisorAssignedPage onLogout={signOut} session={session} tournamentId={tournamentId} />;
}
