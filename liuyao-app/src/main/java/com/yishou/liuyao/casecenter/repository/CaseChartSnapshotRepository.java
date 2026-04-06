package com.yishou.liuyao.casecenter.repository;

import com.yishou.liuyao.casecenter.domain.CaseChartSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CaseChartSnapshotRepository extends JpaRepository<CaseChartSnapshot, Long> {

    Optional<CaseChartSnapshot> findByCaseId(Long caseId);
}
