import SimulatedCoinCasting, { type CoinCastResult } from './SimulatedCoinCasting';

interface Props {
  questionText: string;
  casts: CoinCastResult[];
  isCasting: boolean;
  submitting: boolean;
  onQuestionChange: (value: string) => void;
  onCast: () => void;
  onResetCasting: () => void;
  onSubmit: () => void;
}

export default function AskedMatterForm({
  questionText,
  casts,
  isCasting,
  submitting,
  onQuestionChange,
  onCast,
  onResetCasting,
  onSubmit,
}: Props) {
  const readyToSubmit = questionText.trim().length > 0 && casts.length === 6;

  return (
    <section className="entry-shell">
      <div className="entry-copy">
        <span className="entry-kicker">问事</span>
        <h1 className="entry-title">写下所问，随后掷钱成卦。</h1>
        <p className="entry-description">
          把事情写明即可，系统会依所问内容自定方向；若你熟悉六爻排法，可转入手工起卦自行设定。
        </p>
      </div>

      <div className="entry-layout entry-layout-single">
        <div className="entry-main entry-main-wide">
          <label className="form-label">所问何事</label>
          <textarea
            className="form-textarea"
            value={questionText}
            onChange={(event) => onQuestionChange(event.target.value)}
            placeholder="例如：这段感情往后还有没有转机？"
            rows={3}
          />

          <div className="entry-inline-note asked-note">
            <span className="entry-inline-note-label">写明即可</span>
            <p>不必先定问题方向，也不必先记爻位名称。先把所问写清，再起六摇。</p>
          </div>

          <SimulatedCoinCasting
            casts={casts}
            isCasting={isCasting}
            submitting={submitting}
            disabled={!questionText.trim()}
            onCast={onCast}
            onReset={onResetCasting}
          />

          <button className="submit-btn" type="button" onClick={onSubmit} disabled={!readyToSubmit || isCasting || submitting}>
            {submitting ? '入卦问事中...' : readyToSubmit ? '六摇既成，入卦问事' : '先完成六次掷铜钱'}
          </button>
        </div>
      </div>
    </section>
  );
}
