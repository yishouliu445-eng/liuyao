package com.yishou.liuyao.rule.basic;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.rule.RuleHit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovingLineExistsRuleTest {

    @Test
    void shouldHitWhenAnyLineIsMoving() {
        LineInfo moving = new LineInfo();
        moving.setIndex(2);
        moving.setMoving(true);
        moving.setChangeTo("阴");
        moving.setChangeBranch("丑");
        moving.setChangeWuXing("土");
        moving.setChangeLiuQin("父母");

        ChartSnapshot chartSnapshot = new ChartSnapshot();
        chartSnapshot.setLines(List.of(moving));

        RuleHit hit = new MovingLineExistsRule().evaluate(chartSnapshot);

        assertTrue(Boolean.TRUE.equals(hit.getHit()));
        assertEquals(1, hit.getEvidence().get("movingLineCount"));
        @SuppressWarnings("unchecked")
        Map<String, Object> changeTarget = (Map<String, Object>) ((List<?>) hit.getEvidence().get("changeTargets")).get(0);
        assertEquals("丑", changeTarget.get("changeBranch"));
        assertEquals("父母", changeTarget.get("changeLiuQin"));
    }
}
