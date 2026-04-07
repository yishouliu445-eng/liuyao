package com.yishou.liuyao.rule.service;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.rule.Rule;
import com.yishou.liuyao.rule.RuleHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RuleEngineService {

    private static final Logger log = LoggerFactory.getLogger(RuleEngineService.class);

    private final List<Rule> rules;

    public RuleEngineService(List<Rule> rules) {
        this.rules = rules;
    }

    public List<RuleHit> evaluate(ChartSnapshot chartSnapshot) {
        // 规则引擎统一只返回命中的规则，未命中的规则交由各自测试覆盖。
        List<RuleHit> hits = rules.stream()
                .map(rule -> rule.evaluate(chartSnapshot))
                .filter(ruleHit -> Boolean.TRUE.equals(ruleHit.getHit()))
                .toList();
        log.info("规则引擎完成: mainHexagram={}, useGod={}, hitCount={}, hitRules={}",
                chartSnapshot == null ? "" : chartSnapshot.getMainHexagram(),
                chartSnapshot == null ? "" : chartSnapshot.getUseGod(),
                hits.size(),
                hits.stream().map(RuleHit::getRuleCode).toList());
        if (log.isDebugEnabled()) {
            hits.forEach(hit -> log.debug("规则命中详情: ruleCode={}, impactLevel={}, evidenceKeys={}",
                    hit.getRuleCode(),
                    hit.getImpactLevel(),
                    hit.getEvidence() == null ? List.of() : hit.getEvidence().keySet()));
        }
        return hits;
    }
}
