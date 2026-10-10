import { useTranslation } from 'react-i18next';
import { translate as t } from '../i18n';
import { Link, useLocation } from 'react-router-dom';
import { useStaffAuth } from '../auth/StaffAuthContext';

export function AppFooter() {
  useTranslation();
  const { session } = useStaffAuth();
  const location = useLocation();
  if (session) return null;
  const returnTo = `${location.pathname}${location.search}`;
  return (
    <footer className="app-footer">
      <span>© 2026 LeeDoGwan. All rights reserved.</span>
      <Link to={`/staff/login?returnTo=${encodeURIComponent(returnTo)}`}>{t('common:staffLogin')}</Link>
    </footer>
  );
}
