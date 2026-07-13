import { useCallback, useEffect, useMemo, useState } from 'react';
import { login, logout } from '../api/auth';
import { getRingBouts, moveToNextBout, startBout, startRound, updateBoutStatus } from '../api/ringManager';
import { StatePanel } from '../components/StatePanel';

const SESSION_KEY = 'boxing.ring-manager.session';
const STATUS_LABELS = {
  CANCELED: '취소',
  FINISHED: '종료',
  IN_PROGRESS: '진행 중',
  READY: '준비',
  SCHEDULED: '예정',
  SCORING: '채점 중',
};

function readSession() {
  try {
    const stored = window.sessionStorage.getItem(SESSION_KEY);
    const session = stored ? JSON.parse(stored) : null;
    return session?.accessToken && session?.account?.role === 'RING_MANAGER' ? session : null;
  } catch {
    return null;
  }
}

function statusLabel(status) {
  return STATUS_LABELS[status] || status || '상태 미정';
}

function readInitialRingId() {
  const value = Number.parseInt(new URLSearchParams(window.location.search).get('ringId'), 10);
  return Number.isInteger(value) && value > 0 ? value : 1;
}

function LoginForm({ onLogin }) {
  const [form, setForm] = useState({ loginId: '', password: '' });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const session = await login(form.loginId, form.password);
      if (session.account?.role !== 'RING_MANAGER') {
        throw new Error('RING_MANAGER_ONLY');
      }
      onLogin(session);
    } catch (requestError) {
      setError(requestError.message === 'RING_MANAGER_ONLY' ? '링 운영자 계정으로 로그인해 주세요.' : '로그인 정보를 확인해 주세요.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="page-shell auth-shell">
      <section className="auth-panel">
        <p className="eyebrow">RING MANAGER DESK</p>
        <h2>링 운영자 로그인</h2>
        <p>링 경기 상태를 관리하려면 링 운영자 계정으로 로그인하세요.</p>
        <form onSubmit={handleSubmit}>
          <label>아이디<input autoComplete="username" onChange={(event) => setForm((current) => ({ ...current, loginId: event.target.value }))} required value={form.loginId} /></label>
          <label>비밀번호<input autoComplete="current-password" onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))} required type="password" value={form.password} /></label>
          {error && <p aria-live="polite" className="form-error" role="alert">{error}</p>}
          <button className="command-button" disabled={submitting} type="submit">{submitting ? '로그인 중...' : '로그인'}</button>
        </form>
      </section>
    </main>
  );
}

