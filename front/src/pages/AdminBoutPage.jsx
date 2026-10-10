import { apiErrorMessage } from '../i18n/errors';
import { useTranslation } from 'react-i18next';
import { translate as t, localizedMessage, resolveMessage } from '../i18n';
import { useCallback, useEffect, useState } from 'react';
import { createBout, deleteBout, getBouts, importBouts, updateBout } from '../api/adminBouts';
import { getAthletes } from '../api/adminAthletes';
import { getRings } from '../api/adminRings';
import { useStaffAuth } from '../auth/StaffAuthContext';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { StatePanel } from '../components/StatePanel';

const STATUS_LABELS = { CANCELED: localizedMessage('common:status.canceled'), FINISHED: localizedMessage('common:finished'), IN_PROGRESS: localizedMessage('common:inProgress'), READY: localizedMessage('common:ready'), SCHEDULED: localizedMessage('common:scheduled'), SCORING: localizedMessage('common:scoring') };
const CSV_HEADERS = 'tournamentId,ringId,matchType,redAthleteId,blueAthleteId,totalRounds,scheduledOrder,eventBout';

function blankForm(tournamentId) {
  return { blueAthleteId: '', eventBout: false, matchType: '', redAthleteId: '', ringId: '', scheduledOrder: '', totalRounds: '3', tournamentId: String(tournamentId) };
}

function formFromBout(bout) {
  return { blueAthleteId: String(bout.blueAthleteId || ''), eventBout: bout.eventBout, matchType: bout.matchType || '', redAthleteId: String(bout.redAthleteId || ''), ringId: String(bout.ringId || ''), scheduledOrder: String(bout.scheduledOrder || ''), totalRounds: String(bout.totalRounds || ''), tournamentId: String(bout.tournamentId) };
}

function statusLabel(status) {
  return STATUS_LABELS[status] || status || localizedMessage('common:statusUnknown');
}

function toNumber(value) {
  const parsed = Number.parseInt(value, 10);
  return Number.isInteger(parsed) ? parsed : null;
}

