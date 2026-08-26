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
      setListError('공지 목록을 불러오지 못했습니다. 대회 ID와 권한을 확인해 주세요.');
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
        setMessage('공지를 수정했습니다.');
      } else {
        const created = await createNotice(payload, session.accessToken);
        setNotices((current) => [...current, created].sort((left, right) => (left.displayOrder - right.displayOrder) || (left.noticeId - right.noticeId)));
        setSelectedId(created.noticeId);
        setMessage('공지를 생성했습니다.');
      }
    } catch (requestError) {
      setActionError(requestError.message || '공지 저장에 실패했습니다.');
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
      setMessage('공지를 삭제했습니다.');
      setConfirmingDelete(false);
    } catch (requestError) {
      setActionError(requestError.message || '공지 삭제에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <main className="page-shell admin-shell">
      <div className="judge-heading">
        <div><p className="eyebrow">NOTICE ADMIN DESK</p><h2>공지 관리</h2><p>로그인 계정: {session.account.name} · 대회 ID {tournamentId}</p></div>
        <div className="operations-heading-actions"><button className="command-button" disabled={loading} onClick={loadNotices} type="button">새로고침</button><button className="secondary-button" onClick={onLogout} type="button">로그아웃</button></div>
      </div>
      {actionError && <p aria-live="polite" className="form-error admin-action-message" role="alert">{actionError}</p>}
      {message && <p aria-live="polite" className="admin-success-message">{message}</p>}
      {loading ? <StatePanel title="공지 목록을 불러오는 중입니다.">잠시만 기다려 주세요.</StatePanel> : null}
      {listError && !loading ? <StatePanel action={<button className="command-button" onClick={loadNotices} type="button">다시 시도</button>} title="공지 목록을 불러오지 못했습니다." tone="error">대회 ID와 운영자 권한을 확인한 뒤 다시 시도해 주세요.</StatePanel> : null}
      {!loading && !listError ? (
        <div className="admin-setup-layout">
          <aside className="admin-list-panel">
            <div className="operation-panel-heading"><div><p className="eyebrow">NOTICES</p><h3>공지 목록</h3></div><span>{notices.length}건</span></div>
            <button className={`admin-new-option${selectedId === null ? ' selected' : ''}`} onClick={() => setSelectedId(null)} type="button">+ 새 공지</button>
            <div className="admin-entity-list">{notices.map((notice) => <button className={`admin-entity-option${selectedId === notice.noticeId ? ' selected' : ''}`} key={notice.noticeId} onClick={() => setSelectedId(notice.noticeId)} type="button"><strong>{notice.title}</strong><span>{notice.active ? '게시 중' : '비활성'} · 순서 {notice.displayOrder}</span></button>)}</div>
            {!notices.length ? <p className="operation-empty">등록된 공지가 없습니다.</p> : null}
          </aside>
          <section className="admin-form-panel">
            <div className="operation-panel-heading"><div><p className="eyebrow">{selectedId ? `NOTICE ${selectedId}` : 'NEW NOTICE'}</p><h3>{selectedId ? '공지 정보 수정' : '공지 생성'}</h3></div>{selectedId ? <span>#{selectedId}</span> : null}</div>
            <form onSubmit={handleSubmit}>
              <div className="admin-form-grid"><label>제목<input onChange={(event) => updateField('title', event.target.value)} required value={form.title} /></label><label>표시 순서<input min="0" onChange={(event) => updateField('displayOrder', event.target.value)} type="number" value={form.displayOrder} /></label><label className="admin-wide-field">내용<textarea onChange={(event) => updateField('content', event.target.value)} required rows="8" value={form.content} /></label><label className="admin-toggle-field"><input checked={form.active} onChange={(event) => updateField('active', event.target.checked)} type="checkbox" /> 공개 중</label></div>
              <div className="admin-form-actions"><button className="command-button" disabled={saving} type="submit">{selectedId ? '공지 저장' : '공지 생성'}</button>{selectedId ? <button className="danger-button" disabled={saving} onClick={() => setConfirmingDelete(true)} type="button">공지 삭제</button> : null}</div>
            </form>
          </section>
        </div>
      ) : null}
      {confirmingDelete ? <ConfirmDialog busy={saving} description="선택한 공지를 삭제합니다. 이 작업은 되돌릴 수 없습니다." onCancel={() => setConfirmingDelete(false)} onConfirm={handleDelete} title="공지 삭제 확인" /> : null}
    </main>
  );
}

export function AdminNoticePage({ tournamentId }) {
  const { session, signOut } = useStaffAuth();
  if (!session || !['GAME_MANAGER', 'SERVICE_MANAGER'].includes(session.account.role)) return null;
  return <NoticeWorkspace onLogout={signOut} session={session} tournamentId={tournamentId} />;
}
