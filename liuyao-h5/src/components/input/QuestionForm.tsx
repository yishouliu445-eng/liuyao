import { useState } from 'react';
import type { SessionThreadDTO } from '../../types/session';
import { createSession } from '../../api/sessions';
import CategoryTags from './CategoryTags';
import LineInput from './LineInput';

interface Props {
  onLoading: () => void;
  onResult: (data: SessionThreadDTO) => void;
  onError: (msg: string) => void;
}

export default function QuestionForm({ onLoading, onResult, onError }: Props) {
  const [questionText, setQuestionText] = useState('');
  const [category, setCategory] = useState('出行');
  const [divinationTime, setDivinationTime] = useState(() => {
    const now = new Date();
    const offset = now.getTimezoneOffset();
    const local = new Date(now.getTime() - offset * 60000);
    return local.toISOString().slice(0, 16);
  });
  const [lines, setLines] = useState<string[]>([
    '少阳', '少阴', '少阳', '少阴', '少阳', '少阴',
  ]);
  const [movingLines, setMovingLines] = useState<Set<number>>(new Set());
  const [submitting, setSubmitting] = useState(false);

  function toggleMoving(index: number) {
    setMovingLines((prev) => {
      const next = new Set(prev);
      if (next.has(index)) {
        next.delete(index);
      } else {
        next.add(index);
      }
      return next;
    });
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!questionText.trim()) {
      onError('请输入问题');
      return;
    }
    onLoading();
    setSubmitting(true);
    try {
      const data = await createSession({
        questionText: questionText.trim(),
        questionCategory: category,
        divinationTime: divinationTime.length <= 16 ? divinationTime + ':00' : divinationTime,
        divinationMethod: '手工起卦',
        rawLines: lines,
        movingLines: Array.from(movingLines).map((i) => i + 1),
      });
      onResult(data);
    } catch (err) {
      onError(err instanceof Error ? err.message : '创建会话失败');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="question-form" onSubmit={handleSubmit}>
      <div>
        <label className="form-label">所问何事</label>
        <textarea
          className="form-textarea"
          value={questionText}
          onChange={(e) => setQuestionText(e.target.value)}
          placeholder="请描述你想占问的事..."
          rows={2}
        />
      </div>

      <div>
        <label className="form-label">问题分类</label>
        <CategoryTags value={category} onChange={setCategory} />
      </div>

      <div className="form-row">
        <div>
          <label className="form-label">起卦时间</label>
          <input
            className="form-input"
            type="datetime-local"
            value={divinationTime}
            onChange={(e) => setDivinationTime(e.target.value)}
          />
        </div>
        <div>
          <label className="form-label">起卦方式</label>
          <input
            className="form-input"
            type="text"
            value="手工起卦"
            readOnly
          />
        </div>
      </div>

      <div>
        <label className="form-label">六爻（点击切换阴阳，长按或双击设动爻）</label>
        <LineInput
          lines={lines}
          movingLines={movingLines}
          onToggleLine={(index) => {
            setLines((prev) => {
              const next = [...prev];
              next[index] = next[index] === '老阳' || next[index] === '少阳' ? '少阴' : '少阳';
              return next;
            });
          }}
          onToggleMoving={toggleMoving}
        />
      </div>

      <button className="submit-btn" type="submit">
        {submitting ? '进入会话中...' : '进入会话'}
      </button>
    </form>
  );
}
