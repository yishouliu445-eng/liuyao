import type { VerificationEventDTO } from '../../types/calendar';

interface Props {
  events: VerificationEventDTO[];
  selectedEventId: string | null;
  onSelectEvent: (event: VerificationEventDTO) => void;
}

function formatDate(value?: string) {
  if (!value) return '时间未知';
  return value.replace('T', ' ').slice(0, 16);
}

function formatDay(value: string) {
  return value.slice(5, 10).replace('-', ' / ');
}

export default function TimelineView({ events, selectedEventId, onSelectEvent }: Props) {
  return (
    <section className="calendar-panel">
      <header className="calendar-panel-header">
        <div>
          <p className="calendar-kicker">时间线</p>
          <h2 className="calendar-panel-title">按时间倒序查看应验事件</h2>
        </div>
        <span className="calendar-panel-caption">{events.length} 条记录</span>
      </header>

      <div className="timeline-list">
        {events.map((event) => {
          const selected = event.eventId === selectedEventId;
          return (
            <button
              key={event.eventId}
              type="button"
              className={`timeline-item ${selected ? 'active' : ''}`}
              onClick={() => onSelectEvent(event)}
            >
              <div className="timeline-item-badge">
                <span className="timeline-item-date">{formatDay(event.predictedDate)}</span>
                <span className={`timeline-item-status timeline-item-status-${event.status.toLowerCase()}`}>
                  {event.status}
                </span>
              </div>
              <div className="timeline-item-copy">
                <h3 className="timeline-item-title">{event.predictionSummary || '未命名应验事件'}</h3>
                <p className="timeline-item-meta">
                  {event.questionCategory || '未分类'}
                  <span className="timeline-item-dot">·</span>
                  {event.predictedPrecision || 'UNKNOWN'}
                  <span className="timeline-item-dot">·</span>
                  {formatDate(event.createdAt)}
                </p>
              </div>
            </button>
          );
        })}
      </div>
    </section>
  );
}
