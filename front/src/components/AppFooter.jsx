import { Link, useLocation } from 'react-router-dom';
import { useStaffAuth } from '../auth/StaffAuthContext';

export function AppFooter() {
  const { session } = useStaffAuth();
  const location = useLocation();
  if (session) return null;
  const returnTo = `${location.pathname}${location.search}`;
  return (
    <footer className="app-footer">
      <Link to={`/staff/login?returnTo=${encodeURIComponent(returnTo)}`}>스태프 로그인</Link>
    </footer>
  );
}
