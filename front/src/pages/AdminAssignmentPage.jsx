import { useCallback, useEffect, useMemo, useState } from 'react';
import { getAccounts } from '../api/adminAccounts';
import { getRings } from '../api/adminRings';
import { getTournaments } from '../api/adminTournaments';
import { changeAssignmentActive, createAssignment, getAssignments } from '../api/staffAssignments';
import { useStaffAuth } from '../auth/StaffAuthContext';
import { StatePanel } from '../components/StatePanel';

const STAFF_ROLES = { JUDGE: '심판', SUPERVISOR: '감독관', RING_MANAGER: '링 매니저' };

function AssignmentWorkspace({ onLogout, session }) {
  const [tournaments, setTournaments] = useState([]);
  const [accounts, setAccounts] = useState([]);
  const [rings, setRings] = useState([]);
  const [assignments, setAssignments] = useState([]);
  const [tournamentId, setTournamentId] = useState('');
  const [role, setRole] = useState('JUDGE');
  const [accountId, setAccountId] = useState('');
  const [ringId, setRingId] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const load = useCallback(async (nextTournamentId = '') => {
    setLoading(true);
    setError('');
    try {
      const [nextTournaments, nextAccounts] = await Promise.all([
        getTournaments(session.accessToken),
        getAccounts({}, session.accessToken),
      ]);
      setTournaments(nextTournaments || []);
      setAccounts(nextAccounts || []);
      const selectedTournament = Number(nextTournamentId || nextTournaments?.[0]?.tournamentId);
      if (!Number.isInteger(selectedTournament)) return;
      setTournamentId(String(selectedTournament));
      const [nextRings, nextAssignments] = await Promise.all([
        getRings(selectedTournament, session.accessToken),
        getAssignments({ tournamentId: selectedTournament }, session.accessToken),
      ]);
      setRings(nextRings || []);
      setAssignments(nextAssignments || []);
      setRingId((current) => current && nextRings.some((ring) => String(ring.ringId) === current) ? current : String(nextRings?.[0]?.ringId || ''));
    } catch (requestError) {
      setError(requestError.status === 403 && session.account.role === 'GAME_MANAGER'
        ? '배정에 필요한 계정 목록은 서비스 매니저만 조회할 수 있습니다. 서비스 매니저에게 배정을 요청해 주세요.'
        : '배정 목록을 불러오지 못했습니다. 로그인 상태와 권한을 확인해 주세요.');
    } finally {
      setLoading(false);
    }
  }, [session.accessToken, session.account.role]);

  useEffect(() => { load(); }, [load]);

  const roleAccounts = useMemo(() => accounts.filter((account) => account.role === role && account.status === 'ACTIVE'), [accounts, role]);

  async function handleCreate(event) {
    event.preventDefault();
    setSaving(true);
    setError('');
    setMessage('');
    try {
      await createAssignment({ accountId: Number(accountId), ringId: Number(ringId), role, tournamentId: Number(tournamentId) }, session.accessToken);
      await load(tournamentId);
      setMessage('배정을 생성했습니다.');
    } catch (requestError) {
      setError(requestError.message || '배정 생성에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  }

  async function handleActive(assignment) {
    setSaving(true);
    setError('');
    try {
      await changeAssignmentActive(assignment.assignmentId, !assignment.active, session.accessToken);
      await load(tournamentId);
    } catch (requestError) {
      setError(requestError.message || '배정 변경에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <main className="page-shell admin-shell">
      <div className="judge-heading"><div><p className="eyebrow">관계자 배정</p><h2>배정 관리</h2><p>로그인 계정: {session.account.name}</p></div><button className="secondary-button" onClick={onLogout} type="button">로그아웃</button></div>
      {error && <p className="form-error" role="alert">{error}</p>}
      {message && <p className="admin-success-message">{message}</p>}
      {loading ? <StatePanel title="배정 목록을 불러오는 중">잠시 기다려 주세요.</StatePanel> : (
        <div className="admin-setup-layout">
          <section className="admin-form-panel">
            <form onSubmit={handleCreate}>
              <div className="admin-form-grid">
                <label>대회<select onChange={(event) => load(event.target.value)} value={tournamentId}>{tournaments.map((tournament) => <option key={tournament.tournamentId} value={tournament.tournamentId}>{tournament.name} #{tournament.tournamentId}</option>)}</select></label>
                <label>역할<select onChange={(event) => { setRole(event.target.value); setAccountId(''); }} value={role}>{Object.entries(STAFF_ROLES).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
                <label>계정<select onChange={(event) => setAccountId(event.target.value)} required value={accountId}><option value="">계정을 선택하세요</option>{roleAccounts.map((account) => <option key={account.accountId} value={account.accountId}>{account.name} · {account.loginId}</option>)}</select></label>
                <label>링<select onChange={(event) => setRingId(event.target.value)} required value={ringId}><option value="">링을 선택하세요</option>{rings.map((ring) => <option key={ring.ringId} value={ring.ringId}>{ring.name} #{ring.ringId}</option>)}</select></label>
              </div>
              <div className="admin-form-actions"><button className="command-button" disabled={saving || !tournamentId} type="submit">배정 생성</button></div>
            </form>
          </section>
          <section className="admin-list-panel"><h3>현재 배정</h3><div className="admin-entity-list">{assignments.length ? assignments.map((assignment) => <div className="admin-entity-option" key={assignment.assignmentId}><strong>{STAFF_ROLES[assignment.role] || assignment.role} · 링 #{assignment.ringId}</strong><span>계정 #{assignment.accountId} · {assignment.active ? '활성' : '비활성'}</span><button className="secondary-button" disabled={saving} onClick={() => handleActive(assignment)} type="button">{assignment.active ? '비활성화' : '활성화'}</button></div>) : <p className="empty-copy">등록된 배정이 없습니다.</p>}</div></section>
        </div>
      )}
    </main>
  );
}

export function AdminAssignmentPage() {
  const { session, signOut } = useStaffAuth();
  if (!session || !['GAME_MANAGER', 'SERVICE_MANAGER'].includes(session.account.role)) return null;
  return <AssignmentWorkspace onLogout={signOut} session={session} />;
}