function RingManagerWorkspace({ onLogout, session }) {
  const [ringIdInput, setRingIdInput] = useState(String(readInitialRingId()));
  const [activeRingId, setActiveRingId] = useState(readInitialRingId);
  const [bouts, setBouts] = useState([]);
  const [selectedBoutId, setSelectedBoutId] = useState(null);
  const [listLoading, setListLoading] = useState(true);
  const [listError, setListError] = useState('');
  const [actionError, setActionError] = useState('');
  const [actionMessage, setActionMessage] = useState('');
  const [actionLoading, setActionLoading] = useState(false);
  const [statusValue, setStatusValue] = useState('READY');
  const [roundValue, setRoundValue] = useState('1');

  const selectedBout = useMemo(() => bouts.find((bout) => bout.boutId === selectedBoutId) || null, [bouts, selectedBoutId]);
  const currentBout = useMemo(() => bouts.find((bout) => bout.status === 'IN_PROGRESS') || bouts.find((bout) => bout.status === 'READY') || null, [bouts]);

  const loadRingBouts = useCallback(async () => {
    setListLoading(true);
    setListError('');
    try {
      const nextBouts = (await getRingBouts(activeRingId, session.accessToken)) || [];
      setBouts(nextBouts);
      setSelectedBoutId((current) => current && nextBouts.some((bout) => bout.boutId === current)
        ? current
        : nextBouts.find((bout) => bout.status === 'IN_PROGRESS')?.boutId || nextBouts[0]?.boutId || null);
    } catch {
      setListError('링 경기 목록을 불러오지 못했습니다.');
    } finally {
      setListLoading(false);
    }
  }, [activeRingId, session.accessToken]);

  useEffect(() => {
    loadRingBouts();
  }, [loadRingBouts]);

  useEffect(() => {
    if (!selectedBout) {
      return;
    }
    setStatusValue(selectedBout.status || 'READY');
    setRoundValue(String(Math.max(1, (selectedBout.currentRound || 0) + 1)));
  }, [selectedBout]);

  async function handleRingChange(event) {
    event.preventDefault();
    const nextRingId = Number.parseInt(ringIdInput, 10);
    if (!Number.isInteger(nextRingId) || nextRingId < 1) {
      setActionError('링 ID는 1 이상의 정수로 입력하세요.');
      return;
    }
    setActionError('');
    setActionMessage('');
    setActiveRingId(nextRingId);
  }

  async function runAction(action, message) {
    setActionLoading(true);
    setActionError('');
    setActionMessage('');
    try {
      await action();
      await loadRingBouts();
      setActionMessage(message);
    } catch (error) {
      setActionError(error.message || '운영 명령을 처리하지 못했습니다.');
    } finally {
      setActionLoading(false);
    }
  }

  return (
    <main className="page-shell ring-manager-shell">
      <div className="judge-heading">
        <div><p className="eyebrow">RING MANAGER DESK</p><h2>링 경기 운영</h2><p>로그인 계정: {session.account.name} · 운영자 ID {session.account.accountId}</p></div>
        <button className="secondary-button" onClick={onLogout} type="button">로그아웃</button>
      </div>

      <form className="ring-selector" onSubmit={handleRingChange}>
        <label>링 ID<input min="1" onChange={(event) => setRingIdInput(event.target.value)} type="number" value={ringIdInput} /></label>
        <button className="command-button" type="submit">링 불러오기</button>
        <span>현재 링: {activeRingId}</span>
      </form>

      {actionError && <p aria-live="polite" className="form-error ring-action-message" role="alert">{actionError}</p>}
      {actionMessage && <p aria-live="polite" className="ring-success-message">{actionMessage}</p>}
      {listLoading ? <StatePanel title="링 경기 목록을 불러오는 중입니다.">잠시만 기다려 주세요.</StatePanel> : null}
      {listError ? <StatePanel action={<button className="command-button" onClick={loadRingBouts} type="button">다시 시도</button>} title="링 경기 목록을 불러오지 못했습니다." tone="error">링 ID와 권한을 확인해 주세요.</StatePanel> : null}
      {!listLoading && !listError ? (
        <div className="ring-manager-layout">
          <aside>
            <div className="judge-section-heading"><div><p className="eyebrow">RING {activeRingId}</p><h3>경기 순서</h3></div><span>{bouts.length}경기</span></div>
            {bouts.length ? <div aria-label="링 경기 목록" className="judge-bout-list">{bouts.map((bout) => <button className={`judge-bout-option${selectedBoutId === bout.boutId ? ' selected' : ''}`} key={bout.boutId} onClick={() => setSelectedBoutId(bout.boutId)} type="button"><span>경기 {bout.boutNumber}</span><strong>{bout.matchType || '일반 경기'}</strong><small>{statusLabel(bout.status)} · {bout.currentRound || 0}라운드</small></button>)}</div> : <StatePanel title="경기가 없습니다.">해당 링에 등록된 공식 경기가 없습니다.</StatePanel>}
          </aside>

          <section aria-label="링 운영 명령" className="ring-manager-panel">
            <div className="ring-current-card"><p className="eyebrow">CURRENT BOUT</p><h3>{currentBout ? `경기 ${currentBout.boutNumber}` : '현재 경기 없음'}</h3><p>{currentBout ? `${currentBout.matchType || '일반 경기'} · ${statusLabel(currentBout.status)}` : '다음 경기를 준비하세요.'}</p></div>
            {selectedBout ? (
              <>
                <div className="ring-selected-heading"><div><p className="eyebrow">SELECTED BOUT {selectedBout.boutNumber}</p><h3>{selectedBout.matchType || '일반 경기'}</h3><p>현재 상태: {statusLabel(selectedBout.status)} · {selectedBout.currentRound || 0}/{selectedBout.totalRounds || '?'} 라운드</p></div><span className="status-pill">{statusLabel(selectedBout.status)}</span></div>
                <div className="ring-command-grid">
                  <button className="command-button" disabled={actionLoading} onClick={() => runAction(() => startBout(selectedBout.boutId, session.accessToken), '경기를 시작했습니다.')} type="button">경기 시작</button>
                  <label>시작할 라운드<input aria-label="시작할 라운드" min="1" onChange={(event) => setRoundValue(event.target.value)} type="number" value={roundValue} /></label>
                  <button className="command-button" disabled={actionLoading} onClick={() => runAction(() => startRound(selectedBout.boutId, Number.parseInt(roundValue, 10), session.accessToken), `${roundValue}라운드를 시작했습니다.`)} type="button">라운드 시작</button>
                  <label>경기 상태<select aria-label="경기 상태" onChange={(event) => setStatusValue(event.target.value)} value={statusValue}>{Object.entries(STATUS_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
                  <button className="command-button" disabled={actionLoading} onClick={() => runAction(() => updateBoutStatus(selectedBout.boutId, statusValue, session.accessToken), '경기 상태를 변경했습니다.')} type="button">상태 저장</button>
                </div>
              </>
            ) : null}
            <div className="ring-next-action"><div><p className="eyebrow">NEXT</p><h3>다음 경기</h3><p>현재 경기가 종료된 후 다음 경기를 준비합니다.</p></div><button className="secondary-button" disabled={actionLoading || !bouts.length} onClick={() => runAction(() => moveToNextBout(activeRingId, session.accessToken), '다음 경기를 준비했습니다.')} type="button">다음 경기 준비</button></div>
          </section>
        </div>
      ) : null}
    </main>
  );
}

export function RingManagerPage() {
  const [session, setSession] = useState(readSession);

  function handleLogin(nextSession) {
    window.sessionStorage.setItem(SESSION_KEY, JSON.stringify(nextSession));
    setSession(nextSession);
  }

  async function handleLogout() {
    if (session?.accessToken) {
      await logout(session.accessToken).catch(() => undefined);
    }
    window.sessionStorage.removeItem(SESSION_KEY);
    setSession(null);
  }

  return session ? <RingManagerWorkspace onLogout={handleLogout} session={session} /> : <LoginForm onLogin={handleLogin} />;
}
