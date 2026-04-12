import { useState } from 'react';

export default function FeedbackStrip() {
  const [useful, setUseful] = useState<boolean | null>(null);

  function handleClick(value: boolean) {
    setUseful(value);
    try {
      const records = JSON.parse(localStorage.getItem('liuyao_feedback') || '[]');
      records.push({ useful: value, ts: Date.now() });
      localStorage.setItem('liuyao_feedback', JSON.stringify(records));
    } catch {
      // ignore
    }
  }

  return (
    <div className="feedback-strip">
      <span className="text-muted" style={{ fontSize: 13, lineHeight: '32px' }}>分析有帮助吗？</span>
      <button
        className={`feedback-btn ${useful === true ? 'selected' : ''}`}
        onClick={() => handleClick(true)}
        disabled={useful !== null}
      >
        有用
      </button>
      <button
        className={`feedback-btn ${useful === false ? 'selected' : ''}`}
        onClick={() => handleClick(false)}
        disabled={useful !== null}
      >
        一般
      </button>
    </div>
  );
}
