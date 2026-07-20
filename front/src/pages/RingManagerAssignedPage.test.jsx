import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { moveToNextBout, startBout, startRound, updateBoutStatus } from '../api/ringManager';
import { getAssignedBouts, getAssignedRings } from '../api/staffAssignments';
import { useBoutEventStream } from '../hooks/useBoutEventStream';
import { RingManagerAssignedPage } from './RingManagerAssignedPage';

vi.mock('../api/auth', () => ({ login: vi.fn(), logout: vi.fn() }));
vi.mock('../api/ringManager', () => ({ moveToNextBout: vi.fn(), startBout: vi.fn(), startRound: vi.fn(), updateBoutStatus: vi.fn() }));
vi.mock('../api/staffAssignments', () => ({ getAssignedBouts: vi.fn(), getAssignedRings: vi.fn() }));
vi.mock('../hooks/useBoutEventStream', () => ({ useBoutEventStream: vi.fn(() => 'connected') }));

const session = { accessToken: 'ring-manager-token', account: { accountId: 9, name: 'Manager', role: 'RING_MANAGER' } };

const assignedRing = { currentBoutId: 12, name: 'Ring A', ringId: 4, status: 'IN_PROGRESS' };
const bout = { boutId: 12, boutNumber: 12, currentRound: 1, matchType: 'Final', ringId: 4, scheduledOrder: 1, status: 'IN_PROGRESS', totalRounds: 3 };

beforeEach(() => {
  vi.clearAllMocks();
  useBoutEventStream.mockReturnValue('connected');
  getAssignedRings.mockResolvedValue([assignedRing]);
  getAssignedBouts.mockResolvedValue([bout]);
  moveToNextBout.mockResolvedValue({ ...bout, status: 'READY' });
  startBout.mockResolvedValue({ ...bout, status: 'IN_PROGRESS' });
  startRound.mockResolvedValue({ ...bout, currentRound: 2 });
  updateBoutStatus.mockResolvedValue({ ...bout, status: 'SCORING' });
});

describe('RingManagerAssignedPage', () => {
  it('loads assigned rings and does not expose arbitrary ring or status inputs', async () => {
    render(<RingManagerAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);

    expect(await screen.findByRole('heading', { name: 'Bout 12' })).toBeInTheDocument();
    expect(screen.getByLabelText('Assigned ring')).toBeInTheDocument();
    expect(screen.queryByLabelText('Ring ID')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Status')).not.toBeInTheDocument();
    expect(getAssignedRings).toHaveBeenCalledWith(1, 'ring-manager-token');
    expect(getAssignedBouts).toHaveBeenCalledWith(4, 'ring-manager-token');
  });

  it('only enables the next round in sequence', async () => {
    render(<RingManagerAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);

    const roundInput = await screen.findByLabelText('Next round');
    await waitFor(() => expect(roundInput).toHaveValue(2));
    fireEvent.change(roundInput, { target: { value: '3' } });
    expect(screen.getByRole('button', { name: 'Start round' })).toBeDisabled();
    fireEvent.change(roundInput, { target: { value: '2' } });
    fireEvent.click(screen.getByRole('button', { name: 'Start round' }));

    await waitFor(() => expect(startRound).toHaveBeenCalledWith(12, 2, 'ring-manager-token'));
  });

  it('requires confirmation before starting a ready bout and preserves cancellation', async () => {
    getAssignedRings.mockResolvedValue([{ ...assignedRing, currentBoutId: 12 }]);
    getAssignedBouts.mockResolvedValue([{ ...bout, currentRound: 0, status: 'READY' }]);

    render(<RingManagerAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);
    fireEvent.click(await screen.findByRole('button', { name: 'Start bout' }));
    expect(screen.getByRole('dialog', { name: 'Confirm ring operation' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(screen.queryByRole('dialog', { name: 'Confirm ring operation' })).not.toBeInTheDocument();
    expect(startBout).not.toHaveBeenCalled();
  });

  it('calls start once when the confirmed start action is double-clicked', async () => {
    getAssignedRings.mockResolvedValue([{ ...assignedRing, currentBoutId: 12 }]);
    getAssignedBouts.mockResolvedValue([{ ...bout, currentRound: 0, status: 'READY' }]);

    render(<RingManagerAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);
    fireEvent.click(await screen.findByRole('button', { name: 'Start bout' }));
    const confirmButton = screen.getByRole('button', { name: 'Confirm' });
    fireEvent.click(confirmButton);
    fireEvent.click(confirmButton);

    await waitFor(() => expect(startBout).toHaveBeenCalledTimes(1));
  });

  it('uses the server-selected next bout and confirms cancellation for scheduled bouts', async () => {
    getAssignedRings.mockResolvedValue([{ ...assignedRing, currentBoutId: null, status: 'READY' }]);
    getAssignedBouts.mockResolvedValue([{ ...bout, currentRound: 0, status: 'SCHEDULED' }]);

    render(<RingManagerAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);
    fireEvent.click(await screen.findByRole('button', { name: 'Prepare next bout' }));
    fireEvent.click(screen.getByRole('button', { name: 'Confirm' }));
    await waitFor(() => expect(moveToNextBout).toHaveBeenCalledWith(4, 'ring-manager-token'));

    fireEvent.click(screen.getByRole('button', { name: 'Cancel bout' }));
    fireEvent.click(screen.getByRole('button', { name: 'Confirm' }));
    await waitFor(() => expect(updateBoutStatus).toHaveBeenCalledWith(12, 'CANCELED', 'ring-manager-token'));
  });

  it('requires the final round before sending a bout to scoring', async () => {
    getAssignedBouts.mockResolvedValue([{ ...bout, currentRound: 3, status: 'IN_PROGRESS' }]);

    render(<RingManagerAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);
    fireEvent.click(await screen.findByRole('button', { name: 'Send to scoring' }));
    fireEvent.click(screen.getByRole('button', { name: 'Confirm' }));

    await waitFor(() => expect(updateBoutStatus).toHaveBeenCalledWith(12, 'SCORING', 'ring-manager-token'));
  });

  it('maps transition conflicts to a venue message without exposing the server code', async () => {
    startBout.mockRejectedValue(Object.assign(new Error('BOUT_ALREADY_IN_PROGRESS'), { status: 409 }));
    getAssignedBouts.mockResolvedValue([{ ...bout, currentRound: 0, status: 'READY' }]);

    render(<RingManagerAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);
    fireEvent.click(await screen.findByRole('button', { name: 'Start bout' }));
    fireEvent.click(screen.getByRole('button', { name: 'Confirm' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Another bout is already active on this ring.');
    expect(screen.queryByText('BOUT_ALREADY_IN_PROGRESS')).not.toBeInTheDocument();
  });

  it('refreshes after live result confirmation and locks ring commands', async () => {
    let currentBouts = [bout];
    getAssignedBouts.mockImplementation(() => Promise.resolve(currentBouts));
    render(<RingManagerAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);
    expect(await screen.findByRole('heading', { name: 'Bout 12' })).toBeInTheDocument();
    currentBouts = [{ ...bout, resultConfirmed: true, status: 'FINISHED' }];
    const streamOptions = useBoutEventStream.mock.calls.slice(-1)[0][1];

    act(() => streamOptions.onEvent({ boutId: 12, eventType: 'RESULT_CONFIRMED' }));

    await waitFor(() => expect(screen.getByText('Result confirmed. Prepare the next bout when the ring is ready.')).toBeInTheDocument());
    expect(screen.queryByRole('button', { name: 'Start bout' })).not.toBeInTheDocument();
    expect(startBout).not.toHaveBeenCalled();
  });
});
