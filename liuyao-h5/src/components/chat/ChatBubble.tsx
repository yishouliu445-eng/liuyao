import AnalysisSection from '../analysis/AnalysisSection';
import VerdictBox from '../analysis/VerdictBox';
import type { SessionMessageDTO } from '../../types/session';

interface Props {
  message: SessionMessageDTO;
  defaultOpen?: boolean;
}

function formatTime(value?: string) {
  if (!value) return '';
  const normalized = value.replace('T', ' ');
  return normalized.length > 16 ? normalized.slice(0, 16) : normalized;
}

export default function ChatBubble({ message, defaultOpen = false }: Props) {
  const isUser = message.role === 'user';
  const isSystem = message.role === 'system';
  const hasStructured = Boolean(message.structuredResult || message.ruleHits?.length);
  const hasAnalysis = Boolean(message.analysis);

  return (
    <article className={`chat-bubble ${isUser ? 'chat-bubble-user' : 'chat-bubble-assistant'} ${isSystem ? 'chat-bubble-system' : ''}`}>
      <header className="chat-bubble-header">
        <span className="chat-bubble-role">
          {isUser ? '我' : isSystem ? '系统' : 'AI'}
        </span>
        {message.createdAt && (
          <span className="chat-bubble-time">{formatTime(message.createdAt)}</span>
        )}
      </header>

      <div className="chat-bubble-content">
        {message.content && <p className="chat-bubble-text">{message.content}</p>}

        {!isUser && message.status === 'pending' && (
          <p className="chat-bubble-hint">正在整理排盘与应答……</p>
        )}

        {!isUser && hasStructured && (
          <details className="chat-bubble-details" open={defaultOpen}>
            <summary>展开结构化结果</summary>
            {message.structuredResult && <VerdictBox structured={message.structuredResult} />}
            {message.analysis && message.structuredResult && (
              <AnalysisSection
                analysisText={message.analysis}
                ruleHits={message.ruleHits ?? []}
                structured={message.structuredResult}
              />
            )}
          </details>
        )}

        {!isUser && !hasStructured && hasAnalysis && (
          <p className="chat-bubble-analysis">{message.analysis}</p>
        )}
      </div>
    </article>
  );
}
