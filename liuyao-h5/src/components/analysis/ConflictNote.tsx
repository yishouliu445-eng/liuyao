import type { RuleConflictSummaryDTO } from '../../types/rule';

interface Props {
  summary: RuleConflictSummaryDTO;
}

const DECISION_MAP: Record<string, string> = {
  'POSITIVE_DOMINANT': '利好主导',
  'NEGATIVE_DOMINANT': '利空主导',
  'MIXED': '多空交织',
};

const CATEGORY_MAP: Record<string, string> = {
  YONGSHEN_STATE: '用神状态',
  SHI_STATE: '世爻状态',
  SHI_YING: '世应联系',
  MOVING_CHANGE: '动变影响',
  COMPOSITE: '综合裁定',
};

export default function ConflictNote({ summary }: Props) {
  const decision = DECISION_MAP[summary.decision] ?? summary.decision;
  const netText = summary.netScore != null ? `净得分 ${summary.netScore}` : '';
  const categoryLabel = CATEGORY_MAP[summary.category] || summary.category;

  return (
    <div className="conflict-note">
      <strong>{categoryLabel}</strong>：{decision}
      {netText && ` · ${netText}`}
      {summary.positiveCount > 0 && ` · 利好 ${summary.positiveCount} 条 (+${summary.positiveScore})`}
      {summary.negativeCount > 0 && ` · 利空 ${summary.negativeCount} 条 (${summary.negativeScore})`}
    </div>
  );
}
