import cashCoinsPhoto from '../../assets/chinese_cash_coins_a.jpg';

const TOTAL_CASTS = 6;
const COUNT_NAMES = ['零', '一', '二', '三', '四', '五', '六'];

export type CoinFace = 'yin' | 'yang';
export type CoinLine = '少阴' | '少阳';

export interface CoinCastResult {
  id: string;
  coins: CoinFace[];
  line: CoinLine;
  moving: boolean;
}

export interface SimulatedCoinCastingProps {
  casts: CoinCastResult[];
  isCasting: boolean;
  submitting: boolean;
  disabled?: boolean;
  onCast: () => void;
  onReset: () => void;
}

function countName(count: number): string {
  return COUNT_NAMES[count] ?? String(count);
}

function castHeading(castCount: number, isCasting: boolean, disabled: boolean): string {
  if (isCasting) {
    return '铜钱将定';
  }
  if (castCount >= TOTAL_CASTS) {
    return '六摇既足';
  }
  if (disabled && castCount === 0) {
    return '先写所问';
  }
  return `请起第${countName(castCount + 1)}摇`;
}

function castDescription(castCount: number, isCasting: boolean, disabled: boolean): string {
  if (isCasting) {
    return '三枚铜钱已掷出，稍候片刻，这一摇便会落定。';
  }
  if (castCount >= TOTAL_CASTS) {
    return '六摇俱足，可以据此入卦问事。';
  }
  if (disabled && castCount === 0) {
    return '先把事情写明，再开第一摇。';
  }
  const remaining = TOTAL_CASTS - castCount;
  return `三枚铜钱，连摇六次。此刻还余${countName(remaining)}摇。`;
}

function castActionLabel(castCount: number, isCasting: boolean): string {
  if (isCasting) {
    return '此摇将定';
  }
  if (castCount >= TOTAL_CASTS) {
    return '六摇已足';
  }
  return `起第${countName(castCount + 1)}摇`;
}

function latestCastText(lastCast?: CoinCastResult): string {
  if (!lastCast) {
    return '落下第一摇之后，卦象便会从下往上渐次显出。';
  }
  if (lastCast.moving) {
    return '最近一摇气势有变，这一层变化已收入卦中。';
  }
  return '最近一摇已经落定，卦上又添了一画。';
}

export function createCoinCastResult(random: () => number = Math.random): CoinCastResult {
  const coins = Array.from({ length: 3 }, () => (random() >= 0.5 ? 'yang' : 'yin')) as CoinFace[];
  const yangCount = coins.filter((face) => face === 'yang').length;
  const line: CoinLine = yangCount === 1 || yangCount === 3 ? '少阳' : '少阴';
  const moving = yangCount === 0 || yangCount === 3;

  return {
    id: `cast-${Date.now()}-${Math.round(random() * 1_000_000)}`,
    coins,
    line,
    moving,
  };
}

export function buildCoinCastingPayload(casts: CoinCastResult[]) {
  return {
    rawLines: casts.map((cast) => cast.line),
    movingLines: casts.flatMap((cast, index) => (cast.moving ? [index + 1] : [])),
  };
}

export default function SimulatedCoinCasting({
  casts,
  isCasting,
  submitting,
  disabled = false,
  onCast,
  onReset,
}: SimulatedCoinCastingProps) {
  const castCount = casts.length;
  const lastCast = castCount > 0 ? casts[castCount - 1] : undefined;
  const canCast = !disabled && !isCasting && !submitting && castCount < TOTAL_CASTS;
  const canReset = castCount > 0 && !isCasting && !submitting;

  return (
    <section className="coin-casting-panel">
      <div className="coin-casting-head">
        <div>
          <span className="entry-side-label">掷钱成卦</span>
          <h2 className="coin-casting-title">{castHeading(castCount, isCasting, disabled)}</h2>
          <p className="coin-casting-text">{castDescription(castCount, isCasting, disabled)}</p>
        </div>

        <div className="coin-progress">
          <strong>{castCount}/{TOTAL_CASTS}</strong>
          <span>已成之画</span>
        </div>
      </div>

      <div className={`coin-photo-strip ${isCasting ? 'rolling' : ''}`} aria-hidden="true">
        <div className="coin-photo-frame coin-photo-frame-a">
          <img className="coin-photo coin-photo-a" src={cashCoinsPhoto} alt="" />
        </div>
        <div className="coin-photo-frame coin-photo-frame-b">
          <img className="coin-photo coin-photo-b" src={cashCoinsPhoto} alt="" />
        </div>
        <div className="coin-photo-frame coin-photo-frame-c">
          <img className="coin-photo coin-photo-c" src={cashCoinsPhoto} alt="" />
        </div>
      </div>

      <div className="coin-casting-actions">
        <button className="coin-cast-trigger" type="button" onClick={onCast} disabled={!canCast}>
          {castActionLabel(castCount, isCasting)}
        </button>
        <button className="coin-reset-btn" type="button" onClick={onReset} disabled={!canReset}>
          重整六摇
        </button>
      </div>

      <div className="coin-preview-panel">
        <div className="coin-preview-copy">
          <span className="entry-side-label">卦象渐成</span>
          <p className="coin-preview-text">{latestCastText(lastCast)}</p>
        </div>

        <div className="coin-line-stack" aria-label="当前已成卦象">
          {Array.from({ length: TOTAL_CASTS }, (_, slotIndex) => {
            const cast = casts[TOTAL_CASTS - 1 - slotIndex];
            const isYang = cast?.line === '少阳';
            const isFilled = Boolean(cast);

            return (
              <div
                key={cast?.id ?? `slot-${slotIndex}`}
                className={`coin-line-row ${isFilled ? 'filled' : 'empty'}`}
              >
                <div className={`coin-line ${isYang ? 'yang' : 'yin'} ${cast?.moving ? 'moving' : ''}`}>
                  <span className="coin-line-segment" />
                  {!isYang && <span className="coin-line-gap" />}
                  <span className="coin-line-segment" />
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
