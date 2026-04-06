package com.example.liuyao.rule;

import com.example.liuyao.divination.domain.ChartSnapshot;

public interface Rule {
    RuleHit evaluate(ChartSnapshot chart);
}
