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
});
