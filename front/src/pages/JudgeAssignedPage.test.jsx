import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { getBoutDetail } from '../api/audience';
import { getJudgeScores, submitRoundScore } from '../api/judge';
import { getAssignedBouts, getAssignedRings } from '../api/staffAssignments';
import { useBoutEventStream } from '../hooks/useBoutEventStream';
import { JudgeAssignedPage } from './JudgeAssignedPage';

vi.mock('../api/audience', () => ({ getBoutDetail: vi.fn() }));
vi.mock('../api/judge', () => ({ getJudgeScores: vi.fn(), submitRoundScore: vi.fn() }));
vi.mock('../api/staffAssignments', () => ({ getAssignedBouts: vi.fn(), getAssignedRings: vi.fn() }));
vi.mock('../hooks/useBoutEventStream', () => ({ useBoutEventStream: vi.fn(() => 'connected') }));

const session = { accessToken: 'judge-token', account: { accountId: 7, name: 'Judge', role: 'JUDGE' } };
const ring = { name: 'Ring A', ringId: 4 };
const bout = { boutId: 12, boutNumber: 12, currentRound: 1, redAthlete: { name: 'Red' }, blueAthlete: { name: 'Blue' }, status: 'IN_PROGRESS', totalRounds: 1 };

beforeEach(() => {
  vi.clearAllMocks();
  useBoutEventStream.mockReturnValue('connected');
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
    fireEvent.click(screen.getByRole('button', { name: 'Confirm score' }));
    await waitFor(() => expect(submitRoundScore).toHaveBeenCalledWith(12, 1, { blueScore: 9, redScore: 10 }, 'judge-token'));
    expect(submitRoundScore.mock.calls[0][2]).not.toHaveProperty('judgeId');
  });

  it('requires confirmation and preserves input when confirmation is cancelled', async () => {
    render(<JudgeAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);
    expect(await screen.findByText('Red vs Blue')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Red score'), { target: { value: '10' } });
    fireEvent.change(screen.getByLabelText('Blue score'), { target: { value: '9' } });
    fireEvent.click(screen.getByRole('button', { name: 'Submit round' }));
    expect(screen.getByRole('dialog', { name: 'Confirm round 1 score' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(screen.getByLabelText('Red score')).toHaveValue(10);
    expect(screen.getByLabelText('Blue score')).toHaveValue(9);
    expect(submitRoundScore).not.toHaveBeenCalled();
  });

  it('shows validation error for negative or non-integer input', async () => {
    render(<JudgeAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);
    expect(await screen.findByText('Red vs Blue')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Red score'), { target: { value: '-1' } });
    fireEvent.change(screen.getByLabelText('Blue score'), { target: { value: '9.5' } });
    fireEvent.click(screen.getByRole('button', { name: 'Submit round' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Scores must be non-negative whole numbers.');
    expect(submitRoundScore).not.toHaveBeenCalled();
  });

  it('keeps an unsubmitted score while a live refresh completes', async () => {
    render(<JudgeAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);
    expect(await screen.findByText('Red vs Blue')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Red score'), { target: { value: '10' } });
    fireEvent.change(screen.getByLabelText('Blue score'), { target: { value: '9' } });
    const streamOptions = useBoutEventStream.mock.calls.slice(-1)[0][1];

    act(() => streamOptions.onEvent({ boutId: 12, eventType: 'ROUND_STARTED' }));

    await waitFor(() => expect(getAssignedBouts).toHaveBeenCalledTimes(2));
    expect(screen.getByLabelText('Red score')).toHaveValue(10);
    expect(screen.getByLabelText('Blue score')).toHaveValue(9);
  });

  it('marks the current round and disables future round submission', async () => {
    getBoutDetail.mockResolvedValue({ ...bout, totalRounds: 3 });
    render(<JudgeAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);
    expect(await screen.findByText('Red vs Blue')).toBeInTheDocument();
    expect(screen.getByText('Current round')).toBeInTheDocument();
    expect(screen.getAllByText('Not started')).toHaveLength(2);
    expect(screen.getAllByRole('button', { name: 'Submit round' })[1]).toBeDisabled();
  });

  it('sends only one request when confirmation is double-clicked', async () => {
    let resolveSubmission;
    submitRoundScore.mockImplementation(() => new Promise((resolve) => { resolveSubmission = resolve; }));
    render(<JudgeAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);
    expect(await screen.findByText('Red vs Blue')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Red score'), { target: { value: '10' } });
    fireEvent.change(screen.getByLabelText('Blue score'), { target: { value: '9' } });
    fireEvent.click(screen.getByRole('button', { name: 'Submit round' }));
    const confirmButton = screen.getByRole('button', { name: 'Confirm score' });
    fireEvent.click(confirmButton);
    fireEvent.click(confirmButton);
    expect(submitRoundScore).toHaveBeenCalledTimes(1);
    await act(async () => resolveSubmission({ boutId: 12, blueScore: 9, redScore: 10, roundNo: 1, status: 'SUBMITTED' }));
  });

  it('refreshes the selected bout after a live status event', async () => {
    render(<JudgeAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);
    expect(await screen.findByText('Red vs Blue')).toBeInTheDocument();
    const streamOptions = useBoutEventStream.mock.calls.slice(-1)[0][1];

    act(() => streamOptions.onEvent({ boutId: 12, eventType: 'RESULT_CONFIRMED' }));

    await waitFor(() => expect(getAssignedBouts).toHaveBeenCalledTimes(2));
    expect(getBoutDetail).toHaveBeenCalledTimes(2);
    expect(getJudgeScores).toHaveBeenCalledTimes(2);
  });
});
