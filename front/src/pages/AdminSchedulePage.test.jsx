import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { createSchedule, deleteSchedule, getSchedules, updateSchedule } from '../api/adminSchedules';
import { useStaffAuth } from '../auth/StaffAuthContext';
import { AdminSchedulePage } from './AdminSchedulePage';

vi.mock('../auth/StaffAuthContext', () => ({ useStaffAuth: vi.fn() }));

vi.mock('../api/adminSchedules', () => ({
  createSchedule: vi.fn(),
  deleteSchedule: vi.fn(),
  getSchedules: vi.fn(),
  updateSchedule: vi.fn(),
}));

const session = {
  accessToken: 'admin-token',
  account: { accountId: 50, name: 'Game Manager', role: 'GAME_MANAGER' },
};

const schedule = {
  endTime: '2026-08-01T10:00:00',
  relatedBoutId: null,
  ringId: null,
  scheduleId: 20,
  startTime: '2026-08-01T09:00:00',
  status: 'SCHEDULED',
  title: 'Opening ceremony',
  tournamentId: 1,
  type: 'EVENT',
};

beforeEach(() => {
  window.sessionStorage.clear();
  vi.clearAllMocks();
  useStaffAuth.mockReturnValue({ session, signOut: vi.fn() });
  getSchedules.mockResolvedValue([schedule]);
  createSchedule.mockResolvedValue({ ...schedule, scheduleId: 21, title: 'Lunch' });
  updateSchedule.mockResolvedValue({ ...schedule, title: 'Updated ceremony' });
  deleteSchedule.mockResolvedValue(undefined);
});

describe('AdminSchedulePage', () => {
  it('loads schedules for the tournament from the shared staff session', async () => {
    render(<AdminSchedulePage tournamentId={1} />);

    expect(await screen.findByRole('heading', { name: '일정 관리' })).toBeInTheDocument();
    expect(getSchedules).toHaveBeenCalledWith(1, 'admin-token');
    expect(await screen.findByText('Opening ceremony')).toBeInTheDocument();
  });

  it('creates a schedule with date and type fields', async () => {
    render(<AdminSchedulePage tournamentId={1} />);
    expect(await screen.findByText('Opening ceremony')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '+ 새 일정' }));
    fireEvent.change(screen.getByLabelText('일정 유형'), { target: { value: 'LUNCH' } });
    fireEvent.change(screen.getByLabelText('제목'), { target: { value: 'Lunch' } });
    fireEvent.change(screen.getByLabelText('시작 시간'), { target: { value: '2026-08-01T12:00' } });
    fireEvent.change(screen.getByLabelText('종료 시간'), { target: { value: '2026-08-01T13:00' } });
    fireEvent.click(screen.getByRole('button', { name: '일정 생성' }));

    await waitFor(() => expect(createSchedule).toHaveBeenCalledWith({
      endTime: '2026-08-01T13:00',
      relatedBoutId: null,
      ringId: null,
      startTime: '2026-08-01T12:00',
      status: 'SCHEDULED',
      title: 'Lunch',
      tournamentId: 1,
      type: 'LUNCH',
    }, 'admin-token'));
  });

  it('updates and deletes a selected schedule', async () => {
    render(<AdminSchedulePage tournamentId={1} />);
    expect(await screen.findByText('Opening ceremony')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('제목'), { target: { value: 'Updated ceremony' } });
    fireEvent.click(screen.getByRole('button', { name: '일정 저장' }));

    await waitFor(() => expect(updateSchedule).toHaveBeenCalledWith(20, expect.objectContaining({ title: 'Updated ceremony' }), 'admin-token'));
    fireEvent.click(screen.getByRole('button', { name: '일정 삭제' }));
    fireEvent.click(screen.getByRole('button', { name: '확인' }));
    await waitFor(() => expect(deleteSchedule).toHaveBeenCalledWith(20, 'admin-token'));
  });
});
