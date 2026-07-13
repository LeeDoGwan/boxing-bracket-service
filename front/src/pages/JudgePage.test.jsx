import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { getBoutDetail, getBouts } from '../api/audience';
import { login, logout } from '../api/auth';
import { getJudgeScores, submitRoundScore } from '../api/judge';
import { JudgePage } from './JudgePage';

vi.mock('../api/audience', () => ({
  getBoutDetail: vi.fn(),
  getBouts: vi.fn(),
}));

vi.mock('../api/auth', () => ({
  login: vi.fn(),
  logout: vi.fn(),
}));

vi.mock('../api/judge', () => ({
  getJudgeScores: vi.fn(),
  submitRoundScore: vi.fn(),
}));

const session = {
  accessToken: 'token-1',
  account: { accountId: 7, name: 'Judge One', role: 'JUDGE' },
};

const bout = {
  blueAthlete: { affiliation: 'Blue Gym', name: 'Blue Boxer' },
  boutId: 12,
  boutNumber: 12,
  currentRound: 1,
  matchType: 'Final',
  redAthlete: { affiliation: 'Red Gym', name: 'Red Boxer' },
  status: 'IN_PROGRESS',
  totalRounds: 3,
};

beforeEach(() => {
  window.sessionStorage.clear();
  vi.clearAllMocks();
  getBouts.mockResolvedValue([bout]);
  getBoutDetail.mockResolvedValue(bout);
  getJudgeScores.mockResolvedValue([]);
  logout.mockResolvedValue(undefined);
});

describe('JudgePage', () => {
  it('signs in a judge and shows the score workspace', async () => {
    login.mockResolvedValue(session);

    render(<JudgePage tournamentId={1} />);
    fireEvent.change(screen.getByLabelText('아이디'), { target: { value: 'judge01' } });
    fireEvent.change(screen.getByLabelText('비밀번호'), { target: { value: 'password' } });
    fireEvent.click(screen.getByRole('button', { name: '로그인' }));

    expect(await screen.findByRole('heading', { name: '심판 점수 입력' })).toBeInTheDocument();
    expect(login).toHaveBeenCalledWith('judge01', 'password');
    expect(getBouts).toHaveBeenCalledWith(1);
  });

  it('submits a round score for the signed-in judge', async () => {
    window.sessionStorage.setItem('boxing.judge.session', JSON.stringify(session));
    submitRoundScore.mockResolvedValue({
      blueScore: 9,
      boutId: 12,
      judgeId: 7,
      redScore: 10,
      roundNo: 1,
      status: 'SUBMITTED',
    });

    render(<JudgePage tournamentId={1} />);
    expect(await screen.findByText('Red Boxer')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('레드 점수 1라운드'), { target: { value: '10' } });
    fireEvent.change(screen.getByLabelText('블루 점수 1라운드'), { target: { value: '9' } });
    fireEvent.click(screen.getByRole('button', { name: '1라운드 제출' }));

    await waitFor(() => expect(submitRoundScore).toHaveBeenCalledWith(
      12,
      1,
      { blueScore: 9, judgeId: 7, redScore: 10 },
      'token-1',
    ));
    expect(await screen.findByText('제출됨')).toBeInTheDocument();
  });

  it('shows an empty state when the tournament has no bouts', async () => {
    window.sessionStorage.setItem('boxing.judge.session', JSON.stringify(session));
    getBouts.mockResolvedValue([]);

    render(<JudgePage tournamentId={1} />);

    expect(await screen.findByText('선택할 경기가 없습니다.')).toBeInTheDocument();
    expect(screen.getByText('점수 입력 경기가 없습니다.')).toBeInTheDocument();
  });
});
