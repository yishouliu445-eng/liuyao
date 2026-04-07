package com.yishou.liuyao.rule.advanced;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.rule.RuleHit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShiYingRelationRuleTest {

    @Test
    void shouldExposeStructuredShiYingEvidence() {
        LineInfo shi = new LineInfo();
        shi.setIndex(1);
        shi.setShi(true);
        shi.setLiuQin("父母");
        shi.setBranch("卯");
        shi.setWuXing("木");

        LineInfo ying = new LineInfo();
        ying.setIndex(4);
        ying.setYing(true);
        ying.setLiuQin("妻财");
        ying.setBranch("午");
        ying.setWuXing("火");

        ChartSnapshot chartSnapshot = new ChartSnapshot();
        chartSnapshot.setPalace("乾");
        chartSnapshot.setPalaceWuXing("金");
        chartSnapshot.setLines(List.of(shi, ying));

        RuleHit hit = new ShiYingRelationRule().evaluate(chartSnapshot);

        assertTrue(Boolean.TRUE.equals(hit.getHit()));
        assertEquals("世生应", hit.getEvidence().get("relation"));
        assertEquals("乾", hit.getEvidence().get("palace"));
        assertEquals(2, hit.getEvidence().get("targetCount"));
        assertEquals(2, ((List<?>) hit.getEvidence().get("targetSummary")).size());
        assertEquals(2, ((List<?>) hit.getEvidence().get("targets")).size());
        @SuppressWarnings("unchecked")
        Map<String, Object> shiLine = (Map<String, Object>) hit.getEvidence().get("shiLine");
        assertEquals("卯", shiLine.get("branch"));
    }
}
