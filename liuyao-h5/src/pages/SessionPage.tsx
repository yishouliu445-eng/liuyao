import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import CollapsibleChart from '../components/chart/CollapsibleChart';
import ChatBubble from '../components/chat/ChatBubble';
import ChatInput from '../components/chat/ChatInput';
import SmartPromptBar from '../components/chat/SmartPromptBar';
import LoadingInk from '../components/feedback/LoadingInk';
import { buildOptimisticAssistantMessage, getSession, getSessionMessages, mergeSessionState, sendSessionMessage } from '../api/sessions';
import type { SessionMessageDTO, SessionThreadDTO } from '../types/session';

const DEFAULT_PROMPTS: Record<string, string[]> = {
  工作: ['这件事最先要看什么信号？', '如果推进，风险点在哪里？', '现在更适合主动还是等待？'],
  感情: ['对方真实态度是什么？', '关系的关键阻碍在哪里？', '接下来一周适合怎么做？'],
  财运: ['资金面最需要注意什么？', '有无明显回撤风险？', '适合继续投入还是先收手？'],
  出行: ['路上最可能出现什么变数？', '时间上有没有延迟迹象？', '现在出发是否合适？'],
};

const FALLBACK_PROMPTS: string[] = [
  '这卦最关键的判断是什么？',
  '如果要验证，应先观察什么？',
  '下一步最稳妥的动作是什么？',
];

function mergeMessageList(base: SessionMessageDTO[], incoming: SessionMessageDTO[]) {
  const merged: SessionMessageDTO[] = [];
  const seen = new Set<string>();

  for (const message of [...base, ...incoming]) {
    if (!seen.has(message.messageId)) {
      seen.add(message.messageId);
      merged.push(message);
    }
  }

  return merged;
}

function mergeThreadWithMessages(previous: SessionThreadDTO, next: SessionThreadDTO): SessionThreadDTO {
  return {
    ...previous,
    ...next,
    sessionId: next.sessionId || previous.sessionId,
    questionText: next.questionText || previous.questionText,
    questionCategory: next.questionCategory || previous.questionCategory,
    divinationTime: next.divinationTime || previous.divinationTime,
    divinationMethod: next.divinationMethod || previous.divinationMethod,
    status: next.status || previous.status,
    chartSnapshot: next.chartSnapshot || previous.chartSnapshot,
    structuredResult: next.structuredResult || previous.structuredResult,
    analysis: next.analysis || previous.analysis,
    smartPrompts: next.smartPrompts.length ? next.smartPrompts : previous.smartPrompts,
    messages: mergeMessageList(previous.messages, next.messages),
  };
}

