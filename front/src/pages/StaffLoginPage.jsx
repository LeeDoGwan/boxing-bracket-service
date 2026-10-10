import { useTranslation } from 'react-i18next';
import { translate as t, localizedMessage, resolveMessage } from '../i18n';
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
  useTranslation();
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
        ? localizedMessage('errors:auth.staffRequired')
        : localizedMessage('errors:auth.credentials'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="page-shell auth-shell staff-login-shell">
      <section className="auth-panel staff-login-panel">
        <p className="eyebrow">{t('common:staffOnly')}</p>
        <h2>{t('common:staffLogin')}</h2>
        <form onSubmit={handleSubmit}>
          <label htmlFor="staff-login-id">{t('common:userId')}<input autoComplete="username" id="staff-login-id" onChange={(event) => setLoginId(event.target.value)} required value={loginId} /></label>
          <label htmlFor="staff-login-password">{t('common:password')}<input autoComplete="current-password" id="staff-login-password" onChange={(event) => setPassword(event.target.value)} required type="password" value={password} /></label>
          {error && <p aria-live="polite" className="form-error" role="alert">{resolveMessage(error)}</p>}
          <button className="command-button" disabled={submitting} type="submit">{submitting ? t('common:signingIn') : t('common:signIn')}</button>
        </form>
        <p className="staff-login-note">{t('common:supportedRoles', { value1: Object.values(STAFF_ROLE_LABELS).map(resolveMessage).join(' · ') })}</p>
      </section>
    </main>
  );
}
