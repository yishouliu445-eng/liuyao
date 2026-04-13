import type { ChartSnapshotDTO } from './chart';

/** 规则命中 */
export interface RuleHitDTO {
  ruleId: string;
  ruleCode: string;
  ruleName: string;
  category: string;
  priority: number;
  hitReason: string;
  impactLevel: string;
  scoreDelta: number;
  tags: string[];
  evidence: Record<string, unknown>;
}

/** 分类评分汇总 */
export interface RuleCategorySummaryDTO {
  category: string;
  hitCount: number;
  score: number;
  effectiveHitCount: number;
  effectiveScore: number;
  stageOrder: number;
}

/** 冲突汇总 */
export interface RuleConflictSummaryDTO {
  category: string;
  positiveCount: number;
  negativeCount: number;
  positiveScore: number;
  negativeScore: number;
  netScore: number;
  decision: string;
  positiveRules: string[];
  negativeRules: string[];
  effectiveRules: string[];
  suppressedRules: string[];
}

/** 结构化分析结果 */
export interface StructuredAnalysisResultDTO {
  score: number;
  resultLevel: string;
  effectiveScore: number;
  effectiveResultLevel: string;
  tags: string[];
  effectiveRuleCodes: string[];
  suppressedRuleCodes: string[];
  summary: string;
  categorySummaries: RuleCategorySummaryDTO[];
  conflictSummaries: RuleConflictSummaryDTO[];
}

/** 分析上下文 */
export interface AnalysisContextDTO {
  contextVersion: string;
  question: string;
  questionCategory: string;
  useGod: string;
  mainHexagram: string;
  changedHexagram: string;
  chartSnapshot: ChartSnapshotDTO;
  ruleCount: number;
  ruleCodes: string[];
  knowledgeSnippets: string[];
  ruleHits: RuleHitDTO[];
  structuredResult: StructuredAnalysisResultDTO;
}
