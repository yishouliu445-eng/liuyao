import { get, post } from './client';
import type { ChartSnapshotDTO } from '../types/chart';
import type { RuleHitDTO, StructuredAnalysisResultDTO } from '../types/rule';
import type {
  SessionCreateRequestDTO,
  SessionListItemDTO,
  SessionListResponseDTO,
  SessionMessageDTO,
  SessionMessageRequestDTO,
  SessionThreadDTO,
  SessionRole,
} from '../types/session';

type RecordLike = Record<string, unknown>;

function isRecord(value: unknown): value is RecordLike {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value);
}

function toRecord(value: unknown): RecordLike | undefined {
  return isRecord(value) ? value : undefined;
}

function firstDefined<T>(...values: T[]): T | undefined {
  return values.find((value) => value !== undefined && value !== null);
}

function toStringValue(value: unknown, fallback = ''): string {
  if (typeof value === 'string') return value;
  if (typeof value === 'number' || typeof value === 'boolean') return String(value);
  return fallback;
}

function normalizeRole(value: unknown): SessionRole {
  const role = toStringValue(value).toLowerCase();
  if (role.includes('user') || role === 'question' || role === 'ask') {
    return 'user';
  }
  if (role.includes('system')) {
    return 'system';
  }
  return 'assistant';
}

function normalizeChartSnapshot(value: unknown): ChartSnapshotDTO | null {
  const record = toRecord(value);
  if (!record) return null;

  const snapshot: ChartSnapshotDTO = {
    question: toStringValue(firstDefined(record.question, record.questionText, record.title)),
    questionCategory: toStringValue(firstDefined(record.questionCategory, record.category)),
    divinationMethod: toStringValue(record.divinationMethod),
    divinationTime: toStringValue(record.divinationTime),
    mainHexagram: toStringValue(record.mainHexagram),
    changedHexagram: toStringValue(record.changedHexagram),
    mainHexagramCode: toStringValue(record.mainHexagramCode),
    changedHexagramCode: toStringValue(record.changedHexagramCode),
    mainUpperTrigram: toStringValue(record.mainUpperTrigram),
    mainLowerTrigram: toStringValue(record.mainLowerTrigram),
    changedUpperTrigram: toStringValue(record.changedUpperTrigram),
    changedLowerTrigram: toStringValue(record.changedLowerTrigram),
    palace: toStringValue(record.palace),
    palaceWuXing: toStringValue(record.palaceWuXing),
    shi: Number(firstDefined(record.shi, 0)),
    ying: Number(firstDefined(record.ying, 0)),
    useGod: toStringValue(record.useGod),
    riChen: toStringValue(record.riChen),
    yueJian: toStringValue(record.yueJian),
    snapshotVersion: toStringValue(record.snapshotVersion),
    calendarVersion: toStringValue(record.calendarVersion),
    kongWang: Array.isArray(record.kongWang) ? record.kongWang.map((item) => toStringValue(item)) : [],
    lines: Array.isArray(record.lines) ? (record.lines as ChartSnapshotDTO['lines']) : [],
  };

  return snapshot;
}

function normalizeStructuredResult(value: unknown): StructuredAnalysisResultDTO | null {
  const record = toRecord(value);
  if (!record) return null;

  return {
    score: Number(firstDefined(record.score, 0)),
    resultLevel: toStringValue(record.resultLevel),
    effectiveScore: Number(firstDefined(record.effectiveScore, record.score, 0)),
    effectiveResultLevel: toStringValue(firstDefined(record.effectiveResultLevel, record.resultLevel)),
    tags: Array.isArray(record.tags) ? record.tags.map((item) => toStringValue(item)) : [],
    effectiveRuleCodes: Array.isArray(record.effectiveRuleCodes)
      ? record.effectiveRuleCodes.map((item) => toStringValue(item))
      : [],
    suppressedRuleCodes: Array.isArray(record.suppressedRuleCodes)
      ? record.suppressedRuleCodes.map((item) => toStringValue(item))
      : [],
    summary: toStringValue(record.summary),
    categorySummaries: Array.isArray(record.categorySummaries)
      ? (record.categorySummaries as StructuredAnalysisResultDTO['categorySummaries'])
      : [],
    conflictSummaries: Array.isArray(record.conflictSummaries)
      ? (record.conflictSummaries as StructuredAnalysisResultDTO['conflictSummaries'])
      : [],
  };
}

