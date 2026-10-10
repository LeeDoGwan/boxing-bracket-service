import { apiErrorMessage } from '../i18n/errors';
import { useTranslation } from 'react-i18next';
import { translate as t, localizedMessage, resolveMessage } from '../i18n';
import { useCallback, useEffect, useState } from 'react';
import { createSchedule, deleteSchedule, getSchedules, updateSchedule } from '../api/adminSchedules';
import { useStaffAuth } from '../auth/StaffAuthContext';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { StatePanel } from '../components/StatePanel';

const TYPE_OPTIONS = [
  ['BOUT', localizedMessage('common:bout')],
  ['BREAK', localizedMessage('common:break')],
  ['LUNCH', localizedMessage('common:lunch')],
  ['PERFORMANCE', localizedMessage('common:performance')],
  ['EVENT', localizedMessage('common:event')],
];
const STATUS_OPTIONS = [
  ['SCHEDULED', localizedMessage('common:scheduled')],
  ['IN_PROGRESS', localizedMessage('common:inProgress')],
  ['COMPLETED', localizedMessage('common:finished')],
];

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

function ScheduleWorkspace({ onLogout, session, tournamentId }) {
  useTranslation();
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
      setListError(localizedMessage('errors:schedule.listPermission'));
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
        setMessage(localizedMessage('admin:schedule.updated'));
      } else {
        const created = await createSchedule(payload, session.accessToken);
        setSchedules((current) => [...current, created].sort((left, right) => left.startTime.localeCompare(right.startTime)));
        setSelectedId(created.scheduleId);
        setMessage(localizedMessage('admin:schedule.created'));
      }
    } catch (requestError) {
      setActionError(apiErrorMessage(requestError, localizedMessage('errors:schedule.save')));
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
      setMessage(localizedMessage('admin:schedule.deleted'));
      setConfirmingDelete(false);
    } catch (requestError) {
      setActionError(apiErrorMessage(requestError, localizedMessage('errors:schedule.delete')));
    } finally {
      setSaving(false);
    }
  }

  return (
    <main className="page-shell admin-shell">
      <div className="judge-heading">
        <div><p className="eyebrow">{t('admin:schedule.desk')}</p><h2>{t('admin:schedule.title')}</h2><p>{t('common:operatorTournament', { value1: session.account.name, value2: tournamentId })}</p></div>
        <div className="operations-heading-actions"><button className="command-button" disabled={loading} onClick={loadSchedules} type="button">{t('common:refresh')}</button><button className="secondary-button" onClick={onLogout} type="button">{t('common:signOut')}</button></div>
      </div>
      {actionError && <p aria-live="polite" className="form-error admin-action-message" role="alert">{resolveMessage(actionError)}</p>}
      {message && <p aria-live="polite" className="admin-success-message">{resolveMessage(message)}</p>}
      {loading ? <StatePanel title={t('admin:schedule.loading')}>{t('common:wait')}</StatePanel> : null}
      {listError && !loading ? <StatePanel action={<button className="command-button" onClick={loadSchedules} type="button">{t('common:retry')}</button>} title={t('errors:schedule.list')} tone="error">{t('errors:tournamentPermissionRetry')}</StatePanel> : null}
      {!loading && !listError ? (
        <div className="admin-setup-layout">
          <aside className="admin-list-panel">
            <div className="operation-panel-heading"><div><p className="eyebrow">{t('common:schedule')}</p><h3>{t('admin:schedule.list')}</h3></div><span>{t('common:recordCount', { value1: schedules.length })}</span></div>
            <button className={`admin-new-option${selectedId === null ? ' selected' : ''}`} onClick={() => setSelectedId(null)} type="button">{t('admin:schedule.add')}</button>
            <div className="admin-entity-list">{schedules.map((schedule) => <button className={`admin-entity-option${selectedId === schedule.scheduleId ? ' selected' : ''}`} key={schedule.scheduleId} onClick={() => setSelectedId(schedule.scheduleId)} type="button"><strong>{schedule.title}</strong><span>{schedule.type} · {resolveMessage(schedule.startTime.replace('T', ' '))} · {schedule.status}</span></button>)}</div>
            {!schedules.length ? <p className="operation-empty">{t('admin:schedule.empty')}</p> : null}
          </aside>
          <section className="admin-form-panel">
            <div className="operation-panel-heading"><div><p className="eyebrow">{selectedId ? t('admin:schedule.identifier', { value1: selectedId }) : t('admin:schedule.new')}</p><h3>{selectedId ? t('admin:schedule.edit') : t('admin:schedule.create')}</h3></div>{selectedId ? <span>#{selectedId}</span> : null}</div>
            <form onSubmit={handleSubmit}>
              <div className="admin-form-grid">
                <label>{t('common:scheduleType')}<select onChange={(event) => updateField('type', event.target.value)} value={form.type}>{TYPE_OPTIONS.map(([value, label]) => <option key={value} value={value}>{resolveMessage(label)}</option>)}</select></label>
                <label>{t('common:status')}<select onChange={(event) => updateField('status', event.target.value)} value={form.status}>{STATUS_OPTIONS.map(([value, label]) => <option key={value} value={value}>{resolveMessage(label)}</option>)}</select></label>
                <label className="admin-wide-field">{t('common:title')}<input onChange={(event) => updateField('title', event.target.value)} required value={form.title} /></label>
                <label>{t('common:startTime')}<input onChange={(event) => updateField('startTime', event.target.value)} required type="datetime-local" value={form.startTime} /></label>
                <label>{t('common:endTime')}<input onChange={(event) => updateField('endTime', event.target.value)} type="datetime-local" value={form.endTime} /></label>
                <label>{t('common:ringId')}<input min="1" onChange={(event) => updateField('ringId', event.target.value)} type="number" value={form.ringId} /></label>
                <label>{t('common:linkedBoutId')}<input min="1" onChange={(event) => updateField('relatedBoutId', event.target.value)} type="number" value={form.relatedBoutId} /></label>
              </div>
              <div className="admin-form-actions"><button className="command-button" disabled={saving} type="submit">{selectedId ? t('admin:schedule.save') : t('admin:schedule.create')}</button>{selectedId ? <button className="danger-button" disabled={saving} onClick={() => setConfirmingDelete(true)} type="button">{t('admin:schedule.delete')}</button> : null}</div>
            </form>
          </section>
        </div>
      ) : null}
      {confirmingDelete ? <ConfirmDialog busy={saving} description={t('admin:schedule.deleteWarning')} onCancel={() => setConfirmingDelete(false)} onConfirm={handleDelete} title={t('admin:schedule.deleteConfirm')} /> : null}
    </main>
  );
}

export function AdminSchedulePage({ tournamentId }) {
  useTranslation();
  const { session, signOut } = useStaffAuth();
  if (!session || !['GAME_MANAGER', 'SERVICE_MANAGER'].includes(session.account.role)) return null;
  return <ScheduleWorkspace onLogout={signOut} session={session} tournamentId={tournamentId} />;
}
