import type { ChartSnapshotDTO } from './chart';
import type { RuleHitDTO, StructuredAnalysisResultDTO, AnalysisContextDTO } from './rule';

/** 案例摘要（列表项） */
export interface CaseSummaryDTO {
  caseId: number;
  questionText: string;
  questionCategory: string;
  divinationTime: string;
  status: string;
  mainHexagram: string;
  changedHexagram: string;
  palace: string;
  useGod: string;
}

/** 案例列表分页响应 */
export interface CaseListResponseDTO {
  page: number;
  size: number;
  total: number;
  items: CaseSummaryDTO[];
}

/** 案例详情 */
export interface CaseDetailDTO {
  caseId: number;
  questionText: string;
  questionCategory: string;
  divinationTime: string;
  status: string;
  chartSnapshot: ChartSnapshotDTO;
  ruleHits: RuleHitDTO[];
  analysis: string;
  analysisContext: AnalysisContextDTO;
  structuredResult: StructuredAnalysisResultDTO;
}
