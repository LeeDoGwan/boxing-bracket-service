import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { logout } from '../api/auth';
import { getAccounts } from '../api/adminAccounts';
import { getRings } from '../api/adminRings';
import { getTournaments } from '../api/adminTournaments';
import { changeAssignmentActive, createAssignment, getAssignments } from '../api/staffAssignments';
import { AdminAssignmentPage } from './AdminAssignmentPage';

vi.mock('../api/auth', () => ({ login: vi.fn(), logout: vi.fn() }));
vi.mock('../api/adminAccounts', () => ({ getAccounts: vi.fn() }));
vi.mock('../api/adminRings', () => ({ getRings: vi.fn() }));
vi.mock('../api/adminTournaments', () => ({ getTournaments: vi.fn() }));
vi.mock('../api/staffAssignments', () => ({ changeAssignmentActive: vi.fn(), createAssignment: vi.fn(), getAssignments: vi.fn() }));

const session = { accessToken: 'service-token', account: { accountId: 1, name: 'Service', role: 'SERVICE_MANAGER' } };
const account = { accountId: 7, loginId: 'judge01', name: 'Judge One', role: 'JUDGE', status: 'ACTIVE' };

beforeEach(() => {
  window.sessionStorage.clear();
  vi.clearAllMocks();
  getTournaments.mockResolvedValue([{ name: 'Cup', tournamentId: 1 }]);
  getAccounts.mockResolvedValue([account]);
  getRings.mockResolvedValue([{ name: 'Ring A', ringId: 4 }]);
  getAssignments.mockResolvedValue([{ accountId: 7, active: true, assignmentId: 9, ringId: 4, role: 'JUDGE', tournamentId: 1 }]);
  createAssignment.mockResolvedValue({ accountId: 7, assignmentId: 10, ringId: 4, role: 'JUDGE', tournamentId: 1 });
  changeAssignmentActive.mockResolvedValue({ assignmentId: 9, active: false });
  logout.mockResolvedValue(undefined);
});

describe('AdminAssignmentPage', () => {
  it('creates a ring assignment from active role-matched references', async () => {
    window.sessionStorage.setItem('boxing.operations.session', JSON.stringify(session));
    render(<AdminAssignmentPage />);
    expect(await screen.findByRole('button', { name: 'Create assignment' })).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Account'), { target: { value: '7' } });
    fireEvent.click(screen.getByRole('button', { name: 'Create assignment' }));
    await waitFor(() => expect(createAssignment).toHaveBeenCalledWith({ accountId: 7, ringId: 4, role: 'JUDGE', tournamentId: 1 }, 'service-token'));
  });

  it('deactivates an existing assignment', async () => {
    window.sessionStorage.setItem('boxing.operations.session', JSON.stringify(session));
    render(<AdminAssignmentPage />);
    const button = await screen.findByRole('button', { name: 'Deactivate' });
    fireEvent.click(button);
    await waitFor(() => expect(changeAssignmentActive).toHaveBeenCalledWith(9, false, 'service-token'));
  });
});
