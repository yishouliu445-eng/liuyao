import type { ChartSnapshotDTO } from './chart';
import type { RuleHitDTO, StructuredAnalysisResultDTO } from './rule';
import type { DivinationAnalyzeRequest } from './divination';

export type SessionRole = 'user' | 'assistant' | 'system';

export interface DirectionResolutionDTO {
  detectedDirection: string;
  userSelectedDirection: string;
  finalDirection: string;
  suggestedDirection?: string;
  requiresConfirmation?: boolean;
  source?: string;
  confidence?: number;
}

export interface SessionMessageDTO {
  messageId: string;
  sessionId?: string;
  role: SessionRole;
  content: string;
  createdAt?: string;
  updatedAt?: string;
  chartSnapshot?: ChartSnapshotDTO | null;
  structuredResult?: StructuredAnalysisResultDTO | null;
  ruleHits?: RuleHitDTO[];
  analysis?: string;
  suggestions?: string[];
  status?: 'pending' | 'sent' | 'failed';
  raw?: Record<string, unknown>;
}

export interface SessionThreadDTO {
  sessionId: string;
  questionText: string;
  questionCategory: string;
  detectedDirection?: string;
  userSelectedDirection?: string;
  finalDirection?: string;
  suggestedDirection?: string;
  divinationTime?: string;
  divinationMethod?: string;
  status?: string;
  createdAt?: string;
  updatedAt?: string;
  chartSnapshot?: ChartSnapshotDTO | null;
  structuredResult?: StructuredAnalysisResultDTO | null;
  analysis?: string;
  messages: SessionMessageDTO[];
  smartPrompts: string[];
  raw?: Record<string, unknown>;
}

export interface SessionListItemDTO {
  sessionId: string;
  questionText: string;
  questionCategory: string;
  status: string;
  messageCount: number;
  createdAt?: string;
  lastActiveAt?: string;
  closedAt?: string;
  raw?: Record<string, unknown>;
}

export interface SessionListResponseDTO {
  page: number;
  size: number;
  total: number;
  items: SessionListItemDTO[];
}

export interface SessionCreateRequestDTO extends DivinationAnalyzeRequest {}

export interface SessionMessageRequestDTO {
  content: string;
}
