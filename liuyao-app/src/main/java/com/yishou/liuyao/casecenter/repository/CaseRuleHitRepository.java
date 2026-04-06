package com.yishou.liuyao.casecenter.repository;

import com.yishou.liuyao.casecenter.domain.CaseRuleHit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CaseRuleHitRepository extends JpaRepository<CaseRuleHit, Long> {

    List<CaseRuleHit> findByCaseIdOrderByIdAsc(Long caseId);
}
