import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { listMonthlyEvents, listTimeline } from '../api/calendar';
import FeedbackForm from '../components/calendar/FeedbackForm';
import MonthView from '../components/calendar/MonthView';
import TimelineView from '../components/calendar/TimelineView';
import LoadingInk from '../components/feedback/LoadingInk';
import type { VerificationEventDTO } from '../types/calendar';

type ViewMode = 'month' | 'timeline';

function getMonthParts(date = new Date()) {
  return {
    year: date.getFullYear(),
    month: date.getMonth() + 1,
  };
}

function formatMonthLabel(year: number, month: number) {
  return `${year}年${String(month).padStart(2, '0')}月`;
}

function formatDateTime(value?: string) {
  if (!value) return '时间未知';
  return value.replace('T', ' ').slice(0, 16);
}

function toDateKey(value: string) {
  return value.slice(0, 10);
}

export default function CalendarPage() {
  const [viewMode, setViewMode] = useState<ViewMode>('month');
  const [yearMonth, setYearMonth] = useState(getMonthParts);
  const [selectedDate, setSelectedDate] = useState<string | null>(null);
  const [selectedEvent, setSelectedEvent] = useState<VerificationEventDTO | null>(null);
  const [monthEvents, setMonthEvents] = useState<VerificationEventDTO[]>([]);
  const [timelineEvents, setTimelineEvents] = useState<VerificationEventDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);

      try {
        const [monthly, timeline] = await Promise.all([
          listMonthlyEvents({ ...yearMonth, page: 1, size: 31 }),
          listTimeline({ page: 1, size: 50 }),
        ]);
        if (cancelled) return;

        const sortedMonthly = [...monthly.items].sort((a, b) => a.predictedDate.localeCompare(b.predictedDate));
        const sortedTimeline = [...timeline.items].sort((a, b) => b.predictedDate.localeCompare(a.predictedDate));

        setMonthEvents(sortedMonthly);
        setTimelineEvents(sortedTimeline);

        const firstSelectedDate = sortedMonthly[0]?.predictedDate?.slice(0, 10) ?? null;
        setSelectedDate(firstSelectedDate);

        if (firstSelectedDate) {
          const dayEvent = sortedMonthly.find((event) => toDateKey(event.predictedDate) === firstSelectedDate)
            ?? sortedTimeline.find((event) => toDateKey(event.predictedDate) === firstSelectedDate)
            ?? null;
          setSelectedEvent(dayEvent);
        } else {
          setSelectedEvent(sortedTimeline[0] ?? sortedMonthly[0] ?? null);
        }
      } catch (e) {
        if (!cancelled) {
          setError(e instanceof Error ? e.message : '加载日历失败');
          setMonthEvents([]);
          setTimelineEvents([]);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void load();

    return () => {
      cancelled = true;
    };
  }, [yearMonth]);

  const selectedDayEvents = useMemo(
    () => monthEvents.filter((event) => toDateKey(event.predictedDate) === selectedDate),
    [monthEvents, selectedDate],
  );

  const activeEvents = viewMode === 'month' ? monthEvents : timelineEvents;
  const activeCount = activeEvents.length;

  function shiftMonth(delta: number) {
    setYearMonth((current) => {
      const next = new Date(current.year, current.month - 1 + delta, 1);
      return getMonthParts(next);
    });
    setSelectedDate(null);
    setSelectedEvent(null);
  }

  function handleDateSelect(date: string) {
    setSelectedDate(date);
    const dayEvent = monthEvents.find((event) => toDateKey(event.predictedDate) === date)
      ?? timelineEvents.find((event) => toDateKey(event.predictedDate) === date)
      ?? null;
    setSelectedEvent(dayEvent);
  }

  function handleFeedbackSubmitted(updated: VerificationEventDTO) {
    setMonthEvents((prev) => prev.map((event) => (event.eventId === updated.eventId ? updated : event)));
    setTimelineEvents((prev) => prev.map((event) => (event.eventId === updated.eventId ? updated : event)));
    setSelectedEvent(updated);
  }

  if (loading) {
    return <LoadingInk />;
  }

  return (
    <div className="calendar-page">
      <header className="calendar-hero">
        <div className="calendar-hero-copy">
          <Link to="/" className="calendar-back-link">← 回到起卦</Link>
          <p className="calendar-kicker">应验日历</p>
          <h1 className="calendar-title">看见每一次应期与反馈闭环</h1>
          <p className="calendar-subtitle">
            当前 {formatMonthLabel(yearMonth.year, yearMonth.month)} · {activeCount} 条记录
          </p>
        </div>
        <div className="calendar-hero-actions">
          <button type="button" className="calendar-hero-btn" onClick={() => shiftMonth(-1)}>上月</button>
          <button type="button" className="calendar-hero-btn" onClick={() => shiftMonth(1)}>下月</button>
        </div>
      </header>

      <div className="calendar-switcher" role="tablist" aria-label="日历视图切换">
        <button
          type="button"
          role="tab"
          className={`calendar-switcher-btn ${viewMode === 'month' ? 'active' : ''}`}
          onClick={() => setViewMode('month')}
        >
          月视图
        </button>
        <button
          type="button"
          role="tab"
          className={`calendar-switcher-btn ${viewMode === 'timeline' ? 'active' : ''}`}
          onClick={() => setViewMode('timeline')}
        >
          时间线
        </button>
      </div>

      {error && <p className="error-text calendar-error">{error}</p>}

      {viewMode === 'month' ? (
        <>
          <MonthView
            year={yearMonth.year}
            month={yearMonth.month}
            events={monthEvents}
            selectedDate={selectedDate}
            onSelectDate={handleDateSelect}
          />
          <section className="calendar-panel">
            <header className="calendar-panel-header">
              <div>
                <p className="calendar-kicker">选中日期</p>
                <h2 className="calendar-panel-title">{selectedDate || '请选择日期'}</h2>
              </div>
              <span className="calendar-panel-caption">{selectedDayEvents.length} 个事件</span>
            </header>

            {selectedDayEvents.length === 0 ? (
              <p className="empty-hint calendar-empty">这一天还没有应验事件。</p>
            ) : (
              <div className="calendar-day-list">
                {selectedDayEvents.map((event) => (
                  <button
                    key={event.eventId}
                    type="button"
                    className={`calendar-day-card ${selectedEvent?.eventId === event.eventId ? 'active' : ''}`}
                    onClick={() => setSelectedEvent(event)}
                  >
                    <span className="calendar-day-card-title">{event.predictionSummary || '未命名事件'}</span>
                    <span className="calendar-day-card-meta">
                      {event.questionCategory || '未分类'}
                      <span className="calendar-day-card-dot">·</span>
                      {event.predictedPrecision || 'UNKNOWN'}
                      <span className="calendar-day-card-dot">·</span>
                      {event.status}
                    </span>
                  </button>
                ))}
              </div>
            )}
          </section>
        </>
      ) : (
        <TimelineView
          events={timelineEvents}
          selectedEventId={selectedEvent?.eventId ?? null}
          onSelectEvent={setSelectedEvent}
        />
      )}

      {selectedEvent && (
        <section className="calendar-panel calendar-detail">
          <header className="calendar-panel-header">
            <div>
              <p className="calendar-kicker">反馈入口</p>
              <h2 className="calendar-panel-title">事件详情</h2>
            </div>
            <span className={`calendar-status calendar-status-${selectedEvent.status.toLowerCase()}`}>
              {selectedEvent.status}
            </span>
          </header>

          <div className="calendar-detail-grid">
            <div className="calendar-detail-copy">
              <p className="calendar-detail-question">{selectedEvent.predictionSummary || '暂无摘要'}</p>
              <p className="calendar-detail-meta">
                {selectedEvent.questionCategory || '未分类'}
                <span className="calendar-detail-dot">·</span>
                {selectedEvent.predictedPrecision || 'UNKNOWN'}
              </p>
              <p className="calendar-detail-meta">
                预测日期 {formatDateTime(selectedEvent.predictedDate)}
              </p>
              <Link to={`/session/${selectedEvent.sessionId}`} className="calendar-session-link">
                打开对应会话
              </Link>
            </div>

            {!selectedEvent.feedbackSubmitted ? (
              <FeedbackForm event={selectedEvent} onSubmitted={handleFeedbackSubmitted} />
            ) : (
              <div className="calendar-feedback-done">
                <p className="calendar-feedback-done-title">反馈已提交</p>
                <p className="calendar-feedback-done-meta">
                  结果：{selectedEvent.feedbackAccuracy || '未知'}
                </p>
              </div>
            )}
          </div>
        </section>
      )}
    </div>
  );
}
