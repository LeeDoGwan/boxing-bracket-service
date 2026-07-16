import {
  STAFF_SESSION_KEY,
  clearStaffSession,
  readStaffSession,
  writeStaffSession,
} from './StaffAuthContext';

const session = {
  accessToken: 'staff-token',
  account: { accountId: 10, name: 'Judge One', role: 'JUDGE' },
};

beforeEach(() => {
  window.sessionStorage.clear();
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
