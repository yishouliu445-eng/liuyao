package com.yishou.liuyao.rule.advanced;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.rule.RuleHit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FanFuYinRuleTest {

    @Test
    void shouldDetectChartLevelFuYin() {
        LineInfo line = new LineInfo();
        line.setIndex(2);
        line.setMoving(true);
        line.setBranch("寅");
        line.setChangeBranch("寅");
        line.setChangeWuXing("木");

        ChartSnapshot chartSnapshot = new ChartSnapshot();
        chartSnapshot.setMainHexagram("山火贲");
        chartSnapshot.setMutualHexagram("雷水解");
        chartSnapshot.setOppositeHexagram("泽水困");
        chartSnapshot.setReversedHexagram("火雷噬嗑");
        chartSnapshot.setLines(List.of(line));

        RuleHit hit = new FanFuYinRule().evaluate(chartSnapshot);

        assertTrue(Boolean.TRUE.equals(hit.getHit()));
        assertEquals(true, hit.getEvidence().get("hasFuYin"));
        assertEquals(true, hit.getEvidence().get("chartFuYin"));
        assertEquals(false, hit.getEvidence().get("hasFanYin"));
    }

    @Test
    void shouldDetectChartLevelFanYin() {
        LineInfo line = new LineInfo();
        line.setIndex(5);
        line.setMoving(true);
        line.setBranch("子");
        line.setChangeBranch("午");
        line.setChangeWuXing("火");

        ChartSnapshot chartSnapshot = new ChartSnapshot();
        chartSnapshot.setMainHexagram("乾为天");
        chartSnapshot.setMutualHexagram("乾为天");
        chartSnapshot.setOppositeHexagram("坤为地");
        chartSnapshot.setReversedHexagram("乾为天");
        chartSnapshot.setLines(List.of(line));

        RuleHit hit = new FanFuYinRule().evaluate(chartSnapshot);

        assertTrue(Boolean.TRUE.equals(hit.getHit()));
        assertEquals(false, hit.getEvidence().get("hasFuYin"));
        assertEquals(true, hit.getEvidence().get("hasFanYin"));
        assertEquals(true, hit.getEvidence().get("chartFanYin"));
    }
}
