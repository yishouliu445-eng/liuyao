import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import type { CaseListResponseDTO, CaseSummaryDTO } from '../types/case';
import { searchCases } from '../api/cases';
import { QUESTION_CATEGORIES } from '../constants/categories';

export default function CasesPage() {
  const [category, setCategory] = useState('');
  const [response, setResponse] = useState<CaseListResponseDTO | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    searchCases({ questionCategory: category || undefined, page: 1, size: 20 })
      .then(setResponse)
      .catch(() => setResponse(null))
      .finally(() => setLoading(false));
  }, [category]);

  return (
    <div className="cases-page">
      <div className="cases-header">
        <h1 className="cases-title">案例回放</h1>
        <select
          className="cases-filter"
          value={category}
          onChange={(e) => setCategory(e.target.value)}
        >
          <option value="">全部分类</option>
          {QUESTION_CATEGORIES.map((c) => (
            <option key={c.value} value={c.value}>{c.label}</option>
          ))}
        </select>
      </div>

      {loading && <p className="text-muted" style={{ textAlign: 'center', padding: 32 }}>加载中...</p>}

      {!loading && response && response.items.length === 0 && (
        <p className="text-muted" style={{ textAlign: 'center', padding: 32 }}>暂无案例</p>
      )}

      {!loading && response && (
        <div className="case-list">
          {response.items.map(renderCaseCard)}
        </div>
      )}
    </div>
  );
}

function renderCaseCard(item: CaseSummaryDTO) {
  return (
    <Link key={item.caseId} to={`/cases/${item.caseId}`} className="case-card">
      <p className="case-card-question">{item.questionText}</p>
      <p className="case-card-meta">
        {item.questionCategory} · {item.status} · {item.divinationTime?.replace('T', ' ')}
      </p>
      <p className="case-card-hexagram">
        {item.mainHexagram} → {item.changedHexagram} | {item.palace}宫 | 用神 {item.useGod}
      </p>
    </Link>
  );
}
