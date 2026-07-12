import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { login, logout } from '../api/auth';
import { createBout, deleteBout, getBouts, importBouts, updateBout } from '../api/adminBouts';
import { getAthletes } from '../api/adminAthletes';
import { getRings } from '../api/adminRings';
import { AdminBoutPage } from './AdminBoutPage';

vi.mock('../api/auth', () => ({
  login: vi.fn(),
  logout: vi.fn(),
}));

vi.mock('../api/adminBouts', () => ({
  createBout: vi.fn(),
  deleteBout: vi.fn(),
  getBouts: vi.fn(),
  importBouts: vi.fn(),
  updateBout: vi.fn(),
}));

vi.mock('../api/adminAthletes', () => ({
  getAthletes: vi.fn(),
}));

vi.mock('../api/adminRings', () => ({
  getRings: vi.fn(),
}));

const session = {
  accessToken: 'admin-token',
  account: { accountId: 50, name: 'Game Manager', role: 'GAME_MANAGER' },
};

const bout = { blueAthleteId: 11, boutId: 12, boutNumber: 1, eventBout: false, matchType: 'Final', redAthleteId: 10, ringId: 1, scheduledOrder: 1, status: 'SCHEDULED', totalRounds: 3, tournamentId: 1 };

beforeEach(() => {
  window.sessionStorage.clear();
  vi.clearAllMocks();
  getBouts.mockResolvedValue([bout]);
  getAthletes.mockResolvedValue([{ athleteId: 10, name: 'Red Boxer' }, { athleteId: 11, name: 'Blue Boxer' }]);
  getRings.mockResolvedValue([{ name: 'Ring A', ringId: 1 }]);
  createBout.mockResolvedValue({ ...bout, boutId: 13, boutNumber: 2 });
  updateBout.mockResolvedValue({ ...bout, boutNumber: 3, matchType: 'Semi Final' });
  deleteBout.mockResolvedValue(undefined);
  importBouts.mockResolvedValue({ boutIds: [13, 14], importedCount: 2 });
  logout.mockResolvedValue(undefined);
});

describe('AdminBoutPage', () => {
  it('signs in an admin and loads tournament bouts', async () => {
    login.mockResolvedValue(session);

    render(<AdminBoutPage tournamentId={1} />);
    fireEvent.change(screen.getByLabelText('아이디'), { target: { value: 'game01' } });
    fireEvent.change(screen.getByLabelText('비밀번호'), { target: { value: 'password' } });
    fireEvent.click(screen.getByRole('button', { name: '로그인' }));

    expect(await screen.findByRole('heading', { name: '대진 관리' })).toBeInTheDocument();
    expect(getBouts).toHaveBeenCalledWith(1, 'admin-token');
    expect(await screen.findByText('경기 1 · Final')).toBeInTheDocument();
  });

  it('creates a new bout and imports CSV rows', async () => {
    window.sessionStorage.setItem('boxing.operations.session', JSON.stringify(session));

    render(<AdminBoutPage tournamentId={1} />);
    expect(await screen.findByText('경기 1 · Final')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '+ 새 경기' }));
    fireEvent.change(screen.getByLabelText('링'), { target: { value: '1' } });
    fireEvent.change(screen.getByLabelText('경기 번호'), { target: { value: '2' } });
    fireEvent.change(screen.getByLabelText('빨강 선수'), { target: { value: '10' } });
    fireEvent.change(screen.getByLabelText('파랑 선수'), { target: { value: '11' } });
    fireEvent.change(screen.getByLabelText('진행 순서'), { target: { value: '2' } });
    fireEvent.click(screen.getByRole('button', { name: '경기 생성' }));
    await waitFor(() => expect(createBout).toHaveBeenCalledWith(expect.objectContaining({ boutNumber: 2, ringId: 1, tournamentId: 1 }), 'admin-token'));

    const csv = new File(['tournamentId,ringId'], 'bouts.csv', { type: 'text/csv' });
    fireEvent.change(screen.getByLabelText('CSV 파일'), { target: { files: [csv] } });
    fireEvent.click(screen.getByRole('button', { name: 'CSV 가져오기' }));
    await waitFor(() => expect(importBouts).toHaveBeenCalledWith(csv, 'admin-token'));
    expect(await screen.findByText('2건의 경기를 가져왔습니다.')).toBeInTheDocument();
  });

  it('downloads the CSV template', async () => {
    window.sessionStorage.setItem('boxing.operations.session', JSON.stringify(session));
    const originalCreateObjectUrl = URL.createObjectURL;
    const originalRevokeObjectUrl = URL.revokeObjectURL;
    const createObjectUrl = vi.fn().mockReturnValue('blob:template');
    const revokeObjectUrl = vi.fn();
    const clickAnchor = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
    URL.createObjectURL = createObjectUrl;
    URL.revokeObjectURL = revokeObjectUrl;

    render(<AdminBoutPage tournamentId={1} />);
    expect(await screen.findByText('경기 1 · Final')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'CSV 양식 다운로드' }));

    expect(createObjectUrl).toHaveBeenCalledWith(expect.any(Blob));
    expect(revokeObjectUrl).toHaveBeenCalledWith('blob:template');
    expect(clickAnchor).toHaveBeenCalled();
    URL.createObjectURL = originalCreateObjectUrl;
    URL.revokeObjectURL = originalRevokeObjectUrl;
    clickAnchor.mockRestore();
  });

  it('updates and deletes a selected bout', async () => {
    window.sessionStorage.setItem('boxing.operations.session', JSON.stringify(session));

    render(<AdminBoutPage tournamentId={1} />);
    expect(await screen.findByText('경기 1 · Final')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('경기 번호'), { target: { value: '3' } });
    fireEvent.change(screen.getByLabelText('경기 유형'), { target: { value: 'Semi Final' } });
    fireEvent.click(screen.getByRole('button', { name: '경기 저장' }));
    await waitFor(() => expect(updateBout).toHaveBeenCalledWith(12, expect.objectContaining({ boutNumber: 3, matchType: 'Semi Final' }), 'admin-token'));

    fireEvent.click(screen.getByRole('button', { name: '경기 삭제' }));
    await waitFor(() => expect(deleteBout).toHaveBeenCalledWith(12, 'admin-token'));
    expect(await screen.findByText('경기를 삭제했습니다.')).toBeInTheDocument();
  });
});
