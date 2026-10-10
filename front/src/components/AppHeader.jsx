import { useTranslation } from 'react-i18next';
import { translate as t, resolveMessage } from '../i18n';
import { NavLink } from 'react-router-dom';
import { LanguageSelector } from './LanguageSelector';
import { STAFF_ROLE_LABELS, useStaffAuth } from '../auth/StaffAuthContext';

export function AppHeader({ tournamentId }) {
  useTranslation();
  const { session, signOut } = useStaffAuth();
  const publicLinks = [
    ['/', t('common:nav.home')],
    ['/bracket', t('common:nav.bracket')],
  ];
  const staffLinks = {
    JUDGE: [['/judge', t('operations:judge.title')]],
    SUPERVISOR: [['/supervisor', t('operations:supervisor.title')]],
    RING_MANAGER: [['/ring-manager', t('operations:ring.title')]],
    GAME_MANAGER: [
      ['/operations', t('common:nav.operations')], ['/audit-logs', t('common:nav.audit')],
      ['/admin/tournaments', t('admin:tournament.title')], ['/admin/rings', t('admin:ring.title')],
      ['/admin/athletes', t('admin:athlete.title')], ['/admin/notices', t('admin:notice.title')],
      ['/admin/schedules', t('admin:schedule.title')], ['/admin/bouts', t('admin:bout.title')],
      ['/admin/assignments', t('admin:assignment.title')],
    ],
    SERVICE_MANAGER: [
      ['/operations', t('common:nav.operations')], ['/audit-logs', t('common:nav.audit')],
      ['/admin/tournaments', t('admin:tournament.title')], ['/admin/rings', t('admin:ring.title')],
      ['/admin/athletes', t('admin:athlete.title')], ['/admin/notices', t('admin:notice.title')],
      ['/admin/schedules', t('admin:schedule.title')], ['/admin/bouts', t('admin:bout.title')],
      ['/admin/accounts', t('admin:account.title')], ['/admin/assignments', t('admin:assignment.title')],
    ],
  };
  const operationLinks = session ? staffLinks[session.account.role] || [] : [];
  return (
    <header className={`app-header${session ? ' is-staff' : ' is-public'}`}>
      <div className="brand-block">
        <div className="brand-lockup">
          <span aria-hidden="true" className="brand-mark">BX</span>
          <div>
            <p className="eyebrow">{t('common:brand.live')}</p>
            <h1>{t('common:brand.title')}</h1>
            <p className="brand-context">{t('common:tournamentNumber', { value1: tournamentId })}</p>
          </div>
        </div>
      </div>
      <nav aria-label={t('common:nav.public')} className="main-nav">
        {publicLinks.map(([path, label]) => (
          <NavLink end={path === '/'} key={path} to={`${path}?tournamentId=${tournamentId}`}>{resolveMessage(label)}</NavLink>
        ))}
      </nav>
      <LanguageSelector />
      {session && <details className="operations-menu">
        <summary>{t('common:nav.staff')}</summary>
        <nav aria-label={t('common:nav.staff')} className="operations-menu-panel">
          {operationLinks.map(([path, label]) => <NavLink key={path} to={`${path}?tournamentId=${tournamentId}`}>{resolveMessage(label)}</NavLink>)}
        </nav>
      </details>}
      {session && <div className="staff-session">
        <span><b>{session.account.name}</b><small>{t('common:roleTournament', { value1: STAFF_ROLE_LABELS[session.account.role], value2: tournamentId })}</small></span>
        <button aria-label={t('common:signOut')} className="header-logout" onClick={signOut} type="button">{t('common:signOut')}</button>
      </div>}
    </header>
  );
}
