import type { RuleCategorySummaryDTO } from '../../types/rule';

interface Props {
  summaries: RuleCategorySummaryDTO[];
}

const CATEGORY_MAP: Record<string, string> = {
  YONGSHEN_STATE: '用神状态',
  SHI_STATE: '世爻状态',
  SHI_YING: '世应联系',
  MOVING_CHANGE: '动变影响',
  COMPOSITE: '综合裁定',
};

export default function CategoryBreakdown({ summaries }: Props) {
  const maxScore = Math.max(...summaries.map((s) => Math.abs(s.effectiveScore ?? s.score ?? 0)), 1);

  return (
    <div className="category-breakdown">
      {summaries.map((s) => {
        const score = s.effectiveScore ?? s.score ?? 0;
        const absScore = Math.abs(score);
        const pct = Math.round((absScore / maxScore) * 100);
        const label = CATEGORY_MAP[s.category] || s.category;
        const isNegative = score < 0;

        return (
          <div key={s.category} className="category-bar-row">
            <span className="category-bar-label">{label}</span>
            <div className="category-bar-track">
              <div 
                className="category-bar-fill" 
                style={{ 
                  width: `${pct}%`,
                  background: isNegative 
                    ? 'linear-gradient(90deg, rgba(196, 92, 92, 0.4), var(--danger))' 
                    : undefined
                }} 
              />
            </div>
            <span 
               className="category-bar-value" 
               style={{ color: isNegative ? 'var(--danger)' : 'var(--text-muted)' }}
            >
              {score}
            </span>
          </div>
        );
      })}
    </div>
  );
}
