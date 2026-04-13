import { useState } from 'react';
import HexagramChart from './HexagramChart';
import type { ChartSnapshotDTO } from '../../types/chart';

interface Props {
  snapshot: ChartSnapshotDTO | null | undefined;
  questionText?: string;
  questionCategory?: string;
  status?: string;
  messageCount?: number;
  defaultOpen?: boolean;
}

export default function CollapsibleChart({
  snapshot,
  questionText,
  questionCategory,
  status,
  messageCount,
  defaultOpen = true,
}: Props) {
  const [open, setOpen] = useState(defaultOpen);

  if (!snapshot) return null;

  const meta = [
    questionCategory,
    status,
    messageCount != null ? `${messageCount} 条消息` : '',
  ].filter(Boolean);

  return (
    <section className="chart-collapsible animate-fade-in">
      <button
        type="button"
        className="chart-collapsible-toggle"
        onClick={() => setOpen((prev) => !prev)}
        aria-expanded={open}
      >
        <div className="chart-collapsible-copy">
          <span className="chart-collapsible-kicker">会话卦盘</span>
          <strong className="chart-collapsible-title">
            {snapshot.mainHexagram}
            <span className="chart-change-arrow">→</span>
            {snapshot.changedHexagram || '无变卦'}
          </strong>
          <p className="chart-collapsible-question">{questionText || snapshot.question}</p>
        </div>
        <div className="chart-collapsible-meta">
          {meta.map((item) => (
            <span key={item} className="chart-meta-tag">
              {item}
            </span>
          ))}
          <span className="chart-meta-tag">{open ? '点击收起' : '点击展开'}</span>
        </div>
      </button>

      {open && (
        <div className="chart-collapsible-body">
          <HexagramChart snapshot={snapshot} />
        </div>
      )}
    </section>
  );
}
