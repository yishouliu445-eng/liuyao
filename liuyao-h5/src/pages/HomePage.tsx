import { useNavigate } from 'react-router-dom';
import { useState } from 'react';
import type { SessionThreadDTO } from '../types/session';
import QuestionForm from '../components/input/QuestionForm';
import LoadingInk from '../components/feedback/LoadingInk';

type PageState = 'input' | 'loading';

export default function HomePage() {
  const navigate = useNavigate();
  const [pageState, setPageState] = useState<PageState>('input');
  const [error, setError] = useState<string | null>(null);

  function handleResult(data: SessionThreadDTO) {
    setPageState('input');
    setError(null);
    navigate(`/session/${data.sessionId}`);
  }

  function handleError(msg: string) {
    setError(msg);
    setPageState('input');
  }

  function handleLoading() {
    setPageState('loading');
    setError(null);
  }

  if (pageState === 'loading') {
    return <LoadingInk />;
  }

  return (
    <div className="page-input">
      <QuestionForm
        onLoading={handleLoading}
        onResult={handleResult}
        onError={handleError}
      />
      {error && <p className="error-text">{error}</p>}
    </div>
  );
}
