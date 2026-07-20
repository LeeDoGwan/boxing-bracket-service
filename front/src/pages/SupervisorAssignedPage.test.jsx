import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { getBoutDetail } from '../api/audience';
import { confirmResult, createPenalty, getPenalties, getSupervisorScores } from '../api/supervisor';
import { getAssignedBouts, getAssignedRings } from '../api/staffAssignments';
import { useBoutEventStream } from '../hooks/useBoutEventStream';
import { SupervisorAssignedPage } from './SupervisorAssignedPage';

vi.mock('../api/audience', () => ({ getBoutDetail: vi.fn() }));
vi.mock('../api/supervisor', () => ({ confirmResult: vi.fn(), createPenalty: vi.fn(), getPenalties: vi.fn(), getSupervisorScores: vi.fn() }));
vi.mock('../api/staffAssignments', () => ({ getAssignedBouts: vi.fn(), getAssignedRings: vi.fn() }));
vi.mock('../hooks/useBoutEventStream', () => ({ useBoutEventStream: vi.fn(() => 'connected') }));

const session = { accessToken: 'supervisor-token', account: { accountId: 8, name: 'Supervisor', role: 'SUPERVISOR' } };
const bout = { boutId: 12, boutNumber: 12, redAthlete: { name: 'Red' }, blueAthlete: { name: 'Blue' }, status: 'IN_PROGRESS' };

beforeEach(() => {
  vi.clearAllMocks();
  useBoutEventStream.mockReturnValue('connected');
  getAssignedRings.mockResolvedValue([{ name: 'Ring A', ringId: 4 }]);
  getAssignedBouts.mockResolvedValue([{ boutId: 12, boutNumber: 12, ringId: 4, status: 'IN_PROGRESS' }]);
  getBoutDetail.mockResolvedValue(bout);
  getSupervisorScores.mockResolvedValue([]);
  getPenalties.mockResolvedValue([]);
  createPenalty.mockResolvedValue({ penaltyId: 1, penaltyPoint: 1, reason: 'hold', targetSide: 'RED' });
  confirmResult.mockResolvedValue(undefined);
});

