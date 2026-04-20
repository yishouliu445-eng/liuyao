package com.yishou.liuyao.rule.batch;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.rule.RuleHit;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovingLineAffectUseGodRuleTest {

    @Test
    void shouldEvaluateBothMovingAndChangedWuXingAgainstUseGod() {
        LineInfo moving = new LineInfo();
        moving.setIndex(1);
        moving.setMoving(true);
        moving.setLiuQin("兄弟");
        moving.setBranch("子");
        moving.setWuXing("水");
        moving.setChangeTo("阴");
        moving.setChangeBranch("寅");
        moving.setChangeWuXing("木");
        moving.setChangeLiuQin("父母");

        LineInfo useGod = new LineInfo();
        useGod.setIndex(4);
        useGod.setLiuQin("妻财");
        useGod.setBranch("午");
        useGod.setWuXing("火");

        ChartSnapshot chartSnapshot = new ChartSnapshot();
        chartSnapshot.setLines(List.of(moving, useGod));
        chartSnapshot.setExt(new LinkedHashMap<>());
        chartSnapshot.getExt().put("useGod", "妻财");

        RuleHit hit = new MovingLineAffectUseGodRule().evaluate(chartSnapshot);

        assertTrue(Boolean.TRUE.equals(hit.getHit()));
        assertEquals(1, hit.getEvidence().get("targetCount"));
        assertEquals(1, ((List<?>) hit.getEvidence().get("targetSummary")).size());
        assertEquals(1, ((List<?>) hit.getEvidence().get("targets")).size());
        @SuppressWarnings("unchecked")
        Map<String, Object> effect = (Map<String, Object>) ((List<?>) hit.getEvidence().get("effects")).get(0);
        assertEquals("动爻克用神", effect.get("relation"));
        assertEquals("变爻生用神", effect.get("changeRelation"));
        assertEquals("寅", effect.get("changeBranch"));
        assertEquals(false, effect.get("sameLineAsUseGod"));
    }

    @Test
    void shouldExposeSelfTransformWhenUseGodLineIsMoving() {
        LineInfo movingUseGod = new LineInfo();
        movingUseGod.setIndex(2);
        movingUseGod.setMoving(true);
        movingUseGod.setLiuQin("妻财");
        movingUseGod.setBranch("午");
        movingUseGod.setWuXing("火");
        movingUseGod.setChangeTo("阴");
        movingUseGod.setChangeBranch("酉");
        movingUseGod.setChangeWuXing("金");
        movingUseGod.setChangeLiuQin("官鬼");

        ChartSnapshot chartSnapshot = new ChartSnapshot();
        chartSnapshot.setLines(List.of(movingUseGod));
        chartSnapshot.setExt(new LinkedHashMap<>());
        chartSnapshot.getExt().put("useGod", "妻财");

        RuleHit hit = new MovingLineAffectUseGodRule().evaluate(chartSnapshot);

        assertTrue(Boolean.TRUE.equals(hit.getHit()));
        assertEquals(1, hit.getEvidence().get("targetCount"));
        assertEquals(1, ((List<?>) hit.getEvidence().get("targetSummary")).size());
        @SuppressWarnings("unchecked")
        Map<String, Object> effect = (Map<String, Object>) ((List<?>) hit.getEvidence().get("effects")).get(0);
        assertEquals(true, effect.get("sameLineAsUseGod"));
        assertEquals("用神发动后转出他亲", effect.get("selfTransform"));
    }

    @Test
    void shouldExposeTransformTrendAndShiInterferenceSignals() {
        LineInfo moving = new LineInfo();
        moving.setIndex(3);
        moving.setMoving(true);
        moving.setShi(true);
        moving.setLiuQin("兄弟");
        moving.setBranch("寅");
        moving.setWuXing("木");
        moving.setChangeTo("阴");
        moving.setChangeBranch("卯");
        moving.setChangeWuXing("木");
        moving.setChangeLiuQin("兄弟");

        LineInfo useGod = new LineInfo();
        useGod.setIndex(5);
        useGod.setLiuQin("妻财");
        useGod.setBranch("丑");
        useGod.setWuXing("土");

        ChartSnapshot chartSnapshot = new ChartSnapshot();
        chartSnapshot.setShi(3);
        chartSnapshot.setLines(List.of(moving, useGod));
        chartSnapshot.setExt(new LinkedHashMap<>());
        chartSnapshot.getExt().put("useGod", "妻财");
        chartSnapshot.getExt().put("useGodLineIndex", 5);

        RuleHit hit = new MovingLineAffectUseGodRule().evaluate(chartSnapshot);

        assertTrue(Boolean.TRUE.equals(hit.getHit()));
        @SuppressWarnings("unchecked")
        Map<String, Object> effect = (Map<String, Object>) ((List<?>) hit.getEvidence().get("effects")).get(0);
        assertEquals("化进", effect.get("transformTrend"));
        assertEquals(true, effect.get("affectsShi"));
        assertInstanceOf(List.class, effect.get("movementSignals"));
        assertTrue(((List<?>) effect.get("movementSignals")).contains("世爻发动"));
    }
}