function createImportKey() {
  if (typeof window !== 'undefined' && window.crypto?.randomUUID) {
    return window.crypto.randomUUID();
  }
  return `bout-import-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

function BoutWorkspace({ onLogout, session, tournamentId }) {
  useTranslation();
  const [bouts, setBouts] = useState([]);
  const [rings, setRings] = useState([]);
  const [athletes, setAthletes] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [form, setForm] = useState(() => blankForm(tournamentId));
  const [file, setFile] = useState(null);
  const [importKey, setImportKey] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [listError, setListError] = useState('');
  const [referenceError, setReferenceError] = useState('');
  const [actionError, setActionError] = useState('');
  const [message, setMessage] = useState('');
  const [confirmingDelete, setConfirmingDelete] = useState(false);

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
      setListError(localizedMessage('errors:bout.listPermission'));
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
        getAthletes(tournamentId, '', session.accessToken),
      ]);
      setRings(nextRings || []);
      setAthletes(nextAthletes || []);
    } catch {
      setReferenceError(localizedMessage('errors:bout.references'));
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
        setMessage(localizedMessage('admin:bout.updated'));
      } else {
        const created = await createBout(payload, session.accessToken);
        setBouts((current) => [...current, created].sort((left, right) => (left.scheduledOrder || 0) - (right.scheduledOrder || 0)));
        setSelectedId(created.boutId);
        setMessage(localizedMessage('admin:bout.created'));
      }
    } catch (requestError) {
      setActionError(apiErrorMessage(requestError, localizedMessage('errors:bout.save')));
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
      setMessage(localizedMessage('admin:bout.deleted'));
      setConfirmingDelete(false);
    } catch (requestError) {
      setActionError(apiErrorMessage(requestError, localizedMessage('errors:bout.delete')));
    } finally {
      setSaving(false);
    }
  }

  async function handleImport(event) {
    event.preventDefault();
    const formElement = event.currentTarget;
    if (!file) {
      setActionError(localizedMessage('errors:bout.fileRequired'));
      return;
    }
    setSaving(true);
    setActionError('');
    setMessage('');
    try {
      const result = await importBouts(file, session.accessToken, importKey);
      await loadBouts();
      setFile(null);
      setImportKey('');
      formElement.reset();
      setMessage(localizedMessage('admin:bout.imported', { value1: result.importedCount }));
    } catch (requestError) {
      setActionError(apiErrorMessage(requestError, localizedMessage('errors:bout.import')));
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
        <div><p className="eyebrow">{t('admin:bout.desk')}</p><h2>{t('admin:bout.title')}</h2><p>{t('common:operatorTournament', { value1: session.account.name, value2: tournamentId })}</p></div>
        <div className="operations-heading-actions"><button className="command-button" disabled={loading} onClick={loadBouts} type="button">{t('common:refresh')}</button><button className="secondary-button" onClick={onLogout} type="button">{t('common:signOut')}</button></div>
      </div>
      {actionError && <p aria-live="polite" className="form-error admin-action-message" role="alert">{resolveMessage(actionError)}</p>}
      {referenceError && <p aria-live="polite" className="form-error admin-reference-message" role="alert">{resolveMessage(referenceError)}</p>}
      {message && <p aria-live="polite" className="admin-success-message">{resolveMessage(message)}</p>}
      <form className="bout-import-panel" onSubmit={handleImport}><label>{t('admin:bout.file')}<input aria-label={t('admin:bout.file')} accept=".csv,.xls,.xlsx,text/csv,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" onChange={(event) => { const nextFile = event.target.files?.[0] || null; setFile(nextFile); setImportKey(nextFile ? createImportKey() : ''); }} type="file" /></label><div className="bout-import-actions"><button className="command-button" disabled={saving} type="submit">{t('admin:bout.import')}</button><button className="secondary-button" onClick={downloadTemplate} type="button">{t('admin:bout.downloadTemplate')}</button></div><small>{t('admin:bout.importHint')}</small></form>
      {loading ? <StatePanel title={t('admin:bout.loading')}>{t('common:wait')}</StatePanel> : null}
      {listError && !loading ? <StatePanel action={<button className="command-button" onClick={loadBouts} type="button">{t('common:retry')}</button>} title={t('errors:bout.list')} tone="error">{t('errors:tournamentPermissionRetry')}</StatePanel> : null}
      {!loading && !listError ? (
        <div className="admin-setup-layout">
          <aside className="admin-list-panel">
            <div className="operation-panel-heading"><div><p className="eyebrow">{t('admin:bout.bracket')}</p><h3>{t('admin:bout.list')}</h3></div><span>{t('common:recordCount', { value1: bouts.length })}</span></div>
            <button className={`admin-new-option${selectedId === null ? ' selected' : ''}`} onClick={() => setSelectedId(null)} type="button">{t('admin:bout.add')}</button>
            <div className="admin-entity-list">{bouts.map((bout) => <button className={`admin-entity-option${selectedId === bout.boutId ? ' selected' : ''}`} key={bout.boutId} onClick={() => setSelectedId(bout.boutId)} type="button"><strong>{t('admin:bout.numberType', { value1: bout.boutNumber, value2: bout.matchType || t('common:standard') })}</strong><span>{t('admin:bout.summary', { value1: bout.ringId, value2: statusLabel(bout.status), value3: bout.redAthleteId, value4: bout.blueAthleteId })}</span></button>)}</div>
            {!bouts.length ? <p className="operation-empty">{t('admin:bout.empty')}</p> : null}
          </aside>
          <section className="admin-form-panel">
            <div className="operation-panel-heading"><div><p className="eyebrow">{selectedId ? t('common:boutNumber', { value1: selectedId }) : t('admin:bout.new')}</p><h3>{selectedId ? t('admin:bout.edit') : t('admin:bout.create')}</h3></div>{selectedId ? <span>#{selectedId}</span> : null}</div>
            <form onSubmit={handleSubmit}>
              <div className="admin-form-grid"><label>{t('common:ring')}<select aria-label={t('common:ring')} onChange={(event) => updateField('ringId', event.target.value)} required value={form.ringId}><option value="">{t('common:selectRing')}</option>{form.ringId && !rings.some((ring) => String(ring.ringId) === form.ringId) ? <option value={form.ringId}>{t('common:ringIdValue', { value1: form.ringId })}</option> : null}{rings.map((ring) => <option key={ring.ringId} value={ring.ringId}>{ring.name} · #{ring.ringId}</option>)}</select></label><label>{t('common:redAthlete')}<select aria-label={t('common:redAthlete')} onChange={(event) => updateField('redAthleteId', event.target.value)} required value={form.redAthleteId}><option value="">{t('common:selectAthlete')}</option>{form.redAthleteId && !athletes.some((athlete) => String(athlete.athleteId) === form.redAthleteId) ? <option value={form.redAthleteId}>{t('common:athleteIdValue', { value1: form.redAthleteId })}</option> : null}{athletes.map((athlete) => <option key={athlete.athleteId} value={athlete.athleteId}>{athlete.name} · #{athlete.athleteId}</option>)}</select></label><label>{t('common:blueAthlete')}<select aria-label={t('common:blueAthlete')} onChange={(event) => updateField('blueAthleteId', event.target.value)} required value={form.blueAthleteId}><option value="">{t('common:selectAthlete')}</option>{form.blueAthleteId && !athletes.some((athlete) => String(athlete.athleteId) === form.blueAthleteId) ? <option value={form.blueAthleteId}>{t('common:athleteIdValue', { value1: form.blueAthleteId })}</option> : null}{athletes.map((athlete) => <option key={athlete.athleteId} value={athlete.athleteId}>{athlete.name} · #{athlete.athleteId}</option>)}</select></label><label>{t('common:matchType')}<input onChange={(event) => updateField('matchType', event.target.value)} value={form.matchType} /></label><label>{t('common:totalRounds')}<input min="1" onChange={(event) => updateField('totalRounds', event.target.value)} required type="number" value={form.totalRounds} /></label><label>{t('common:scheduledOrder')}<input min="1" onChange={(event) => updateField('scheduledOrder', event.target.value)} required type="number" value={form.scheduledOrder} /></label><label className="admin-toggle-field"><input checked={form.eventBout} onChange={(event) => updateField('eventBout', event.target.checked)} type="checkbox" />{t('common:eventBout')}</label></div>
              <div className="admin-form-actions"><button className="command-button" disabled={saving} type="submit">{selectedId ? t('admin:bout.save') : t('admin:bout.create')}</button>{selectedId ? <button className="danger-button" disabled={saving} onClick={() => setConfirmingDelete(true)} type="button">{t('admin:bout.delete')}</button> : null}</div>
            </form>
          </section>
        </div>
      ) : null}
      {confirmingDelete ? <ConfirmDialog busy={saving} description={t('admin:bout.deleteWarning')} onCancel={() => setConfirmingDelete(false)} onConfirm={handleDelete} title={t('admin:bout.deleteConfirm')} /> : null}
    </main>
  );
}

export function AdminBoutPage({ tournamentId }) {
  useTranslation();
  const { session, signOut } = useStaffAuth();
  if (!session || !['GAME_MANAGER', 'SERVICE_MANAGER'].includes(session.account.role)) return null;
  return <BoutWorkspace onLogout={signOut} session={session} tournamentId={tournamentId} />;
}
