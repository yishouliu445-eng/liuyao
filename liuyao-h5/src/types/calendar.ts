export type VerificationAccuracy = 'ACCURATE' | 'PARTIALLY_ACCURATE' | 'INACCURATE' | 'UNSURE';

export interface VerificationEventDTO {
  eventId: string;
  sessionId: string;
  userId?: number;
  predictedDate: string;
  predictedPrecision: string;
  predictionSummary: string;
  questionCategory: string;
  status: string;
  reminderSentAt?: string;
  createdAt?: string;
  feedbackSubmitted: boolean;
  feedbackAccuracy?: string;
  raw?: Record<string, unknown>;
}

export interface VerificationEventPageDTO {
  page: number;
  size: number;
  total: number;
  items: VerificationEventDTO[];
}

export interface VerificationFeedbackSubmitRequestDTO {
  accuracy: VerificationAccuracy;
  actualOutcome: string;
  tags: string[];
}
