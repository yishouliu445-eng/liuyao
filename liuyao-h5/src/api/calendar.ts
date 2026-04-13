import { get, post } from './client';
import type {
  VerificationAccuracy,
  VerificationEventDTO,
  VerificationEventPageDTO,
  VerificationFeedbackSubmitRequestDTO,
} from '../types/calendar';

type RecordLike = Record<string, unknown>;

function isRecord(value: unknown): value is RecordLike {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value);
}

function firstDefined<T>(...values: T[]): T | undefined {
  return values.find((value) => value !== undefined && value !== null);
}

function toStringValue(value: unknown, fallback = ''): string {
  if (typeof value === 'string') return value;
  if (typeof value === 'number' || typeof value === 'boolean') return String(value);
  return fallback;
}

function normalizeEvent(value: unknown): VerificationEventDTO | null {
  const record = isRecord(value) ? value : null;
  if (!record) return null;

  return {
    eventId: toStringValue(firstDefined(record.eventId, record.event_id, record.id)),
    sessionId: toStringValue(firstDefined(record.sessionId, record.session_id)),
    userId: typeof firstDefined(record.userId, record.user_id) === 'number'
      ? Number(firstDefined(record.userId, record.user_id))
      : undefined,
    predictedDate: toStringValue(firstDefined(record.predictedDate, record.predicted_date)),
    predictedPrecision: toStringValue(firstDefined(record.predictedPrecision, record.predicted_date_precision)),
    predictionSummary: toStringValue(firstDefined(record.predictionSummary, record.prediction_summary)),
    questionCategory: toStringValue(firstDefined(record.questionCategory, record.question_category)),
    status: toStringValue(firstDefined(record.status, record.eventStatus)),
    reminderSentAt: toStringValue(firstDefined(record.reminderSentAt, record.reminder_sent_at)) || undefined,
    createdAt: toStringValue(firstDefined(record.createdAt, record.created_at)) || undefined,
    feedbackSubmitted: Boolean(firstDefined(record.feedbackSubmitted, record.feedback_submitted)),
    feedbackAccuracy: toStringValue(firstDefined(record.feedbackAccuracy, record.feedback_accuracy)) || undefined,
    raw: record,
  };
}

function normalizePage(value: unknown, page: number, size: number): VerificationEventPageDTO {
  const record = isRecord(value) ? value : {};
  const rawItems = firstDefined(record.items, record.records, record.data, record.list, record.content);
  const items = Array.isArray(rawItems)
    ? rawItems.map((item) => normalizeEvent(item)).filter((item): item is VerificationEventDTO => Boolean(item))
    : [];

  return {
    page,
    size,
    total: Number(firstDefined(record.total, record.totalElements, record.count, items.length)),
    items,
  };
}

function buildMonthlyQuery(params: {
  userId?: string | number;
  year: number;
  month: number;
  page?: number;
  size?: number;
}) {
  const query = new URLSearchParams();
  query.set('year', String(params.year));
  query.set('month', String(params.month));
  query.set('page', String(params.page ?? 1));
  query.set('size', String(params.size ?? 20));
  if (params.userId !== undefined && params.userId !== null && String(params.userId).trim() !== '') {
    query.set('userId', String(params.userId));
  }
  return query.toString();
}

function buildTimelineQuery(params: { userId?: string | number; page?: number; size?: number }) {
  const query = new URLSearchParams();
  query.set('page', String(params.page ?? 1));
  query.set('size', String(params.size ?? 20));
  if (params.userId !== undefined && params.userId !== null && String(params.userId).trim() !== '') {
    query.set('userId', String(params.userId));
  }
  return query.toString();
}

export async function listMonthlyEvents(params: {
  userId?: string | number;
  year: number;
  month: number;
  page?: number;
  size?: number;
}): Promise<VerificationEventPageDTO> {
  const query = buildMonthlyQuery(params);
  const response = await get<unknown>(`/calendar/events?${query}`);
  return normalizePage(response, params.page ?? 1, params.size ?? 20);
}

export async function listTimeline(params: {
  userId?: string | number;
  page?: number;
  size?: number;
}): Promise<VerificationEventPageDTO> {
  const query = buildTimelineQuery(params);
  const response = await get<unknown>(`/calendar/timeline?${query}`);
  return normalizePage(response, params.page ?? 1, params.size ?? 20);
}

export async function submitFeedback(
  eventId: string,
  request: VerificationFeedbackSubmitRequestDTO,
): Promise<VerificationEventDTO> {
  const response = await post<unknown>(`/calendar/events/${eventId}/feedback`, request);
  const normalized = normalizeEvent(response);
  if (!normalized) {
    throw new Error('反馈提交失败');
  }
  return normalized;
}

export type { VerificationAccuracy };
