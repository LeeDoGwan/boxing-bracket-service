import { apiErrorMessage } from '../i18n/errors';
import { useTranslation } from 'react-i18next';
import { translate as t, localizedMessage, resolveMessage } from '../i18n';
import { useCallback, useEffect, useState } from 'react';
import { createAccount, deleteAccount, getAccounts, updateAccount } from '../api/adminAccounts';
import { useStaffAuth } from '../auth/StaffAuthContext';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { StatePanel } from '../components/StatePanel';

const ROLE_LABELS = { GAME_MANAGER: localizedMessage('common:role.gameManager'), JUDGE: localizedMessage('common:role.judge'), RING_MANAGER: localizedMessage('common:role.ringManager'), SERVICE_MANAGER: localizedMessage('common:role.serviceManager'), SUPERVISOR: localizedMessage('common:role.supervisor') };
const STATUS_LABELS = { ACTIVE: localizedMessage('common:active'), INACTIVE: localizedMessage('common:inactive') };

function blankForm() {
  return { loginId: '', name: '', passwordHash: '', role: 'JUDGE', status: 'ACTIVE' };
}

function blankFilters() {
  return { keyword: '', role: '', status: '' };
}

function formFromAccount(account) {
  return { loginId: account.loginId || '', name: account.name || '', passwordHash: '', role: account.role || 'JUDGE', status: account.status || 'ACTIVE' };
}

