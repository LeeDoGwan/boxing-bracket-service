import { Link, Navigate, useLocation } from 'react-router-dom';
import { useStaffAuth } from '../auth/StaffAuthContext';
import { StatePanel } from './StatePanel';

export function StaffRoute({ allowedRoles, children }) {
  const { session } = useStaffAuth();
  const location = useLocation();
  if (!session) {
    const returnTo = `${location.pathname}${location.search}`;
    return <Navigate replace to={`/staff/login?returnTo=${encodeURIComponent(returnTo)}`} />;
  }
  if (allowedRoles && !allowedRoles.includes(session.account.role)) {
    return (
      <main className="page-shell">
        <StatePanel action={<Link className="command-button" to="/">현황으로 돌아가기</Link>} title="접근 권한이 없습니다." tone="error">
          현재 계정의 역할로는 이 화면을 사용할 수 없습니다.
        </StatePanel>
      </main>
    );
  }
  return children;
}
