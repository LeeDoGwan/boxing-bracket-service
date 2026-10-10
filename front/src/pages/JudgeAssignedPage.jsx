import { useTranslation } from 'react-i18next';
import { translate as t, localizedMessage, resolveMessage } from '../i18n';
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
  BOUT_ACCESS_DENIED: localizedMessage('errors:judge.notAssigned'),
  BOUT_NOT_STARTED: localizedMessage('errors:bout.notStarted'),
  INVALID_BOUT_STATE: localizedMessage('errors:score.boutClosed'),
  INVALID_ROUND_NUMBER: localizedMessage('errors:round.outOfRange'),
  INVALID_SCORE_VALUE: localizedMessage('errors:score.range'),
  RING_ACCESS_DENIED: localizedMessage('errors:assignment.inactive'),
  ROUND_NOT_STARTED: localizedMessage('errors:round.notStarted'),
  SCORE_ALREADY_SUBMITTED: localizedMessage('errors:score.alreadySubmitted'),
};

function scoreErrorMessage(requestError) {
  if (requestError.status === 401) return localizedMessage('errors:session.expired');
  return SCORE_ERROR_MESSAGES[requestError.message] || localizedMessage('errors:score.submit');
}

export function JudgeAssignedPage({ session, onLogout, tournamentId }) {
  useTranslation();
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
      setError(requestError.status === 401 ? localizedMessage('errors:session.expired') : requestError.status === 403 ? localizedMessage('errors:assignment.denied') : localizedMessage('errors:assignment.rings'));
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
      setError(requestError.status === 401 ? localizedMessage('errors:session.expired') : localizedMessage('errors:assignment.revoked'));
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
        setDetailError(requestError.status === 403 ? localizedMessage('errors:bout.accessDenied') : localizedMessage('errors:bout.detail'));
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
  if (loading) return <main className="page-shell"><StatePanel title={t('operations:assignment.loading')}>{t('common:waitBriefly')}</StatePanel></main>;
  return <main className="page-shell judge-shell">
    <div className="judge-heading">
      <div><p className="eyebrow">{t('common:role.judge')}</p><h2>{t('operations:judge.title')}</h2><p>{t('operations:assignment.operator', { value1: session.account.name, value2: tournamentId })}</p><p aria-live="polite" className="stream-status" data-testid="stream-status">{t('operations:connection', { value1: streamStateLabel(streamState) })}</p></div>
      <button className="secondary-button" onClick={onLogout} type="button">{t('common:signOut')}</button>
    </div>
    {error && <StatePanel action={<button className="command-button" onClick={loadRings} type="button">{t('common:retry')}</button>} title={resolveMessage(error)} tone="error">{t('operations:assignment.scope')}</StatePanel>}
    {!error && !rings.length && <StatePanel title={t('operations:assignment.noRings')}>{t('operations:assignment.request')}</StatePanel>}
    {!error && rings.length > 0 && <div className="judge-layout">
      <aside>
        <label>{t('operations:assignment.ring')}<select aria-label={t('operations:assignment.ring')} onChange={(event) => { setRingId(Number(event.target.value)); setBoutId(null); }} value={ringId || ''}>{rings.map((ring) => <option key={ring.ringId} value={ring.ringId}>{ring.name} #{ring.ringId}</option>)}</select></label>
        <h3>{t('operations:assignment.bouts')}</h3>
        {bouts.length ? <div className="judge-bout-list">{bouts.map((item) => <button className={`judge-bout-option${boutId === item.boutId ? ' selected' : ''}`} key={item.boutId} onClick={() => setBoutId(item.boutId)} type="button"><span>{t('common:boutNumber', { value1: item.boutNumber })}</span><strong>{item.matchType || t('common:officialBout')}</strong><small>{resolveMessage(statusLabel(item.status))}</small></button>)}</div> : <StatePanel title={t('operations:assignment.noBouts')}>{t('operations:assignment.emptyRing')}</StatePanel>}
      </aside>
      <section className="judge-score-panel" aria-label={t('operations:judge.scoreEntry')}>
        {detailError && <StatePanel title={detailError} tone="error">{t('common:refreshAndRetry')}</StatePanel>}
        {bout && <><h3>{t('common:boutNumber', { value1: bout.boutNumber })}</h3><p>{bout.redAthlete?.name || t('common:red')} vs {bout.blueAthlete?.name || t('common:blue')}</p>{rounds.map((roundNo) => <ScoreCard key={`${bout.boutId}-${roundNo}`} busy={busy} bout={bout} onSubmit={submit} roundNo={roundNo} score={scores.find((score) => score.roundNo === roundNo)} />)}</>}
      </section>
    </div>}
  </main>;
}

const SESSION_KEY = 'boxing.judge.session';

export function AssignedJudgeRoute({ tournamentId }) {
  useTranslation();
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
    } catch (requestError) { setError(requestError.message === 'JUDGE_ONLY' ? localizedMessage('errors:role.judge') : localizedMessage('errors:auth.login')); }
  }
  async function signOut() { await logout(session.accessToken).catch(() => undefined); window.sessionStorage.removeItem(SESSION_KEY); setSession(null); }
  if (!session) return <main className="page-shell auth-shell"><section className="auth-panel"><h2>{t('operations:judge.title')}</h2><form onSubmit={signIn}><label>{t('common:loginId')}<input required value={loginId} onChange={(event) => setLoginId(event.target.value)} /></label><label>{t('common:password')}<input required type="password" value={password} onChange={(event) => setPassword(event.target.value)} /></label>{error && <p className="form-error" role="alert">{resolveMessage(error)}</p>}<button className="command-button" type="submit">{t('common:signIn')}</button></form></section></main>;
  return <JudgeAssignedPage onLogout={signOut} session={session} tournamentId={tournamentId} />;
}

