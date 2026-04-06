package com.yishou.liuyao.rule;

import com.yishou.liuyao.divination.domain.ChartSnapshot;

public interface Rule {
    RuleHit evaluate(ChartSnapshot chart);
}
