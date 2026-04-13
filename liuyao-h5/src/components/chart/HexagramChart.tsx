import type { ChartSnapshotDTO } from '../../types/chart';
import HexagramLine from './HexagramLine';

interface Props {
  snapshot: ChartSnapshotDTO;
}

export default function HexagramChart({ snapshot }: Props) {
  const lines = snapshot.lines ?? [];
  // lines 从下到上排列（index 0 = 初爻），但 UI 从上到下显示，所以反转
  const displayLines = [...lines].reverse();

  return (
    <div className="chart-section animate-fade-in">
      <div className="chart-header">
        <div>
          <span className="chart-hexagram-name">{snapshot.mainHexagram}</span>
          <span className="chart-change-arrow">→</span>
          <span className="chart-hexagram-name">{snapshot.changedHexagram || '无变卦'}</span>
        </div>
        <div className="chart-meta-row">
          <span className="chart-meta-tag">{snapshot.palace}宫 · {snapshot.palaceWuXing}</span>
          <span className="chart-meta-tag">用神 {snapshot.useGod}</span>
          <span className="chart-meta-tag">世{snapshot.shi} 应{snapshot.ying}</span>
          <span className="chart-meta-tag">日辰 {snapshot.riChen}</span>
          <span className="chart-meta-tag">月建 {snapshot.yueJian}</span>
          {snapshot.kongWang?.length > 0 && (
            <span className="chart-meta-tag">空亡 {snapshot.kongWang.join(' ')}</span>
          )}
        </div>
      </div>

      <div className="hexagram-lines">
        {displayLines.map((line) => (
          <HexagramLine
            key={line.index}
            line={line}
            useGod={snapshot.useGod}
          />
        ))}
      </div>
    </div>
  );
}
