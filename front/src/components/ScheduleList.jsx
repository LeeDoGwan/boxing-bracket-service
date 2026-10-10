import { useTranslation } from 'react-i18next';
import { translate as t, localizedMessage, resolveMessage, formatVenueDateTime } from '../i18n';
const TYPE_LABELS = {
  BOUT: localizedMessage('common:bout'),
  BREAK: localizedMessage('common:break'),
  LUNCH: localizedMessage('common:lunch'),
  PERFORMANCE: localizedMessage('common:performance'),
  EVENT: localizedMessage('common:event'),
};

const STATUS_LABELS = {
  SCHEDULED: localizedMessage('common:scheduled'),
  IN_PROGRESS: localizedMessage('common:inProgress'),
  COMPLETED: localizedMessage('common:finished'),
};

function formatScheduleTime(value) {
  return formatVenueDateTime(value, { second: undefined });
}

export function ScheduleList({ schedules }) {
  useTranslation();
  if (!schedules.length) {
    return <p className="empty-copy">{t('audience:schedule.empty')}</p>;
  }

  return (
    <ol aria-label={t('admin:schedule.desk')} className="schedule-list">
      {schedules.map((schedule) => (
        <li className={`schedule-item schedule-${schedule.status?.toLowerCase() || 'scheduled'}`} key={schedule.scheduleId}>
          <time dateTime={schedule.startTime}>{resolveMessage(formatScheduleTime(schedule.startTime))}</time>
          <span className="schedule-type">{resolveMessage(TYPE_LABELS[schedule.type] || schedule.type)}</span>
          <strong>{schedule.title}</strong>
          <span className="schedule-status">{resolveMessage(STATUS_LABELS[schedule.status] || schedule.status)}</span>
        </li>
      ))}
    </ol>
  );
}
