import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { createRing, deleteRing, getRings, updateRing } from '../api/adminRings';
import { useStaffAuth } from '../auth/StaffAuthContext';
import { AdminRingPage } from './AdminRingPage';

vi.mock('../auth/StaffAuthContext', () => ({ useStaffAuth: vi.fn() }));

vi.mock('../api/adminRings', () => ({
  createRing: vi.fn(),
  deleteRing: vi.fn(),
  getRings: vi.fn(),
  updateRing: vi.fn(),
}));

const session = {
  accessToken: 'admin-token',
  account: { accountId: 50, name: 'Game Manager', role: 'GAME_MANAGER' },
};

const ring = { currentBoutId: 12, name: 'Ring A', ringId: 1, status: 'READY', tournamentId: 1 };

beforeEach(() => {
  window.sessionStorage.clear();
  vi.clearAllMocks();
  useStaffAuth.mockReturnValue({ session, signOut: vi.fn() });
  getRings.mockResolvedValue([ring]);
  createRing.mockResolvedValue({ currentBoutId: null, name: 'Ring B', ringId: 2, status: 'READY', tournamentId: 1 });
  updateRing.mockResolvedValue({ ...ring, name: 'Main Ring', status: 'IN_PROGRESS' });
  deleteRing.mockResolvedValue(undefined);
});

describe('AdminRingPage', () => {
  it('loads rings for the tournament from the shared staff session', async () => {
    render(<AdminRingPage tournamentId={1} />);

    expect(await screen.findByRole('heading', { name: '링 관리' })).toBeInTheDocument();
    expect(getRings).toHaveBeenCalledWith(1, 'admin-token');
    expect(await screen.findByText('Ring A')).toBeInTheDocument();
  });

  it('creates a new ring', async () => {
    render(<AdminRingPage tournamentId={1} />);
    expect(await screen.findByText('Ring A')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '+ 새 링' }));
    fireEvent.change(screen.getByLabelText('링 이름'), { target: { value: 'Ring B' } });
    fireEvent.click(screen.getByRole('button', { name: '링 생성' }));

    await waitFor(() => expect(createRing).toHaveBeenCalledWith({ name: 'Ring B', status: 'READY', tournamentId: 1 }, 'admin-token'));
    expect(await screen.findByText('링을 생성했습니다.')).toBeInTheDocument();
  });

  it('updates and deletes a selected ring', async () => {
    render(<AdminRingPage tournamentId={1} />);
    expect(await screen.findByText('Ring A')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('링 이름'), { target: { value: 'Main Ring' } });
    fireEvent.change(screen.getByLabelText('상태'), { target: { value: 'IN_PROGRESS' } });
    fireEvent.click(screen.getByRole('button', { name: '링 저장' }));
    await waitFor(() => expect(updateRing).toHaveBeenCalledWith(1, { name: 'Main Ring', status: 'IN_PROGRESS', tournamentId: 1 }, 'admin-token'));

    fireEvent.click(screen.getByRole('button', { name: '링 삭제' }));
    fireEvent.click(screen.getByRole('button', { name: '확인' }));
    await waitFor(() => expect(deleteRing).toHaveBeenCalledWith(1, 'admin-token'));
    expect(await screen.findByText('링을 삭제했습니다.')).toBeInTheDocument();
  });
});
