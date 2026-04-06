package com.yishou.liuyao.rule.batch;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.rule.RuleHit;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        @SuppressWarnings("unchecked")
        Map<String, Object> effect = (Map<String, Object>) ((List<?>) hit.getEvidence().get("effects")).get(0);
        assertEquals("动爻克用神", effect.get("relation"));
        assertEquals("变爻生用神", effect.get("changeRelation"));
        assertEquals("寅", effect.get("changeBranch"));
    }
}
