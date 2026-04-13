import type { VerificationEventDTO } from '../../types/calendar';

interface Props {
  year: number;
  month: number;
  events: VerificationEventDTO[];
  selectedDate: string | null;
  onSelectDate: (date: string) => void;
}

const WEEKDAYS = ['一', '二', '三', '四', '五', '六', '日'];

function formatMonthLabel(year: number, month: number) {
  return `${year}年${String(month).padStart(2, '0')}月`;
}

function toDateKey(date: Date) {
  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, '0');
  const dd = String(date.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
}

function startOfCalendarGrid(year: number, month: number) {
  const firstOfMonth = new Date(year, month - 1, 1);
  const day = firstOfMonth.getDay();
  const offset = day === 0 ? 6 : day - 1;
  const gridStart = new Date(firstOfMonth);
  gridStart.setDate(firstOfMonth.getDate() - offset);
  return gridStart;
}

export default function MonthView({ year, month, events, selectedDate, onSelectDate }: Props) {
  const gridStart = startOfCalendarGrid(year, month);
  const monthLabel = formatMonthLabel(year, month);
  const eventMap = new Map<string, VerificationEventDTO[]>();

  for (const event of events) {
    const key = event.predictedDate.slice(0, 10);
    const bucket = eventMap.get(key) ?? [];
    bucket.push(event);
    eventMap.set(key, bucket);
  }

  const cells = Array.from({ length: 42 }, (_, index) => {
    const date = new Date(gridStart);
    date.setDate(gridStart.getDate() + index);
    const dateKey = toDateKey(date);
    const inMonth = date.getMonth() === month - 1;
    const dayEvents = eventMap.get(dateKey) ?? [];

    return {
      date,
      dateKey,
      inMonth,
      dayEvents,
    };
  });

  return (
    <section className="calendar-panel">
      <header className="calendar-panel-header">
        <div>
          <p className="calendar-kicker">月视图</p>
          <h2 className="calendar-panel-title">{monthLabel}</h2>
        </div>
        <span className="calendar-panel-caption">{events.length} 个应验事件</span>
      </header>

      <div className="calendar-weekdays" aria-hidden="true">
        {WEEKDAYS.map((weekday) => (
          <span key={weekday} className="calendar-weekday">{weekday}</span>
        ))}
      </div>

      <div className="calendar-grid">
        {cells.map(({ date, dateKey, inMonth, dayEvents }) => {
          const active = selectedDate === dateKey;
          return (
            <button
              key={dateKey}
              type="button"
              className={`calendar-day ${inMonth ? '' : 'calendar-day-outside'} ${active ? 'active' : ''}`}
              onClick={() => onSelectDate(dateKey)}
            >
              <span className="calendar-day-number">{date.getDate()}</span>
              <span className="calendar-day-events">
                {dayEvents.slice(0, 3).map((event) => (
                  <i
                    key={event.eventId}
                    className={`calendar-dot calendar-dot-${event.status.toLowerCase()}`}
                    title={event.predictionSummary}
                  />
                ))}
                {dayEvents.length > 3 && <i className="calendar-dot calendar-dot-more" />}
              </span>
            </button>
          );
        })}
      </div>
    </section>
  );
}
