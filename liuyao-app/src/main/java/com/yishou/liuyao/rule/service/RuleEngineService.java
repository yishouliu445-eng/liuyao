package com.yishou.liuyao.rule.service;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.rule.Rule;
import com.yishou.liuyao.rule.RuleHit;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RuleEngineService {

    private final List<Rule> rules;

    public RuleEngineService(List<Rule> rules) {
        this.rules = rules;
    }

    public List<RuleHit> evaluate(ChartSnapshot chartSnapshot) {
        return rules.stream()
                .map(rule -> rule.evaluate(chartSnapshot))
                .filter(ruleHit -> Boolean.TRUE.equals(ruleHit.getHit()))
                .toList();
    }
}
