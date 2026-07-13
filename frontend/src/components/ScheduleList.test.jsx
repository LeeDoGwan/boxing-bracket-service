import { render, screen } from '@testing-library/react';
import { ScheduleList } from './ScheduleList';

describe('ScheduleList', () => {
  it('renders schedule time, type, title, and status', () => {
    render(<ScheduleList schedules={[{
      scheduleId: 1,
      startTime: '2026-08-01T09:00:00',
      status: 'SCHEDULED',
      title: '개회식',
      type: 'EVENT',
    }]} />);

    expect(screen.getByRole('list', { name: '대회 일정' })).toBeInTheDocument();
    expect(screen.getByText('2026-08-01 09:00')).toBeInTheDocument();
    expect(screen.getByText('행사')).toBeInTheDocument();
    expect(screen.getByText('개회식')).toBeInTheDocument();
    expect(screen.getByText('예정')).toBeInTheDocument();
  });

  it('renders an empty state without schedule items', () => {
    render(<ScheduleList schedules={[]} />);

    expect(screen.getByText('등록된 일정 정보가 없습니다.')).toBeInTheDocument();
  });
});
