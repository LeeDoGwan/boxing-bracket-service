import { useCallback, useEffect, useMemo, useState } from 'react';
import { login, logout } from '../api/auth';
import { getOperationStatus } from '../api/operations';
import { StatePanel } from '../components/StatePanel';

const SESSION_KEY = 'boxing.operations.session';
const ALLOWED_ROLES = new Set(['GAME_MANAGER', 'SERVICE_MANAGER']);
const STATUS_LABELS = {
  CANCELED: '취소',
  FINISHED: '종료',
  IN_PROGRESS: '진행 중',
  READY: '준비',
  SCHEDULED: '예정',
  SCORING: '채점 중',
};
const RING_STATUS_LABELS = {
  CLOSED: '운영 종료',
  IN_PROGRESS: '진행 중',
  READY: '준비',
};
const STATUS_ORDER = ['IN_PROGRESS', 'SCORING', 'READY', 'SCHEDULED', 'FINISHED', 'CANCELED'];

function readSession() {
  try {
    const stored = window.sessionStorage.getItem(SESSION_KEY);
    const session = stored ? JSON.parse(stored) : null;
    return session?.accessToken && ALLOWED_ROLES.has(session?.account?.role) ? session : null;
  } catch {
    return null;
  }
}

function statusLabel(status) {
  return STATUS_LABELS[status] || status || '상태 미정';
}

function ringStatusLabel(status) {
  return RING_STATUS_LABELS[status] || status || '상태 미정';
}

