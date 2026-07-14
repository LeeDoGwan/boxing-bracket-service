import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { login, logout } from '../api/auth';
import { getRingBouts, moveToNextBout, startBout, startRound, updateBoutStatus } from '../api/ringManager';
import { RingManagerPage } from './RingManagerPage';

vi.mock('../api/auth', () => ({
  login: vi.fn(),
  logout: vi.fn(),
}));

vi.mock('../api/ringManager', () => ({
  getRingBouts: vi.fn(),
  moveToNextBout: vi.fn(),
  startBout: vi.fn(),
  startRound: vi.fn(),
  updateBoutStatus: vi.fn(),
}));

const session = {
  accessToken: 'ring-token',
  account: { accountId: 40, name: 'Ring Manager One', role: 'RING_MANAGER' },
};

const bout = {
  boutId: 12,
  boutNumber: 12,
  currentRound: 0,
  matchType: 'Final',
  ringId: 1,
  scheduledOrder: 1,
  status: 'SCHEDULED',
};

beforeEach(() => {
  window.sessionStorage.clear();
  vi.clearAllMocks();
  getRingBouts.mockResolvedValue([bout]);
  startBout.mockResolvedValue({ ...bout, status: 'IN_PROGRESS' });
  startRound.mockResolvedValue({ ...bout, currentRound: 1, status: 'IN_PROGRESS' });
  updateBoutStatus.mockResolvedValue({ ...bout, status: 'READY' });
  moveToNextBout.mockResolvedValue({ ...bout, status: 'READY' });
  logout.mockResolvedValue(undefined);
});

describe('RingManagerPage', () => {
  it('signs in a ring manager and loads the selected ring', async () => {
    login.mockResolvedValue(session);

    render(<RingManagerPage />);
    fireEvent.change(screen.getByLabelText('아이디'), { target: { value: 'ring01' } });
    fireEvent.change(screen.getByLabelText('비밀번호'), { target: { value: 'password' } });
    fireEvent.click(screen.getByRole('button', { name: '로그인' }));

    expect(await screen.findByRole('heading', { name: '링 경기 운영' })).toBeInTheDocument();
    expect(login).toHaveBeenCalledWith('ring01', 'password');
    expect(getRingBouts).toHaveBeenCalledWith(1, 'ring-token');
  });

  it('runs ring start, round, status, and next-bout commands', async () => {
    window.sessionStorage.setItem('boxing.ring-manager.session', JSON.stringify(session));

    render(<RingManagerPage />);
    expect(await screen.findByRole('heading', { name: 'Final' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '경기 시작' }));
    await waitFor(() => expect(startBout).toHaveBeenCalledWith(12, 'ring-token'));

    fireEvent.change(screen.getByLabelText('시작할 라운드'), { target: { value: '1' } });
    fireEvent.click(screen.getByRole('button', { name: '라운드 시작' }));
    await waitFor(() => expect(startRound).toHaveBeenCalledWith(12, 1, 'ring-token'));

    fireEvent.change(screen.getByLabelText('경기 상태'), { target: { value: 'READY' } });
    fireEvent.click(screen.getByRole('button', { name: '상태 저장' }));
    await waitFor(() => expect(updateBoutStatus).toHaveBeenCalledWith(12, 'READY', 'ring-token'));

    fireEvent.click(screen.getByRole('button', { name: '다음 경기 준비' }));
    await waitFor(() => expect(moveToNextBout).toHaveBeenCalledWith(1, 'ring-token'));
  });
});
