import { useCallback, useEffect, useState } from 'react';
import { login, logout } from '../api/auth';
import { getAuditLogs } from '../api/auditLogs';
import { StatePanel } from '../components/StatePanel';

const SESSION_KEY = 'boxing.operations.session';
const ALLOWED_ROLES = new Set(['GAME_MANAGER', 'SERVICE_MANAGER']);
const ACTION_LABELS = {
  ACCOUNT_CREATED: '계정 생성',
  ACCOUNT_DELETED: '계정 삭제',
  ACCOUNT_UPDATED: '계정 수정',
  BOUT_CREATED: '경기 생성',
  BOUT_DELETED: '경기 삭제',
  BOUT_IMPORTED: '경기 가져오기',
  BOUT_STARTED: '경기 시작',
  BOUT_STATUS_CHANGED: '경기 상태 변경',
  BOUT_UPDATED: '경기 수정',
  LOGIN_FAILED: '로그인 실패',
  LOGIN_SUCCEEDED: '로그인 성공',
  LOGOUT: '로그아웃',
  NEXT_BOUT_READY: '다음 경기 준비',
  NOTICE_CREATED: '공지 생성',
  NOTICE_DELETED: '공지 삭제',
  NOTICE_UPDATED: '공지 수정',
  PENALTY_CREATED: '감점 생성',
  RESULT_CONFIRMED: '결과 확정',
  ROUND_STARTED: '라운드 시작',
  SCORE_SUBMITTED: '점수 제출',
};
const TARGET_LABELS = {
  ACCOUNT: '계정',
  AUTH: '인증',
  BOUT: '경기',
  BOUT_RESULT: '경기 결과',
  NOTICE: '공지',
  PENALTY: '감점',
  RING: '링',
  ROUND_SCORE: '라운드 점수',
};
const ROLE_LABELS = {
  GAME_MANAGER: '게임 매니저',
  JUDGE: '심판',
  RING_MANAGER: '링 운영자',
  SERVICE_MANAGER: '서비스 매니저',
  SUPERVISOR: '감독',
};
const INITIAL_FILTERS = {
  actionType: '',
  actorAccountId: '',
  actorRole: '',
  boutId: '',
  from: '',
  page: 0,
  ringId: '',
  size: 20,
  success: '',
  targetType: '',
  to: '',
  tournamentId: '',
};

function readSession() {
  try {
    const stored = window.sessionStorage.getItem(SESSION_KEY);
    const session = stored ? JSON.parse(stored) : null;
    return session?.accessToken && ALLOWED_ROLES.has(session?.account?.role) ? session : null;
  } catch {
    return null;
  }
}

function initialFilters(tournamentId) {
  return { ...INITIAL_FILTERS, tournamentId: String(tournamentId) };
}

function labelFor(labels, value) {
  return labels[value] || value || '-';
}

function formatDateTime(value) {
  if (!value) {
    return '-';
  }
  return new Date(value).toLocaleString('ko-KR');
}

