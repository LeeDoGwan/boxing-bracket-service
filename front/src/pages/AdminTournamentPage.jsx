import { apiErrorMessage } from '../i18n/errors';
import { useTranslation } from 'react-i18next';
import { translate as t, localizedMessage, resolveMessage } from '../i18n';
import { useCallback, useEffect, useState } from 'react';
import { createTournament, deleteTournament, getTournaments, updateTournament } from '../api/adminTournaments';
import { useStaffAuth } from '../auth/StaffAuthContext';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { StatePanel } from '../components/StatePanel';

const STATUS_LABELS = { FINISHED: localizedMessage('common:finished'), IN_PROGRESS: localizedMessage('common:inProgress'), READY: localizedMessage('common:ready') };

function blankForm() {
  return { endDate: '', judgeCount: '3', location: '', name: '', startDate: '', status: 'READY' };
}

function formFromTournament(tournament) {
  return {
    endDate: tournament.endDate || '',
    judgeCount: String(tournament.judgeCount || 3),
    location: tournament.location || '',
    name: tournament.name || '',
    startDate: tournament.startDate || '',
    status: tournament.status || 'READY',
  };
}

function statusLabel(status) {
  return STATUS_LABELS[status] || status || localizedMessage('common:statusUnknown');
}

function TournamentWorkspace({ onLogout, session }) {
  useTranslation();
  const [tournaments, setTournaments] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [form, setForm] = useState(blankForm);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [confirmingDelete, setConfirmingDelete] = useState(false);

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
      setError(localizedMessage('errors:tournament.listPermission'));
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
    const payload = { ...form, endDate: form.endDate || null, judgeCount: Number(form.judgeCount), startDate: form.startDate || null };
    try {
      if (selectedId) {
        const updated = await updateTournament(selectedId, payload, session.accessToken);
        setTournaments((current) => current.map((tournament) => tournament.tournamentId === selectedId ? updated : tournament));
        setMessage(localizedMessage('admin:tournament.updated'));
      } else {
        const created = await createTournament(payload, session.accessToken);
        setTournaments((current) => [...current, created]);
        setSelectedId(created.tournamentId);
        setMessage(localizedMessage('admin:tournament.created'));
      }
    } catch (requestError) {
      setError(apiErrorMessage(requestError, localizedMessage('errors:tournament.save')));
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
      setMessage(localizedMessage('admin:tournament.deleted'));
      setConfirmingDelete(false);
    } catch (requestError) {
      setError(apiErrorMessage(requestError, localizedMessage('errors:tournament.delete')));
    } finally {
      setSaving(false);
    }
  }

  return (
    <main className="page-shell admin-shell">
      <div className="judge-heading">
        <div><p className="eyebrow">{t('admin:tournament.desk')}</p><h2>{t('admin:tournament.title')}</h2><p>{t('common:signedInAs', { value1: session.account.name })}</p></div>
        <div className="operations-heading-actions"><button className="command-button" disabled={loading} onClick={loadTournaments} type="button">{t('common:refresh')}</button><button className="secondary-button" onClick={onLogout} type="button">{t('common:signOut')}</button></div>
      </div>
      {error && <p aria-live="polite" className="form-error admin-action-message" role="alert">{resolveMessage(error)}</p>}
      {message && <p aria-live="polite" className="admin-success-message">{resolveMessage(message)}</p>}
      {loading ? <StatePanel title={t('admin:tournament.loading')}>{t('common:wait')}</StatePanel> : null}
      {!loading && !error ? (
        <div className="admin-setup-layout">
          <aside className="admin-list-panel">
            <div className="operation-panel-heading"><div><p className="eyebrow">{t('admin:tournament.registered')}</p><h3>{t('admin:tournament.list')}</h3></div><span>{t('common:recordCount', { value1: tournaments.length })}</span></div>
            <button className={`admin-new-option${selectedId === null ? ' selected' : ''}`} onClick={() => setSelectedId(null)} type="button">{t('admin:tournament.add')}</button>
            <div className="admin-entity-list">{tournaments.map((tournament) => <button className={`admin-entity-option${selectedId === tournament.tournamentId ? ' selected' : ''}`} key={tournament.tournamentId} onClick={() => setSelectedId(tournament.tournamentId)} type="button"><strong>{tournament.name}</strong><span>{resolveMessage(statusLabel(tournament.status))} · {tournament.location || t('common:venueUnknown')}</span></button>)}</div>
            {!tournaments.length ? <p className="operation-empty">{t('admin:tournament.empty')}</p> : null}
          </aside>
          <section className="admin-form-panel">
            <div className="operation-panel-heading"><div><p className="eyebrow">{selectedId ? t('common:tournamentNumber', { value1: selectedId }) : t('admin:tournament.new')}</p><h3>{selectedId ? t('admin:tournament.edit') : t('admin:tournament.create')}</h3></div>{selectedId ? <span>#{selectedId}</span> : null}</div>
            <form onSubmit={handleSubmit}>
              <div className="admin-form-grid">
                <label>{t('common:tournamentName')}<input onChange={(event) => updateField('name', event.target.value)} required value={form.name} /></label>
                <label>{t('common:venue')}<input onChange={(event) => updateField('location', event.target.value)} value={form.location} /></label>
                <label>{t('common:startDate')}<input onChange={(event) => updateField('startDate', event.target.value)} type="date" value={form.startDate} /></label>
                <label>{t('common:endDate')}<input onChange={(event) => updateField('endDate', event.target.value)} type="date" value={form.endDate} /></label>
                <label>{t('common:status')}<select onChange={(event) => updateField('status', event.target.value)} value={form.status}>{Object.entries(STATUS_LABELS).map(([value, label]) => <option key={value} value={value}>{resolveMessage(label)}</option>)}</select></label>
                <label>{t('common:judgeCount')}<select onChange={(event) => updateField('judgeCount', event.target.value)} value={form.judgeCount}><option value="3">3</option><option value="5">5</option></select></label>
              </div>
              <div className="admin-form-actions"><button className="command-button" disabled={saving} type="submit">{selectedId ? t('admin:tournament.save') : t('admin:tournament.create')}</button>{selectedId ? <button className="danger-button" disabled={saving} onClick={() => setConfirmingDelete(true)} type="button">{t('admin:tournament.delete')}</button> : null}</div>
            </form>
          </section>
        </div>
      ) : null}
      {confirmingDelete ? <ConfirmDialog busy={saving} description={t('admin:tournament.deleteWarning')} onCancel={() => setConfirmingDelete(false)} onConfirm={handleDelete} title={t('admin:tournament.deleteConfirm')} /> : null}
    </main>
  );
}

export function AdminTournamentPage() {
  useTranslation();
  const { session, signOut } = useStaffAuth();
  if (!session || !['GAME_MANAGER', 'SERVICE_MANAGER'].includes(session.account.role)) return null;
  return <TournamentWorkspace onLogout={signOut} session={session} />;
}
