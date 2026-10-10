import { apiErrorMessage } from '../i18n/errors';
import { useTranslation } from 'react-i18next';
import { translate as t, localizedMessage, resolveMessage } from '../i18n';
import { useCallback, useEffect, useState } from 'react';
import { createNotice, deleteNotice, getNotices, updateNotice } from '../api/adminNotices';
import { useStaffAuth } from '../auth/StaffAuthContext';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { StatePanel } from '../components/StatePanel';

function blankForm() {
  return { active: true, content: '', displayOrder: '0', title: '' };
}

function formFromNotice(notice) {
  return { active: notice.active, content: notice.content || '', displayOrder: String(notice.displayOrder ?? 0), title: notice.title || '' };
}

function NoticeWorkspace({ onLogout, session, tournamentId }) {
  useTranslation();
  const [notices, setNotices] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [form, setForm] = useState(blankForm);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [listError, setListError] = useState('');
  const [actionError, setActionError] = useState('');
  const [message, setMessage] = useState('');
  const [confirmingDelete, setConfirmingDelete] = useState(false);

  const loadNotices = useCallback(async () => {
    setLoading(true);
    setListError('');
    setActionError('');
    try {
      const nextNotices = (await getNotices(tournamentId, session.accessToken)) || [];
      setNotices(nextNotices);
      setSelectedId((current) => current && nextNotices.some((item) => item.noticeId === current)
        ? current
        : nextNotices[0]?.noticeId || null);
    } catch {
      setListError(localizedMessage('errors:notice.listPermission'));
    } finally {
      setLoading(false);
    }
  }, [session.accessToken, tournamentId]);

  useEffect(() => {
    loadNotices();
  }, [loadNotices]);

  useEffect(() => {
    const selected = notices.find((notice) => notice.noticeId === selectedId);
    setForm(selected ? formFromNotice(selected) : blankForm());
  }, [notices, selectedId]);

  function updateField(key, value) {
    setForm((current) => ({ ...current, [key]: value }));
    setMessage('');
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setSaving(true);
    setActionError('');
    setMessage('');
    const displayOrder = Number.parseInt(form.displayOrder, 10);
    const payload = { active: form.active, content: form.content, displayOrder: Number.isInteger(displayOrder) ? displayOrder : 0, title: form.title, tournamentId: Number(tournamentId) };
    try {
      if (selectedId) {
        const updated = await updateNotice(selectedId, payload, session.accessToken);
        setNotices((current) => current.map((notice) => notice.noticeId === selectedId ? updated : notice));
        setMessage(localizedMessage('admin:notice.updated'));
      } else {
        const created = await createNotice(payload, session.accessToken);
        setNotices((current) => [...current, created].sort((left, right) => (left.displayOrder - right.displayOrder) || (left.noticeId - right.noticeId)));
        setSelectedId(created.noticeId);
        setMessage(localizedMessage('admin:notice.created'));
      }
    } catch (requestError) {
      setActionError(apiErrorMessage(requestError, localizedMessage('errors:notice.save')));
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
      await deleteNotice(selectedId, session.accessToken);
      const remaining = notices.filter((notice) => notice.noticeId !== selectedId);
      setNotices(remaining);
      setSelectedId(remaining[0]?.noticeId || null);
      setMessage(localizedMessage('admin:notice.deleted'));
      setConfirmingDelete(false);
    } catch (requestError) {
      setActionError(apiErrorMessage(requestError, localizedMessage('errors:notice.delete')));
    } finally {
      setSaving(false);
    }
  }

  return (
    <main className="page-shell admin-shell">
      <div className="judge-heading">
        <div><p className="eyebrow">{t('admin:notice.desk')}</p><h2>{t('admin:notice.title')}</h2><p>{t('common:operatorTournament', { value1: session.account.name, value2: tournamentId })}</p></div>
        <div className="operations-heading-actions"><button className="command-button" disabled={loading} onClick={loadNotices} type="button">{t('common:refresh')}</button><button className="secondary-button" onClick={onLogout} type="button">{t('common:signOut')}</button></div>
      </div>
      {actionError && <p aria-live="polite" className="form-error admin-action-message" role="alert">{resolveMessage(actionError)}</p>}
      {message && <p aria-live="polite" className="admin-success-message">{resolveMessage(message)}</p>}
      {loading ? <StatePanel title={t('admin:notice.loading')}>{t('common:wait')}</StatePanel> : null}
      {listError && !loading ? <StatePanel action={<button className="command-button" onClick={loadNotices} type="button">{t('common:retry')}</button>} title={t('errors:notice.list')} tone="error">{t('errors:tournamentPermissionRetry')}</StatePanel> : null}
      {!loading && !listError ? (
        <div className="admin-setup-layout">
          <aside className="admin-list-panel">
            <div className="operation-panel-heading"><div><p className="eyebrow">{t('common:notice')}</p><h3>{t('admin:notice.list')}</h3></div><span>{t('common:recordCount', { value1: notices.length })}</span></div>
            <button className={`admin-new-option${selectedId === null ? ' selected' : ''}`} onClick={() => setSelectedId(null)} type="button">{t('admin:notice.add')}</button>
            <div className="admin-entity-list">{notices.map((notice) => <button className={`admin-entity-option${selectedId === notice.noticeId ? ' selected' : ''}`} key={notice.noticeId} onClick={() => setSelectedId(notice.noticeId)} type="button"><strong>{notice.title}</strong><span>{t('admin:notice.statusOrder', { value1: notice.active ? t('common:published') : t('common:inactive'), value2: notice.displayOrder })}</span></button>)}</div>
            {!notices.length ? <p className="operation-empty">{t('admin:notice.empty')}</p> : null}
          </aside>
          <section className="admin-form-panel">
            <div className="operation-panel-heading"><div><p className="eyebrow">{selectedId ? t('admin:notice.identifier', { value1: selectedId }) : t('admin:notice.new')}</p><h3>{selectedId ? t('admin:notice.edit') : t('admin:notice.create')}</h3></div>{selectedId ? <span>#{selectedId}</span> : null}</div>
            <form onSubmit={handleSubmit}>
              <div className="admin-form-grid"><label>{t('common:title')}<input onChange={(event) => updateField('title', event.target.value)} required value={form.title} /></label><label>{t('common:displayOrder')}<input min="0" onChange={(event) => updateField('displayOrder', event.target.value)} type="number" value={form.displayOrder} /></label><label className="admin-wide-field">{t('common:content')}<textarea onChange={(event) => updateField('content', event.target.value)} required rows="8" value={form.content} /></label><label className="admin-toggle-field"><input checked={form.active} onChange={(event) => updateField('active', event.target.checked)} type="checkbox" />{t('common:visible')}</label></div>
              <div className="admin-form-actions"><button className="command-button" disabled={saving} type="submit">{selectedId ? t('admin:notice.save') : t('admin:notice.create')}</button>{selectedId ? <button className="danger-button" disabled={saving} onClick={() => setConfirmingDelete(true)} type="button">{t('admin:notice.delete')}</button> : null}</div>
            </form>
          </section>
        </div>
      ) : null}
      {confirmingDelete ? <ConfirmDialog busy={saving} description={t('admin:notice.deleteWarning')} onCancel={() => setConfirmingDelete(false)} onConfirm={handleDelete} title={t('admin:notice.deleteConfirm')} /> : null}
    </main>
  );
}

export function AdminNoticePage({ tournamentId }) {
  useTranslation();
  const { session, signOut } = useStaffAuth();
  if (!session || !['GAME_MANAGER', 'SERVICE_MANAGER'].includes(session.account.role)) return null;
  return <NoticeWorkspace onLogout={signOut} session={session} tournamentId={tournamentId} />;
}
