package com.yishou.liuyao.casecenter.repository;

import com.yishou.liuyao.casecenter.domain.CaseAnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CaseAnalysisResultRepository extends JpaRepository<CaseAnalysisResult, Long> {

    Optional<CaseAnalysisResult> findTopByCaseIdOrderByIdDesc(Long caseId);
}
