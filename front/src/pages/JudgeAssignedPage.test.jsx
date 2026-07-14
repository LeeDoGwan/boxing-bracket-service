import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { getBoutDetail } from '../api/audience';
import { getJudgeScores, submitRoundScore } from '../api/judge';
import { getAssignedBouts, getAssignedRings } from '../api/staffAssignments';
import { JudgeAssignedPage } from './JudgeAssignedPage';

vi.mock('../api/audience', () => ({ getBoutDetail: vi.fn() }));
vi.mock('../api/judge', () => ({ getJudgeScores: vi.fn(), submitRoundScore: vi.fn() }));
vi.mock('../api/staffAssignments', () => ({ getAssignedBouts: vi.fn(), getAssignedRings: vi.fn() }));

const session = { accessToken: 'judge-token', account: { accountId: 7, name: 'Judge', role: 'JUDGE' } };
const ring = { name: 'Ring A', ringId: 4 };
const bout = { boutId: 12, boutNumber: 12, currentRound: 1, redAthlete: { name: 'Red' }, blueAthlete: { name: 'Blue' }, status: 'IN_PROGRESS', totalRounds: 1 };

beforeEach(() => {
  vi.clearAllMocks();
  getAssignedRings.mockResolvedValue([ring]);
  getAssignedBouts.mockResolvedValue([{ boutId: 12, boutNumber: 12, matchType: 'Final', ringId: 4, status: 'IN_PROGRESS' }]);
  getBoutDetail.mockResolvedValue(bout);
  getJudgeScores.mockResolvedValue([]);
  submitRoundScore.mockResolvedValue({ boutId: 12, blueScore: 9, redScore: 10, roundNo: 1, status: 'SUBMITTED' });
});

describe('JudgeAssignedPage', () => {
  it('shows an explicit empty state when no ring is assigned', async () => {
    getAssignedRings.mockResolvedValue([]);
    render(<JudgeAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);
    expect(await screen.findByText('No assigned rings')).toBeInTheDocument();
    expect(getAssignedBouts).not.toHaveBeenCalled();
  });

  it('loads assigned bouts and does not send judgeId in the score payload', async () => {
    render(<JudgeAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);
    expect(await screen.findByText('Red vs Blue')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Red score'), { target: { value: '10' } });
    fireEvent.change(screen.getByLabelText('Blue score'), { target: { value: '9' } });
    fireEvent.click(screen.getByRole('button', { name: 'Submit round' }));
    await waitFor(() => expect(submitRoundScore).toHaveBeenCalledWith(12, 1, { blueScore: 9, redScore: 10 }, 'judge-token'));
    expect(submitRoundScore.mock.calls[0][2]).not.toHaveProperty('judgeId');
  });
});