function AccountWorkspace({ onLogout, session }) {
  useTranslation();
  const [accounts, setAccounts] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [form, setForm] = useState(blankForm);
  const [filterForm, setFilterForm] = useState(blankFilters);
  const [filters, setFilters] = useState(blankFilters);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [listError, setListError] = useState('');
  const [actionError, setActionError] = useState('');
  const [message, setMessage] = useState('');
  const [confirmingDelete, setConfirmingDelete] = useState(false);

  const loadAccounts = useCallback(async () => {
    setLoading(true);
    setListError('');
    setActionError('');
    try {
      const nextAccounts = (await getAccounts(filters, session.accessToken)) || [];
      setAccounts(nextAccounts);
      setSelectedId((current) => current && nextAccounts.some((item) => item.accountId === current)
        ? current
        : nextAccounts[0]?.accountId || null);
    } catch {
      setListError(localizedMessage('errors:account.listPermission'));
    } finally {
      setLoading(false);
    }
  }, [filters, session.accessToken]);

  useEffect(() => {
    loadAccounts();
  }, [loadAccounts]);

  useEffect(() => {
    const selected = accounts.find((account) => account.accountId === selectedId);
    setForm(selected ? formFromAccount(selected) : blankForm());
  }, [accounts, selectedId]);

  function updateField(key, value) {
    setForm((current) => ({ ...current, [key]: value }));
    setMessage('');
  }

  function handleFilterSubmit(event) {
    event.preventDefault();
    setFilters({ ...filterForm, keyword: filterForm.keyword.trim() });
  }

  function handleFilterReset() {
    const nextFilters = blankFilters();
    setFilterForm(nextFilters);
    setFilters(nextFilters);
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setSaving(true);
    setActionError('');
    setMessage('');
    try {
      if (selectedId) {
        const updated = await updateAccount(selectedId, form, session.accessToken);
        setAccounts((current) => current.map((account) => account.accountId === selectedId ? updated : account));
        setMessage(localizedMessage('admin:account.updated'));
      } else {
        const created = await createAccount(form, session.accessToken);
        setAccounts((current) => [...current, created]);
        setSelectedId(created.accountId);
        setMessage(localizedMessage('admin:account.created'));
      }
    } catch (requestError) {
      setActionError(apiErrorMessage(requestError, localizedMessage('errors:account.save')));
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
      await deleteAccount(selectedId, session.accessToken);
      const remaining = accounts.filter((account) => account.accountId !== selectedId);
      setAccounts(remaining);
      setSelectedId(remaining[0]?.accountId || null);
      setMessage(localizedMessage('admin:account.deleted'));
      setConfirmingDelete(false);
    } catch (requestError) {
      setActionError(apiErrorMessage(requestError, localizedMessage('errors:account.delete')));
    } finally {
      setSaving(false);
    }
  }

  return (
    <main className="page-shell admin-shell">
      <div className="judge-heading">
        <div><p className="eyebrow">{t('admin:account.desk')}</p><h2>{t('admin:account.title')}</h2><p>{t('admin:account.operator', { value1: session.account.name })}</p></div>
        <div className="operations-heading-actions"><button className="command-button" disabled={loading} onClick={loadAccounts} type="button">{t('common:refresh')}</button><button className="secondary-button" onClick={onLogout} type="button">{t('common:signOut')}</button></div>
      </div>
      {actionError && <p aria-live="polite" className="form-error admin-action-message" role="alert">{resolveMessage(actionError)}</p>}
      {message && <p aria-live="polite" className="admin-success-message">{resolveMessage(message)}</p>}
      {loading ? <StatePanel title={t('admin:account.loading')}>{t('common:wait')}</StatePanel> : null}
      {listError && !loading ? <StatePanel action={<button className="command-button" onClick={loadAccounts} type="button">{t('common:retry')}</button>} title={t('errors:account.list')} tone="error">{t('errors:servicePermissionRetry')}</StatePanel> : null}
      {!loading && !listError ? (
        <div className="admin-setup-layout">
          <aside className="admin-list-panel">
            <form className="account-search" onSubmit={handleFilterSubmit}>
              <label>{t('common:search')}<input aria-label={t('admin:account.search')} onChange={(event) => setFilterForm((current) => ({ ...current, keyword: event.target.value }))} placeholder={t('admin:account.searchPlaceholder')} value={filterForm.keyword} /></label>
              <label>{t('common:roleFilter')}<select aria-label={t('common:roleFilter')} onChange={(event) => setFilterForm((current) => ({ ...current, role: event.target.value }))} value={filterForm.role}><option value="">{t('common:allRoles')}</option>{Object.entries(ROLE_LABELS).map(([value, label]) => <option key={value} value={value}>{resolveMessage(label)}</option>)}</select></label>
              <label>{t('common:statusFilter')}<select aria-label={t('common:statusFilter')} onChange={(event) => setFilterForm((current) => ({ ...current, status: event.target.value }))} value={filterForm.status}><option value="">{t('common:allStatuses')}</option>{Object.entries(STATUS_LABELS).map(([value, label]) => <option key={value} value={value}>{resolveMessage(label)}</option>)}</select></label>
              <div className="account-search-actions"><button className="command-button" type="submit">{t('common:applyFilters')}</button><button className="secondary-button" onClick={handleFilterReset} type="button">{t('common:reset')}</button></div>
            </form>
            <div className="operation-panel-heading"><div><p className="eyebrow">{t('admin:account.staff')}</p><h3>{t('admin:account.list')}</h3></div><span>{t('common:recordCount', { value1: accounts.length })}</span></div>
            <button className={`admin-new-option${selectedId === null ? ' selected' : ''}`} onClick={() => setSelectedId(null)} type="button">{t('admin:account.add')}</button>
            <div className="admin-entity-list">{accounts.map((account) => <button className={`admin-entity-option${selectedId === account.accountId ? ' selected' : ''}`} key={account.accountId} onClick={() => setSelectedId(account.accountId)} type="button"><strong>{account.loginId}</strong><span>{account.name} · {resolveMessage(ROLE_LABELS[account.role] || account.role)} · {resolveMessage(STATUS_LABELS[account.status] || account.status)}</span></button>)}</div>
            {!accounts.length ? <p className="operation-empty">{t('admin:account.empty')}</p> : null}
          </aside>
          <section className="admin-form-panel">
            <div className="operation-panel-heading"><div><p className="eyebrow">{selectedId ? t('admin:account.identifier', { value1: selectedId }) : t('admin:account.new')}</p><h3>{selectedId ? t('admin:account.edit') : t('admin:account.create')}</h3></div>{selectedId ? <span>#{selectedId}</span> : null}</div>
            <form onSubmit={handleSubmit}><div className="admin-form-grid"><label>{t('common:loginId')}<input autoComplete="username" onChange={(event) => updateField('loginId', event.target.value)} required value={form.loginId} /></label><label>{t('common:name')}<input onChange={(event) => updateField('name', event.target.value)} required value={form.name} /></label><label>{t('common:password')}<input autoComplete="new-password" onChange={(event) => updateField('passwordHash', event.target.value)} required type="password" value={form.passwordHash} /></label><label>{t('common:role')}<select onChange={(event) => updateField('role', event.target.value)} value={form.role}>{Object.entries(ROLE_LABELS).map(([value, label]) => <option key={value} value={value}>{resolveMessage(label)}</option>)}</select></label><label>{t('common:status')}<select onChange={(event) => updateField('status', event.target.value)} value={form.status}>{Object.entries(STATUS_LABELS).map(([value, label]) => <option key={value} value={value}>{resolveMessage(label)}</option>)}</select></label></div><div className="admin-form-actions"><button className="command-button" disabled={saving} type="submit">{selectedId ? t('admin:account.save') : t('admin:account.create')}</button>{selectedId ? <button className="danger-button" disabled={saving} onClick={() => setConfirmingDelete(true)} type="button">{t('admin:account.delete')}</button> : null}</div></form>
          </section>
        </div>
      ) : null}
      {confirmingDelete ? <ConfirmDialog busy={saving} description={t('admin:account.deleteWarning')} onCancel={() => setConfirmingDelete(false)} onConfirm={handleDelete} title={t('admin:account.deleteConfirm')} /> : null}
    </main>
  );
}

export function AdminAccountPage() {
  useTranslation();
  const { session, signOut } = useStaffAuth();
  if (!session || session.account.role !== 'SERVICE_MANAGER') return null;
  return <AccountWorkspace onLogout={signOut} session={session} />;
}
