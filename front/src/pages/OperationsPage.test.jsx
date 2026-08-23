import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { getOperationStatus } from '../api/operations';
import { useStaffAuth } from '../auth/StaffAuthContext';
import { OperationsPage } from './OperationsPage';

vi.mock('../auth/StaffAuthContext', () => ({ useStaffAuth: vi.fn() }));

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
  useStaffAuth.mockReturnValue({ session, signOut: vi.fn() });
  getOperationStatus.mockResolvedValue(operationStatus);
});

describe('OperationsPage', () => {
  it('loads the tournament summary from the shared staff session', async () => {
    render(<OperationsPage tournamentId={1} />);

    expect(await screen.findByRole('heading', { name: '대회 운영 현황' })).toBeInTheDocument();
    expect(getOperationStatus).toHaveBeenCalledWith(1, 'operations-token');
    expect(await screen.findByText('지연 경기')).toBeInTheDocument();
  });

  it('refreshes the summary and retries after an API failure', async () => {
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
