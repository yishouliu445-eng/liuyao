import type { StructuredAnalysisResultDTO } from '../../types/rule';

interface Props {
  structured: StructuredAnalysisResultDTO;
}

const LEVEL_MAP: Record<string, string> = {
  'STRONG_POSITIVE': '大吉',
  'POSITIVE': '吉',
  'GOOD': '吉',
  'WEAK_POSITIVE': '小吉',
  'NEUTRAL': '平',
  'WEAK_NEGATIVE': '小凶',
  'NEGATIVE': '凶',
  'BAD': '凶',
  'STRONG_NEGATIVE': '大凶',
};

export default function VerdictBox({ structured }: Props) {
  if (!structured) return null;

  const score = structured.effectiveScore ?? structured.score ?? 0;
  const level = structured.effectiveResultLevel ?? structured.resultLevel ?? 'NEUTRAL';
  const levelText = LEVEL_MAP[level] ?? level;
  const tags = structured.tags ?? [];

  return (
    <div className="verdict-box animate-fade-in-delay-1">
      <div style={{ display: 'flex', alignItems: 'baseline', gap: 16 }}>
        <span className="verdict-score">{score}</span>
        <span className="verdict-level">{levelText}</span>
      </div>
      {tags.length > 0 && (
        <div className="verdict-tags">
          {tags.map((tag) => (
            <span key={tag} className="verdict-tag">{tag}</span>
          ))}
        </div>
      )}
    </div>
  );
}
