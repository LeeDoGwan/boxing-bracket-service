import { useCallback, useEffect, useState } from 'react';
import { login, logout } from '../api/auth';
import { createAthlete, deleteAthlete, getAthletes, updateAthlete } from '../api/adminAthletes';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { StatePanel } from '../components/StatePanel';

const SESSION_KEY = 'boxing.operations.session';
const ALLOWED_ROLES = new Set(['GAME_MANAGER', 'SERVICE_MANAGER']);

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
  return { affiliation: '', name: '' };
}

function formFromAthlete(athlete) {
  return { affiliation: athlete.affiliation || '', name: athlete.name || '' };
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
        <p className="eyebrow">ATHLETE ADMIN DESK</p>
        <h2>선수 관리 로그인</h2>
        <p>선수 정보를 관리하려면 운영자 계정으로 로그인하세요.</p>
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

function AthleteWorkspace({ onLogout, session }) {
  const [athletes, setAthletes] = useState([]);
  const [searchInput, setSearchInput] = useState('');
  const [keyword, setKeyword] = useState('');
  const [selectedId, setSelectedId] = useState(null);
  const [form, setForm] = useState(blankForm);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [listError, setListError] = useState('');
  const [actionError, setActionError] = useState('');
  const [message, setMessage] = useState('');
  const [confirmingDelete, setConfirmingDelete] = useState(false);

  const loadAthletes = useCallback(async () => {
    setLoading(true);
    setListError('');
    setActionError('');
    try {
      const nextAthletes = (await getAthletes(keyword, session.accessToken)) || [];
      setAthletes(nextAthletes);
      setSelectedId((current) => current && nextAthletes.some((item) => item.athleteId === current)
        ? current
        : nextAthletes[0]?.athleteId || null);
    } catch {
      setListError('선수 목록을 불러오지 못했습니다. 권한을 확인해 주세요.');
    } finally {
      setLoading(false);
    }
  }, [keyword, session.accessToken]);

  useEffect(() => {
    loadAthletes();
  }, [loadAthletes]);

  useEffect(() => {
    const selected = athletes.find((athlete) => athlete.athleteId === selectedId);
    setForm(selected ? formFromAthlete(selected) : blankForm());
  }, [athletes, selectedId]);

  function updateField(key, value) {
    setForm((current) => ({ ...current, [key]: value }));
    setMessage('');
  }

  function handleSearch(event) {
    event.preventDefault();
    setKeyword(searchInput.trim());
    setSelectedId(null);
  }

  function clearSearch() {
    setSearchInput('');
    setKeyword('');
    setSelectedId(null);
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setSaving(true);
    setActionError('');
    setMessage('');
    const payload = { ...form, affiliation: form.affiliation || null };
    try {
      if (selectedId) {
        const updated = await updateAthlete(selectedId, payload, session.accessToken);
        setAthletes((current) => current.map((athlete) => athlete.athleteId === selectedId ? updated : athlete));
        setMessage('선수 정보를 수정했습니다.');
      } else {
        const created = await createAthlete(payload, session.accessToken);
        setAthletes((current) => [...current, created]);
        setSelectedId(created.athleteId);
        setMessage('선수를 생성했습니다.');
      }
    } catch (requestError) {
      setActionError(requestError.message || '선수 저장에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete() {
    if (!selectedId) {
      return;
    }
    setSaving(true);
    setActionError('');
    setMessage('');
    try {
      await deleteAthlete(selectedId, session.accessToken);
      const remaining = athletes.filter((athlete) => athlete.athleteId !== selectedId);
      setAthletes(remaining);
      setSelectedId(remaining[0]?.athleteId || null);
      setMessage('선수를 삭제했습니다.');
      setConfirmingDelete(false);
    } catch (requestError) {
      setActionError(requestError.message || '선수 삭제에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <main className="page-shell admin-shell">
      <div className="judge-heading">
        <div><p className="eyebrow">ATHLETE ADMIN DESK</p><h2>선수 관리</h2><p>로그인 계정: {session.account.name}</p></div>
        <div className="operations-heading-actions"><button className="command-button" disabled={loading} onClick={loadAthletes} type="button">새로고침</button><button className="secondary-button" onClick={onLogout} type="button">로그아웃</button></div>
      </div>
      {actionError && <p aria-live="polite" className="form-error admin-action-message" role="alert">{actionError}</p>}
      {message && <p aria-live="polite" className="admin-success-message">{message}</p>}
      {loading ? <StatePanel title="선수 목록을 불러오는 중입니다.">잠시만 기다려 주세요.</StatePanel> : null}
      {listError && !loading ? <StatePanel action={<button className="command-button" onClick={loadAthletes} type="button">다시 시도</button>} title="선수 목록을 불러오지 못했습니다." tone="error">운영자 권한을 확인한 뒤 다시 시도해 주세요.</StatePanel> : null}
      {!loading && !listError ? (
        <div className="admin-setup-layout">
          <aside className="admin-list-panel">
            <div className="operation-panel-heading"><div><p className="eyebrow">ATHLETES</p><h3>선수 목록</h3></div><span>{athletes.length}건</span></div>
            <form className="athlete-search" onSubmit={handleSearch}><input aria-label="선수 검색" onChange={(event) => setSearchInput(event.target.value)} placeholder="이름 또는 소속" value={searchInput} /><button className="command-button" type="submit">검색</button></form>
            {keyword ? <button className="secondary-button athlete-clear-search" onClick={clearSearch} type="button">검색 초기화: {keyword}</button> : null}
            <button className={`admin-new-option${selectedId === null ? ' selected' : ''}`} onClick={() => setSelectedId(null)} type="button">+ 새 선수</button>
            <div className="admin-entity-list">{athletes.map((athlete) => <button className={`admin-entity-option${selectedId === athlete.athleteId ? ' selected' : ''}`} key={athlete.athleteId} onClick={() => setSelectedId(athlete.athleteId)} type="button"><strong>{athlete.name}</strong><span>{athlete.affiliation || '소속 미정'} · #{athlete.athleteId}</span></button>)}</div>
            {!athletes.length ? <p className="operation-empty">조건에 맞는 선수가 없습니다.</p> : null}
          </aside>
          <section className="admin-form-panel">
            <div className="operation-panel-heading"><div><p className="eyebrow">{selectedId ? `ATHLETE ${selectedId}` : 'NEW ATHLETE'}</p><h3>{selectedId ? '선수 정보 수정' : '선수 생성'}</h3></div>{selectedId ? <span>#{selectedId}</span> : null}</div>
            <form onSubmit={handleSubmit}>
              <div className="admin-form-grid"><label>선수명<input onChange={(event) => updateField('name', event.target.value)} required value={form.name} /></label><label>소속<input onChange={(event) => updateField('affiliation', event.target.value)} value={form.affiliation} /></label></div>
              <div className="admin-form-actions"><button className="command-button" disabled={saving} type="submit">{selectedId ? '선수 저장' : '선수 생성'}</button>{selectedId ? <button className="danger-button" disabled={saving} onClick={() => setConfirmingDelete(true)} type="button">선수 삭제</button> : null}</div>
            </form>
          </section>
        </div>
      ) : null}
      {confirmingDelete ? <ConfirmDialog busy={saving} description="선택한 선수를 삭제합니다. 연결된 경기 기록이 있으면 서버에서 거절될 수 있습니다." onCancel={() => setConfirmingDelete(false)} onConfirm={handleDelete} title="선수 삭제 확인" /> : null}
    </main>
  );
}

export function AdminAthletePage() {
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

  return session ? <AthleteWorkspace onLogout={handleLogout} session={session} /> : <LoginForm onLogin={handleLogin} />;
}
