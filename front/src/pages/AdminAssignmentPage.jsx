import { useCallback, useEffect, useMemo, useState } from 'react';
import { login, logout } from '../api/auth';
import { getAccounts } from '../api/adminAccounts';
import { getRings } from '../api/adminRings';
import { getTournaments } from '../api/adminTournaments';
import { changeAssignmentActive, createAssignment, getAssignments } from '../api/staffAssignments';
import { StatePanel } from '../components/StatePanel';

const SESSION_KEY = 'boxing.operations.session';
const ALLOWED_ROLES = new Set(['GAME_MANAGER', 'SERVICE_MANAGER']);
const STAFF_ROLES = { JUDGE: 'Judge', SUPERVISOR: 'Supervisor', RING_MANAGER: 'Ring Manager' };

function readSession() {
  try {
    const stored = window.sessionStorage.getItem(SESSION_KEY);
    const session = stored ? JSON.parse(stored) : null;
    return session?.accessToken && ALLOWED_ROLES.has(session?.account?.role) ? session : null;
  } catch {
    return null;
  }
}

function LoginForm({ onLogin }) {
  const [form, setForm] = useState({ loginId: '', password: '' });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      const session = await login(form.loginId, form.password);
      if (!ALLOWED_ROLES.has(session.account?.role)) throw new Error('ADMIN_ONLY');
      onLogin(session);
    } catch (requestError) {
      setError(requestError.message === 'ADMIN_ONLY' ? 'Admin role is required.' : 'Login failed.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="page-shell auth-shell">
      <section className="auth-panel">
        <p className="eyebrow">ASSIGNMENT ADMIN</p>
        <h2>Staff assignments</h2>
        <form onSubmit={handleSubmit}>
          <label>Login ID<input autoComplete="username" onChange={(event) => setForm({ ...form, loginId: event.target.value })} required value={form.loginId} /></label>
          <label>Password<input autoComplete="current-password" onChange={(event) => setForm({ ...form, password: event.target.value })} required type="password" value={form.password} /></label>
          {error && <p className="form-error" role="alert">{error}</p>}
          <button className="command-button" disabled={submitting} type="submit">{submitting ? 'Signing in...' : 'Sign in'}</button>
        </form>
      </section>
    </main>
  );
}

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
  const [session, setSession] = useState(readSession);
  function handleLogin(nextSession) { window.sessionStorage.setItem(SESSION_KEY, JSON.stringify(nextSession)); setSession(nextSession); }
  async function handleLogout() { if (session?.accessToken) await logout(session.accessToken).catch(() => undefined); window.sessionStorage.removeItem(SESSION_KEY); setSession(null); }
  return session ? <AssignmentWorkspace onLogout={handleLogout} session={session} /> : <LoginForm onLogin={handleLogin} />;
}
