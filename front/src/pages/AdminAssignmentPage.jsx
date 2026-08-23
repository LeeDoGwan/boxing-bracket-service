import { useCallback, useEffect, useMemo, useState } from 'react';
import { getAccounts } from '../api/adminAccounts';
import { getRings } from '../api/adminRings';
import { getTournaments } from '../api/adminTournaments';
import { changeAssignmentActive, createAssignment, getAssignments } from '../api/staffAssignments';
import { useStaffAuth } from '../auth/StaffAuthContext';
import { StatePanel } from '../components/StatePanel';

const STAFF_ROLES = { JUDGE: 'Judge', SUPERVISOR: 'Supervisor', RING_MANAGER: 'Ring Manager' };

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
      setError(requestError.message || 'Failed to load assignments.');
    } finally {
      setLoading(false);
    }
  }, [session.accessToken]);

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
      setMessage('Assignment created.');
    } catch (requestError) {
      setError(requestError.message || 'Assignment creation failed.');
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
      setError(requestError.message || 'Assignment update failed.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <main className="page-shell admin-shell">
      <div className="judge-heading"><div><p className="eyebrow">ASSIGNMENT ADMIN</p><h2>Staff ring assignments</h2><p>Signed in as {session.account.name}</p></div><button className="secondary-button" onClick={onLogout} type="button">Sign out</button></div>
      {error && <p className="form-error" role="alert">{error}</p>}
      {message && <p className="admin-success-message">{message}</p>}
      {loading ? <StatePanel title="Loading assignments">Please wait.</StatePanel> : (
        <div className="admin-setup-layout">
          <section className="admin-form-panel">
            <form onSubmit={handleCreate}>
              <div className="admin-form-grid">
                <label>Tournament<select onChange={(event) => load(event.target.value)} value={tournamentId}>{tournaments.map((tournament) => <option key={tournament.tournamentId} value={tournament.tournamentId}>{tournament.name} #{tournament.tournamentId}</option>)}</select></label>
                <label>Role<select onChange={(event) => { setRole(event.target.value); setAccountId(''); }} value={role}>{Object.entries(STAFF_ROLES).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
                <label>Account<select onChange={(event) => setAccountId(event.target.value)} required value={accountId}><option value="">Select account</option>{roleAccounts.map((account) => <option key={account.accountId} value={account.accountId}>{account.name} · {account.loginId}</option>)}</select></label>
                <label>Ring<select onChange={(event) => setRingId(event.target.value)} required value={ringId}><option value="">Select ring</option>{rings.map((ring) => <option key={ring.ringId} value={ring.ringId}>{ring.name} #{ring.ringId}</option>)}</select></label>
              </div>
              <div className="admin-form-actions"><button className="command-button" disabled={saving || !tournamentId} type="submit">Create assignment</button></div>
            </form>
          </section>
          <section className="admin-list-panel"><h3>Current assignments</h3><div className="admin-entity-list">{assignments.length ? assignments.map((assignment) => <div className="admin-entity-option" key={assignment.assignmentId}><strong>{STAFF_ROLES[assignment.role] || assignment.role} · Ring #{assignment.ringId}</strong><span>Account #{assignment.accountId} · {assignment.active ? 'Active' : 'Inactive'}</span><button className="secondary-button" disabled={saving} onClick={() => handleActive(assignment)} type="button">{assignment.active ? 'Deactivate' : 'Activate'}</button></div>) : <p className="empty-copy">No assignments.</p>}</div></section>
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
