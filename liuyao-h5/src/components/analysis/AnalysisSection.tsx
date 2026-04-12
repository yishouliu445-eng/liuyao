import type { RuleHitDTO, StructuredAnalysisResultDTO } from '../../types/rule';
import RuleCard from './RuleCard';
import CategoryBreakdown from './CategoryBreakdown';
import ConflictNote from './ConflictNote';

interface Props {
  analysisText: string;
  ruleHits: RuleHitDTO[];
  structured: StructuredAnalysisResultDTO;
}

export default function AnalysisSection({ analysisText, ruleHits, structured }: Props) {
  return (
    <div className="analysis-section animate-fade-in-delay-2">
      <p className="section-title">分析详情</p>

      <div className="analysis-text">{analysisText}</div>

      {structured?.categorySummaries && structured.categorySummaries.length > 0 && (
        <>
          <p className="section-title">分类评分</p>
          <CategoryBreakdown summaries={structured.categorySummaries} />
        </>
      )}

      {structured?.conflictSummaries && structured.conflictSummaries.length > 0 && (
        <>
          <p className="section-title">冲突裁剪</p>
          {structured.conflictSummaries.map((cs, i) => (
            <ConflictNote key={i} summary={cs} />
          ))}
        </>
      )}

      {ruleHits && ruleHits.length > 0 && (
        <>
          <p className="section-title">规则命中 ({ruleHits.length})</p>
          <div className="rule-cards">
            {ruleHits.map((hit, i) => (
              <RuleCard key={hit.ruleCode + i} hit={hit} />
            ))}
          </div>
        </>
      )}
    </div>
  );
}
