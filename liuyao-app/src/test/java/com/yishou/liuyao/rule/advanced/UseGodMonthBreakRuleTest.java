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

class UseGodMonthBreakRuleTest {

    @Test
    void shouldExposeChangedLineDetailsWhenMovingUseGodLineIsMonthBroken() {
        LineInfo lineInfo = new LineInfo();
        lineInfo.setIndex(3);
        lineInfo.setMoving(true);
        lineInfo.setLiuQin("妻财");
        lineInfo.setBranch("酉");
        lineInfo.setWuXing("金");
        lineInfo.setChangeTo("阴");
        lineInfo.setChangeBranch("亥");
        lineInfo.setChangeWuXing("水");
        lineInfo.setChangeLiuQin("子孙");

        ChartSnapshot chartSnapshot = new ChartSnapshot();
        chartSnapshot.setYueJian("卯");
        chartSnapshot.setLines(List.of(lineInfo));
        chartSnapshot.setExt(new LinkedHashMap<>());
        chartSnapshot.getExt().put("useGod", "妻财");

        RuleHit hit = new UseGodMonthBreakRule().evaluate(chartSnapshot);

        assertTrue(Boolean.TRUE.equals(hit.getHit()));
        assertEquals("卯", hit.getEvidence().get("yueBranch"));
        assertEquals(1, hit.getEvidence().get("targetCount"));
        assertEquals(1, ((List<?>) hit.getEvidence().get("targetSummary")).size());
        @SuppressWarnings("unchecked")
        Map<String, Object> target = (Map<String, Object>) ((List<?>) hit.getEvidence().get("targets")).get(0);
        assertEquals("亥", target.get("changeBranch"));
        assertEquals("子孙", target.get("changeLiuQin"));
    }
}
