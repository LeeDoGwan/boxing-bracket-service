import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { getAccounts } from '../api/adminAccounts';
import { getRings } from '../api/adminRings';
import { getTournaments } from '../api/adminTournaments';
import { changeAssignmentActive, createAssignment, getAssignments } from '../api/staffAssignments';
import { useStaffAuth } from '../auth/StaffAuthContext';
import { AdminAssignmentPage } from './AdminAssignmentPage';

vi.mock('../auth/StaffAuthContext', () => ({ useStaffAuth: vi.fn() }));
vi.mock('../api/adminAccounts', () => ({ getAccounts: vi.fn() }));
vi.mock('../api/adminRings', () => ({ getRings: vi.fn() }));
vi.mock('../api/adminTournaments', () => ({ getTournaments: vi.fn() }));
vi.mock('../api/staffAssignments', () => ({ changeAssignmentActive: vi.fn(), createAssignment: vi.fn(), getAssignments: vi.fn() }));

const session = { accessToken: 'service-token', account: { accountId: 1, name: 'Service', role: 'SERVICE_MANAGER' } };
const account = { accountId: 7, loginId: 'judge01', name: 'Judge One', role: 'JUDGE', status: 'ACTIVE' };

beforeEach(() => {
  window.sessionStorage.clear();
  vi.clearAllMocks();
  useStaffAuth.mockReturnValue({ session, signOut: vi.fn() });
  getTournaments.mockResolvedValue([{ name: 'Cup', tournamentId: 1 }]);
  getAccounts.mockResolvedValue([account]);
  getRings.mockResolvedValue([{ name: 'Ring A', ringId: 4 }]);
  getAssignments.mockResolvedValue([{ accountId: 7, active: true, assignmentId: 9, ringId: 4, role: 'JUDGE', tournamentId: 1 }]);
  createAssignment.mockResolvedValue({ accountId: 7, assignmentId: 10, ringId: 4, role: 'JUDGE', tournamentId: 1 });
  changeAssignmentActive.mockResolvedValue({ assignmentId: 9, active: false });
});

describe('AdminAssignmentPage', () => {
  it('creates a ring assignment from active role-matched references', async () => {
    render(<AdminAssignmentPage />);
    expect(await screen.findByRole('button', { name: '배정 생성' })).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('계정'), { target: { value: '7' } });
    fireEvent.click(screen.getByRole('button', { name: '배정 생성' }));
    await waitFor(() => expect(createAssignment).toHaveBeenCalledWith({ accountId: 7, ringId: 4, role: 'JUDGE', tournamentId: 1 }, 'service-token'));
  });

  it('explains the account lookup restriction to a game manager', async () => {
    useStaffAuth.mockReturnValue({
      session: { ...session, account: { ...session.account, role: 'GAME_MANAGER' } },
      signOut: vi.fn(),
    });
    getAccounts.mockRejectedValue({ status: 403, message: 'FORBIDDEN' });
    render(<AdminAssignmentPage />);

    expect(await screen.findByRole('alert')).toHaveTextContent('서비스 매니저에게 배정을 요청해 주세요.');
    expect(screen.getByRole('button', { name: '배정 생성' })).toBeDisabled();
    expect(createAssignment).not.toHaveBeenCalled();
  });

  it('deactivates an existing assignment', async () => {
    render(<AdminAssignmentPage />);
    const button = await screen.findByRole('button', { name: '비활성화' });
    fireEvent.click(button);
    await waitFor(() => expect(changeAssignmentActive).toHaveBeenCalledWith(9, false, 'service-token'));
  });
});