describe('SupervisorAssignedPage', () => {
  it('loads the assigned ring and bout APIs before reviewing a bout', async () => {
    render(<SupervisorAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);

    expect(await screen.findByText('Red vs Blue')).toBeInTheDocument();
    expect(getAssignedRings).toHaveBeenCalledWith(1, 'supervisor-token');
    expect(getAssignedBouts).toHaveBeenCalledWith(4, 'supervisor-token');
  });

  it('refreshes scores and penalties without clearing an in-progress penalty input', async () => {
    render(<SupervisorAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);
    expect(await screen.findByText('Red vs Blue')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Reason'), { target: { value: 'hold' } });
    const streamOptions = useBoutEventStream.mock.calls.slice(-1)[0][1];

    act(() => streamOptions.onEvent({ boutId: 12, eventType: 'SCORE_SUBMITTED' }));

    await waitFor(() => expect(getSupervisorScores).toHaveBeenCalledTimes(2));
    expect(getPenalties).toHaveBeenCalledTimes(2);
    expect(screen.getByLabelText('Reason')).toHaveValue('hold');
  });

  it('shows draft scores and prevents result confirmation until scores are submitted', async () => {
    getSupervisorScores.mockResolvedValue([
      { blueScore: 9, judgeId: 10, redScore: 10, roundNo: 1, status: 'SUBMITTED' },
      { blueScore: null, judgeId: 11, redScore: null, roundNo: 1, status: 'DRAFT' },
    ]);

    render(<SupervisorAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);

    expect(await screen.findByText('Submitted 1 · Draft 1')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Review result' })).toBeDisabled();
  });

  it('blocks zero penalty points without calling the API', async () => {
    render(<SupervisorAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);
    await screen.findByText('Red vs Blue');

    fireEvent.change(screen.getByLabelText('Points'), { target: { value: '0' } });
    fireEvent.click(screen.getByRole('button', { name: 'Add penalty' }));

    expect(await screen.findByText('Penalty points must be a positive whole number.')).toBeInTheDocument();
    expect(createPenalty).not.toHaveBeenCalled();
  });

  it('keeps result selections when the confirmation is cancelled', async () => {
    getSupervisorScores.mockResolvedValue([{ blueScore: 9, judgeId: 10, redScore: 10, roundNo: 1, status: 'SUBMITTED' }]);
    render(<SupervisorAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);
    await screen.findByText('Red vs Blue');

    fireEvent.change(screen.getByLabelText('Winner'), { target: { value: 'BLUE' } });
    fireEvent.change(screen.getByLabelText('Decision'), { target: { value: 'KO' } });
    fireEvent.click(screen.getByRole('button', { name: 'Review result' }));
    expect(screen.getByRole('dialog', { name: 'Confirm result' })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(screen.getByLabelText('Winner')).toHaveValue('BLUE');
    expect(screen.getByLabelText('Decision')).toHaveValue('KO');
    expect(confirmResult).not.toHaveBeenCalled();
  });

  it('sends the authenticated action without client actor IDs', async () => {
    getSupervisorScores.mockResolvedValue([{ blueScore: 9, judgeId: 10, redScore: 10, roundNo: 1, status: 'SUBMITTED' }]);
    render(<SupervisorAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);
    await screen.findByText('Red vs Blue');

    fireEvent.change(screen.getByLabelText('Reason'), { target: { value: 'hold' } });
    fireEvent.change(screen.getByLabelText('Points'), { target: { value: '1' } });
    fireEvent.click(screen.getByRole('button', { name: 'Add penalty' }));
    await waitFor(() => expect(createPenalty).toHaveBeenCalledWith(
      12,
      { penaltyPoint: 1, reason: 'hold', roundNo: 1, targetSide: 'RED' },
      'supervisor-token',
    ));

    fireEvent.click(screen.getByRole('button', { name: 'Review result' }));
    fireEvent.click(screen.getByRole('button', { name: 'Confirm result' }));
    await waitFor(() => expect(confirmResult).toHaveBeenCalledWith(
      12,
      { decisionType: 'POINTS', winnerSide: 'RED' },
      'supervisor-token',
    ));
  });

  it('calls result confirmation once when the confirm action is double clicked', async () => {
    getSupervisorScores.mockResolvedValue([{ blueScore: 9, judgeId: 10, redScore: 10, roundNo: 1, status: 'SUBMITTED' }]);
    let resolveRequest;
    confirmResult.mockImplementation(() => new Promise((resolve) => { resolveRequest = resolve; }));
    render(<SupervisorAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);
    await screen.findByText('Red vs Blue');

    fireEvent.click(screen.getByRole('button', { name: 'Review result' }));
    const confirmButton = screen.getByRole('button', { name: 'Confirm result' });
    fireEvent.click(confirmButton);
    fireEvent.click(confirmButton);

    expect(confirmResult).toHaveBeenCalledTimes(1);
    resolveRequest({ decisionType: 'POINTS', winnerSide: 'RED' });
    await waitFor(() => expect(screen.getByText('Result confirmed. Further changes are locked.')).toBeInTheDocument());
  });

  it('locks penalty actions when a result confirmation event refreshes the bout', async () => {
    getSupervisorScores.mockResolvedValue([{ blueScore: 9, judgeId: 10, redScore: 10, roundNo: 1, status: 'SUBMITTED' }]);
    render(<SupervisorAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);
    await screen.findByText('Red vs Blue');
    getBoutDetail.mockResolvedValue({ ...bout, result: { decisionType: 'POINTS', winnerSide: 'RED' }, resultConfirmed: true, status: 'FINISHED' });
    const streamOptions = useBoutEventStream.mock.calls.slice(-1)[0][1];

    act(() => streamOptions.onEvent({ boutId: 12, eventType: 'RESULT_CONFIRMED' }));

    await waitFor(() => expect(screen.getByText('Result confirmed. Further changes are locked.')).toBeInTheDocument());
    expect(screen.getByRole('button', { name: 'Add penalty' })).toBeDisabled();
  });
});
