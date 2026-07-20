import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { login, logout } from '../api/auth';
import { createAthlete, deleteAthlete, getAthletes, updateAthlete } from '../api/adminAthletes';
import { AdminAthletePage } from './AdminAthletePage';

vi.mock('../api/auth', () => ({
  login: vi.fn(),
  logout: vi.fn(),
}));

vi.mock('../api/adminAthletes', () => ({
  createAthlete: vi.fn(),
  deleteAthlete: vi.fn(),
  getAthletes: vi.fn(),
  updateAthlete: vi.fn(),
}));

const session = {
  accessToken: 'admin-token',
  account: { accountId: 50, name: 'Game Manager', role: 'GAME_MANAGER' },
};

const athlete = { affiliation: 'Red Gym', athleteId: 10, name: 'Red Boxer' };

beforeEach(() => {
  window.sessionStorage.clear();
  vi.clearAllMocks();
  getAthletes.mockResolvedValue([athlete]);
  createAthlete.mockResolvedValue({ affiliation: 'Blue Gym', athleteId: 11, name: 'Blue Boxer' });
  updateAthlete.mockResolvedValue({ ...athlete, affiliation: 'Main Gym', name: 'Main Boxer' });
  deleteAthlete.mockResolvedValue(undefined);
  logout.mockResolvedValue(undefined);
});

describe('AdminAthletePage', () => {
  it('signs in an admin and loads athletes', async () => {
    login.mockResolvedValue(session);

    render(<AdminAthletePage />);
    fireEvent.change(screen.getByLabelText('아이디'), { target: { value: 'game01' } });
    fireEvent.change(screen.getByLabelText('비밀번호'), { target: { value: 'password' } });
    fireEvent.click(screen.getByRole('button', { name: '로그인' }));

    expect(await screen.findByRole('heading', { name: '선수 관리' })).toBeInTheDocument();
    expect(getAthletes).toHaveBeenCalledWith('', 'admin-token');
    expect(await screen.findByText('Red Boxer')).toBeInTheDocument();
  });

  it('searches and creates a new athlete', async () => {
    window.sessionStorage.setItem('boxing.operations.session', JSON.stringify(session));

    render(<AdminAthletePage />);
    expect(await screen.findByText('Red Boxer')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('선수 검색'), { target: { value: 'Blue' } });
    fireEvent.click(screen.getByRole('button', { name: '검색' }));
    await waitFor(() => expect(getAthletes).toHaveBeenCalledWith('Blue', 'admin-token'));
    fireEvent.click(screen.getByRole('button', { name: '+ 새 선수' }));
    fireEvent.change(screen.getByLabelText('선수명'), { target: { value: 'Blue Boxer' } });
    fireEvent.change(screen.getByLabelText('소속'), { target: { value: 'Blue Gym' } });
    fireEvent.click(screen.getByRole('button', { name: '선수 생성' }));

    await waitFor(() => expect(createAthlete).toHaveBeenCalledWith({ affiliation: 'Blue Gym', name: 'Blue Boxer' }, 'admin-token'));
    expect(await screen.findByText('선수를 생성했습니다.')).toBeInTheDocument();
  });

  it('updates and deletes a selected athlete', async () => {
    window.sessionStorage.setItem('boxing.operations.session', JSON.stringify(session));

    render(<AdminAthletePage />);
    expect(await screen.findByText('Red Boxer')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('선수명'), { target: { value: 'Main Boxer' } });
    fireEvent.change(screen.getByLabelText('소속'), { target: { value: 'Main Gym' } });
    fireEvent.click(screen.getByRole('button', { name: '선수 저장' }));
    await waitFor(() => expect(updateAthlete).toHaveBeenCalledWith(10, { affiliation: 'Main Gym', name: 'Main Boxer' }, 'admin-token'));

    fireEvent.click(screen.getByRole('button', { name: '선수 삭제' }));
    fireEvent.click(screen.getByRole('button', { name: '확인' }));
    await waitFor(() => expect(deleteAthlete).toHaveBeenCalledWith(10, 'admin-token'));
    expect(await screen.findByText('선수를 삭제했습니다.')).toBeInTheDocument();
  });
});
