import { apiErrorMessage } from '../i18n/errors';
import { useTranslation } from 'react-i18next';
import { translate as t, localizedMessage, resolveMessage } from '../i18n';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { getAccounts } from '../api/adminAccounts';
import { getRings } from '../api/adminRings';
import { getTournaments } from '../api/adminTournaments';
import { changeAssignmentActive, createAssignment, getAssignments } from '../api/staffAssignments';
import { useStaffAuth } from '../auth/StaffAuthContext';
import { StatePanel } from '../components/StatePanel';

const STAFF_ROLES = { JUDGE: localizedMessage('common:role.judge'), SUPERVISOR: localizedMessage('common:role.supervisor'), RING_MANAGER: localizedMessage('common:role.ringManager') };

function AssignmentWorkspace({ onLogout, session }) {
  useTranslation();
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
        ? localizedMessage('errors:assignment.accountPermission')
        : localizedMessage('errors:assignment.load'));
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
      setMessage(localizedMessage('admin:assignment.created'));
    } catch (requestError) {
      setError(apiErrorMessage(requestError, localizedMessage('errors:assignment.create')));
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
      setError(apiErrorMessage(requestError, localizedMessage('errors:assignment.update')));
    } finally {
      setSaving(false);
    }
  }

  return (
    <main className="page-shell admin-shell">
      <div className="judge-heading"><div><p className="eyebrow">{t('admin:assignment.staff')}</p><h2>{t('admin:assignment.title')}</h2><p>{t('common:signedInAs', { value1: session.account.name })}</p></div><button className="secondary-button" onClick={onLogout} type="button">{t('common:signOut')}</button></div>
      {error && <p className="form-error" role="alert">{resolveMessage(error)}</p>}
      {message && <p className="admin-success-message">{resolveMessage(message)}</p>}
      {loading ? <StatePanel title={t('admin:assignment.loading')}>{t('common:waitBriefly')}</StatePanel> : (
        <div className="admin-setup-layout">
          <section className="admin-form-panel">
            <form onSubmit={handleCreate}>
              <div className="admin-form-grid">
                <label>{t('common:tournament')}<select onChange={(event) => load(event.target.value)} value={tournamentId}>{tournaments.map((tournament) => <option key={tournament.tournamentId} value={tournament.tournamentId}>{tournament.name} #{tournament.tournamentId}</option>)}</select></label>
                <label>{t('common:role')}<select onChange={(event) => { setRole(event.target.value); setAccountId(''); }} value={role}>{Object.entries(STAFF_ROLES).map(([value, label]) => <option key={value} value={value}>{resolveMessage(label)}</option>)}</select></label>
                <label>{t('common:account')}<select onChange={(event) => setAccountId(event.target.value)} required value={accountId}><option value="">{t('admin:assignment.selectAccount')}</option>{roleAccounts.map((account) => <option key={account.accountId} value={account.accountId}>{account.name} · {account.loginId}</option>)}</select></label>
                <label>{t('common:ring')}<select onChange={(event) => setRingId(event.target.value)} required value={ringId}><option value="">{t('common:selectRing')}</option>{rings.map((ring) => <option key={ring.ringId} value={ring.ringId}>{ring.name} #{ring.ringId}</option>)}</select></label>
              </div>
              <div className="admin-form-actions"><button className="command-button" disabled={saving || !tournamentId} type="submit">{t('admin:assignment.create')}</button></div>
            </form>
          </section>
          <section className="admin-list-panel"><h3>{t('admin:assignment.current')}</h3><div className="admin-entity-list">{assignments.length ? assignments.map((assignment) => <div className="admin-entity-option" key={assignment.assignmentId}><strong>{t('admin:assignment.roleRing', { value1: STAFF_ROLES[assignment.role] || assignment.role, value2: assignment.ringId })}</strong><span>{t('admin:assignment.accountStatus', { value1: assignment.accountId, value2: assignment.active ? t('common:active') : t('common:inactive') })}</span><button className="secondary-button" disabled={saving} onClick={() => handleActive(assignment)} type="button">{assignment.active ? t('common:deactivate') : t('common:activate')}</button></div>) : <p className="empty-copy">{t('admin:assignment.empty')}</p>}</div></section>
        </div>
      )}
    </main>
  );
}

export function AdminAssignmentPage() {
  useTranslation();
  const { session, signOut } = useStaffAuth();
  if (!session || !['GAME_MANAGER', 'SERVICE_MANAGER'].includes(session.account.role)) return null;
  return <AssignmentWorkspace onLogout={signOut} session={session} />;
}
