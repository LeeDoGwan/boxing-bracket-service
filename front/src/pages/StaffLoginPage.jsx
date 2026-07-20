import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { login } from '../api/auth';
import { STAFF_ROLE_LABELS, STAFF_ROLES, useStaffAuth } from '../auth/StaffAuthContext';

const DEFAULT_ROUTES = {
  GAME_MANAGER: '/operations',
  JUDGE: '/judge',
  RING_MANAGER: '/ring-manager',
  SERVICE_MANAGER: '/operations',
  SUPERVISOR: '/supervisor',
};

function safeReturnPath(value) {
  return value?.startsWith('/') && !value.startsWith('//') ? value : null;
}

export function StaffLoginPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { session, signIn } = useStaffAuth();
  const [loginId, setLoginId] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const destination = safeReturnPath(searchParams.get('returnTo')) || (session ? DEFAULT_ROUTES[session.account.role] : null) || '/';

  useEffect(() => {
    if (session) navigate(destination, { replace: true });
  }, [destination, navigate, session]);

  if (session) return null;

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const nextSession = await login(loginId, password);
      if (!STAFF_ROLES.includes(nextSession.account?.role)) throw new Error('STAFF_ROLE_REQUIRED');
      signIn(nextSession);
      navigate(safeReturnPath(searchParams.get('returnTo')) || DEFAULT_ROUTES[nextSession.account.role] || '/', { replace: true });
    } catch (requestError) {
      setError(requestError.message === 'STAFF_ROLE_REQUIRED'
        ? '운영 권한이 있는 계정으로 로그인해 주세요.'
        : '아이디 또는 비밀번호를 확인해 주세요.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="page-shell auth-shell staff-login-shell">
      <section className="auth-panel staff-login-panel">
        <p className="eyebrow">STAFF ACCESS</p>
        <h2>스태프 로그인</h2>
        <p>로그인 후 담당 역할과 링에 맞는 운영 화면으로 이동합니다.</p>
        <form onSubmit={handleSubmit}>
          <label htmlFor="staff-login-id">아이디<input autoComplete="username" id="staff-login-id" onChange={(event) => setLoginId(event.target.value)} required value={loginId} /></label>
          <label htmlFor="staff-login-password">비밀번호<input autoComplete="current-password" id="staff-login-password" onChange={(event) => setPassword(event.target.value)} required type="password" value={password} /></label>
          {error && <p aria-live="polite" className="form-error" role="alert">{error}</p>}
          <button className="command-button" disabled={submitting} type="submit">{submitting ? '로그인 중...' : '로그인'}</button>
        </form>
        <p className="staff-login-note">지원 역할: {Object.values(STAFF_ROLE_LABELS).join(' · ')}</p>
      </section>
    </main>
  );
}