function ScoreCard({ bout, busy, onSubmit, roundNo, score }) {
  useTranslation();
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
  const roundStatus = submitted ? t('operations:score.submitted') : closed ? t('common:finished') : futureRound ? t('operations:round.notStarted') : roundNo === currentRound ? t('operations:round.current') : t('operations:round.previous');
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
      setValidationError(localizedMessage('errors:score.bothRequired'));
      return;
    }
    if (!/^\d+$/.test(String(redScore)) || !/^\d+$/.test(String(blueScore))
      || Number(redScore) > MAX_SCORE || Number(blueScore) > MAX_SCORE) {
      setValidationError(localizedMessage('errors:score.range'));
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
    <h4>{t('common:roundNumber', { value1: roundNo })}</h4><p className="round-status">{roundStatus}</p>
    <label><span>{t('operations:score.red')}<small className="score-range-hint">{t('operations:score.range')}</small></span><input aria-describedby={validationError ? `round-${roundNo}-error` : undefined} aria-label={t('operations:score.red')} disabled={busy || submitted || closed || futureRound} inputMode="numeric" max={MAX_SCORE} min="0" pattern="[0-9]*" required type="number" value={redScore} onChange={(event) => { setDirty(true); setValidationError(''); setRedScore(event.target.value); }} /></label>
    <label><span>{t('operations:score.blue')}<small className="score-range-hint">{t('operations:score.range')}</small></span><input aria-describedby={validationError ? `round-${roundNo}-error` : undefined} aria-label={t('operations:score.blue')} disabled={busy || submitted || closed || futureRound} inputMode="numeric" max={MAX_SCORE} min="0" pattern="[0-9]*" required type="number" value={blueScore} onChange={(event) => { setDirty(true); setValidationError(''); setBlueScore(event.target.value); }} /></label>
    {validationError && <p className="form-error" id={`round-${roundNo}-error`} role="alert">{resolveMessage(validationError)}</p>}
    {!submitted && !closed && <button className="command-button" disabled={!canSubmit} type="submit">{t('operations:score.submitRound')}</button>}
    {confirming && <div aria-label={t('operations:score.confirmRound', { value1: roundNo })} className="score-confirmation" role="dialog"><p>{t('operations:score.review', { value1: roundNo, value2: redScore, value3: blueScore })}</p><div><button className="command-button" disabled={busy} onClick={confirmSubmission} type="button">{t('operations:score.confirmSubmit')}</button><button className="secondary-button" onClick={() => setConfirming(false)} type="button">{t('common:cancel')}</button></div></div>}
  </form>;
}
