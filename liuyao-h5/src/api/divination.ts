import { post } from './client';
import type { DivinationAnalyzeRequest, DivinationAnalyzeResponse } from '../types/divination';

export function analyze(req: DivinationAnalyzeRequest): Promise<DivinationAnalyzeResponse> {
  return post<DivinationAnalyzeResponse>('/divinations/analyze', req);
}
