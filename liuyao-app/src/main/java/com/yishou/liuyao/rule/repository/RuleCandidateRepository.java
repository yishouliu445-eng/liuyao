package com.yishou.liuyao.rule.repository;

import com.yishou.liuyao.rule.domain.RuleCandidate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RuleCandidateRepository extends JpaRepository<RuleCandidate, Long>, JpaSpecificationExecutor<RuleCandidate> {
    
    Page<RuleCandidate> findByStatus(String status, Pageable pageable);
    
    List<RuleCandidate> findByStatus(String status);
}
