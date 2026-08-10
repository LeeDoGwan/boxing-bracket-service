import {
  STAFF_SESSION_KEY,
  StaffAuthProvider,
  clearStaffSession,
  readStaffSession,
  useStaffAuth,
  writeStaffSession,
} from './StaffAuthContext';

import { render, screen, waitFor } from '@testing-library/react';

vi.mock('../api/auth', () => ({
  getCurrentAccount: vi.fn(),
  logout: vi.fn(),
}));

import { getCurrentAccount } from '../api/auth';

const session = {
  accessToken: 'staff-token',
  account: { accountId: 10, name: 'Judge One', role: 'JUDGE' },
};

beforeEach(() => {
  window.sessionStorage.clear();
  vi.clearAllMocks();
});

describe('staff session storage', () => {
  it('writes a shared session and keeps the legacy role key compatible', () => {
    writeStaffSession(session);

    expect(JSON.parse(window.sessionStorage.getItem(STAFF_SESSION_KEY))).toEqual(session);
    expect(JSON.parse(window.sessionStorage.getItem('boxing.judge.session'))).toEqual(session);
    expect(readStaffSession()).toEqual(session);
  });

  it('recovers a valid session from a legacy role key', () => {
    window.sessionStorage.setItem('boxing.operations.session', JSON.stringify({
      ...session,
      account: { ...session.account, role: 'SERVICE_MANAGER' },
    }));

    expect(readStaffSession().account.role).toBe('SERVICE_MANAGER');
  });

  it('removes the shared and legacy sessions together', () => {
    writeStaffSession(session);

    clearStaffSession();

    expect(readStaffSession()).toBeNull();
    expect(window.sessionStorage.length).toBe(0);
  });
});

function SessionProbe() {
  const { isChecking, session } = useStaffAuth();
  return <output data-checking={isChecking} data-testid="session-probe">{session?.account.name || 'signed-out'}</output>;
}

describe('staff session validation', () => {
  it('refreshes a stored session with the current account', async () => {
    writeStaffSession(session);
    getCurrentAccount.mockResolvedValue({ ...session.account, name: 'Updated Judge' });

    render(<StaffAuthProvider><SessionProbe /></StaffAuthProvider>);

    expect(await screen.findByText('Updated Judge')).toBeInTheDocument();
    expect(getCurrentAccount).toHaveBeenCalledWith('staff-token');
    expect(JSON.parse(window.sessionStorage.getItem(STAFF_SESSION_KEY)).account.name).toBe('Updated Judge');
  });

  it('clears stored sessions when the current account cannot be loaded', async () => {
    writeStaffSession(session);
    getCurrentAccount.mockRejectedValue(new Error('Authentication required'));

    render(<StaffAuthProvider><SessionProbe /></StaffAuthProvider>);

    await waitFor(() => expect(screen.getByTestId('session-probe')).toHaveTextContent('signed-out'));
    expect(window.sessionStorage.length).toBe(0);
  });
});
