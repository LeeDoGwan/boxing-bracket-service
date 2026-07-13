import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { login, logout } from '../api/auth';
import { getAuditLogs } from '../api/auditLogs';
import { AuditLogPage } from './AuditLogPage';

vi.mock('../api/auth', () => ({
  login: vi.fn(),
  logout: vi.fn(),
}));

vi.mock('../api/auditLogs', () => ({
  getAuditLogs: vi.fn(),
}));

const session = {
  accessToken: 'audit-token',
  account: { accountId: 50, name: 'Service Manager', role: 'SERVICE_MANAGER' },
};

const auditLog = {
  actionType: 'BOUT_STARTED',
  actorAccountId: 40,
  actorRole: 'RING_MANAGER',
  actorUsername: 'ring01',
  afterData: '{"status":"IN_PROGRESS"}',
  beforeData: '{"status":"READY"}',
  boutId: 12,
  createdAt: '2026-07-12T10:20:30',
  id: 100,
  success: true,
  targetId: 12,
  targetType: 'BOUT',
};

const pageResponse = {
  content: [auditLog],
  page: 0,
  size: 20,
  totalElements: 21,
  totalPages: 2,
};

beforeEach(() => {
  window.sessionStorage.clear();
  vi.clearAllMocks();
  getAuditLogs.mockResolvedValue(pageResponse);
  logout.mockResolvedValue(undefined);
});

describe('AuditLogPage', () => {
  it('signs in an operations manager and loads masked audit records', async () => {
    login.mockResolvedValue(session);

    render(<AuditLogPage tournamentId={1} />);
    fireEvent.change(screen.getByLabelText('아이디'), { target: { value: 'service01' } });
    fireEvent.change(screen.getByLabelText('비밀번호'), { target: { value: 'password' } });
    fireEvent.click(screen.getByRole('button', { name: '로그인' }));

    expect(await screen.findByRole('heading', { name: '관리자 감사 로그' })).toBeInTheDocument();
    expect(getAuditLogs).toHaveBeenCalledWith(expect.objectContaining({ page: 0, size: 20, tournamentId: '1' }), 'audit-token');
    expect((await screen.findAllByText('경기 시작')).length).toBeGreaterThanOrEqual(2);
    fireEvent.click(screen.getByText('변경 데이터'));
    expect(await screen.findByText(/"status": "IN_PROGRESS"/)).toBeInTheDocument();
  });

  it('applies filters and moves to the next page', async () => {
    window.sessionStorage.setItem('boxing.operations.session', JSON.stringify(session));
    getAuditLogs.mockResolvedValueOnce(pageResponse).mockResolvedValueOnce({ ...pageResponse, page: 0, totalPages: 2 }).mockResolvedValueOnce({ ...pageResponse, page: 1 });

    render(<AuditLogPage tournamentId={1} />);
    expect(await screen.findByRole('heading', { name: '관리자 감사 로그' })).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('행위자 역할'), { target: { value: 'RING_MANAGER' } });
    fireEvent.change(screen.getByLabelText('행위 유형'), { target: { value: 'BOUT_STARTED' } });
    fireEvent.click(screen.getByRole('button', { name: '필터 적용' }));
    await waitFor(() => expect(getAuditLogs).toHaveBeenCalledWith(expect.objectContaining({ actionType: 'BOUT_STARTED', actorRole: 'RING_MANAGER', page: 0 }), 'audit-token'));
    fireEvent.click(screen.getByRole('button', { name: '다음' }));
    await waitFor(() => expect(getAuditLogs).toHaveBeenCalledWith(expect.objectContaining({ page: 1 }), 'audit-token'));
  });

  it('shows an API error and retries the query', async () => {
    window.sessionStorage.setItem('boxing.operations.session', JSON.stringify(session));
    getAuditLogs.mockRejectedValueOnce(new Error('FORBIDDEN')).mockResolvedValueOnce(pageResponse);

    render(<AuditLogPage tournamentId={1} />);
    expect(await screen.findByRole('heading', { name: '감사 로그를 불러오지 못했습니다.' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '다시 시도' }));
    await waitFor(() => expect(getAuditLogs).toHaveBeenCalledTimes(2));
    expect((await screen.findAllByText('경기 시작')).length).toBeGreaterThanOrEqual(2);
  });
});
