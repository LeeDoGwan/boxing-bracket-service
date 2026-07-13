import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { login, logout } from '../api/auth';
import { getOperationStatus } from '../api/operations';
import { OperationsPage } from './OperationsPage';

vi.mock('../api/auth', () => ({
  login: vi.fn(),
  logout: vi.fn(),
}));

vi.mock('../api/operations', () => ({
  getOperationStatus: vi.fn(),
}));

const session = {
  accessToken: 'operations-token',
  account: { accountId: 50, name: 'Service Manager', role: 'SERVICE_MANAGER' },
};

const operationStatus = {
  boutStatusCounts: { CANCELED: 0, FINISHED: 2, IN_PROGRESS: 1, READY: 1, SCHEDULED: 1, SCORING: 0 },
  judgeScoreSubmissionStatuses: [{ boutId: 12, boutNumber: 12, complete: false, roundNo: 2, submittedJudgeIds: [30], unsubmittedJudgeIds: [31, 32] }],
  pendingResultBouts: [{ boutId: 15, boutNumber: 15, ringId: 2, status: 'FINISHED' }],
  rings: [{ currentBout: { boutId: 12, boutNumber: 12, currentRound: 2, matchType: 'Semi Final', resultConfirmed: false, status: 'IN_PROGRESS', totalRounds: 3 }, nextBout: { boutId: 13, boutNumber: 13, currentRound: 0, matchType: 'Final', resultConfirmed: false, status: 'READY', totalRounds: 3 }, ringId: 1, ringName: 'Ring A', ringStatus: 'IN_PROGRESS' }],
  stalledBouts: [{ boutId: 12, boutNumber: 12, ringId: 1, status: 'IN_PROGRESS' }],
  totalBoutCount: 5,
  tournamentId: 1,
};

beforeEach(() => {
  window.sessionStorage.clear();
  vi.clearAllMocks();
  getOperationStatus.mockResolvedValue(operationStatus);
  logout.mockResolvedValue(undefined);
});

describe('OperationsPage', () => {
  it('signs in an operations manager and loads the tournament summary', async () => {
    login.mockResolvedValue(session);

    render(<OperationsPage tournamentId={1} />);
    fireEvent.change(screen.getByLabelText('아이디'), { target: { value: 'service01' } });
    fireEvent.change(screen.getByLabelText('비밀번호'), { target: { value: 'password' } });
    fireEvent.click(screen.getByRole('button', { name: '로그인' }));

    expect(await screen.findByRole('heading', { name: '대회 운영 현황' })).toBeInTheDocument();
    expect(login).toHaveBeenCalledWith('service01', 'password');
    expect(getOperationStatus).toHaveBeenCalledWith(1, 'operations-token');
    expect(await screen.findByText('지연 경기')).toBeInTheDocument();
  });

  it('refreshes the summary and retries after an API failure', async () => {
    window.sessionStorage.setItem('boxing.operations.session', JSON.stringify(session));
    getOperationStatus.mockRejectedValueOnce(new Error('FORBIDDEN')).mockResolvedValueOnce(operationStatus);

    render(<OperationsPage tournamentId={1} />);
    expect(await screen.findByRole('heading', { name: '운영 현황을 불러오지 못했습니다.' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '다시 시도' }));
    await waitFor(() => expect(getOperationStatus).toHaveBeenCalledTimes(2));
    expect(await screen.findByRole('heading', { name: '경기 진행 요약' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '새로고침' }));
    await waitFor(() => expect(getOperationStatus).toHaveBeenCalledTimes(3));
  });
});
