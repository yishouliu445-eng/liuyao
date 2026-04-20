import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createSession } from '../api/sessions';
import LoadingInk from '../components/feedback/LoadingInk';
import AskedMatterForm from '../components/input/AskedMatterForm';
import DirectionConfirmationDialog from '../components/input/DirectionConfirmationDialog';
import EntryModeTabs, { type EntryMode } from '../components/input/EntryModeTabs';
import ManualDivinationForm from '../components/input/ManualDivinationForm';
import { buildCoinCastingPayload, createCoinCastResult, type CoinCastResult } from '../components/input/SimulatedCoinCasting';
import { useDirectionResolution } from '../components/input/useDirectionResolution';
import type { SessionThreadDTO } from '../types/session';

type PageState = 'input' | 'loading';

const DEFAULT_LINES = ['少阳', '少阴', '少阳', '少阴', '少阳', '少阴'];
const TOTAL_CASTS = 6;
const CASTING_DURATION_MS = 860;

function formatLocalDateTime(date = new Date(), withSeconds = false): string {
  const offset = date.getTimezoneOffset();
  const local = new Date(date.getTime() - offset * 60000);
  return local.toISOString().slice(0, withSeconds ? 19 : 16);
}

export default function HomePage() {
  const navigate = useNavigate();
  const castTimerRef = useRef<number | null>(null);
  const [pageState, setPageState] = useState<PageState>('input');
  const [error, setError] = useState<string | null>(null);
  const [entryMode, setEntryMode] = useState<EntryMode>('asked');
  const [questionText, setQuestionText] = useState('');
  const [direction, setDirection] = useState('');
  const [lines, setLines] = useState<string[]>(DEFAULT_LINES);
  const [movingLines, setMovingLines] = useState<Set<number>>(new Set());
  const [askedCasts, setAskedCasts] = useState<CoinCastResult[]>([]);
  const [isCasting, setIsCasting] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const { pendingResolution, consumeError, clearPendingResolution } = useDirectionResolution();
  const [divinationTime, setDivinationTime] = useState(() => formatLocalDateTime());

  useEffect(() => {
    return () => {
      if (castTimerRef.current !== null) {
        window.clearTimeout(castTimerRef.current);
      }
    };
  }, []);

  function handleResult(data: SessionThreadDTO) {
    setPageState('input');
    setError(null);
    navigate(`/session/${data.sessionId}`);
  }

  function handleError(message: string) {
    setError(message);
    setPageState('input');
  }

  function handleLoading() {
    setPageState('loading');
    setError(null);
  }

  function handleQuestionChange(value: string) {
    setQuestionText(value);
    if (error) {
      setError(null);
    }
  }

  function stopCastingMotion() {
    if (castTimerRef.current !== null) {
      window.clearTimeout(castTimerRef.current);
      castTimerRef.current = null;
    }
    setIsCasting(false);
  }

  function handleEntryModeChange(mode: EntryMode) {
    if (mode !== 'asked') {
      stopCastingMotion();
    }
    clearPendingResolution();
    setEntryMode(mode);
    setError(null);
  }

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

  function toggleLine(index: number) {
    setLines((prev) => {
      const next = [...prev];
      next[index] = next[index] === '老阳' || next[index] === '少阳' ? '少阴' : '少阳';
      return next;
    });
  }

  function resetAskedCasting() {
    stopCastingMotion();
    setAskedCasts([]);
  }

  function handleCoinCast() {
    if (!questionText.trim()) {
      setError('请先写下所问之事，再开始掷铜钱');
      return;
    }
    if (isCasting || submitting || askedCasts.length >= TOTAL_CASTS) {
      return;
    }

    setError(null);
    setIsCasting(true);
    castTimerRef.current = window.setTimeout(() => {
      setAskedCasts((prev) => [...prev, createCoinCastResult()]);
      setIsCasting(false);
      castTimerRef.current = null;
    }, CASTING_DURATION_MS);
  }

  async function submit(mode: EntryMode, finalDirection?: string, selectedDirectionOverride?: string) {
    const normalizedQuestion = questionText.trim();
    if (!normalizedQuestion) {
      handleError('请输入所问之事');
      return;
    }

    let payload:
      | {
          questionCategory?: string;
          userSelectedDirection?: string;
          finalDirection?: string;
          divinationTime: string;
          divinationMethod: string;
          rawLines: string[];
          movingLines: number[];
        }
      | undefined;

    if (mode === 'asked') {
      if (askedCasts.length < TOTAL_CASTS) {
        handleError('六摇未足，请先完成模拟掷铜钱');
        return;
      }

      const castingPayload = buildCoinCastingPayload(askedCasts);
      payload = {
        divinationTime: formatLocalDateTime(new Date(), true),
        divinationMethod: '模拟掷铜钱',
        rawLines: castingPayload.rawLines,
        movingLines: castingPayload.movingLines,
      };
    } else {
      const selectedDirection = selectedDirectionOverride ?? direction;
      payload = {
        questionCategory: selectedDirection || undefined,
        userSelectedDirection: selectedDirection || undefined,
        finalDirection,
        divinationTime: divinationTime.length <= 16 ? `${divinationTime}:00` : divinationTime,
        divinationMethod: '手工起卦',
        rawLines: lines,
        movingLines: Array.from(movingLines).map((index) => index + 1),
      };
    }

    handleLoading();
    setSubmitting(true);
    try {
      const data = await createSession({
        questionText: normalizedQuestion,
        ...payload,
      });
      if (mode === 'manual') {
        const resolvedDirection = data.finalDirection || data.questionCategory || direction;
        if (resolvedDirection) {
          setDirection(resolvedDirection);
        }
      }
      handleResult(data);
    } catch (err) {
      if (consumeError(err)) {
        setPageState('input');
        setError(null);
      } else {
        handleError(err instanceof Error ? err.message : '创建会话失败');
      }
    } finally {
      setSubmitting(false);
    }
  }

  function confirmSuggestedDirection() {
    const nextDirection = pendingResolution?.suggestedDirection || pendingResolution?.detectedDirection || direction;
    clearPendingResolution();
    setDirection(nextDirection);
    void submit('manual', nextDirection, nextDirection);
  }

  function keepSelectedDirection() {
    const nextDirection = pendingResolution?.userSelectedDirection || direction;
    clearPendingResolution();
    setDirection(nextDirection);
    void submit('manual', nextDirection || undefined, nextDirection || undefined);
  }

  if (pageState === 'loading') {
    return <LoadingInk />;
  }

  return (
    <div className="page-input">
      <EntryModeTabs value={entryMode} onChange={handleEntryModeChange} />
      {entryMode === 'asked' ? (
        <AskedMatterForm
          questionText={questionText}
          casts={askedCasts}
          isCasting={isCasting}
          submitting={submitting}
          onQuestionChange={handleQuestionChange}
          onCast={handleCoinCast}
          onResetCasting={resetAskedCasting}
          onSubmit={() => void submit('asked')}
        />
      ) : (
        <ManualDivinationForm
          questionText={questionText}
          direction={direction}
          divinationTime={divinationTime}
          lines={lines}
          movingLines={movingLines}
          submitting={submitting}
          onQuestionChange={handleQuestionChange}
          onDirectionChange={setDirection}
          onDivinationTimeChange={setDivinationTime}
          onToggleLine={toggleLine}
          onToggleMoving={toggleMoving}
          onSubmit={() => void submit('manual')}
        />
      )}
      {error && <p className="error-text">{error}</p>}
      <DirectionConfirmationDialog
        resolution={pendingResolution}
        onConfirmSuggested={confirmSuggestedDirection}
        onKeepSelected={keepSelectedDirection}
        onClose={clearPendingResolution}
      />
    </div>
  );
}
