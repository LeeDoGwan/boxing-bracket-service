import { useCallback, useEffect, useState } from 'react';
import { login, logout } from '../api/auth';
import { createBout, deleteBout, getBouts, importBouts, updateBout } from '../api/adminBouts';
import { getAthletes } from '../api/adminAthletes';
import { getRings } from '../api/adminRings';
import { StatePanel } from '../components/StatePanel';

const SESSION_KEY = 'boxing.operations.session';
const ALLOWED_ROLES = new Set(['GAME_MANAGER', 'SERVICE_MANAGER']);
const STATUS_LABELS = { CANCELED: '취소', FINISHED: '종료', IN_PROGRESS: '진행 중', READY: '준비', SCHEDULED: '예정', SCORING: '채점 중' };
const CSV_HEADERS = 'tournamentId,ringId,boutNumber,matchType,redAthleteId,blueAthleteId,totalRounds,scheduledOrder,eventBout';

function readSession() {
  try {
    const stored = window.sessionStorage.getItem(SESSION_KEY);
    const session = stored ? JSON.parse(stored) : null;
    return session?.accessToken && ALLOWED_ROLES.has(session?.account?.role) ? session : null;
  } catch {
    return null;
  }
}

function blankForm(tournamentId) {
  return { blueAthleteId: '', boutNumber: '', eventBout: false, matchType: '', redAthleteId: '', ringId: '', scheduledOrder: '', totalRounds: '3', tournamentId: String(tournamentId) };
}

function formFromBout(bout) {
  return { blueAthleteId: String(bout.blueAthleteId || ''), boutNumber: String(bout.boutNumber || ''), eventBout: bout.eventBout, matchType: bout.matchType || '', redAthleteId: String(bout.redAthleteId || ''), ringId: String(bout.ringId || ''), scheduledOrder: String(bout.scheduledOrder || ''), totalRounds: String(bout.totalRounds || ''), tournamentId: String(bout.tournamentId) };
}

function statusLabel(status) {
  return STATUS_LABELS[status] || status || '상태 미정';
}

