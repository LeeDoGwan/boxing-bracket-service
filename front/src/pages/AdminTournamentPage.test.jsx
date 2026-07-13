import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { login, logout } from '../api/auth';
import { createTournament, deleteTournament, getTournaments, updateTournament } from '../api/adminTournaments';
import { AdminTournamentPage } from './AdminTournamentPage';

vi.mock('../api/auth', () => ({
  login: vi.fn(),
  logout: vi.fn(),
}));

vi.mock('../api/adminTournaments', () => ({
  createTournament: vi.fn(),
  deleteTournament: vi.fn(),
  getTournaments: vi.fn(),
  updateTournament: vi.fn(),
}));

const session = {
  accessToken: 'admin-token',
  account: { accountId: 50, name: 'Game Manager', role: 'GAME_MANAGER' },
};

const tournament = {
  endDate: '2026-07-14',
  location: 'Seoul Gym',
  name: 'Summer Boxing Open',
  startDate: '2026-07-12',
  status: 'READY',
  tournamentId: 1,
};

beforeEach(() => {
  window.sessionStorage.clear();
  vi.clearAllMocks();
  getTournaments.mockResolvedValue([tournament]);
  createTournament.mockResolvedValue({ ...tournament, name: 'Winter Boxing Open', tournamentId: 2 });
  updateTournament.mockResolvedValue({ ...tournament, location: 'Busan Gym' });
  deleteTournament.mockResolvedValue(undefined);
  logout.mockResolvedValue(undefined);
});

describe('AdminTournamentPage', () => {
  it('signs in a game manager and loads tournaments', async () => {
    login.mockResolvedValue(session);

    render(<AdminTournamentPage />);
    fireEvent.change(screen.getByLabelText('아이디'), { target: { value: 'game01' } });
    fireEvent.change(screen.getByLabelText('비밀번호'), { target: { value: 'password' } });
    fireEvent.click(screen.getByRole('button', { name: '로그인' }));

    expect(await screen.findByRole('heading', { name: '대회 관리' })).toBeInTheDocument();
    expect(getTournaments).toHaveBeenCalledWith('admin-token');
    expect(await screen.findByText('Summer Boxing Open')).toBeInTheDocument();
  });

  it('creates a new tournament from the admin form', async () => {
    window.sessionStorage.setItem('boxing.operations.session', JSON.stringify(session));

    render(<AdminTournamentPage />);
    expect(await screen.findByText('Summer Boxing Open')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '+ 새 대회' }));
    fireEvent.change(screen.getByLabelText('대회명'), { target: { value: 'Winter Boxing Open' } });
    fireEvent.change(screen.getByLabelText('장소'), { target: { value: 'Busan Gym' } });
    fireEvent.click(screen.getByRole('button', { name: '대회 생성' }));

    await waitFor(() => expect(createTournament).toHaveBeenCalledWith({ endDate: null, location: 'Busan Gym', name: 'Winter Boxing Open', startDate: null, status: 'READY' }, 'admin-token'));
    expect(await screen.findByText('대회를 생성했습니다.')).toBeInTheDocument();
  });

  it('updates and deletes a selected tournament', async () => {
    window.sessionStorage.setItem('boxing.operations.session', JSON.stringify(session));

    render(<AdminTournamentPage />);
    expect(await screen.findByText('Summer Boxing Open')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('장소'), { target: { value: 'Busan Gym' } });
    fireEvent.click(screen.getByRole('button', { name: '대회 저장' }));
    await waitFor(() => expect(updateTournament).toHaveBeenCalledWith(1, expect.objectContaining({ location: 'Busan Gym' }), 'admin-token'));

    fireEvent.click(screen.getByRole('button', { name: '대회 삭제' }));
    await waitFor(() => expect(deleteTournament).toHaveBeenCalledWith(1, 'admin-token'));
    expect(await screen.findByText('대회를 삭제했습니다.')).toBeInTheDocument();
  });
});