function formatJudgeIds(ids) {
  return ids.length ? ids.map((id) => `#${id}`).join(', ') : '없음';
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
      if (!ALLOWED_ROLES.has(session.account?.role)) {
        throw new Error('OPERATIONS_ONLY');
      }
      onLogin(session);
    } catch (requestError) {
      setError(requestError.message === 'OPERATIONS_ONLY'
        ? '게임 매니저 또는 서비스 매니저 계정으로 로그인해 주세요.'
        : '로그인 정보를 확인해 주세요.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="page-shell auth-shell">
      <section className="auth-panel">
        <p className="eyebrow">OPERATIONS DESK</p>
        <h2>운영 현황 로그인</h2>
        <p>대회 전체 진행 상태를 확인하려면 운영자 계정으로 로그인하세요.</p>
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

function Metric({ label, value, tone = '' }) {
  return <article className={`operation-stat${tone ? ` operation-stat-${tone}` : ''}`}><span>{label}</span><strong>{value}</strong></article>;
}

function BoutSummary({ bout, emptyLabel = '없음' }) {
  if (!bout) {
    return <p className="operation-empty">{emptyLabel}</p>;
  }
  return (
    <div className="operation-bout-summary">
      <strong>경기 {bout.boutNumber}</strong>
      <span>{statusLabel(bout.status)} · {bout.currentRound || 0}/{bout.totalRounds || '?'} 라운드</span>
      <small>{bout.matchType || '일반 경기'} · 결과 {bout.resultConfirmed ? '확정' : '미확정'}</small>
    </div>
  );
}

function RingStatusCard({ ring }) {
  return (
    <article className="operation-ring-card">
      <div className="operation-ring-heading">
        <div><p className="eyebrow">RING {ring.ringId}</p><h3>{ring.ringName}</h3></div>
        <span className="status-pill">{ringStatusLabel(ring.ringStatus)}</span>
      </div>
      <div className="operation-bout-block"><span>현재 경기</span><BoutSummary bout={ring.currentBout} emptyLabel="진행 중인 경기 없음" /></div>
      <div className="operation-bout-block"><span>다음 경기</span><BoutSummary bout={ring.nextBout} emptyLabel="다음 경기 없음" /></div>
    </article>
  );
}

function JudgeProgress({ progress }) {
  const submitted = progress.submittedJudgeIds || [];
  const unsubmitted = progress.unsubmittedJudgeIds || [];
  const total = submitted.length + unsubmitted.length;
  const percentage = total ? Math.round((submitted.length / total) * 100) : 0;
  return (
    <article className="operation-progress-row">
      <div className="operation-progress-heading"><strong>경기 {progress.boutNumber}</strong><span>{progress.roundNo}라운드 · {submitted.length}/{total || 0}명 제출</span></div>
      <div aria-label={`경기 ${progress.boutNumber} 심판 제출 진행률`} aria-valuemax="100" aria-valuemin="0" aria-valuenow={percentage} className="operation-progress-track" role="progressbar"><span style={{ width: `${percentage}%` }} /></div>
      <div className="operation-progress-detail"><span>제출: {formatJudgeIds(submitted)}</span><span>대기: {formatJudgeIds(unsubmitted)}</span></div>
    </article>
  );
}

function BoutAlertList({ bouts, title, emptyLabel }) {
  return (
    <section className="operation-panel operation-alert-panel">
      <div className="operation-panel-heading"><div><p className="eyebrow">ATTENTION</p><h3>{title}</h3></div><span>{bouts.length}건</span></div>
      {bouts.length ? <ul className="operation-alert-list">{bouts.map((bout) => <li key={bout.boutId}><strong>경기 {bout.boutNumber}</strong><span>{statusLabel(bout.status)} · 링 {bout.ringId || '-'}</span></li>)}</ul> : <p className="operation-empty">{emptyLabel}</p>}
    </section>
  );
}

function OperationsWorkspace({ onLogout, session, tournamentId }) {
  const [status, setStatus] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadStatus = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setStatus(await getOperationStatus(tournamentId, session.accessToken));
    } catch {
      setError('운영 현황을 불러오지 못했습니다. 대회 ID와 권한을 확인해 주세요.');
    } finally {
      setLoading(false);
    }
  }, [session.accessToken, tournamentId]);

  useEffect(() => {
    loadStatus();
  }, [loadStatus]);

  const statusMetrics = useMemo(() => STATUS_ORDER.map((statusKey) => ({ label: statusLabel(statusKey), count: status?.boutStatusCounts?.[statusKey] || 0, statusKey })), [status]);
  const incompleteProgress = useMemo(() => (status?.judgeScoreSubmissionStatuses || []).filter((progress) => !progress.complete), [status]);

  return (
    <main className="page-shell operations-shell">
      <div className="judge-heading">
        <div><p className="eyebrow">OPERATIONS DESK</p><h2>대회 운영 현황</h2><p>로그인 계정: {session.account.name} · 대회 ID {tournamentId}</p></div>
        <div className="operations-heading-actions"><button className="command-button" disabled={loading} onClick={loadStatus} type="button">새로고침</button><button className="secondary-button" onClick={onLogout} type="button">로그아웃</button></div>
      </div>

      {loading ? <StatePanel title="운영 현황을 불러오는 중입니다.">잠시만 기다려 주세요.</StatePanel> : null}
      {error && !loading ? <StatePanel action={<button className="command-button" onClick={loadStatus} type="button">다시 시도</button>} title="운영 현황을 불러오지 못했습니다." tone="error">대회 ID와 운영자 권한을 확인한 뒤 다시 시도해 주세요.</StatePanel> : null}
      {!loading && !error && status ? (
        <>
          <section className="operation-panel operation-summary-panel">
            <div className="operation-panel-heading"><div><p className="eyebrow">TOURNAMENT {status.tournamentId}</p><h3>경기 진행 요약</h3></div><span>전체 {status.totalBoutCount}건</span></div>
            <div className="operation-stat-grid">{statusMetrics.map((metric) => <Metric key={metric.statusKey} label={metric.label} tone={metric.statusKey === 'IN_PROGRESS' ? 'active' : ''} value={metric.count} />)}</div>
          </section>

          <section className="operation-panel operation-rings-panel">
            <div className="operation-panel-heading"><div><p className="eyebrow">RINGS</p><h3>링별 진행 상태</h3></div><span>{status.rings?.length || 0}개 링</span></div>
            {status.rings?.length ? <div className="operation-ring-grid">{status.rings.map((ring) => <RingStatusCard key={ring.ringId} ring={ring} />)}</div> : <p className="operation-empty">등록된 링이 없습니다.</p>}
          </section>

          <div className="operations-two-column">
            <section className="operation-panel operation-progress-panel">
              <div className="operation-panel-heading"><div><p className="eyebrow">JUDGE SUBMISSIONS</p><h3>심판 제출 현황</h3></div><span>{incompleteProgress.length}건 대기</span></div>
              {incompleteProgress.length ? <div className="operation-progress-list">{incompleteProgress.map((progress) => <JudgeProgress key={`${progress.boutId}-${progress.roundNo}`} progress={progress} />)}</div> : <p className="operation-empty">미완료 심판 제출이 없습니다.</p>}
            </section>
            <BoutAlertList bouts={status.pendingResultBouts || []} emptyLabel="미확정 결과가 없습니다." title="결과 확정 대기" />
          </div>

          <BoutAlertList bouts={status.stalledBouts || []} emptyLabel="15분 이상 지연된 경기가 없습니다." title="지연 경기" />
        </>
      ) : null}
    </main>
  );
}

export function OperationsPage({ tournamentId }) {
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

  return session ? <OperationsWorkspace onLogout={handleLogout} session={session} tournamentId={tournamentId} /> : <LoginForm onLogin={handleLogin} />;
}
