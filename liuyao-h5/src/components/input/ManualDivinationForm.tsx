import CategoryTags from './CategoryTags';
import LineInput from './LineInput';

interface Props {
  questionText: string;
  direction: string;
  divinationTime: string;
  lines: string[];
  movingLines: Set<number>;
  submitting: boolean;
  onQuestionChange: (value: string) => void;
  onDirectionChange: (value: string) => void;
  onDivinationTimeChange: (value: string) => void;
  onToggleLine: (index: number) => void;
  onToggleMoving: (index: number) => void;
  onSubmit: () => void;
}

export default function ManualDivinationForm({
  questionText,
  direction,
  divinationTime,
  lines,
  movingLines,
  submitting,
  onQuestionChange,
  onDirectionChange,
  onDivinationTimeChange,
  onToggleLine,
  onToggleMoving,
  onSubmit,
}: Props) {
  return (
    <section className="entry-shell">
      <div className="entry-copy">
        <span className="entry-kicker">手工起卦</span>
        <h1 className="entry-title">所问既明，再定六爻。</h1>
        <p className="entry-description">
          适合已经熟悉排爻方式的用户；问题方向可不设，提交时仍会按所问内容和你给定的方向一并校核。
        </p>
      </div>

      <div className="entry-layout">
        <div className="entry-main">
          <label className="form-label">所问何事</label>
          <textarea
            className="form-textarea"
            value={questionText}
            onChange={(event) => onQuestionChange(event.target.value)}
            placeholder="请写下所问之事"
            rows={3}
          />

          <div>
            <label className="form-label">问题方向</label>
            <CategoryTags value={direction} onChange={onDirectionChange} allowUnset />
          </div>

          <div className="manual-line-panel">
            <div className="manual-line-panel-head">
              <label className="form-label">六爻（点击切换阴阳，双击设动爻）</label>
              <span>由下而上依次设定</span>
            </div>
            <LineInput
              lines={lines}
              movingLines={movingLines}
              onToggleLine={onToggleLine}
              onToggleMoving={onToggleMoving}
            />
          </div>

          <div className="form-row">
            <div>
              <label className="form-label">起卦时间</label>
              <input
                className="form-input"
                type="datetime-local"
                value={divinationTime}
                onChange={(event) => onDivinationTimeChange(event.target.value)}
              />
            </div>
            <div>
              <label className="form-label">起卦方式</label>
              <input className="form-input" type="text" value="手工起卦" readOnly />
            </div>
          </div>

          <button className="submit-btn" type="button" onClick={onSubmit} disabled={submitting}>
            {submitting ? '开始起卦中...' : '开始起卦'}
          </button>
        </div>

        <aside className="entry-side">
          <div className="entry-side-card">
            <span className="entry-side-label">当前重心</span>
            <p>六爻排盘区为主操作区，问题方向只作辅助，不再强制先选。</p>
          </div>
          <div className="entry-side-card">
            <span className="entry-side-label">方向校核</span>
            <p>若你所定方向与所问内容明显不合，提交前会轻提示一次。</p>
          </div>
        </aside>
      </div>
    </section>
  );
}
