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
  BOUT_ALREADY_IN_PROGRESS: '이 링에서 다른 경기가 진행 중입니다.',
  BOUT_ALREADY_STARTED: '이 경기는 이미 진행 중입니다.',
  BOUT_RESULT_REQUIRED: '경기를 종료하려면 감독관의 결과 확정이 필요합니다.',
  CONCURRENT_MODIFICATION: '다른 운영자가 경기를 변경했습니다. 새로고침 후 확인해 주세요.',
  CURRENT_BOUT_NOT_FINISHED: '현재 경기를 종료해야 다음 경기를 준비할 수 있습니다.',
  INVALID_BOUT_STATE: '현재 경기 상태에서는 처리할 수 없습니다.',
  INVALID_BOUT_TRANSITION: '현재 경기 상태에서는 해당 상태로 변경할 수 없습니다.',
  NEXT_BOUT_NOT_FOUND: '이 링에는 다음 공식 경기가 없습니다.',
  RING_ALREADY_HAS_CURRENT_BOUT: '다른 경기를 시작하기 전에 링의 현재 경기를 확인해 주세요.',
  ROUND_OUT_OF_RANGE: '경기의 라운드 범위를 벗어났습니다.',
  ROUND_SEQUENCE_INVALID: '라운드는 순서대로 시작해 주세요.',
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
  if (error?.status === 401) return '세션이 만료되었습니다. 다시 로그인해 주세요.';
  if (error?.status === 403) return '배정 권한이 없습니다. 관리자에게 확인해 주세요.';
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
      setError(actionErrorMessage(requestError, '배정된 링을 불러오지 못했습니다.'));
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
      setError(actionErrorMessage(requestError, '선택한 링에 접근할 수 없습니다. 배정을 확인해 주세요.'));
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
      setActionError(actionErrorMessage(requestError, '링 운영 처리에 실패했습니다.'));
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

  if (loading) return <main className="page-shell"><StatePanel title="배정된 링을 불러오는 중">잠시 기다려 주세요.</StatePanel></main>;

  return <main className="page-shell ring-manager-shell">
    <div className="judge-heading">
      <div>
        <p className="eyebrow">링 매니저</p>
        <h2>링 운영</h2>
        <p>{session.account.name} | 대회 {tournamentId}</p>
        <p aria-live="polite" className="stream-status" data-testid="stream-status">실시간 연결: {streamStateLabel(streamState)}</p>
      </div>
      <button className="secondary-button" onClick={onLogout} type="button">로그아웃</button>
    </div>

    {error && <StatePanel action={<button className="command-button" onClick={loadRings} type="button">다시 시도</button>} title={error} tone="error">활성 상태로 배정된 링에만 접근할 수 있습니다.</StatePanel>}
    {!error && !rings.length && <StatePanel title="배정된 링이 없습니다">관리자에게 링 배정을 요청해 주세요.</StatePanel>}

    {!error && rings.length > 0 && <div className="ring-manager-layout">
      <aside>
        <label>배정된 링<select aria-label="배정된 링" onChange={(event) => { setRingId(Number(event.target.value)); setBoutId(null); }} value={ringId || ''}>{rings.map((ring) => <option key={ring.ringId} value={ring.ringId}>{ring.name} #{ring.ringId}</option>)}</select></label>
        {bouts.length ? bouts.map((item) => <button className={`judge-bout-option${boutId === item.boutId ? ' selected' : ''}`} key={item.boutId} onClick={() => { setBoutId(item.boutId); setRound(nextRoundValue(item)); }} type="button"><strong>경기 {item.boutNumber}</strong><small>{statusLabel(item.status)} · 라운드 {item.currentRound || 0}{item.totalRounds ? `/${item.totalRounds}` : ''}</small></button>) : <StatePanel title="경기가 없습니다">이 링에는 공식 경기가 없습니다.</StatePanel>}
      </aside>

      <section className="ring-manager-panel">
        <div className="ring-current-card">
          <p className="eyebrow">현재 경기</p>
          <h3>{currentBout ? `경기 ${currentBout.boutNumber}` : '진행 중인 경기가 없습니다'}</h3>
          <p>{currentBout ? `${currentBout.matchType || '공식 경기'} · ${statusLabel(currentBout.status)}` : '준비된 경기가 없습니다.'}</p>
        </div>

        {selected ? <>
          <div className="ring-selected-heading">
            <div>
              <p className="eyebrow">선택한 경기 {selected.boutNumber}</p>
              <h3>{selected.matchType || '공식 경기'}</h3>
              <p>현재 상태: {statusLabel(selected.status)} · 라운드 {selected.currentRound || 0}{selected.totalRounds ? `/${selected.totalRounds}` : ''}</p>
            </div>
            <span className="status-pill">{statusLabel(selected.status)}</span>
          </div>

          {currentBout && currentBout.boutId !== selected.boutId && <p className="form-error" role="status">선택한 경기가 링의 현재 경기와 다릅니다. 현재 경기로 지정될 때까지 시작할 수 없습니다.</p>}
          {actionError && <p aria-live="polite" className="form-error" role="alert">{actionError}</p>}
          {actionMessage && <p aria-live="polite" className="ring-success-message">{actionMessage}</p>}

          <div className="ring-command-grid">
            {selected.status === 'SCHEDULED' && <button className="secondary-button" disabled={busy} onClick={() => askForConfirmation(
              () => updateBoutStatus(selected.boutId, 'CANCELED', session.accessToken),
              '경기 취소',
              `경기 ${selected.boutNumber} · 예정 → 취소`,
              '경기를 취소했습니다.'
            )} type="button">경기 취소</button>}

            {selected.status === 'READY' && <button className="command-button" disabled={busy || !selectedIsCurrent} onClick={() => askForConfirmation(
              () => startBout(selected.boutId, session.accessToken),
              '경기 시작',
              `경기 ${selected.boutNumber} · 준비 → 진행 중`,
              '경기를 시작했습니다.'
            )} type="button">경기 시작</button>}

            {selected.status === 'IN_PROGRESS' && <>
              <label>다음 라운드<input aria-label="다음 라운드" disabled={busy || !selectedIsCurrent} max={selected.totalRounds || undefined} min="1" onChange={(event) => setRound(event.target.value)} type="number" value={round} /></label>
              <button className="command-button" disabled={busy || !canStartRound} onClick={() => run(
                () => startRound(selected.boutId, parsedRound, session.accessToken),
                `라운드 ${parsedRound}를 시작했습니다.`
              )} type="button">라운드 시작</button>
              <button className="secondary-button" disabled={busy || !canSendToScoring} onClick={() => askForConfirmation(
                () => updateBoutStatus(selected.boutId, 'SCORING', session.accessToken),
                '채점 단계로 전환',
                `경기 ${selected.boutNumber} · 진행 중 → 채점 중`,
                '채점 단계로 전환했습니다.'
              )} type="button">채점 단계로 전환</button>
            </>}

            {selected.status === 'SCORING' && <p className="dialog-state">감독관의 결과 확정을 기다리고 있습니다.</p>}
            {selected.status === 'FINISHED' && <p className="dialog-state">결과가 확정되었습니다. 링이 준비되면 다음 경기를 준비할 수 있습니다.</p>}
            {selected.status === 'CANCELED' && <p className="dialog-state">취소된 경기는 변경할 수 없습니다.</p>}

            <button className="secondary-button" disabled={busy || !canPrepareNext} onClick={() => askForConfirmation(
              () => moveToNextBout(ringId, session.accessToken),
              '다음 경기 준비',
              `링 ${selectedRing?.name || ringId} · 진행 순서에 따른 다음 공식 경기`,
              '다음 경기를 준비했습니다.'
            )} type="button">다음 경기 준비</button>
          </div>
        </> : <StatePanel title="경기를 선택하세요">배정된 경기 중 운영할 경기를 선택해 주세요.</StatePanel>}
      </section>
    </div>}

    {pendingAction && <div aria-modal="true" className="dialog-backdrop" role="dialog" aria-label="링 운영 확인">
      <section className="bout-dialog">
        <p className="eyebrow">운영 확인</p>
        <h2>{pendingAction.title}</h2>
        <p className="dialog-status">{pendingAction.details}</p>
        <div className="score-confirmation">
          <div><button className="command-button" disabled={busy} onClick={confirmPendingAction} type="button">확인</button><button className="secondary-button" disabled={busy} onClick={() => setPendingAction(null)} type="button">취소</button></div>
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
      setError(requestError.message === 'RING_MANAGER_ONLY' ? '링 매니저 권한이 필요합니다.' : '로그인에 실패했습니다.');
    }
  }

  async function signOut() {
    await logout(session.accessToken).catch(() => undefined);
    window.sessionStorage.removeItem(SESSION_KEY);
    setSession(null);
  }

  if (!session) return <main className="page-shell auth-shell"><section className="auth-panel"><h2>링 운영</h2><form onSubmit={signIn}><label>로그인 ID<input required value={loginId} onChange={(event) => setLoginId(event.target.value)} /></label><label>비밀번호<input required type="password" value={password} onChange={(event) => setPassword(event.target.value)} /></label>{error && <p className="form-error" role="alert">{error}</p>}<button className="command-button" type="submit">로그인</button></form></section></main>;
  return <RingManagerAssignedPage onLogout={signOut} session={session} tournamentId={tournamentId} />;
}
