package com.yishou.liuyao.rule.advanced;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.rule.RuleHit;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UseGodEmptyRuleTest {

    @Test
    void shouldExposeTargetCountAndTargetBranchWhenUseGodFallsIntoKongWang() {
        LineInfo lineInfo = new LineInfo();
        lineInfo.setIndex(2);
        lineInfo.setLiuQin("父母");
        lineInfo.setBranch("丑");
        lineInfo.setWuXing("土");

        ChartSnapshot chartSnapshot = new ChartSnapshot();
        chartSnapshot.setMainHexagram("山火贲");
        chartSnapshot.setChangedHexagram("风山渐");
        chartSnapshot.setMainUpperTrigram("艮");
        chartSnapshot.setMainLowerTrigram("离");
        chartSnapshot.setChangedUpperTrigram("巽");
        chartSnapshot.setChangedLowerTrigram("艮");
        chartSnapshot.setPalace("艮");
        chartSnapshot.setPalaceWuXing("土");
        chartSnapshot.setKongWang(List.of("子", "丑"));
        chartSnapshot.setLines(List.of(lineInfo));
        chartSnapshot.setExt(new LinkedHashMap<>());
        chartSnapshot.getExt().put("useGod", "父母");

        RuleHit hit = new UseGodEmptyRule().evaluate(chartSnapshot);

        assertTrue(Boolean.TRUE.equals(hit.getHit()));
        assertEquals(1, hit.getEvidence().get("targetCount"));
        assertEquals("艮", hit.getEvidence().get("mainUpperTrigram"));
        assertEquals(1, ((List<?>) hit.getEvidence().get("targetSummary")).size());
        @SuppressWarnings("unchecked")
        Map<String, Object> target = (Map<String, Object>) ((List<?>) hit.getEvidence().get("targets")).get(0);
        assertEquals("丑", target.get("branch"));
    }
}
