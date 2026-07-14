import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { login, logout } from '../api/auth';
import { createNotice, deleteNotice, getNotices, updateNotice } from '../api/adminNotices';
import { AdminNoticePage } from './AdminNoticePage';

vi.mock('../api/auth', () => ({
  login: vi.fn(),
  logout: vi.fn(),
}));

vi.mock('../api/adminNotices', () => ({
  createNotice: vi.fn(),
  deleteNotice: vi.fn(),
  getNotices: vi.fn(),
  updateNotice: vi.fn(),
}));

const session = {
  accessToken: 'admin-token',
  account: { accountId: 50, name: 'Game Manager', role: 'GAME_MANAGER' },
};

const notice = { active: true, content: 'Bring your accreditation.', displayOrder: 1, noticeId: 20, title: 'Check-in notice', tournamentId: 1 };

beforeEach(() => {
  window.sessionStorage.clear();
  vi.clearAllMocks();
  getNotices.mockResolvedValue([notice]);
  createNotice.mockResolvedValue({ ...notice, noticeId: 21, title: 'Venue notice' });
  updateNotice.mockResolvedValue({ ...notice, active: false, content: 'Updated content.', title: 'Updated notice' });
  deleteNotice.mockResolvedValue(undefined);
  logout.mockResolvedValue(undefined);
});

describe('AdminNoticePage', () => {
  it('signs in an admin and loads notices for the tournament', async () => {
    login.mockResolvedValue(session);

    render(<AdminNoticePage tournamentId={1} />);
    fireEvent.change(screen.getByLabelText('아이디'), { target: { value: 'game01' } });
    fireEvent.change(screen.getByLabelText('비밀번호'), { target: { value: 'password' } });
    fireEvent.click(screen.getByRole('button', { name: '로그인' }));

    expect(await screen.findByRole('heading', { name: '공지 관리' })).toBeInTheDocument();
    expect(getNotices).toHaveBeenCalledWith(1, 'admin-token');
    expect(await screen.findByText('Check-in notice')).toBeInTheDocument();
  });

  it('creates a new notice with display order and active state', async () => {
    window.sessionStorage.setItem('boxing.operations.session', JSON.stringify(session));

    render(<AdminNoticePage tournamentId={1} />);
    expect(await screen.findByText('Check-in notice')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '+ 새 공지' }));
    fireEvent.change(screen.getByLabelText('제목'), { target: { value: 'Venue notice' } });
    fireEvent.change(screen.getByLabelText('내용'), { target: { value: 'Venue details.' } });
    fireEvent.change(screen.getByLabelText('표시 순서'), { target: { value: '2' } });
    fireEvent.click(screen.getByRole('button', { name: '공지 생성' }));

    await waitFor(() => expect(createNotice).toHaveBeenCalledWith({ active: true, content: 'Venue details.', displayOrder: 2, title: 'Venue notice', tournamentId: 1 }, 'admin-token'));
    expect(await screen.findByText('공지를 생성했습니다.')).toBeInTheDocument();
  });

  it('updates and deletes a selected notice', async () => {
    window.sessionStorage.setItem('boxing.operations.session', JSON.stringify(session));

    render(<AdminNoticePage tournamentId={1} />);
    expect(await screen.findByText('Check-in notice')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('제목'), { target: { value: 'Updated notice' } });
    fireEvent.change(screen.getByLabelText('내용'), { target: { value: 'Updated content.' } });
    fireEvent.click(screen.getByLabelText('공개 중'));
    fireEvent.click(screen.getByRole('button', { name: '공지 저장' }));
    await waitFor(() => expect(updateNotice).toHaveBeenCalledWith(20, { active: false, content: 'Updated content.', displayOrder: 1, title: 'Updated notice', tournamentId: 1 }, 'admin-token'));

    fireEvent.click(screen.getByRole('button', { name: '공지 삭제' }));
    await waitFor(() => expect(deleteNotice).toHaveBeenCalledWith(20, 'admin-token'));
    expect(await screen.findByText('공지를 삭제했습니다.')).toBeInTheDocument();
  });
});