function toNumber(value) {
  const parsed = Number.parseInt(value, 10);
  return Number.isInteger(parsed) ? parsed : null;
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
        <p className="eyebrow">BOUT ADMIN DESK</p>
        <h2>대진 관리 로그인</h2>
        <p>경기 대진을 관리하려면 운영자 계정으로 로그인하세요.</p>
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

function BoutWorkspace({ onLogout, session, tournamentId }) {
  const [bouts, setBouts] = useState([]);
  const [rings, setRings] = useState([]);
  const [athletes, setAthletes] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [form, setForm] = useState(() => blankForm(tournamentId));
  const [file, setFile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [listError, setListError] = useState('');
  const [referenceError, setReferenceError] = useState('');
  const [actionError, setActionError] = useState('');
  const [message, setMessage] = useState('');

  const loadBouts = useCallback(async () => {
    setLoading(true);
    setListError('');
    setActionError('');
    try {
      const nextBouts = (await getBouts(tournamentId, session.accessToken)) || [];
      setBouts(nextBouts);
      setSelectedId((current) => current && nextBouts.some((item) => item.boutId === current)
        ? current
        : nextBouts[0]?.boutId || null);
    } catch {
      setListError('경기 목록을 불러오지 못했습니다. 대회 ID와 권한을 확인해 주세요.');
    } finally {
      setLoading(false);
    }
  }, [session.accessToken, tournamentId]);

  useEffect(() => {
    loadBouts();
  }, [loadBouts]);

  const loadReferences = useCallback(async () => {
    setReferenceError('');
    try {
      const [nextRings, nextAthletes] = await Promise.all([
        getRings(tournamentId, session.accessToken),
        getAthletes('', session.accessToken),
      ]);
      setRings(nextRings || []);
      setAthletes(nextAthletes || []);
    } catch {
      setReferenceError('링 또는 선수 목록을 불러오지 못했습니다. ID를 직접 확인해 주세요.');
    }
  }, [session.accessToken, tournamentId]);

  useEffect(() => {
    loadReferences();
  }, [loadReferences]);

  useEffect(() => {
    const selected = bouts.find((bout) => bout.boutId === selectedId);
    setForm(selected ? formFromBout(selected) : blankForm(tournamentId));
  }, [bouts, selectedId, tournamentId]);

  function updateField(key, value) {
    setForm((current) => ({ ...current, [key]: value }));
    setMessage('');
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setSaving(true);
    setActionError('');
    setMessage('');
    const payload = {
      blueAthleteId: toNumber(form.blueAthleteId),
      boutNumber: toNumber(form.boutNumber),
      eventBout: form.eventBout,
      matchType: form.matchType || null,
      redAthleteId: toNumber(form.redAthleteId),
      ringId: toNumber(form.ringId),
      scheduledOrder: toNumber(form.scheduledOrder),
      totalRounds: toNumber(form.totalRounds),
      tournamentId: Number(tournamentId),
    };
    try {
      if (selectedId) {
        const updated = await updateBout(selectedId, payload, session.accessToken);
        setBouts((current) => current.map((bout) => bout.boutId === selectedId ? updated : bout));
        setMessage('경기 정보를 수정했습니다.');
      } else {
        const created = await createBout(payload, session.accessToken);
        setBouts((current) => [...current, created].sort((left, right) => (left.scheduledOrder || 0) - (right.scheduledOrder || 0)));
        setSelectedId(created.boutId);
        setMessage('경기를 생성했습니다.');
      }
    } catch (requestError) {
      setActionError(requestError.message || '경기 저장에 실패했습니다.');
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
      await deleteBout(selectedId, session.accessToken);
      const remaining = bouts.filter((bout) => bout.boutId !== selectedId);
      setBouts(remaining);
      setSelectedId(remaining[0]?.boutId || null);
      setMessage('경기를 삭제했습니다.');
    } catch (requestError) {
      setActionError(requestError.message || '경기 삭제에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  }

  async function handleImport(event) {
    event.preventDefault();
    const formElement = event.currentTarget;
    if (!file) {
      setActionError('CSV 파일을 선택해 주세요.');
      return;
    }
    setSaving(true);
    setActionError('');
    setMessage('');
    try {
      const result = await importBouts(file, session.accessToken);
      await loadBouts();
      setFile(null);
      formElement.reset();
      setMessage(`${result.importedCount}건의 경기를 가져왔습니다.`);
    } catch (requestError) {
      setActionError(requestError.message || 'CSV 가져오기에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  }

  function downloadTemplate() {
    const blob = new Blob([`${CSV_HEADERS}\n`], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'boxing-bouts-template.csv';
    link.click();
    URL.revokeObjectURL(url);
  }

  return (
    <main className="page-shell admin-shell">
      <div className="judge-heading">
        <div><p className="eyebrow">BOUT ADMIN DESK</p><h2>대진 관리</h2><p>로그인 계정: {session.account.name} · 대회 ID {tournamentId}</p></div>
        <div className="operations-heading-actions"><button className="command-button" disabled={loading} onClick={loadBouts} type="button">새로고침</button><button className="secondary-button" onClick={onLogout} type="button">로그아웃</button></div>
      </div>
      {actionError && <p aria-live="polite" className="form-error admin-action-message" role="alert">{actionError}</p>}
      {referenceError && <p aria-live="polite" className="form-error admin-reference-message" role="alert">{referenceError}</p>}
      {message && <p aria-live="polite" className="admin-success-message">{message}</p>}
      <form className="bout-import-panel" onSubmit={handleImport}><label>대진 파일<input aria-label="대진 파일" accept=".csv,.xls,.xlsx,text/csv,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" onChange={(event) => setFile(event.target.files?.[0] || null)} type="file" /></label><div className="bout-import-actions"><button className="command-button" disabled={saving} type="submit">파일 가져오기</button><button className="secondary-button" onClick={downloadTemplate} type="button">CSV 양식 다운로드</button></div><small>필수 열: {CSV_HEADERS} · CSV, XLS, XLSX</small></form>
      {loading ? <StatePanel title="경기 목록을 불러오는 중입니다.">잠시만 기다려 주세요.</StatePanel> : null}
      {listError && !loading ? <StatePanel action={<button className="command-button" onClick={loadBouts} type="button">다시 시도</button>} title="경기 목록을 불러오지 못했습니다." tone="error">대회 ID와 운영자 권한을 확인한 뒤 다시 시도해 주세요.</StatePanel> : null}
      {!loading && !listError ? (
        <div className="admin-setup-layout">
          <aside className="admin-list-panel">
            <div className="operation-panel-heading"><div><p className="eyebrow">BOUTS</p><h3>경기 목록</h3></div><span>{bouts.length}건</span></div>
            <button className={`admin-new-option${selectedId === null ? ' selected' : ''}`} onClick={() => setSelectedId(null)} type="button">+ 새 경기</button>
            <div className="admin-entity-list">{bouts.map((bout) => <button className={`admin-entity-option${selectedId === bout.boutId ? ' selected' : ''}`} key={bout.boutId} onClick={() => setSelectedId(bout.boutId)} type="button"><strong>경기 {bout.boutNumber} · {bout.matchType || '일반'}</strong><span>링 {bout.ringId} · {statusLabel(bout.status)} · {bout.redAthleteId} vs {bout.blueAthleteId}</span></button>)}</div>
            {!bouts.length ? <p className="operation-empty">등록된 경기가 없습니다.</p> : null}
          </aside>
          <section className="admin-form-panel">
            <div className="operation-panel-heading"><div><p className="eyebrow">{selectedId ? `BOUT ${selectedId}` : 'NEW BOUT'}</p><h3>{selectedId ? '경기 정보 수정' : '경기 생성'}</h3></div>{selectedId ? <span>#{selectedId}</span> : null}</div>
            <form onSubmit={handleSubmit}>
              <div className="admin-form-grid"><label>링<select aria-label="링" onChange={(event) => updateField('ringId', event.target.value)} required value={form.ringId}><option value="">링을 선택하세요</option>{form.ringId && !rings.some((ring) => String(ring.ringId) === form.ringId) ? <option value={form.ringId}>링 #{form.ringId}</option> : null}{rings.map((ring) => <option key={ring.ringId} value={ring.ringId}>{ring.name} · #{ring.ringId}</option>)}</select></label><label>경기 번호<input min="1" onChange={(event) => updateField('boutNumber', event.target.value)} required type="number" value={form.boutNumber} /></label><label>빨강 선수<select aria-label="빨강 선수" onChange={(event) => updateField('redAthleteId', event.target.value)} required value={form.redAthleteId}><option value="">선수를 선택하세요</option>{form.redAthleteId && !athletes.some((athlete) => String(athlete.athleteId) === form.redAthleteId) ? <option value={form.redAthleteId}>선수 #{form.redAthleteId}</option> : null}{athletes.map((athlete) => <option key={athlete.athleteId} value={athlete.athleteId}>{athlete.name} · #{athlete.athleteId}</option>)}</select></label><label>파랑 선수<select aria-label="파랑 선수" onChange={(event) => updateField('blueAthleteId', event.target.value)} required value={form.blueAthleteId}><option value="">선수를 선택하세요</option>{form.blueAthleteId && !athletes.some((athlete) => String(athlete.athleteId) === form.blueAthleteId) ? <option value={form.blueAthleteId}>선수 #{form.blueAthleteId}</option> : null}{athletes.map((athlete) => <option key={athlete.athleteId} value={athlete.athleteId}>{athlete.name} · #{athlete.athleteId}</option>)}</select></label><label>경기 유형<input onChange={(event) => updateField('matchType', event.target.value)} value={form.matchType} /></label><label>라운드 수<input min="1" onChange={(event) => updateField('totalRounds', event.target.value)} required type="number" value={form.totalRounds} /></label><label>진행 순서<input min="1" onChange={(event) => updateField('scheduledOrder', event.target.value)} required type="number" value={form.scheduledOrder} /></label><label className="admin-toggle-field"><input checked={form.eventBout} onChange={(event) => updateField('eventBout', event.target.checked)} type="checkbox" /> 이벤트 경기</label></div>
              <div className="admin-form-actions"><button className="command-button" disabled={saving} type="submit">{selectedId ? '경기 저장' : '경기 생성'}</button>{selectedId ? <button className="danger-button" disabled={saving} onClick={handleDelete} type="button">경기 삭제</button> : null}</div>
            </form>
          </section>
        </div>
      ) : null}
    </main>
  );
}

export function AdminBoutPage({ tournamentId }) {
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

  return session ? <BoutWorkspace onLogout={handleLogout} session={session} tournamentId={tournamentId} /> : <LoginForm onLogin={handleLogin} />;
}
