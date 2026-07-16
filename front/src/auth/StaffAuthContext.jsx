import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { logout } from '../api/auth';

export const STAFF_SESSION_KEY = 'boxing.staff.session';
const LEGACY_SESSION_KEYS = {
  JUDGE: 'boxing.judge.session',
  SUPERVISOR: 'boxing.supervisor.session',
  RING_MANAGER: 'boxing.ring-manager.session',
  GAME_MANAGER: 'boxing.operations.session',
  SERVICE_MANAGER: 'boxing.operations.session',
};
export const STAFF_ROLES = Object.keys(LEGACY_SESSION_KEYS);
export const STAFF_ROLE_LABELS = {
  JUDGE: '심판',
  SUPERVISOR: '감독관',
  RING_MANAGER: '링 매니저',
  GAME_MANAGER: '게임 매니저',
  SERVICE_MANAGER: '서비스 매니저',
};

function parseSession(value) {
  try {
    const session = value ? JSON.parse(value) : null;
    return session?.accessToken && STAFF_ROLES.includes(session?.account?.role) ? session : null;
  } catch {
    return null;
  }
}

export function readStaffSession() {
  const shared = parseSession(window.sessionStorage.getItem(STAFF_SESSION_KEY));
  if (shared) return shared;
  for (const key of Object.values(LEGACY_SESSION_KEYS)) {
    const session = parseSession(window.sessionStorage.getItem(key));
    if (session) return session;
  }
  return null;
}

export function writeStaffSession(session) {
  const serialized = JSON.stringify(session);
  window.sessionStorage.setItem(STAFF_SESSION_KEY, serialized);
  const legacyKey = LEGACY_SESSION_KEYS[session.account.role];
  if (legacyKey) window.sessionStorage.setItem(legacyKey, serialized);
  window.dispatchEvent(new Event('boxing:staff-session-changed'));
}

export function clearStaffSession() {
  window.sessionStorage.removeItem(STAFF_SESSION_KEY);
  Object.values(LEGACY_SESSION_KEYS).forEach((key) => window.sessionStorage.removeItem(key));
  window.dispatchEvent(new Event('boxing:staff-session-changed'));
}

const StaffAuthContext = createContext(null);

export function StaffAuthProvider({ children }) {
  const [session, setSession] = useState(readStaffSession);

  useEffect(() => {
    const syncSession = () => setSession(readStaffSession());
    const clearSession = () => {
      clearStaffSession();
      setSession(null);
    };
    window.addEventListener('boxing:staff-session-changed', syncSession);
    window.addEventListener('boxing:staff-logout', clearSession);
    return () => {
      window.removeEventListener('boxing:staff-session-changed', syncSession);
      window.removeEventListener('boxing:staff-logout', clearSession);
    };
  }, []);

  const value = useMemo(() => ({
    session,
    signIn: (nextSession) => {
      writeStaffSession(nextSession);
      setSession(nextSession);
    },
    signOut: async () => {
      const token = session?.accessToken;
      clearStaffSession();
      setSession(null);
      if (token) await logout(token).catch(() => undefined);
    },
  }), [session]);

  return <StaffAuthContext.Provider value={value}>{children}</StaffAuthContext.Provider>;
}

export function useStaffAuth() {
  const context = useContext(StaffAuthContext);
  if (!context) throw new Error('useStaffAuth must be used within StaffAuthProvider');
  return context;
}
