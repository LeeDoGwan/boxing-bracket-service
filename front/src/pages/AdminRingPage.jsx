import { useCallback, useEffect, useState } from 'react';
import { login, logout } from '../api/auth';
import { createRing, deleteRing, getRings, updateRing } from '../api/adminRings';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { StatePanel } from '../components/StatePanel';

const SESSION_KEY = 'boxing.operations.session';
const ALLOWED_ROLES = new Set(['GAME_MANAGER', 'SERVICE_MANAGER']);
const STATUS_LABELS = { CLOSED: '운영 종료', IN_PROGRESS: '진행 중', READY: '준비' };

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
  return { name: '', status: 'READY' };
}

function formFromRing(ring) {
  return { name: ring.name || '', status: ring.status || 'READY' };
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
        <p className="eyebrow">RING ADMIN DESK</p>
        <h2>링 관리 로그인</h2>
        <p>대회 링 정보를 관리하려면 운영자 계정으로 로그인하세요.</p>
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

function RingWorkspace({ onLogout, session, tournamentId }) {
  const [rings, setRings] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [form, setForm] = useState(blankForm);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [listError, setListError] = useState('');
  const [actionError, setActionError] = useState('');
  const [message, setMessage] = useState('');
  const [confirmingDelete, setConfirmingDelete] = useState(false);

  const loadRings = useCallback(async () => {
    setLoading(true);
    setListError('');
    setActionError('');
    try {
      const nextRings = (await getRings(tournamentId, session.accessToken)) || [];
      setRings(nextRings);
      setSelectedId((current) => current && nextRings.some((item) => item.ringId === current)
        ? current
        : nextRings[0]?.ringId || null);
    } catch {
      setListError('링 목록을 불러오지 못했습니다. 대회 ID와 권한을 확인해 주세요.');
    } finally {
      setLoading(false);
    }
  }, [session.accessToken, tournamentId]);

  useEffect(() => {
    loadRings();
  }, [loadRings]);

  useEffect(() => {
    const selected = rings.find((ring) => ring.ringId === selectedId);
    setForm(selected ? formFromRing(selected) : blankForm());
  }, [rings, selectedId]);

  function updateField(key, value) {
    setForm((current) => ({ ...current, [key]: value }));
    setMessage('');
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setSaving(true);
    setActionError('');
    setMessage('');
    const payload = { ...form, tournamentId: Number(tournamentId) };
    try {
      if (selectedId) {
        const updated = await updateRing(selectedId, payload, session.accessToken);
        setRings((current) => current.map((ring) => ring.ringId === selectedId ? updated : ring));
        setMessage('링 정보를 수정했습니다.');
      } else {
        const created = await createRing(payload, session.accessToken);
        setRings((current) => [...current, created]);
        setSelectedId(created.ringId);
        setMessage('링을 생성했습니다.');
      }
    } catch (requestError) {
      setActionError(requestError.message || '링 저장에 실패했습니다.');
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
      await deleteRing(selectedId, session.accessToken);
      const remaining = rings.filter((ring) => ring.ringId !== selectedId);
      setRings(remaining);
      setSelectedId(remaining[0]?.ringId || null);
      setMessage('링을 삭제했습니다.');
      setConfirmingDelete(false);
    } catch (requestError) {
      setActionError(requestError.message || '링 삭제에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <main className="page-shell admin-shell">
      <div className="judge-heading">
        <div><p className="eyebrow">RING ADMIN DESK</p><h2>링 관리</h2><p>로그인 계정: {session.account.name} · 대회 ID {tournamentId}</p></div>
        <div className="operations-heading-actions"><button className="command-button" disabled={loading} onClick={loadRings} type="button">새로고침</button><button className="secondary-button" onClick={onLogout} type="button">로그아웃</button></div>
      </div>
      {actionError && <p aria-live="polite" className="form-error admin-action-message" role="alert">{actionError}</p>}
      {message && <p aria-live="polite" className="admin-success-message">{message}</p>}
      {loading ? <StatePanel title="링 목록을 불러오는 중입니다.">잠시만 기다려 주세요.</StatePanel> : null}
      {listError && !loading ? <StatePanel action={<button className="command-button" onClick={loadRings} type="button">다시 시도</button>} title="링 목록을 불러오지 못했습니다." tone="error">대회 ID와 운영자 권한을 확인한 뒤 다시 시도해 주세요.</StatePanel> : null}
      {!loading && !listError ? (
        <div className="admin-setup-layout">
          <aside className="admin-list-panel">
            <div className="operation-panel-heading"><div><p className="eyebrow">RINGS</p><h3>링 목록</h3></div><span>{rings.length}건</span></div>
            <button className={`admin-new-option${selectedId === null ? ' selected' : ''}`} onClick={() => setSelectedId(null)} type="button">+ 새 링</button>
            <div className="admin-entity-list">{rings.map((ring) => <button className={`admin-entity-option${selectedId === ring.ringId ? ' selected' : ''}`} key={ring.ringId} onClick={() => setSelectedId(ring.ringId)} type="button"><strong>{ring.name}</strong><span>{statusLabel(ring.status)} · 현재 경기 {ring.currentBoutId || '없음'}</span></button>)}</div>
            {!rings.length ? <p className="operation-empty">등록된 링이 없습니다.</p> : null}
          </aside>
          <section className="admin-form-panel">
            <div className="operation-panel-heading"><div><p className="eyebrow">{selectedId ? `RING ${selectedId}` : 'NEW RING'}</p><h3>{selectedId ? '링 정보 수정' : '링 생성'}</h3></div>{selectedId ? <span>#{selectedId}</span> : null}</div>
            <form onSubmit={handleSubmit}>
              <div className="admin-form-grid"><label>링 이름<input onChange={(event) => updateField('name', event.target.value)} required value={form.name} /></label><label>상태<select onChange={(event) => updateField('status', event.target.value)} value={form.status}>{Object.entries(STATUS_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label></div>
              <div className="admin-form-actions"><button className="command-button" disabled={saving} type="submit">{selectedId ? '링 저장' : '링 생성'}</button>{selectedId ? <button className="danger-button" disabled={saving} onClick={() => setConfirmingDelete(true)} type="button">링 삭제</button> : null}</div>
            </form>
          </section>
        </div>
      ) : null}
      {confirmingDelete ? <ConfirmDialog busy={saving} description="선택한 링을 삭제합니다. 연결된 경기나 운영 데이터가 있으면 서버에서 거절될 수 있습니다." onCancel={() => setConfirmingDelete(false)} onConfirm={handleDelete} title="링 삭제 확인" /> : null}
    </main>
  );
}

export function AdminRingPage({ tournamentId }) {
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

  return session ? <RingWorkspace onLogout={handleLogout} session={session} tournamentId={tournamentId} /> : <LoginForm onLogin={handleLogin} />;
}