function normalizeRuleHits(value: unknown): RuleHitDTO[] {
  if (!Array.isArray(value)) return [];
  return value.filter(isRecord).map((item, index) => ({
    ruleId: toStringValue(firstDefined(item.ruleId, item.ruleCode, index)),
    ruleCode: toStringValue(firstDefined(item.ruleCode, item.code, item.ruleId, index)),
    ruleName: toStringValue(firstDefined(item.ruleName, item.name, item.ruleCode)),
    category: toStringValue(item.category),
    priority: Number(firstDefined(item.priority, 0)),
    hitReason: toStringValue(firstDefined(item.hitReason, item.reason, item.description)),
    impactLevel: toStringValue(firstDefined(item.impactLevel, item.level)),
    scoreDelta: Number(firstDefined(item.scoreDelta, item.delta, 0)),
    tags: Array.isArray(item.tags) ? item.tags.map((tag) => toStringValue(tag)) : [],
    evidence: isRecord(item.evidence) ? item.evidence : {},
  }));
}

function normalizeMessage(value: unknown, index = 0): SessionMessageDTO | null {
  const record = toRecord(value);
  if (!record) return null;
  const source = toRecord(firstDefined(record.message, record.data, record.payload, record.result)) ?? record;

  const structuredResult = normalizeStructuredResult(
    firstDefined(source.structuredResult, source.structured, source.analysisResult, source.result),
  );
  const chartSnapshot = normalizeChartSnapshot(
    firstDefined(source.chartSnapshot, source.chart, source.snapshot),
  );
  const analysis = toStringValue(firstDefined(source.analysis, source.content, source.messageText, source.text, source.reply));
  const fallbackContent = analysis || toStringValue(firstDefined(source.summary, source.answer, source.message));

  return {
    messageId: toStringValue(firstDefined(source.messageId, source.id, source.messageIdStr, index)),
    sessionId: toStringValue(firstDefined(source.sessionId, source.session_id, record.sessionId, record.session_id)),
    role: normalizeRole(firstDefined(source.role, source.sender, source.type, source.messageRole)),
    content: fallbackContent,
    createdAt: toStringValue(firstDefined(source.createdAt, source.created_at)),
    updatedAt: toStringValue(firstDefined(source.updatedAt, source.updated_at)),
    chartSnapshot,
    structuredResult,
    ruleHits: normalizeRuleHits(firstDefined(source.ruleHits, source.rules, source.ruleHitList)),
    analysis: analysis || undefined,
    suggestions: Array.isArray(firstDefined(source.suggestions, source.smartPrompts, source.promptSuggestions))
      ? (firstDefined(source.suggestions, source.smartPrompts, source.promptSuggestions) as unknown[]).map((item) => toStringValue(item))
      : undefined,
    status: toStringValue(firstDefined(source.status, source.messageStatus)) as SessionMessageDTO['status'],
    raw: record,
  };
}

function normalizeMessages(value: unknown): SessionMessageDTO[] {
  if (!Array.isArray(value)) return [];
  return value
    .map((item, index) => normalizeMessage(item, index))
    .filter((item): item is SessionMessageDTO => Boolean(item));
}

function normalizeSessionListItem(value: unknown): SessionListItemDTO | null {
  const record = toRecord(value);
  if (!record) return null;

  return {
    sessionId: toStringValue(firstDefined(record.sessionId, record.session_id, record.id)),
    questionText: toStringValue(firstDefined(record.questionText, record.originalQuestion, record.question, record.title)),
    questionCategory: toStringValue(firstDefined(record.questionCategory, record.category)),
    status: toStringValue(firstDefined(record.status, record.sessionStatus)),
    messageCount: Number(firstDefined(record.messageCount, record.message_count, 0)),
    createdAt: toStringValue(firstDefined(record.createdAt, record.created_at)) || undefined,
    lastActiveAt: toStringValue(firstDefined(record.lastActiveAt, record.last_active_at)) || undefined,
    closedAt: toStringValue(firstDefined(record.closedAt, record.closed_at)) || undefined,
    raw: record,
  };
}

