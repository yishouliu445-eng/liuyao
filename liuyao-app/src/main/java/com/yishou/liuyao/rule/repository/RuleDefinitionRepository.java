package com.yishou.liuyao.rule.repository;

import com.yishou.liuyao.rule.domain.RuleDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RuleDefinitionRepository extends JpaRepository<RuleDefinitionEntity, Long> {

    Optional<RuleDefinitionEntity> findByRuleCode(String ruleCode);

    List<RuleDefinitionEntity> findAllByOrderByPriorityAscRuleCodeAsc();
}
