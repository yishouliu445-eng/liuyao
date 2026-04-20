import type { ChartSnapshotDTO } from '../../types/chart';
import HexagramLine from './HexagramLine';

interface Props {
  snapshot: ChartSnapshotDTO;
}

function formatLineIndexes(lineIndexes: number[]): string {
  if (!lineIndexes.length) return '';
  return `第${lineIndexes.join('、')}爻`;
}

function formatShenShaScope(scope: string): string {
  if (scope === 'line') return '爻位';
  if (scope === 'hexagram') return '卦体';
  if (scope === 'use_god') return '用神';
  return scope || '盘面';
}

function formatMatchedBy(matchedBy: string): string {
  if (matchedBy === 'riChen') return '日辰起例';
  if (matchedBy === 'yueJian') return '月建起例';
  return matchedBy || '规则命中';
}

export default function HexagramChart({ snapshot }: Props) {
  const lines = snapshot.lines ?? [];
  const derivedHexagrams = [
    { key: 'mutual', label: '互卦', name: snapshot.mutualHexagram, code: snapshot.mutualHexagramCode },
    { key: 'opposite', label: '错卦', name: snapshot.oppositeHexagram, code: snapshot.oppositeHexagramCode },
    { key: 'reversed', label: '综卦', name: snapshot.reversedHexagram, code: snapshot.reversedHexagramCode },
  ].filter((item) => item.name);
  const shenShaHits = snapshot.shenShaHits ?? [];
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

      {(derivedHexagrams.length > 0 || shenShaHits.length > 0) && (
        <div className="chart-phase-grid">
          {derivedHexagrams.length > 0 && (
            <section className="chart-phase-card">
              <div className="chart-phase-head">
                <span className="chart-phase-title">衍生卦</span>
                <span className="chart-phase-caption">phase two</span>
              </div>
              <div className="chart-derived-list">
                {derivedHexagrams.map((item) => (
                  <div key={item.key} className="chart-derived-item">
                    <span className="chart-derived-label">{item.label}</span>
                    <strong className="chart-derived-name">{item.name}</strong>
                    {item.code && <span className="chart-derived-code">{item.code}</span>}
                  </div>
                ))}
              </div>
            </section>
          )}

          {shenShaHits.length > 0 && (
            <section className="chart-phase-card">
              <div className="chart-phase-head">
                <span className="chart-phase-title">神煞命中</span>
                <span className="chart-phase-caption">{shenShaHits.length} 项</span>
              </div>
              <div className="chart-shensha-list">
                {shenShaHits.map((hit) => (
                  <article key={`${hit.code}-${hit.branch}-${hit.lineIndexes.join('-')}`} className="chart-shensha-item">
                    <div className="chart-shensha-top">
                      <strong className="chart-shensha-name">{hit.name}</strong>
                      <div className="chart-shensha-tags">
                        <span className="chart-meta-tag">{formatShenShaScope(hit.scope)}</span>
                        {hit.branch && <span className="chart-meta-tag">{hit.branch}</span>}
                        {hit.lineIndexes.length > 0 && (
                          <span className="chart-meta-tag">{formatLineIndexes(hit.lineIndexes)}</span>
                        )}
                      </div>
                    </div>
                    <p className="chart-shensha-summary">
                      {hit.summary || `${hit.name}命中，按${formatMatchedBy(hit.matchedBy)}落在${hit.branch}。`}
                    </p>
                  </article>
                ))}
              </div>
            </section>
          )}
        </div>
      )}

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
