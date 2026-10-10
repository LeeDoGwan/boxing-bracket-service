import { apiErrorMessage } from '../i18n/errors';
import { useTranslation } from 'react-i18next';
import { translate as t, localizedMessage, resolveMessage } from '../i18n';
import { useCallback, useEffect, useState } from 'react';
import { createRing, deleteRing, getRings, updateRing } from '../api/adminRings';
import { useStaffAuth } from '../auth/StaffAuthContext';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { StatePanel } from '../components/StatePanel';

const STATUS_LABELS = { CLOSED: localizedMessage('common:ringClosed'), IN_PROGRESS: localizedMessage('common:inProgress'), READY: localizedMessage('common:ready') };

function blankForm() {
  return { name: '', status: 'READY' };
}

function formFromRing(ring) {
  return { name: ring.name || '', status: ring.status || 'READY' };
}

function statusLabel(status) {
  return STATUS_LABELS[status] || status || localizedMessage('common:statusUnknown');
}

function RingWorkspace({ onLogout, session, tournamentId }) {
  useTranslation();
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
      setListError(localizedMessage('errors:ring.listPermission'));
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
        setMessage(localizedMessage('admin:ring.updated'));
      } else {
        const created = await createRing(payload, session.accessToken);
        setRings((current) => [...current, created]);
        setSelectedId(created.ringId);
        setMessage(localizedMessage('admin:ring.created'));
      }
    } catch (requestError) {
      setActionError(apiErrorMessage(requestError, localizedMessage('errors:ring.save')));
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
      setMessage(localizedMessage('admin:ring.deleted'));
      setConfirmingDelete(false);
    } catch (requestError) {
      setActionError(apiErrorMessage(requestError, localizedMessage('errors:ring.delete')));
    } finally {
      setSaving(false);
    }
  }

  return (
    <main className="page-shell admin-shell">
      <div className="judge-heading">
        <div><p className="eyebrow">{t('admin:ring.desk')}</p><h2>{t('admin:ring.title')}</h2><p>{t('common:operatorTournament', { value1: session.account.name, value2: tournamentId })}</p></div>
        <div className="operations-heading-actions"><button className="command-button" disabled={loading} onClick={loadRings} type="button">{t('common:refresh')}</button><button className="secondary-button" onClick={onLogout} type="button">{t('common:signOut')}</button></div>
      </div>
      {actionError && <p aria-live="polite" className="form-error admin-action-message" role="alert">{resolveMessage(actionError)}</p>}
      {message && <p aria-live="polite" className="admin-success-message">{resolveMessage(message)}</p>}
      {loading ? <StatePanel title={t('admin:ring.loading')}>{t('common:wait')}</StatePanel> : null}
      {listError && !loading ? <StatePanel action={<button className="command-button" onClick={loadRings} type="button">{t('common:retry')}</button>} title={t('errors:ring.list')} tone="error">{t('errors:tournamentPermissionRetry')}</StatePanel> : null}
      {!loading && !listError ? (
        <div className="admin-setup-layout">
          <aside className="admin-list-panel">
            <div className="operation-panel-heading"><div><p className="eyebrow">{t('admin:ring.venue')}</p><h3>{t('admin:ring.list')}</h3></div><span>{t('common:recordCount', { value1: rings.length })}</span></div>
            <button className={`admin-new-option${selectedId === null ? ' selected' : ''}`} onClick={() => setSelectedId(null)} type="button">{t('admin:ring.add')}</button>
            <div className="admin-entity-list">{rings.map((ring) => <button className={`admin-entity-option${selectedId === ring.ringId ? ' selected' : ''}`} key={ring.ringId} onClick={() => setSelectedId(ring.ringId)} type="button"><strong>{ring.name}</strong><span>{t('admin:ring.currentBout', { value1: statusLabel(ring.status), value2: ring.currentBoutId || t('common:none') })}</span></button>)}</div>
            {!rings.length ? <p className="operation-empty">{t('admin:ring.empty')}</p> : null}
          </aside>
          <section className="admin-form-panel">
            <div className="operation-panel-heading"><div><p className="eyebrow">{selectedId ? t('common:ringNumber', { value1: selectedId }) : t('admin:ring.new')}</p><h3>{selectedId ? t('admin:ring.edit') : t('admin:ring.create')}</h3></div>{selectedId ? <span>#{selectedId}</span> : null}</div>
            <form onSubmit={handleSubmit}>
              <div className="admin-form-grid"><label>{t('common:ringName')}<input onChange={(event) => updateField('name', event.target.value)} required value={form.name} /></label><label>{t('common:status')}<select onChange={(event) => updateField('status', event.target.value)} value={form.status}>{Object.entries(STATUS_LABELS).map(([value, label]) => <option key={value} value={value}>{resolveMessage(label)}</option>)}</select></label></div>
              <div className="admin-form-actions"><button className="command-button" disabled={saving} type="submit">{selectedId ? t('admin:ring.save') : t('admin:ring.create')}</button>{selectedId ? <button className="danger-button" disabled={saving} onClick={() => setConfirmingDelete(true)} type="button">{t('admin:ring.delete')}</button> : null}</div>
            </form>
          </section>
        </div>
      ) : null}
      {confirmingDelete ? <ConfirmDialog busy={saving} description={t('admin:ring.deleteWarning')} onCancel={() => setConfirmingDelete(false)} onConfirm={handleDelete} title={t('admin:ring.deleteConfirm')} /> : null}
    </main>
  );
}

export function AdminRingPage({ tournamentId }) {
  useTranslation();
  const { session, signOut } = useStaffAuth();
  if (!session || !['GAME_MANAGER', 'SERVICE_MANAGER'].includes(session.account.role)) return null;
  return <RingWorkspace onLogout={signOut} session={session} tournamentId={tournamentId} />;
}
