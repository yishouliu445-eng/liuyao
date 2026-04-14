package com.yishou.liuyao.ops.audit.repository;

import com.yishou.liuyao.ops.audit.domain.AnalysisRunCitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisRunCitationRepository extends JpaRepository<AnalysisRunCitation, Long> {

    List<AnalysisRunCitation> findByAnalysisRun_IdOrderByIdAsc(Long analysisRunId);
}
