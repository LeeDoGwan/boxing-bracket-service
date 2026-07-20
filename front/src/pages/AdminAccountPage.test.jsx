import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { login, logout } from '../api/auth';
import { createAccount, deleteAccount, getAccounts, updateAccount } from '../api/adminAccounts';
import { AdminAccountPage } from './AdminAccountPage';

vi.mock('../api/auth', () => ({
  login: vi.fn(),
  logout: vi.fn(),
}));

vi.mock('../api/adminAccounts', () => ({
  createAccount: vi.fn(),
  deleteAccount: vi.fn(),
  getAccounts: vi.fn(),
  updateAccount: vi.fn(),
}));

const session = {
  accessToken: 'service-token',
  account: { accountId: 50, name: 'Service Manager', role: 'SERVICE_MANAGER' },
};

const account = { accountId: 40, loginId: 'judge01', name: 'Judge One', role: 'JUDGE', status: 'ACTIVE' };
const ringAccount = { accountId: 41, loginId: 'ring01', name: 'Ring One', role: 'RING_MANAGER', status: 'INACTIVE' };

beforeEach(() => {
  window.sessionStorage.clear();
  vi.clearAllMocks();
  getAccounts.mockResolvedValue([account]);
  createAccount.mockResolvedValue({ accountId: 41, loginId: 'ring01', name: 'Ring One', role: 'RING_MANAGER', status: 'ACTIVE' });
  updateAccount.mockResolvedValue({ ...account, name: 'Judge Updated', status: 'INACTIVE' });
  deleteAccount.mockResolvedValue(undefined);
  logout.mockResolvedValue(undefined);
});

describe('AdminAccountPage', () => {
  it('requires a service manager and loads accounts without password data', async () => {
    login.mockResolvedValue(session);

    render(<AdminAccountPage />);
    fireEvent.change(screen.getByLabelText('아이디'), { target: { value: 'service01' } });
    fireEvent.change(screen.getByLabelText('비밀번호'), { target: { value: 'password' } });
    fireEvent.click(screen.getByRole('button', { name: '로그인' }));

    expect(await screen.findByRole('heading', { name: '계정 관리' })).toBeInTheDocument();
    expect(getAccounts).toHaveBeenCalledWith({ keyword: '', role: '', status: '' }, 'service-token');
    expect(await screen.findByText('judge01')).toBeInTheDocument();
  });

  it('filters accounts by keyword, role, and status', async () => {
    window.sessionStorage.setItem('boxing.operations.session', JSON.stringify(session));
    getAccounts.mockResolvedValueOnce([account]).mockResolvedValueOnce([ringAccount]);

    render(<AdminAccountPage />);
    expect(await screen.findByText('judge01')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Account search'), { target: { value: 'ring' } });
    fireEvent.change(screen.getByLabelText('Role filter'), { target: { value: 'RING_MANAGER' } });
    fireEvent.change(screen.getByLabelText('Status filter'), { target: { value: 'INACTIVE' } });
    fireEvent.click(screen.getByRole('button', { name: 'Filter' }));

    await waitFor(() => expect(getAccounts).toHaveBeenCalledWith(
      { keyword: 'ring', role: 'RING_MANAGER', status: 'INACTIVE' },
      'service-token'
    ));
    expect(await screen.findByText('ring01')).toBeInTheDocument();
  });

  it('creates a new role account', async () => {
    window.sessionStorage.setItem('boxing.operations.session', JSON.stringify(session));

    render(<AdminAccountPage />);
    expect(await screen.findByText('judge01')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '+ 새 계정' }));
    fireEvent.change(screen.getByLabelText('로그인 ID'), { target: { value: 'ring01' } });
    fireEvent.change(screen.getByLabelText('이름'), { target: { value: 'Ring One' } });
    fireEvent.change(screen.getByLabelText('비밀번호'), { target: { value: 'secret' } });
    fireEvent.change(screen.getByLabelText('역할'), { target: { value: 'RING_MANAGER' } });
    fireEvent.click(screen.getByRole('button', { name: '계정 생성' }));

    await waitFor(() => expect(createAccount).toHaveBeenCalledWith({ loginId: 'ring01', name: 'Ring One', passwordHash: 'secret', role: 'RING_MANAGER', status: 'ACTIVE' }, 'service-token'));
    expect(await screen.findByText('계정을 생성했습니다.')).toBeInTheDocument();
  });

  it('updates and deletes a selected account', async () => {
    window.sessionStorage.setItem('boxing.operations.session', JSON.stringify(session));

    render(<AdminAccountPage />);
    expect(await screen.findByText('judge01')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('이름'), { target: { value: 'Judge Updated' } });
    fireEvent.change(screen.getByLabelText('비밀번호'), { target: { value: 'new-secret' } });
    fireEvent.change(screen.getByLabelText('상태'), { target: { value: 'INACTIVE' } });
    fireEvent.click(screen.getByRole('button', { name: '계정 저장' }));
    await waitFor(() => expect(updateAccount).toHaveBeenCalledWith(40, { loginId: 'judge01', name: 'Judge Updated', passwordHash: 'new-secret', role: 'JUDGE', status: 'INACTIVE' }, 'service-token'));

    fireEvent.click(screen.getByRole('button', { name: '계정 삭제' }));
    fireEvent.click(screen.getByRole('button', { name: '확인' }));
    await waitFor(() => expect(deleteAccount).toHaveBeenCalledWith(40, 'service-token'));
    expect(await screen.findByText('계정을 삭제했습니다.')).toBeInTheDocument();
  });
});
