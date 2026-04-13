import { get } from './client';
import type { CaseListResponseDTO, CaseDetailDTO } from '../types/case';

export function searchCases(params: {
  questionCategory?: string;
  page?: number;
  size?: number;
}): Promise<CaseListResponseDTO> {
  const query = new URLSearchParams();
  query.set('page', String(params.page ?? 1));
  query.set('size', String(params.size ?? 10));
  if (params.questionCategory) {
    query.set('questionCategory', params.questionCategory);
  }
  return get<CaseListResponseDTO>(`/cases/search?${query.toString()}`);
}

export function getCaseDetail(caseId: number): Promise<CaseDetailDTO> {
  return get<CaseDetailDTO>(`/cases/${caseId}`);
}
