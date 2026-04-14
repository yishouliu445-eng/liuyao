package com.yishou.liuyao.ops.audit.repository;

import com.yishou.liuyao.ops.audit.domain.AnalysisRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AnalysisRunRepository extends JpaRepository<AnalysisRun, Long> {

    boolean existsByExecutionId(UUID executionId);

    Optional<AnalysisRun> findByExecutionId(UUID executionId);
}
