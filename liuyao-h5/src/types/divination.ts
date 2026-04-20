import type { ChartSnapshotDTO } from './chart';
import type { RuleHitDTO, StructuredAnalysisResultDTO, AnalysisContextDTO } from './rule';

/** 分析请求 */
export interface DivinationAnalyzeRequest {
  questionText: string;
  questionCategory?: string;
  userSelectedDirection?: string;
  detectedDirection?: string;
  finalDirection?: string;
  suggestedDirection?: string;
  divinationTime: string;
  divinationMethod: string;
  rawLines: string[];
  movingLines: number[];
}

/** 分析响应 */
export interface DivinationAnalyzeResponse {
  chartSnapshot: ChartSnapshotDTO;
  ruleHits: RuleHitDTO[];
  analysis: string;
  analysisContext: AnalysisContextDTO;
  structuredResult: StructuredAnalysisResultDTO;
}