function normalizeSessionListResponse(value: unknown, page: number, size: number): SessionListResponseDTO {
  const record = toRecord(value) ?? {};
  const rawItems = firstDefined(record.records, record.items, record.content, record.list, record.data);
  const items = Array.isArray(rawItems)
    ? rawItems
        .map((item) => normalizeSessionListItem(item))
        .filter((item): item is SessionListItemDTO => Boolean(item))
    : [];

  return {
    page,
    size,
    total: Number(firstDefined(record.total, record.totalElements, record.count, items.length)),
    items,
  };
}

function mergeThread(base: Partial<SessionThreadDTO>, raw: unknown, extraMessages: SessionMessageDTO[] = []): SessionThreadDTO {
  const record = toRecord(raw) ?? {};
  const source = toRecord(firstDefined(record.session, record.data, record.payload, record.result, record.thread)) ?? record;
  const sessionId = toStringValue(firstDefined(
    base.sessionId,
    source.sessionId,
    source.id,
    source.session_id,
  ));
  const chartSnapshot = firstDefined(
    base.chartSnapshot,
    normalizeChartSnapshot(firstDefined(source.chartSnapshot, source.chart, source.snapshot, record.chartSnapshot, record.chart, record.snapshot)),
  );
  const structuredResult = firstDefined(
    base.structuredResult,
    normalizeStructuredResult(firstDefined(source.structuredResult, source.structured, source.analysisResult, source.result)),
  );
  const analysis = toStringValue(firstDefined(base.analysis, source.analysis, source.summary, source.message));
  const questionText = toStringValue(firstDefined(base.questionText, source.questionText, source.question, source.title));
  const questionCategory = toStringValue(firstDefined(base.questionCategory, source.questionCategory, source.category));
  const smartPrompts = base.smartPrompts?.length
    ? base.smartPrompts
    : Array.isArray(firstDefined(source.smartPrompts, source.suggestions, source.promptSuggestions, source.followUpPrompts, record.smartPrompts, record.suggestions, record.promptSuggestions, record.followUpPrompts))
      ? (firstDefined(source.smartPrompts, source.suggestions, source.promptSuggestions, source.followUpPrompts, record.smartPrompts, record.suggestions, record.promptSuggestions, record.followUpPrompts) as unknown[]).map((item) => toStringValue(item))
      : [];

  const messages = [
    ...normalizeMessages(firstDefined(source.messages, source.chatMessages, source.messageList, source.items, source.history, record.messages, record.chatMessages, record.messageList, record.items, record.history)),
    ...extraMessages,
  ];
  const dedupedMessages: SessionMessageDTO[] = [];
  const seenIds = new Set<string>();
  for (const message of messages) {
    if (!seenIds.has(message.messageId)) {
      seenIds.add(message.messageId);
      dedupedMessages.push(message);
    }
  }

  const normalizedMessages = dedupedMessages.length > 0
    ? dedupedMessages
    : synthesizeMessages({
        sessionId,
        questionText,
        questionCategory,
        analysis,
        chartSnapshot,
        structuredResult,
      });

  return {
    sessionId,
    questionText,
    questionCategory,
    divinationTime: toStringValue(firstDefined(base.divinationTime, source.divinationTime, source.divination_time, record.divinationTime)),
    divinationMethod: toStringValue(firstDefined(base.divinationMethod, source.divinationMethod, record.divinationMethod)),
    status: toStringValue(firstDefined(base.status, source.status, source.sessionStatus, record.status, record.sessionStatus)),
    createdAt: toStringValue(firstDefined(base.createdAt, source.createdAt, source.created_at, record.createdAt, record.created_at)),
    updatedAt: toStringValue(firstDefined(base.updatedAt, source.updatedAt, source.updated_at, record.updatedAt, record.updated_at)),
    chartSnapshot,
    structuredResult,
    analysis: analysis || undefined,
    messages: normalizedMessages,
    smartPrompts,
    raw: record,
  };
}

