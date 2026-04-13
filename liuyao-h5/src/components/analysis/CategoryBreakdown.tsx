import type { RuleCategorySummaryDTO } from '../../types/rule';
import { RULE_CATEGORY_MAP } from '../../constants/categories';

interface Props {
  summaries: RuleCategorySummaryDTO[];
}

export default function CategoryBreakdown({ summaries }: Props) {
  const maxScore = Math.max(...summaries.map((s) => Math.abs(s.effectiveScore ?? s.score ?? 0)), 1);

  return (
    <div className="category-breakdown">
      {summaries.map((s) => {
        const score = s.effectiveScore ?? s.score ?? 0;
        const absScore = Math.abs(score);
        const pct = Math.round((absScore / maxScore) * 100);
        const label = RULE_CATEGORY_MAP[s.category] || s.category;
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
