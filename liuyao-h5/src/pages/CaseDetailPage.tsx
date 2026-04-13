import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import type { CaseDetailDTO } from '../types/case';
import { getCaseDetail } from '../api/cases';
import HexagramChart from '../components/chart/HexagramChart';
import AnalysisSection from '../components/analysis/AnalysisSection';
import VerdictBox from '../components/analysis/VerdictBox';

export default function CaseDetailPage() {
  const { caseId } = useParams();
  const [detail, setDetail] = useState<CaseDetailDTO | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!caseId) return;
    const id = Number(caseId);
    if (isNaN(id)) {
      setError('无效的案例 ID');
      return;
    }
    getCaseDetail(id)
      .then(setDetail)
      .catch((e) => setError(e.message));
  }, [caseId]);

  if (error) {
    return <p className="error-text">{error}</p>;
  }

  if (!detail) {
    return <p className="text-muted" style={{ textAlign: 'center', padding: 48 }}>加载中...</p>;
  }

  return (
    <div className="case-detail">
      <div className="case-detail-header">
        <Link to="/cases" className="nav-link" style={{ fontSize: 13, marginBottom: 12, display: 'block' }}>
          ← 返回案例列表
        </Link>
        <h1 className="case-detail-question">{detail.questionText}</h1>
        <div className="case-detail-meta">
          <span className="chart-meta-tag">{detail.questionCategory}</span>
          <span className="chart-meta-tag">{detail.status}</span>
          <span className="chart-meta-tag">{detail.divinationTime?.replace('T', ' ')}</span>
        </div>
      </div>

      {detail.chartSnapshot && <HexagramChart snapshot={detail.chartSnapshot} />}
      {detail.structuredResult && <VerdictBox structured={detail.structuredResult} />}
      {detail.analysis && (
        <AnalysisSection
          analysisText={detail.analysis}
          ruleHits={detail.ruleHits ?? []}
          structured={detail.structuredResult}
        />
      )}
    </div>
  );
}
