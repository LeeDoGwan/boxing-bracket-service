import { useCallback, useEffect, useState } from 'react';
import { login, logout } from '../api/auth';
import { createTournament, deleteTournament, getTournaments, updateTournament } from '../api/adminTournaments';
import { StatePanel } from '../components/StatePanel';

const SESSION_KEY = 'boxing.operations.session';
const ALLOWED_ROLES = new Set(['GAME_MANAGER', 'SERVICE_MANAGER']);
const STATUS_LABELS = { FINISHED: '종료', IN_PROGRESS: '진행 중', READY: '준비' };

function readSession() {
  try {
    const stored = window.sessionStorage.getItem(SESSION_KEY);
    const session = stored ? JSON.parse(stored) : null;
    return session?.accessToken && ALLOWED_ROLES.has(session?.account?.role) ? session : null;
  } catch {
    return null;
  }
}

function blankForm() {
  return { endDate: '', location: '', name: '', startDate: '', status: 'READY' };
}

function formFromTournament(tournament) {
  return {
    endDate: tournament.endDate || '',
    location: tournament.location || '',
    name: tournament.name || '',
    startDate: tournament.startDate || '',
    status: tournament.status || 'READY',
  };
}

function statusLabel(status) {
  return STATUS_LABELS[status] || status || '상태 미정';
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
        throw new Error('ADMIN_ONLY');
      }
      onLogin(session);
    } catch (requestError) {
      setError(requestError.message === 'ADMIN_ONLY'
        ? '게임 매니저 또는 서비스 매니저 계정으로 로그인해 주세요.'
        : '로그인 정보를 확인해 주세요.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="page-shell auth-shell">
      <section className="auth-panel">
        <p className="eyebrow">TOURNAMENT ADMIN DESK</p>
        <h2>대회 관리 로그인</h2>
        <p>대회 기본 정보를 관리하려면 운영자 계정으로 로그인하세요.</p>
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

function TournamentWorkspace({ onLogout, session }) {
  const [tournaments, setTournaments] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [form, setForm] = useState(blankForm);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const loadTournaments = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const nextTournaments = (await getTournaments(session.accessToken)) || [];
      setTournaments(nextTournaments);
      setSelectedId((current) => current && nextTournaments.some((item) => item.tournamentId === current)
        ? current
        : nextTournaments[0]?.tournamentId || null);
    } catch {
      setError('대회 목록을 불러오지 못했습니다. 권한을 확인해 주세요.');
    } finally {
      setLoading(false);
    }
  }, [session.accessToken]);

  useEffect(() => {
    loadTournaments();
  }, [loadTournaments]);

  useEffect(() => {
    const selected = tournaments.find((tournament) => tournament.tournamentId === selectedId);
    setForm(selected ? formFromTournament(selected) : blankForm());
  }, [selectedId, tournaments]);

  function updateField(key, value) {
    setForm((current) => ({ ...current, [key]: value }));
    setMessage('');
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setSaving(true);
    setError('');
    setMessage('');
    const payload = { ...form, endDate: form.endDate || null, startDate: form.startDate || null };
    try {
      if (selectedId) {
        const updated = await updateTournament(selectedId, payload, session.accessToken);
        setTournaments((current) => current.map((tournament) => tournament.tournamentId === selectedId ? updated : tournament));
        setMessage('대회 정보를 수정했습니다.');
      } else {
        const created = await createTournament(payload, session.accessToken);
        setTournaments((current) => [...current, created]);
        setSelectedId(created.tournamentId);
        setMessage('대회를 생성했습니다.');
      }
    } catch (requestError) {
      setError(requestError.message || '대회 저장에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete() {
    if (!selectedId) {
      return;
    }
    setSaving(true);
    setError('');
    setMessage('');
    try {
      await deleteTournament(selectedId, session.accessToken);
      const remaining = tournaments.filter((tournament) => tournament.tournamentId !== selectedId);
      setTournaments(remaining);
      setSelectedId(remaining[0]?.tournamentId || null);
      setMessage('대회를 삭제했습니다.');
    } catch (requestError) {
      setError(requestError.message || '대회 삭제에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <main className="page-shell admin-shell">
      <div className="judge-heading">
        <div><p className="eyebrow">TOURNAMENT ADMIN DESK</p><h2>대회 관리</h2><p>로그인 계정: {session.account.name}</p></div>
        <div className="operations-heading-actions"><button className="command-button" disabled={loading} onClick={loadTournaments} type="button">새로고침</button><button className="secondary-button" onClick={onLogout} type="button">로그아웃</button></div>
      </div>
      {error && <p aria-live="polite" className="form-error admin-action-message" role="alert">{error}</p>}
      {message && <p aria-live="polite" className="admin-success-message">{message}</p>}
      {loading ? <StatePanel title="대회 목록을 불러오는 중입니다.">잠시만 기다려 주세요.</StatePanel> : null}
      {!loading && !error ? (
        <div className="admin-setup-layout">
          <aside className="admin-list-panel">
            <div className="operation-panel-heading"><div><p className="eyebrow">TOURNAMENTS</p><h3>대회 목록</h3></div><span>{tournaments.length}건</span></div>
            <button className={`admin-new-option${selectedId === null ? ' selected' : ''}`} onClick={() => setSelectedId(null)} type="button">+ 새 대회</button>
            <div className="admin-entity-list">{tournaments.map((tournament) => <button className={`admin-entity-option${selectedId === tournament.tournamentId ? ' selected' : ''}`} key={tournament.tournamentId} onClick={() => setSelectedId(tournament.tournamentId)} type="button"><strong>{tournament.name}</strong><span>{statusLabel(tournament.status)} · {tournament.location || '장소 미정'}</span></button>)}</div>
            {!tournaments.length ? <p className="operation-empty">등록된 대회가 없습니다.</p> : null}
          </aside>
          <section className="admin-form-panel">
            <div className="operation-panel-heading"><div><p className="eyebrow">{selectedId ? `TOURNAMENT ${selectedId}` : 'NEW TOURNAMENT'}</p><h3>{selectedId ? '대회 정보 수정' : '대회 생성'}</h3></div>{selectedId ? <span>#{selectedId}</span> : null}</div>
            <form onSubmit={handleSubmit}>
              <div className="admin-form-grid">
                <label>대회명<input onChange={(event) => updateField('name', event.target.value)} required value={form.name} /></label>
                <label>장소<input onChange={(event) => updateField('location', event.target.value)} value={form.location} /></label>
                <label>시작일<input onChange={(event) => updateField('startDate', event.target.value)} type="date" value={form.startDate} /></label>
                <label>종료일<input onChange={(event) => updateField('endDate', event.target.value)} type="date" value={form.endDate} /></label>
                <label>상태<select onChange={(event) => updateField('status', event.target.value)} value={form.status}>{Object.entries(STATUS_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
              </div>
              <div className="admin-form-actions"><button className="command-button" disabled={saving} type="submit">{selectedId ? '대회 저장' : '대회 생성'}</button>{selectedId ? <button className="danger-button" disabled={saving} onClick={handleDelete} type="button">대회 삭제</button> : null}</div>
            </form>
          </section>
        </div>
      ) : null}
    </main>
  );
}

export function AdminTournamentPage() {
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

  return session ? <TournamentWorkspace onLogout={handleLogout} session={session} /> : <LoginForm onLogin={handleLogin} />;
}
