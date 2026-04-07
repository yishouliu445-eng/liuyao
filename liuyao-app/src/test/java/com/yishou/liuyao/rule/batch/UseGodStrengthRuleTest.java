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

class UseGodStrengthRuleTest {

    @Test
    void shouldIncludeResolvedBranchesAndLineBranchInEvidence() {
        LineInfo target = new LineInfo();
        target.setIndex(2);
        target.setLiuQin("父母");
        target.setBranch("卯");
        target.setWuXing("木");
        target.setMoving(true);
        target.setChangeWuXing("火");

        ChartSnapshot chartSnapshot = new ChartSnapshot();
        chartSnapshot.setYueJian("寅");
        chartSnapshot.setRiChen("甲子日");
        chartSnapshot.setKongWang(List.of("戌", "亥"));
        chartSnapshot.setLines(List.of(target));
        chartSnapshot.setExt(new LinkedHashMap<>());
        chartSnapshot.getExt().put("useGod", "父母");

        RuleHit hit = new UseGodStrengthRule().evaluate(chartSnapshot);

        assertTrue(Boolean.TRUE.equals(hit.getHit()));
        assertEquals("寅", hit.getEvidence().get("yueBranch"));
        assertEquals("子", hit.getEvidence().get("riBranch"));
        assertEquals(1, hit.getEvidence().get("targetCount"));
        @SuppressWarnings("unchecked")
        Map<String, Object> detail = (Map<String, Object>) ((List<?>) hit.getEvidence().get("details")).get(0);
        assertEquals("卯", detail.get("branch"));
        assertEquals(true, detail.get("moving"));
        assertEquals(false, detail.get("kongWang"));
        assertEquals("被生", detail.get("changeWuXingRelation"));
        assertEquals("STRONG", detail.get("level"));
    }
}
