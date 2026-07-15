import { act, render, screen, waitFor } from '@testing-library/react';
import { moveToNextBout, startBout, startRound, updateBoutStatus } from '../api/ringManager';
import { getAssignedBouts, getAssignedRings } from '../api/staffAssignments';
import { useBoutEventStream } from '../hooks/useBoutEventStream';
import { RingManagerAssignedPage } from './RingManagerAssignedPage';

vi.mock('../api/auth', () => ({ login: vi.fn(), logout: vi.fn() }));
vi.mock('../api/ringManager', () => ({ moveToNextBout: vi.fn(), startBout: vi.fn(), startRound: vi.fn(), updateBoutStatus: vi.fn() }));
vi.mock('../api/staffAssignments', () => ({ getAssignedBouts: vi.fn(), getAssignedRings: vi.fn() }));
vi.mock('../hooks/useBoutEventStream', () => ({ useBoutEventStream: vi.fn(() => 'connected') }));

const session = { accessToken: 'ring-manager-token', account: { accountId: 9, name: 'Manager', role: 'RING_MANAGER' } };

beforeEach(() => {
  vi.clearAllMocks();
  useBoutEventStream.mockReturnValue('connected');
  getAssignedRings.mockResolvedValue([{ name: 'Ring A', ringId: 4 }]);
  getAssignedBouts.mockResolvedValue([{ boutId: 12, boutNumber: 12, currentRound: 1, matchType: 'Final', ringId: 4, status: 'IN_PROGRESS' }]);
});

describe('RingManagerAssignedPage', () => {
  it('refreshes a ring after live status events without issuing write requests', async () => {
    render(<RingManagerAssignedPage onLogout={vi.fn()} session={session} tournamentId={1} />);
    expect(await screen.findByRole('heading', { name: 'Bout 12' })).toBeInTheDocument();
    const streamOptions = useBoutEventStream.mock.calls.slice(-1)[0][1];

    act(() => streamOptions.onEvent({ boutId: 12, eventType: 'BOUT_STATUS_CHANGED' }));

    await waitFor(() => expect(getAssignedBouts).toHaveBeenCalledTimes(2));
    expect(startBout).not.toHaveBeenCalled();
    expect(startRound).not.toHaveBeenCalled();
    expect(updateBoutStatus).not.toHaveBeenCalled();
    expect(moveToNextBout).not.toHaveBeenCalled();
  });
});