export default function SessionPage() {
  const { id } = useParams<{ id: string }>();
  const [session, setSession] = useState<SessionThreadDTO | null>(null);
  const [messages, setMessages] = useState<SessionMessageDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) {
      setError('缺少会话 ID');
      setLoading(false);
      return;
    }

    const sessionId = id;
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);

      const [detailResult, messageResult] = await Promise.allSettled([
        getSession(sessionId),
        getSessionMessages(sessionId),
      ]);

      const detail = detailResult.status === 'fulfilled' ? detailResult.value : null;
      const detailMessages = detail?.messages ?? [];
      const apiMessages = messageResult.status === 'fulfilled' ? messageResult.value : [];
      const fallbackThread: SessionThreadDTO | null = !detail && apiMessages.length > 0
        ? {
            sessionId,
            questionText: apiMessages.find((message) => message.role === 'user')?.content || apiMessages[0]?.content || '',
            questionCategory: '',
            status: '进行中',
            chartSnapshot: apiMessages.find((message) => message.chartSnapshot)?.chartSnapshot ?? null,
            structuredResult: apiMessages.find((message) => message.structuredResult)?.structuredResult ?? null,
            analysis: apiMessages.find((message) => message.analysis)?.analysis,
            messages: apiMessages,
            smartPrompts: [],
          }
        : null;
      const merged = detail ? mergeSessionState(detail, [...detailMessages, ...apiMessages]) : fallbackThread;

      if (!cancelled) {
        if (!merged) {
          setSession(null);
          setMessages([]);
          setError(detailResult.status === 'rejected'
            ? (detailResult.reason instanceof Error ? detailResult.reason.message : '会话不存在')
            : '会话不存在');
        } else {
          setSession(merged);
          setMessages(merged.messages);
        }
        setLoading(false);
      }
    }

    void load();

    return () => {
      cancelled = true;
    };
  }, [id]);

  const prompts = useMemo<string[]>(() => {
    if (!session) return FALLBACK_PROMPTS.slice(0, 3);
    const promptSource = DEFAULT_PROMPTS[session.questionCategory];
    if (promptSource) {
      return promptSource.slice(0, 3);
    }
    if (session.smartPrompts.length > 0) {
      return session.smartPrompts.slice(0, 3);
    }
    return FALLBACK_PROMPTS.slice(0, 3);
  }, [session]);

  async function handleSend(content: string) {
    if (!id || !session || sending) return;

    const optimisticUserMessage: SessionMessageDTO = {
      messageId: `${id}-user-${Date.now()}`,
      sessionId: id,
      role: 'user',
      content,
      status: 'sent',
    };

    const optimisticAssistant = buildOptimisticAssistantMessage(id, '正在为你继续推演，请稍候。');

    setMessages((prev) => [...prev, optimisticUserMessage, optimisticAssistant]);
    setSending(true);
    setError(null);

    try {
      const nextSession = await sendSessionMessage(id, { content });
      const responseHasMatchingUser = nextSession.messages.some(
        (message) => message.role === 'user' && message.content.trim() === content.trim(),
      );
      const baseMessages = responseHasMatchingUser
        ? messages.filter((message) => message.status !== 'pending' && !(message.role === 'user' && message.content.trim() === content.trim()))
        : messages.filter((message) => message.status !== 'pending');
      const shouldReplaceThread = responseHasMatchingUser && nextSession.messages.length >= baseMessages.length;
      setSession((prev) => {
        if (!prev) return nextSession;
        return mergeThreadWithMessages(prev, nextSession);
      });
      setMessages(shouldReplaceThread ? nextSession.messages : mergeMessageList(baseMessages, nextSession.messages));
    } catch (e) {
      setError(e instanceof Error ? e.message : '发送失败');
      setMessages((prev) => prev.filter((item) => item.status !== 'pending'));
    } finally {
      setSending(false);
    }
  }

  if (loading) {
    return <LoadingInk />;
  }

  if (!session) {
    return (
      <div className="session-page">
        <p className="error-text">{error || '会话加载失败'}</p>
        <Link to="/" className="session-back-link">返回首页重新起卦</Link>
      </div>
    );
  }

  const firstAssistantIndex = messages.findIndex((message) => message.role === 'assistant');

  return (
    <div className="session-page">
      <header className="session-hero">
        <div className="session-hero-copy">
          <Link to="/" className="session-back-link">← 重新起卦</Link>
          <h1 className="session-title">{session.questionText || session.chartSnapshot?.question || '会话对话'}</h1>
          <p className="session-subtitle">
            {session.questionCategory || '未分类'} · {session.status || '进行中'} · {session.divinationTime?.replace('T', ' ') || '时间待定'}
          </p>
        </div>
        <div className="session-hero-stats">
          <span className="chart-meta-tag">{messages.length} 条消息</span>
          {session.chartSnapshot?.useGod && <span className="chart-meta-tag">用神 {session.chartSnapshot.useGod}</span>}
          {session.chartSnapshot?.palace && <span className="chart-meta-tag">{session.chartSnapshot.palace}宫</span>}
        </div>
      </header>

      <CollapsibleChart
        snapshot={session.chartSnapshot}
        questionText={session.questionText}
        questionCategory={session.questionCategory}
        status={session.status}
        messageCount={messages.length}
      />

      {error && <p className="error-text session-error">{error}</p>}

      <SmartPromptBar
        prompts={prompts}
        onPick={handleSend}
        disabled={sending}
      />

      <section className="chat-feed">
        {messages.map((message, index) => (
          <ChatBubble
            key={message.messageId}
            message={message}
            defaultOpen={index === firstAssistantIndex}
          />
        ))}
        {messages.length === 0 && (
          <p className="empty-hint">还没有消息，先从下方追问一轮。</p>
        )}
      </section>

      <div className="session-composer">
        <ChatInput
          onSend={handleSend}
          loading={sending}
          placeholder="继续追问这卦的关键细节、验证路径或时间窗口…"
        />
      </div>
    </div>
  );
}