function formatSnapshot(value) {
  if (!value) {
    return '';
  }
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
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
        <p className="eyebrow">AUDIT LOG DESK</p>
        <h2>감사 로그 로그인</h2>
        <p>운영 기록을 조회하려면 운영자 계정으로 로그인하세요.</p>
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

function FilterForm({ filters, onChange, onClear, onSubmit }) {
  return (
    <form className="audit-filter-panel" onSubmit={onSubmit}>
      <div className="operation-panel-heading"><div><p className="eyebrow">FILTERS</p><h3>감사 로그 필터</h3></div><span>최대 100건</span></div>
      <div className="audit-filter-grid">
        <label>대회 ID<input min="1" onChange={(event) => onChange('tournamentId', event.target.value)} type="number" value={filters.tournamentId} /></label>
        <label>행위자 계정 ID<input min="1" onChange={(event) => onChange('actorAccountId', event.target.value)} type="number" value={filters.actorAccountId} /></label>
        <label>행위자 역할<select aria-label="행위자 역할" onChange={(event) => onChange('actorRole', event.target.value)} value={filters.actorRole}><option value="">전체 역할</option>{Object.entries(ROLE_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
        <label>행위 유형<select aria-label="행위 유형" onChange={(event) => onChange('actionType', event.target.value)} value={filters.actionType}><option value="">전체 행위</option>{Object.entries(ACTION_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
        <label>대상 유형<select aria-label="대상 유형" onChange={(event) => onChange('targetType', event.target.value)} value={filters.targetType}><option value="">전체 대상</option>{Object.entries(TARGET_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
        <label>링 ID<input min="1" onChange={(event) => onChange('ringId', event.target.value)} type="number" value={filters.ringId} /></label>
        <label>경기 ID<input min="1" onChange={(event) => onChange('boutId', event.target.value)} type="number" value={filters.boutId} /></label>
        <label>처리 결과<select aria-label="처리 결과" onChange={(event) => onChange('success', event.target.value)} value={filters.success}><option value="">전체 결과</option><option value="true">성공</option><option value="false">실패</option></select></label>
        <label>시작 일시<input onChange={(event) => onChange('from', event.target.value)} type="datetime-local" value={filters.from} /></label>
        <label>종료 일시<input onChange={(event) => onChange('to', event.target.value)} type="datetime-local" value={filters.to} /></label>
      </div>
      <div className="audit-filter-actions"><button className="command-button" type="submit">필터 적용</button><button className="secondary-button" onClick={onClear} type="button">초기화</button></div>
    </form>
  );
}

function AuditLogRow({ log }) {
  const beforeData = formatSnapshot(log.beforeData);
  const afterData = formatSnapshot(log.afterData);
  return (
    <article className={`audit-log-row${log.success ? '' : ' audit-log-failed'}`} role="row">
      <div className="audit-log-cell audit-log-time"><time dateTime={log.createdAt}>{formatDateTime(log.createdAt)}</time><small>#{log.id}</small></div>
      <div className="audit-log-cell"><strong>{log.actorUsername || '시스템'}</strong><span>{labelFor(ROLE_LABELS, log.actorRole)} · ID {log.actorAccountId || '-'}</span></div>
      <div className="audit-log-cell"><strong>{labelFor(ACTION_LABELS, log.actionType)}</strong><span>{labelFor(TARGET_LABELS, log.targetType)} #{log.targetId || '-'}</span></div>
      <div className="audit-log-cell"><strong className={log.success ? 'audit-success' : 'audit-failure'}>{log.success ? '성공' : '실패'}</strong><span>{log.failureReason || (log.boutId ? `경기 ${log.boutId}` : '대상 정보 없음')}</span></div>
      <div className="audit-log-cell audit-log-data">{beforeData || afterData ? <details><summary>변경 데이터</summary><div className="audit-snapshot-grid">{beforeData && <div><span>이전</span><pre>{beforeData}</pre></div>}{afterData && <div><span>이후</span><pre>{afterData}</pre></div>}</div></details> : <span className="operation-empty">데이터 없음</span>}</div>
    </article>
  );
}

function AuditLogWorkspace({ onLogout, session, tournamentId }) {
  const [formFilters, setFormFilters] = useState(() => initialFilters(tournamentId));
  const [filters, setFilters] = useState(() => initialFilters(tournamentId));
  const [response, setResponse] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadLogs = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setResponse(await getAuditLogs(filters, session.accessToken));
    } catch {
      setError('감사 로그를 불러오지 못했습니다. 대회 ID와 권한을 확인해 주세요.');
    } finally {
      setLoading(false);
    }
  }, [filters, session.accessToken]);

  useEffect(() => {
    loadLogs();
  }, [loadLogs]);

  useEffect(() => {
    const nextTournamentId = String(tournamentId);
    setFormFilters((current) => current.tournamentId === nextTournamentId ? current : ({ ...current, tournamentId: nextTournamentId }));
    setFilters((current) => current.tournamentId === nextTournamentId ? current : ({ ...current, page: 0, tournamentId: nextTournamentId }));
  }, [tournamentId]);

  function updateFilter(key, value) {
    setFormFilters((current) => ({ ...current, [key]: value }));
  }

  function applyFilters(event) {
    event.preventDefault();
    setFilters({ ...formFilters, page: 0 });
  }

  function clearFilters() {
    const cleared = initialFilters(tournamentId);
    setFormFilters(cleared);
    setFilters(cleared);
  }

  function movePage(nextPage) {
    setFilters((current) => ({ ...current, page: nextPage }));
  }

  const logs = response?.content || [];
  const currentPage = response?.page || 0;
  const totalPages = response?.totalPages || 0;

  return (
    <main className="page-shell audit-shell">
      <div className="judge-heading">
        <div><p className="eyebrow">AUDIT LOG DESK</p><h2>관리자 감사 로그</h2><p>로그인 계정: {session.account.name} · 대회 ID {tournamentId}</p></div>
        <button className="secondary-button" onClick={onLogout} type="button">로그아웃</button>
      </div>
      <FilterForm filters={formFilters} onChange={updateFilter} onClear={clearFilters} onSubmit={applyFilters} />
      {loading ? <StatePanel title="감사 로그를 불러오는 중입니다.">잠시만 기다려 주세요.</StatePanel> : null}
      {error && !loading ? <StatePanel action={<button className="command-button" onClick={loadLogs} type="button">다시 시도</button>} title="감사 로그를 불러오지 못했습니다." tone="error">대회 ID와 운영자 권한을 확인한 뒤 다시 시도해 주세요.</StatePanel> : null}
      {!loading && !error ? (
        <section className="operation-panel audit-results-panel">
          <div className="operation-panel-heading"><div><p className="eyebrow">RESULTS</p><h3>변경 기록</h3></div><span>{response?.totalElements || 0}건</span></div>
          {logs.length ? <div aria-label="감사 로그 목록" className="audit-log-table" role="table"><div className="audit-log-header" role="row"><span>일시</span><span>행위자</span><span>행위·대상</span><span>결과</span><span>상세</span></div>{logs.map((log) => <AuditLogRow key={log.id} log={log} />)}</div> : <p className="operation-empty audit-empty">조건에 맞는 감사 로그가 없습니다.</p>}
          {totalPages > 1 ? <div className="audit-pagination"><button className="secondary-button" disabled={currentPage <= 0 || loading} onClick={() => movePage(currentPage - 1)} type="button">이전</button><span>{currentPage + 1} / {totalPages}</span><button className="secondary-button" disabled={currentPage >= totalPages - 1 || loading} onClick={() => movePage(currentPage + 1)} type="button">다음</button></div> : null}
        </section>
      ) : null}
    </main>
  );
}

export function AuditLogPage({ tournamentId }) {
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

  return session ? <AuditLogWorkspace onLogout={handleLogout} session={session} tournamentId={tournamentId} /> : <LoginForm onLogin={handleLogin} />;
}
