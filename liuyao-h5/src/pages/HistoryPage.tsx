import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import SessionCard from '../components/history/SessionCard';
import LoadingInk from '../components/feedback/LoadingInk';
import { listSessions } from '../api/sessions';
import type { SessionListItemDTO } from '../types/session';

export default function HistoryPage() {
  const [sessions, setSessions] = useState<SessionListItemDTO[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);

      try {
        const response = await listSessions({ page: 0, size: 20 });
        if (cancelled) return;
        setSessions(response.items);
        setTotal(response.total);
      } catch (e) {
        if (cancelled) return;
        setSessions([]);
        setTotal(0);
        setError(e instanceof Error ? e.message : '加载历史会话失败');
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void load();

    return () => {
      cancelled = true;
    };
  }, []);

  if (loading) {
    return <LoadingInk />;
  }

  return (
    <div className="history-page">
      <header className="history-hero">
        <div className="history-hero-copy">
          <Link to="/" className="history-back-link">← 回到起卦</Link>
          <p className="history-kicker">历史会话</p>
          <h1 className="history-title">继续追问你关心的每一卦</h1>
          <p className="history-subtitle">
            共 {total} 个会话，最近 20 条默认展示。
          </p>
        </div>
        <Link to="/" className="history-primary-link">重新起卦</Link>
      </header>

      {error && <p className="error-text history-error">{error}</p>}

      {!error && sessions.length === 0 && (
        <div className="history-empty">
          <p className="empty-hint">还没有历史会话，先起一卦再回来看看。</p>
          <Link to="/" className="history-empty-link">去起卦</Link>
        </div>
      )}

      {sessions.length > 0 && (
        <section className="history-list">
          {sessions.map((session) => (
            <SessionCard key={session.sessionId} session={session} />
          ))}
        </section>
      )}
    </div>
  );
}
