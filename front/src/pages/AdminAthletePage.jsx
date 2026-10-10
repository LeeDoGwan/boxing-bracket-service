import { apiErrorMessage } from '../i18n/errors';
import { useTranslation } from 'react-i18next';
import { translate as t, localizedMessage, resolveMessage } from '../i18n';
import { useCallback, useEffect, useState } from 'react';
import { createAthlete, deleteAthlete, getAthletes, updateAthlete } from '../api/adminAthletes';
import { useStaffAuth } from '../auth/StaffAuthContext';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { StatePanel } from '../components/StatePanel';

function blankForm() {
  return { affiliation: '', name: '' };
}

function formFromAthlete(athlete) {
  return { affiliation: athlete.affiliation || '', name: athlete.name || '' };
}

function AthleteWorkspace({ onLogout, session, tournamentId }) {
  useTranslation();
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
      const nextAthletes = (await getAthletes(tournamentId, keyword, session.accessToken)) || [];
      setAthletes(nextAthletes);
      setSelectedId((current) => current && nextAthletes.some((item) => item.athleteId === current)
        ? current
        : nextAthletes[0]?.athleteId || null);
    } catch {
      setListError(localizedMessage('errors:athlete.listPermission'));
    } finally {
      setLoading(false);
    }
  }, [keyword, session.accessToken, tournamentId]);

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
    const payload = { ...form, affiliation: form.affiliation || null, tournamentId };
    try {
      if (selectedId) {
        const updated = await updateAthlete(selectedId, payload, session.accessToken);
        setAthletes((current) => current.map((athlete) => athlete.athleteId === selectedId ? updated : athlete));
        setMessage(localizedMessage('admin:athlete.updated'));
      } else {
        const created = await createAthlete(payload, session.accessToken);
        setAthletes((current) => [...current, created]);
        setSelectedId(created.athleteId);
        setMessage(localizedMessage('admin:athlete.created'));
      }
    } catch (requestError) {
      setActionError(apiErrorMessage(requestError, localizedMessage('errors:athlete.save')));
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
      await deleteAthlete(selectedId, tournamentId, session.accessToken);
      const remaining = athletes.filter((athlete) => athlete.athleteId !== selectedId);
      setAthletes(remaining);
      setSelectedId(remaining[0]?.athleteId || null);
      setMessage(localizedMessage('admin:athlete.deleted'));
      setConfirmingDelete(false);
    } catch (requestError) {
      setActionError(apiErrorMessage(requestError, localizedMessage('errors:athlete.delete')));
    } finally {
      setSaving(false);
    }
  }

  return (
    <main className="page-shell admin-shell">
      <div className="judge-heading">
        <div><p className="eyebrow">{t('admin:athlete.participants')}</p><h2>{t('admin:athlete.title')}</h2><p>{t('common:signedInAs', { value1: session.account.name })}</p></div>
        <div className="operations-heading-actions"><button className="command-button" disabled={loading} onClick={loadAthletes} type="button">{t('common:refresh')}</button><button className="secondary-button" onClick={onLogout} type="button">{t('common:signOut')}</button></div>
      </div>
      {actionError && <p aria-live="polite" className="form-error admin-action-message" role="alert">{resolveMessage(actionError)}</p>}
      {message && <p aria-live="polite" className="admin-success-message">{resolveMessage(message)}</p>}
      {loading ? <StatePanel title={t('admin:athlete.loading')}>{t('common:wait')}</StatePanel> : null}
      {listError && !loading ? <StatePanel action={<button className="command-button" onClick={loadAthletes} type="button">{t('common:retry')}</button>} title={t('errors:athlete.list')} tone="error">{t('errors:operatorPermissionRetry')}</StatePanel> : null}
      {!loading && !listError ? (
        <div className="admin-setup-layout">
          <aside className="admin-list-panel">
            <div className="operation-panel-heading"><div><p className="eyebrow">{t('admin:athlete.registered')}</p><h3>{t('admin:athlete.list')}</h3></div><span>{t('common:recordCount', { value1: athletes.length })}</span></div>
            <form className="athlete-search" onSubmit={handleSearch}><input aria-label={t('admin:athlete.search')} onChange={(event) => setSearchInput(event.target.value)} placeholder={t('admin:athlete.searchPlaceholder')} value={searchInput} /><button className="command-button" type="submit">{t('common:search')}</button></form>
            {keyword ? <button className="secondary-button athlete-clear-search" onClick={clearSearch} type="button">{t('admin:athlete.clearSearch', { value1: keyword })}</button> : null}
            <button className={`admin-new-option${selectedId === null ? ' selected' : ''}`} onClick={() => setSelectedId(null)} type="button">{t('admin:athlete.add')}</button>
            <div className="admin-entity-list">{athletes.map((athlete) => <button className={`admin-entity-option${selectedId === athlete.athleteId ? ' selected' : ''}`} key={athlete.athleteId} onClick={() => setSelectedId(athlete.athleteId)} type="button"><strong>{athlete.name}</strong><span>{athlete.affiliation || t('common:affiliationUnknown')} · #{athlete.athleteId}</span></button>)}</div>
            {!athletes.length ? <p className="operation-empty">{t('admin:athlete.empty')}</p> : null}
          </aside>
          <section className="admin-form-panel">
            <div className="operation-panel-heading"><div><p className="eyebrow">{selectedId ? t('admin:athlete.identifier', { value1: selectedId }) : t('admin:athlete.new')}</p><h3>{selectedId ? t('admin:athlete.edit') : t('admin:athlete.create')}</h3></div>{selectedId ? <span>#{selectedId}</span> : null}</div>
            <form onSubmit={handleSubmit}>
              <div className="admin-form-grid"><label>{t('common:athleteName')}<input onChange={(event) => updateField('name', event.target.value)} required value={form.name} /></label><label>{t('common:affiliation')}<input onChange={(event) => updateField('affiliation', event.target.value)} value={form.affiliation} /></label></div>
              <div className="admin-form-actions"><button className="command-button" disabled={saving} type="submit">{selectedId ? t('admin:athlete.save') : t('admin:athlete.create')}</button>{selectedId ? <button className="danger-button" disabled={saving} onClick={() => setConfirmingDelete(true)} type="button">{t('admin:athlete.delete')}</button> : null}</div>
            </form>
          </section>
        </div>
      ) : null}
      {confirmingDelete ? <ConfirmDialog busy={saving} description={t('admin:athlete.deleteWarning')} onCancel={() => setConfirmingDelete(false)} onConfirm={handleDelete} title={t('admin:athlete.deleteConfirm')} /> : null}
    </main>
  );
}

export function AdminAthletePage({ tournamentId = 1 }) {
  useTranslation();
  const { session, signOut } = useStaffAuth();
  if (!session || !['GAME_MANAGER', 'SERVICE_MANAGER'].includes(session.account.role)) return null;
  return <AthleteWorkspace onLogout={signOut} session={session} tournamentId={tournamentId} />;
}