function synthesizeMessages(input: {
  sessionId: string;
  questionText: string;
  questionCategory: string;
  analysis?: string;
  chartSnapshot?: ChartSnapshotDTO | null;
  structuredResult?: StructuredAnalysisResultDTO | null;
}): SessionMessageDTO[] {
  const messages: SessionMessageDTO[] = [];

  if (input.questionText) {
    messages.push({
      messageId: `${input.sessionId}-user`,
      sessionId: input.sessionId,
      role: 'user',
      content: input.questionText,
      status: 'sent',
    });
  }

  if (input.analysis || input.structuredResult || input.chartSnapshot) {
    messages.push({
      messageId: `${input.sessionId}-assistant`,
      sessionId: input.sessionId,
      role: 'assistant',
      content: input.analysis || input.structuredResult?.summary || '分析已完成。',
      analysis: input.analysis,
      chartSnapshot: input.chartSnapshot ?? null,
      structuredResult: input.structuredResult ?? null,
      status: 'sent',
    });
  }

  return messages;
}

function normalizeSessionThread(value: unknown): SessionThreadDTO {
  const record = toRecord(value) ?? {};
  const message = normalizeMessage(firstDefined(
    record.message,
    record.assistantMessage,
    record.chatMessage,
    record.reply,
  ));

  return mergeThread({
    sessionId: message?.sessionId || '',
    questionText: message?.role === 'user' ? message.content : '',
    questionCategory: '',
    messages: [],
    smartPrompts: [],
  }, value, message ? [message] : []);
}

export async function createSession(req: SessionCreateRequestDTO): Promise<SessionThreadDTO> {
  const response = await post<unknown>('/sessions', req);
  return normalizeSessionThread(response);
}

export async function getSession(sessionId: string | number): Promise<SessionThreadDTO> {
  const response = await get<unknown>(`/sessions/${sessionId}`);
  return normalizeSessionThread(response);
}

export async function getSessionMessages(sessionId: string | number): Promise<SessionMessageDTO[]> {
  const response = await get<unknown>(`/sessions/${sessionId}/messages`);
  return normalizeMessages(
    isRecord(response)
      ? firstDefined(response.messages, response.items, response.data, response.list)
      : response,
  );
}

export async function listSessions(params: {
  userId?: string | number;
  page?: number;
  size?: number;
} = {}): Promise<SessionListResponseDTO> {
  const query = new URLSearchParams();
  query.set('page', String(params.page ?? 0));
  query.set('size', String(params.size ?? 20));
  if (params.userId !== undefined && params.userId !== null && String(params.userId).trim() !== '') {
    query.set('userId', String(params.userId));
  }

  const response = await get<unknown>(`/sessions?${query.toString()}`);
  return normalizeSessionListResponse(response, params.page ?? 0, params.size ?? 20);
}

export async function sendSessionMessage(
  sessionId: string | number,
  req: SessionMessageRequestDTO,
): Promise<SessionThreadDTO> {
  const response = await post<unknown>(`/sessions/${sessionId}/messages`, {
    ...req,
    messageText: req.content,
    text: req.content,
  });
  return normalizeSessionThread(response);
}

export function mergeSessionState(
  detail: SessionThreadDTO | null,
  messages: SessionMessageDTO[] = [],
): SessionThreadDTO | null {
  if (!detail) return null;
  const existingMap = new Map(detail.messages.map((message) => [message.messageId, message]));
  const mergedMessages = [...detail.messages];

  for (const message of messages) {
    if (!existingMap.has(message.messageId)) {
      mergedMessages.push(message);
    }
  }

  return {
    ...detail,
    messages: mergedMessages,
  };
}

export function buildOptimisticAssistantMessage(
  sessionId: string,
  content: string,
): SessionMessageDTO {
  return {
    messageId: `${sessionId}-pending-${Date.now()}`,
    sessionId,
    role: 'assistant',
    content,
    status: 'pending',
  };
}
