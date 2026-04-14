package com.yishou.liuyao.ops.audit.repository;

import com.yishou.liuyao.ops.audit.domain.AnalysisRunIssue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisRunIssueRepository extends JpaRepository<AnalysisRunIssue, Long> {

    List<AnalysisRunIssue> findByAnalysisRun_IdOrderByIdAsc(Long analysisRunId);
}
