import { useCallback, useEffect, useState } from 'react';
import { login, logout } from '../api/auth';
import { createAccount, deleteAccount, getAccounts, updateAccount } from '../api/adminAccounts';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { StatePanel } from '../components/StatePanel';

const SESSION_KEY = 'boxing.operations.session';
const ROLE_LABELS = { GAME_MANAGER: '게임 매니저', JUDGE: '심판', RING_MANAGER: '링 운영자', SERVICE_MANAGER: '서비스 매니저', SUPERVISOR: '감독' };
const STATUS_LABELS = { ACTIVE: '활성', INACTIVE: '비활성' };

function readSession() {
  try {
    const stored = window.sessionStorage.getItem(SESSION_KEY);
    const session = stored ? JSON.parse(stored) : null;
    return session?.accessToken && session?.account?.role === 'SERVICE_MANAGER' ? session : null;
  } catch {
    return null;
  }
}

function blankForm() {
  return { loginId: '', name: '', passwordHash: '', role: 'JUDGE', status: 'ACTIVE' };
}

function blankFilters() {
  return { keyword: '', role: '', status: '' };
}

function formFromAccount(account) {
  return { loginId: account.loginId || '', name: account.name || '', passwordHash: '', role: account.role || 'JUDGE', status: account.status || 'ACTIVE' };
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
      if (session.account?.role !== 'SERVICE_MANAGER') {
        throw new Error('SERVICE_MANAGER_ONLY');
      }
      onLogin(session);
    } catch (requestError) {
      setError(requestError.message === 'SERVICE_MANAGER_ONLY'
        ? '서비스 매니저 계정으로 로그인해 주세요.'
        : '로그인 정보를 확인해 주세요.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="page-shell auth-shell">
      <section className="auth-panel">
        <p className="eyebrow">ACCOUNT ADMIN DESK</p>
        <h2>계정 관리 로그인</h2>
        <p>운영 계정을 관리하려면 서비스 매니저로 로그인하세요.</p>
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

function AccountWorkspace({ onLogout, session }) {
  const [accounts, setAccounts] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [form, setForm] = useState(blankForm);
  const [filterForm, setFilterForm] = useState(blankFilters);
  const [filters, setFilters] = useState(blankFilters);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [listError, setListError] = useState('');
  const [actionError, setActionError] = useState('');
  const [message, setMessage] = useState('');
  const [confirmingDelete, setConfirmingDelete] = useState(false);

  const loadAccounts = useCallback(async () => {
    setLoading(true);
    setListError('');
    setActionError('');
    try {
      const nextAccounts = (await getAccounts(filters, session.accessToken)) || [];
      setAccounts(nextAccounts);
      setSelectedId((current) => current && nextAccounts.some((item) => item.accountId === current)
        ? current
        : nextAccounts[0]?.accountId || null);
    } catch {
      setListError('계정 목록을 불러오지 못했습니다. 서비스 매니저 권한을 확인해 주세요.');
    } finally {
      setLoading(false);
    }
  }, [filters, session.accessToken]);

  useEffect(() => {
    loadAccounts();
  }, [loadAccounts]);

  useEffect(() => {
    const selected = accounts.find((account) => account.accountId === selectedId);
    setForm(selected ? formFromAccount(selected) : blankForm());
  }, [accounts, selectedId]);

  function updateField(key, value) {
    setForm((current) => ({ ...current, [key]: value }));
    setMessage('');
  }

  function handleFilterSubmit(event) {
    event.preventDefault();
    setFilters({ ...filterForm, keyword: filterForm.keyword.trim() });
  }

  function handleFilterReset() {
    const nextFilters = blankFilters();
    setFilterForm(nextFilters);
    setFilters(nextFilters);
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setSaving(true);
    setActionError('');
    setMessage('');
    try {
      if (selectedId) {
        const updated = await updateAccount(selectedId, form, session.accessToken);
        setAccounts((current) => current.map((account) => account.accountId === selectedId ? updated : account));
        setMessage('계정 정보를 수정했습니다.');
      } else {
        const created = await createAccount(form, session.accessToken);
        setAccounts((current) => [...current, created]);
        setSelectedId(created.accountId);
        setMessage('계정을 생성했습니다.');
      }
    } catch (requestError) {
      setActionError(requestError.message || '계정 저장에 실패했습니다.');
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
      await deleteAccount(selectedId, session.accessToken);
      const remaining = accounts.filter((account) => account.accountId !== selectedId);
      setAccounts(remaining);
      setSelectedId(remaining[0]?.accountId || null);
      setMessage('계정을 삭제했습니다.');
      setConfirmingDelete(false);
    } catch (requestError) {
      setActionError(requestError.message || '계정 삭제에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <main className="page-shell admin-shell">
      <div className="judge-heading">
        <div><p className="eyebrow">ACCOUNT ADMIN DESK</p><h2>계정 관리</h2><p>서비스 매니저: {session.account.name}</p></div>
        <div className="operations-heading-actions"><button className="command-button" disabled={loading} onClick={loadAccounts} type="button">새로고침</button><button className="secondary-button" onClick={onLogout} type="button">로그아웃</button></div>
      </div>
      {actionError && <p aria-live="polite" className="form-error admin-action-message" role="alert">{actionError}</p>}
      {message && <p aria-live="polite" className="admin-success-message">{message}</p>}
      {loading ? <StatePanel title="계정 목록을 불러오는 중입니다.">잠시만 기다려 주세요.</StatePanel> : null}
      {listError && !loading ? <StatePanel action={<button className="command-button" onClick={loadAccounts} type="button">다시 시도</button>} title="계정 목록을 불러오지 못했습니다." tone="error">서비스 매니저 권한을 확인한 뒤 다시 시도해 주세요.</StatePanel> : null}
      {!loading && !listError ? (
        <div className="admin-setup-layout">
          <aside className="admin-list-panel">
            <form className="account-search" onSubmit={handleFilterSubmit}>
              <label>Search<input aria-label="Account search" onChange={(event) => setFilterForm((current) => ({ ...current, keyword: event.target.value }))} placeholder="Login ID or name" value={filterForm.keyword} /></label>
              <label>Role<select aria-label="Role filter" onChange={(event) => setFilterForm((current) => ({ ...current, role: event.target.value }))} value={filterForm.role}><option value="">All roles</option>{Object.entries(ROLE_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
              <label>Status<select aria-label="Status filter" onChange={(event) => setFilterForm((current) => ({ ...current, status: event.target.value }))} value={filterForm.status}><option value="">All statuses</option>{Object.entries(STATUS_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
              <div className="account-search-actions"><button className="command-button" type="submit">Filter</button><button className="secondary-button" onClick={handleFilterReset} type="button">Reset</button></div>
            </form>
            <div className="operation-panel-heading"><div><p className="eyebrow">ACCOUNTS</p><h3>계정 목록</h3></div><span>{accounts.length}건</span></div>
            <button className={`admin-new-option${selectedId === null ? ' selected' : ''}`} onClick={() => setSelectedId(null)} type="button">+ 새 계정</button>
            <div className="admin-entity-list">{accounts.map((account) => <button className={`admin-entity-option${selectedId === account.accountId ? ' selected' : ''}`} key={account.accountId} onClick={() => setSelectedId(account.accountId)} type="button"><strong>{account.loginId}</strong><span>{account.name} · {ROLE_LABELS[account.role] || account.role} · {STATUS_LABELS[account.status] || account.status}</span></button>)}</div>
            {!accounts.length ? <p className="operation-empty">등록된 계정이 없습니다.</p> : null}
          </aside>
          <section className="admin-form-panel">
            <div className="operation-panel-heading"><div><p className="eyebrow">{selectedId ? `ACCOUNT ${selectedId}` : 'NEW ACCOUNT'}</p><h3>{selectedId ? '계정 정보 수정' : '계정 생성'}</h3></div>{selectedId ? <span>#{selectedId}</span> : null}</div>
            <form onSubmit={handleSubmit}><div className="admin-form-grid"><label>로그인 ID<input autoComplete="username" onChange={(event) => updateField('loginId', event.target.value)} required value={form.loginId} /></label><label>이름<input onChange={(event) => updateField('name', event.target.value)} required value={form.name} /></label><label>비밀번호<input autoComplete="new-password" onChange={(event) => updateField('passwordHash', event.target.value)} required type="password" value={form.passwordHash} /></label><label>역할<select onChange={(event) => updateField('role', event.target.value)} value={form.role}>{Object.entries(ROLE_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label><label>상태<select onChange={(event) => updateField('status', event.target.value)} value={form.status}>{Object.entries(STATUS_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label></div><div className="admin-form-actions"><button className="command-button" disabled={saving} type="submit">{selectedId ? '계정 저장' : '계정 생성'}</button>{selectedId ? <button className="danger-button" disabled={saving} onClick={() => setConfirmingDelete(true)} type="button">계정 삭제</button> : null}</div></form>
          </section>
        </div>
      ) : null}
      {confirmingDelete ? <ConfirmDialog busy={saving} description="선택한 계정을 삭제합니다. 이 작업은 되돌릴 수 없습니다." onCancel={() => setConfirmingDelete(false)} onConfirm={handleDelete} title="계정 삭제 확인" /> : null}
    </main>
  );
}

export function AdminAccountPage() {
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

  return session ? <AccountWorkspace onLogout={handleLogout} session={session} /> : <LoginForm onLogin={handleLogin} />;
}
