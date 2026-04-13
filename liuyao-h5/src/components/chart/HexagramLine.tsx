import type { LineInfoDTO } from '../../types/chart';

interface Props {
  line: LineInfoDTO;
  useGod: string;
}

export default function HexagramLine({ line, useGod }: Props) {
  const isYang = line.yinYang === '阳';
  const isMoving = line.moving;
  const isUseGod = line.liuQin === useGod;
  const isShi = line.shi;
  const isYing = line.ying;

  return (
    <div className={`hexagram-line-row ${isUseGod ? 'use-god' : ''}`}>
      <span className="line-label liu-shen">{line.liuShen}</span>
      <span className="line-label">{line.liuQin}</span>
      <span className="line-label">{line.branch}</span>

      <div className="yao-drawing">
        {isYang ? (
          <div className="yao-yang" />
        ) : (
          <div className="yao-yin">
            <div className="yao-yin-segment" />
            <div className="yao-yin-segment" />
          </div>
        )}
        {isMoving && <div className="yao-moving-marker" />}
      </div>

      <span className="line-change">
        {isMoving && line.changeBranch ? `${line.changeBranch}` : ''}
      </span>

      <span className="line-label shi-ying">
        {isShi ? '世' : isYing ? '应' : ''}
      </span>
    </div>
  );
}
