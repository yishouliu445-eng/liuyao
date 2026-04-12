import type { RuleHitDTO } from '../../types/rule';

interface Props {
  hit: RuleHitDTO;
}

export default function RuleCard({ hit }: Props) {
  const delta = hit.scoreDelta ?? 0;
  const deltaClass = delta >= 0 ? 'positive' : 'negative';
  const deltaText = delta >= 0 ? `+${delta}` : String(delta);

  return (
    <div className="rule-card">
      <div className="rule-card-header">
        <span className="rule-card-name">{hit.ruleName}</span>
        <span className={`rule-card-delta ${deltaClass}`}>{deltaText}</span>
      </div>
      <p className="rule-card-reason">{hit.hitReason || '无命中说明'}</p>
      <div className="rule-card-tags">
        <span className="rule-card-tag">{hit.ruleCode}</span>
        <span className="rule-card-tag">{hit.category}</span>
        <span className="rule-card-tag">{hit.impactLevel}</span>
      </div>
    </div>
  );
}
