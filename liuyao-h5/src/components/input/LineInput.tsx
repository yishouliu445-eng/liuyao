interface Props {
  lines: string[];
  movingLines: Set<number>;
  onToggleLine: (index: number) => void;
  onToggleMoving: (index: number) => void;
}

const YAO_NAMES = ['初爻', '二爻', '三爻', '四爻', '五爻', '上爻'];

export default function LineInput({ lines, movingLines, onToggleLine, onToggleMoving }: Props) {
  return (
    <div className="line-input-grid">
      {lines.map((line, i) => {
        const isYang = line === '老阳' || line === '少阳';
        const isMoving = movingLines.has(i);

        return (
          <div
            key={i}
            className={`line-cell ${isMoving ? 'active' : ''}`}
            onClick={() => onToggleLine(i)}
            onDoubleClick={(e) => {
              e.preventDefault();
              onToggleMoving(i);
            }}
          >
            <span className="line-cell-label">{YAO_NAMES[i]}</span>
            <span style={{ fontSize: 18 }}>
              {isYang ? '⚊' : '⚋'}
            </span>
            {isMoving && (
              <span style={{ fontSize: 10, color: 'var(--gold)' }}>动</span>
            )}
          </div>
        );
      })}
    </div>
  );
}
