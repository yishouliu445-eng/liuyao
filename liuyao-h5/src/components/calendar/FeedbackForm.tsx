import { useMemo, useState, type FormEvent } from 'react';
import type { VerificationAccuracy, VerificationEventDTO, VerificationFeedbackSubmitRequestDTO } from '../../types/calendar';
import { submitFeedback } from '../../api/calendar';

interface Props {
  event: VerificationEventDTO;
  onSubmitted: (event: VerificationEventDTO) => void;
}

const ACCURACY_OPTIONS: Array<{
  value: VerificationAccuracy;
  label: string;
  description: string;
}> = [
  { value: 'ACCURATE', label: '很准', description: '应期、方向都比较吻合' },
  { value: 'PARTIALLY_ACCURATE', label: '部分准', description: '方向对了，但细节有偏差' },
  { value: 'INACCURATE', label: '不太准', description: '结果与预测差异较大' },
  { value: 'UNSURE', label: '不确定', description: '暂时还看不清结果' },
];

const TAG_OPTIONS = ['时间准', '方向准', '比预期好', '比预期差', '完全没发生', '有变化但不同'];

export default function FeedbackForm({ event, onSubmitted }: Props) {
  const [accuracy, setAccuracy] = useState<VerificationAccuracy>('PARTIALLY_ACCURATE');
  const [actualOutcome, setActualOutcome] = useState('');
  const [tags, setTags] = useState<string[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const selectedTagSet = useMemo(() => new Set(tags), [tags]);

  function toggleTag(tag: string) {
    setTags((prev) => prev.includes(tag) ? prev.filter((item) => item !== tag) : [...prev, tag]);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (submitting) return;
    setSubmitting(true);
    setError(null);

    const payload: VerificationFeedbackSubmitRequestDTO = {
      accuracy,
      actualOutcome: actualOutcome.trim(),
      tags,
    };

    try {
      const updated = await submitFeedback(event.eventId, payload);
      onSubmitted(updated);
      setActualOutcome('');
      setTags([]);
      setAccuracy('PARTIALLY_ACCURATE');
    } catch (err) {
      setError(err instanceof Error ? err.message : '反馈提交失败');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="feedback-form" onSubmit={handleSubmit}>
      <header className="feedback-form-header">
        <div>
          <p className="calendar-kicker">反馈表单</p>
          <h3 className="feedback-form-title">您怎么看这次应期</h3>
        </div>
        <span className="feedback-form-chip">{event.predictedDate.slice(0, 10)}</span>
      </header>

      <p className="feedback-form-summary">AI 预测：{event.predictionSummary || '暂无摘要'}</p>

      <div className="feedback-form-section">
        <p className="feedback-form-label">实际结果如何？</p>
        <div className="feedback-choices">
          {ACCURACY_OPTIONS.map((option) => (
            <button
              key={option.value}
              type="button"
              className={`feedback-choice ${accuracy === option.value ? 'active' : ''}`}
              onClick={() => setAccuracy(option.value)}
            >
              <span className="feedback-choice-label">{option.label}</span>
              <span className="feedback-choice-desc">{option.description}</span>
            </button>
          ))}
        </div>
      </div>

      <div className="feedback-form-section">
        <p className="feedback-form-label">标签（可多选）</p>
        <div className="feedback-tags">
          {TAG_OPTIONS.map((tag) => (
            <button
              key={tag}
              type="button"
              className={`feedback-tag ${selectedTagSet.has(tag) ? 'active' : ''}`}
              onClick={() => toggleTag(tag)}
            >
              {tag}
            </button>
          ))}
        </div>
      </div>

      <div className="feedback-form-section">
        <label className="feedback-form-label" htmlFor={`feedback-${event.eventId}`}>
          补充说明（可选）
        </label>
        <textarea
          id={`feedback-${event.eventId}`}
          className="feedback-textarea"
          value={actualOutcome}
          onChange={(e) => setActualOutcome(e.target.value)}
          placeholder="简单写下实际发生了什么，帮助后续回看。"
          rows={4}
          maxLength={200}
        />
      </div>

      {error && <p className="error-text feedback-error">{error}</p>}

      <button type="submit" className="feedback-submit-btn" disabled={submitting}>
        {submitting ? '提交中...' : '提交反馈'}
      </button>
    </form>
  );
}
