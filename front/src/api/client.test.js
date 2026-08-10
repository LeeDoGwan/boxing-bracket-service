import { requestApi } from './client';

function unauthorizedResponse() {
  return {
    json: async () => ({ message: 'Authentication required', success: false }),
    ok: false,
    status: 401,
  };
}

describe('requestApi session expiry handling', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('clears the shared staff session when an authenticated request expires', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(unauthorizedResponse()));
    const logoutListener = vi.fn();
    window.addEventListener('boxing:staff-logout', logoutListener);

    await expect(requestApi('/api/protected', { token: 'expired-token' }))
      .rejects.toMatchObject({ message: 'Authentication required', status: 401 });

    expect(logoutListener).toHaveBeenCalledTimes(1);
    window.removeEventListener('boxing:staff-logout', logoutListener);
  });

  it('does not clear staff sessions for public 401 responses', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(unauthorizedResponse()));
    const logoutListener = vi.fn();
    window.addEventListener('boxing:staff-logout', logoutListener);

    await expect(requestApi('/api/public', {})).rejects.toMatchObject({ status: 401 });

    expect(logoutListener).not.toHaveBeenCalled();
    window.removeEventListener('boxing:staff-logout', logoutListener);
  });
});
