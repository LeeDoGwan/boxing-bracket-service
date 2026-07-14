import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { getBoutDetail, getBouts } from '../api/audience';
import { login, logout } from '../api/auth';
import { confirmResult, createPenalty, getPenalties, getSupervisorScores } from '../api/supervisor';
import { SupervisorPage } from './SupervisorPage';

vi.mock('../api/audience', () => ({
  getBoutDetail: vi.fn(),
  getBouts: vi.fn(),
}));

vi.mock('../api/auth', () => ({
  login: vi.fn(),
  logout: vi.fn(),
}));

vi.mock('../api/supervisor', () => ({
  confirmResult: vi.fn(),
  createPenalty: vi.fn(),
  getPenalties: vi.fn(),
  getSupervisorScores: vi.fn(),
}));

const session = {
  accessToken: 'supervisor-token',
  account: { accountId: 20, name: 'Supervisor One', role: 'SUPERVISOR' },
};

const bout = {
  blueAthlete: { affiliation: 'Blue Gym', name: 'Blue Boxer' },
  boutId: 12,
  boutNumber: 12,
  currentRound: 3,
  matchType: 'Final',
  redAthlete: { affiliation: 'Red Gym', name: 'Red Boxer' },
  status: 'FINISHED',
  totalRounds: 3,
};

beforeEach(() => {
  window.sessionStorage.clear();
  vi.clearAllMocks();
  getBouts.mockResolvedValue([bout]);
  getBoutDetail.mockResolvedValue(bout);
  getSupervisorScores.mockResolvedValue([
    { blueScore: 9, boutId: 12, judgeId: 30, redScore: 10, roundNo: 1, status: 'SUBMITTED' },
    { blueScore: 10, boutId: 12, judgeId: 31, redScore: 9, roundNo: 1, status: 'SUBMITTED' },
  ]);
  getPenalties.mockResolvedValue([]);
  createPenalty.mockResolvedValue({ boutId: 12, penaltyId: 100, penaltyPoint: 1, reason: 'Warning', targetSide: 'BLUE' });
  confirmResult.mockResolvedValue({ blueTotalScore: 19, boutId: 12, decisionType: 'POINTS', redTotalScore: 19, winnerSide: 'RED' });
  logout.mockResolvedValue(undefined);
});

describe('SupervisorPage', () => {
  it('loads persisted penalties for the selected bout', async () => {
    window.sessionStorage.setItem('boxing.supervisor.session', JSON.stringify(session));
    getPenalties.mockResolvedValue([{ boutId: 12, penaltyId: 101, penaltyPoint: 2, reason: 'Persisted warning', targetSide: 'BLUE' }]);

    render(<SupervisorPage tournamentId={1} />);

    expect(await screen.findByText('Persisted warning')).toBeInTheDocument();
    expect(getPenalties).toHaveBeenCalledWith(12, 'supervisor-token');
  });

  it('signs in a supervisor and shows the review workspace', async () => {
    login.mockResolvedValue(session);

    render(<SupervisorPage tournamentId={1} />);
    fireEvent.change(screen.getByLabelText('아이디'), { target: { value: 'supervisor01' } });
    fireEvent.change(screen.getByLabelText('비밀번호'), { target: { value: 'password' } });
    fireEvent.click(screen.getByRole('button', { name: '로그인' }));

    expect(await screen.findByRole('heading', { name: '결과 검토 및 확정' })).toBeInTheDocument();
    expect(login).toHaveBeenCalledWith('supervisor01', 'password');
    expect(getBouts).toHaveBeenCalledWith(1);
  });

  it('adds a penalty and confirms the selected result', async () => {
    window.sessionStorage.setItem('boxing.supervisor.session', JSON.stringify(session));

    render(<SupervisorPage tournamentId={1} />);
    expect(await screen.findByText('Blue Boxer')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('점수'), { target: { value: '1' } });
    fireEvent.change(screen.getByLabelText('사유'), { target: { value: 'Warning' } });
    fireEvent.click(screen.getByRole('button', { name: '패널티 등록' }));

    await waitFor(() => expect(createPenalty).toHaveBeenCalledWith(
      12,
      { createdBy: 20, penaltyPoint: 1, reason: 'Warning', targetSide: 'RED' },
      'supervisor-token',
    ));
    expect(await screen.findByText('블루 -1')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: '결과 확정' }));
    await waitFor(() => expect(confirmResult).toHaveBeenCalledWith(
      12,
      { confirmedBy: 20, decisionType: 'POINTS', winnerSide: 'RED' },
      'supervisor-token',
    ));
    expect(await screen.findByText('레드 승')).toBeInTheDocument();
  });
});
