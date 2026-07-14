const TYPE_LABELS = {
  BOUT: '경기',
  BREAK: '휴식',
  LUNCH: '점심',
  PERFORMANCE: '공연',
  EVENT: '행사',
};

const STATUS_LABELS = {
  SCHEDULED: '예정',
  IN_PROGRESS: '진행 중',
  COMPLETED: '종료',
};

function formatScheduleTime(value) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-';
}

export function ScheduleList({ schedules }) {
  if (!schedules.length) {
    return <p className="empty-copy">등록된 일정 정보가 없습니다.</p>;
  }

  return (
    <ol aria-label="대회 일정" className="schedule-list">
      {schedules.map((schedule) => (
        <li className={`schedule-item schedule-${schedule.status?.toLowerCase() || 'scheduled'}`} key={schedule.scheduleId}>
          <time dateTime={schedule.startTime}>{formatScheduleTime(schedule.startTime)}</time>
          <span className="schedule-type">{TYPE_LABELS[schedule.type] || schedule.type}</span>
          <strong>{schedule.title}</strong>
          <span className="schedule-status">{STATUS_LABELS[schedule.status] || schedule.status}</span>
        </li>
      ))}
    </ol>
  );
}
