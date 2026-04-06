package com.yishou.liuyao.rule.batch;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.rule.RuleHit;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UseGodDayBreakRuleTest {

    @Test
    void shouldHitWhenUseGodLineIsBrokenByDayBranch() {
        LineInfo lineInfo = new LineInfo();
        lineInfo.setIndex(5);
        lineInfo.setLiuQin("妻财");
        lineInfo.setBranch("午");

        ChartSnapshot chartSnapshot = new ChartSnapshot();
        chartSnapshot.setRiChen("甲子日");
        chartSnapshot.setLines(List.of(lineInfo));
        chartSnapshot.setExt(new LinkedHashMap<>());
        chartSnapshot.getExt().put("useGod", "妻财");

        RuleHit hit = new UseGodDayBreakRule().evaluate(chartSnapshot);

        assertTrue(Boolean.TRUE.equals(hit.getHit()));
        assertEquals("子", hit.getEvidence().get("riBranch"));
    }
}
