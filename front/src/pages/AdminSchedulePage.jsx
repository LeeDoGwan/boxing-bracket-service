import { useCallback, useEffect, useState } from 'react';
import { login, logout } from '../api/auth';
import { createSchedule, deleteSchedule, getSchedules, updateSchedule } from '../api/adminSchedules';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { StatePanel } from '../components/StatePanel';

const SESSION_KEY = 'boxing.operations.session';
const ALLOWED_ROLES = new Set(['GAME_MANAGER', 'SERVICE_MANAGER']);
const TYPE_OPTIONS = [
  ['BOUT', '경기'],
  ['BREAK', '휴식'],
  ['LUNCH', '점심'],
  ['PERFORMANCE', '공연'],
  ['EVENT', '행사'],
];
const STATUS_OPTIONS = [
  ['SCHEDULED', '예정'],
  ['IN_PROGRESS', '진행 중'],
  ['COMPLETED', '종료'],
];

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
  return { endTime: '', relatedBoutId: '', ringId: '', startTime: '', status: 'SCHEDULED', title: '', type: 'EVENT' };
}

function formFromSchedule(schedule) {
  return {
    endTime: schedule.endTime || '',
    relatedBoutId: schedule.relatedBoutId ? String(schedule.relatedBoutId) : '',
    ringId: schedule.ringId ? String(schedule.ringId) : '',
    startTime: schedule.startTime || '',
    status: schedule.status || 'SCHEDULED',
    title: schedule.title || '',
    type: schedule.type || 'EVENT',
  };
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
        <p className="eyebrow">SCHEDULE ADMIN DESK</p>
        <h2>일정 관리 로그인</h2>
        <p>경기와 휴식·행사 일정을 관리하려면 운영자 계정으로 로그인하세요.</p>
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

function ScheduleWorkspace({ onLogout, session, tournamentId }) {
  const [schedules, setSchedules] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [form, setForm] = useState(blankForm);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [listError, setListError] = useState('');
  const [actionError, setActionError] = useState('');
  const [message, setMessage] = useState('');
  const [confirmingDelete, setConfirmingDelete] = useState(false);

  const loadSchedules = useCallback(async () => {
    setLoading(true);
    setListError('');
    setActionError('');
    try {
      const nextSchedules = (await getSchedules(tournamentId, session.accessToken)) || [];
      setSchedules(nextSchedules);
      setSelectedId((current) => current && nextSchedules.some((item) => item.scheduleId === current)
        ? current
        : nextSchedules[0]?.scheduleId || null);
    } catch {
      setListError('일정 목록을 불러오지 못했습니다. 대회 ID와 권한을 확인해 주세요.');
    } finally {
      setLoading(false);
    }
  }, [session.accessToken, tournamentId]);

  useEffect(() => {
    loadSchedules();
  }, [loadSchedules]);

  useEffect(() => {
    const selected = schedules.find((schedule) => schedule.scheduleId === selectedId);
    setForm(selected ? formFromSchedule(selected) : blankForm());
  }, [schedules, selectedId]);

  function updateField(key, value) {
    setForm((current) => ({ ...current, [key]: value }));
    setMessage('');
  }

  function toOptionalId(value) {
    const parsed = Number.parseInt(value, 10);
    return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setSaving(true);
    setActionError('');
    setMessage('');
    const payload = {
      endTime: form.endTime || null,
      relatedBoutId: toOptionalId(form.relatedBoutId),
      ringId: toOptionalId(form.ringId),
      startTime: form.startTime,
      status: form.status,
      title: form.title,
      tournamentId: Number(tournamentId),
      type: form.type,
    };
    try {
      if (selectedId) {
        const updated = await updateSchedule(selectedId, payload, session.accessToken);
        setSchedules((current) => current.map((schedule) => schedule.scheduleId === selectedId ? updated : schedule));
        setMessage('일정을 수정했습니다.');
      } else {
        const created = await createSchedule(payload, session.accessToken);
        setSchedules((current) => [...current, created].sort((left, right) => left.startTime.localeCompare(right.startTime)));
        setSelectedId(created.scheduleId);
        setMessage('일정을 생성했습니다.');
      }
    } catch (requestError) {
      setActionError(requestError.message || '일정 저장에 실패했습니다.');
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
      await deleteSchedule(selectedId, session.accessToken);
      const remaining = schedules.filter((schedule) => schedule.scheduleId !== selectedId);
      setSchedules(remaining);
      setSelectedId(remaining[0]?.scheduleId || null);
      setMessage('일정을 삭제했습니다.');
      setConfirmingDelete(false);
    } catch (requestError) {
      setActionError(requestError.message || '일정 삭제에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <main className="page-shell admin-shell">
      <div className="judge-heading">
        <div><p className="eyebrow">SCHEDULE ADMIN DESK</p><h2>일정 관리</h2><p>로그인 계정: {session.account.name} · 대회 ID {tournamentId}</p></div>
        <div className="operations-heading-actions"><button className="command-button" disabled={loading} onClick={loadSchedules} type="button">새로고침</button><button className="secondary-button" onClick={onLogout} type="button">로그아웃</button></div>
      </div>
      {actionError && <p aria-live="polite" className="form-error admin-action-message" role="alert">{actionError}</p>}
      {message && <p aria-live="polite" className="admin-success-message">{message}</p>}
      {loading ? <StatePanel title="일정 목록을 불러오는 중입니다.">잠시만 기다려 주세요.</StatePanel> : null}
      {listError && !loading ? <StatePanel action={<button className="command-button" onClick={loadSchedules} type="button">다시 시도</button>} title="일정 목록을 불러오지 못했습니다." tone="error">대회 ID와 운영자 권한을 확인한 뒤 다시 시도해 주세요.</StatePanel> : null}
      {!loading && !listError ? (
        <div className="admin-setup-layout">
          <aside className="admin-list-panel">
            <div className="operation-panel-heading"><div><p className="eyebrow">SCHEDULES</p><h3>일정 목록</h3></div><span>{schedules.length}건</span></div>
            <button className={`admin-new-option${selectedId === null ? ' selected' : ''}`} onClick={() => setSelectedId(null)} type="button">+ 새 일정</button>
            <div className="admin-entity-list">{schedules.map((schedule) => <button className={`admin-entity-option${selectedId === schedule.scheduleId ? ' selected' : ''}`} key={schedule.scheduleId} onClick={() => setSelectedId(schedule.scheduleId)} type="button"><strong>{schedule.title}</strong><span>{schedule.type} · {schedule.startTime.replace('T', ' ')} · {schedule.status}</span></button>)}</div>
            {!schedules.length ? <p className="operation-empty">등록된 일정이 없습니다.</p> : null}
          </aside>
          <section className="admin-form-panel">
            <div className="operation-panel-heading"><div><p className="eyebrow">{selectedId ? `SCHEDULE ${selectedId}` : 'NEW SCHEDULE'}</p><h3>{selectedId ? '일정 정보 수정' : '일정 생성'}</h3></div>{selectedId ? <span>#{selectedId}</span> : null}</div>
            <form onSubmit={handleSubmit}>
              <div className="admin-form-grid">
                <label>일정 유형<select onChange={(event) => updateField('type', event.target.value)} value={form.type}>{TYPE_OPTIONS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
                <label>상태<select onChange={(event) => updateField('status', event.target.value)} value={form.status}>{STATUS_OPTIONS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
                <label className="admin-wide-field">제목<input onChange={(event) => updateField('title', event.target.value)} required value={form.title} /></label>
                <label>시작 시간<input onChange={(event) => updateField('startTime', event.target.value)} required type="datetime-local" value={form.startTime} /></label>
                <label>종료 시간<input onChange={(event) => updateField('endTime', event.target.value)} type="datetime-local" value={form.endTime} /></label>
                <label>링 ID<input min="1" onChange={(event) => updateField('ringId', event.target.value)} type="number" value={form.ringId} /></label>
                <label>연결 경기 ID<input min="1" onChange={(event) => updateField('relatedBoutId', event.target.value)} type="number" value={form.relatedBoutId} /></label>
              </div>
              <div className="admin-form-actions"><button className="command-button" disabled={saving} type="submit">{selectedId ? '일정 저장' : '일정 생성'}</button>{selectedId ? <button className="danger-button" disabled={saving} onClick={() => setConfirmingDelete(true)} type="button">일정 삭제</button> : null}</div>
            </form>
          </section>
        </div>
      ) : null}
      {confirmingDelete ? <ConfirmDialog busy={saving} description="선택한 일정을 삭제합니다. 이 작업은 되돌릴 수 없습니다." onCancel={() => setConfirmingDelete(false)} onConfirm={handleDelete} title="일정 삭제 확인" /> : null}
    </main>
  );
}

export function AdminSchedulePage({ tournamentId }) {
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

  return session ? <ScheduleWorkspace onLogout={handleLogout} session={session} tournamentId={tournamentId} /> : <LoginForm onLogin={handleLogin} />;
}
