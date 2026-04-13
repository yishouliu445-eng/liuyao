import { Link } from 'react-router-dom';
import type { SessionListItemDTO } from '../../types/session';

interface Props {
  session: SessionListItemDTO;
}

function formatDateTime(value?: string) {
  if (!value) return '时间未知';
  return value.replace('T', ' ').slice(0, 16);
}

function summarizeQuestion(text: string) {
  const trimmed = text.trim();
  return trimmed || '未命名问题';
}

export default function SessionCard({ session }: Props) {
  return (
    <Link to={`/session/${session.sessionId}`} className="session-card">
      <div className="session-card-copy">
        <div className="session-card-header">
          <span className="session-card-kicker">{session.questionCategory || '未分类'}</span>
          <span className={`session-card-status session-card-status-${session.status || 'UNKNOWN'}`}>
            {session.status || 'UNKNOWN'}
          </span>
        </div>
        <h2 className="session-card-title">{summarizeQuestion(session.questionText)}</h2>
        <p className="session-card-meta">
          {session.messageCount} 条消息
          <span className="session-card-dot">·</span>
          最后活跃 {formatDateTime(session.lastActiveAt)}
        </p>
      </div>
      <span className="session-card-cta">继续追问</span>
    </Link>
  );
}
