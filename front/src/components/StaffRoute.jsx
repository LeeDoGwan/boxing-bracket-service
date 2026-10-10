import { useTranslation } from 'react-i18next';
import { translate as t } from '../i18n';
import { Link, Navigate, useLocation } from 'react-router-dom';
import { useStaffAuth } from '../auth/StaffAuthContext';
import { StatePanel } from './StatePanel';

export function StaffRoute({ allowedRoles, children }) {
  useTranslation();
  const { isChecking, session } = useStaffAuth();
  const location = useLocation();
  if (isChecking) {
    return <main className="page-shell"><StatePanel title={t('common:auth.checking')}>{t('common:waitSession')}</StatePanel></main>;
  }
  if (!session) {
    const returnTo = `${location.pathname}${location.search}`;
    return <Navigate replace to={`/staff/login?returnTo=${encodeURIComponent(returnTo)}`} />;
  }
  if (allowedRoles && !allowedRoles.includes(session.account.role)) {
    return (
      <main className="page-shell">
        <StatePanel action={<Link className="command-button" to="/">{t('common:backToOverview')}</Link>} title={t('errors:auth.denied')} tone="error">{t('errors:auth.roleDenied')}</StatePanel>
      </main>
    );
  }
  return children;
}
