package com.yishou.liuyao.rule.advanced;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.divination.service.ShenShaResolver;
import com.yishou.liuyao.rule.RuleHit;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShenShaRuleTest {

    @Test
    void shouldMarkNoblemanAndTravelHorseFacts() {
        ChartSnapshot chart = new ChartSnapshot();
        chart.setUseGod("妻财");
        chart.setExt(new LinkedHashMap<>());
        chart.getExt().put("useGod", "妻财");
        chart.getExt().put("useGodLineIndex", 1);
        chart.setLines(List.of(
                line(1, "妻财", "丑", false),
                line(2, "兄弟", "寅", true),
                line(3, "官鬼", "酉", false)
        ));
        chart.setShenShaHits(new ShenShaResolver().resolve("甲子", chart.getLines()));

        RuleHit hit = new ShenShaRule().evaluate(chart);

        assertTrue(Boolean.TRUE.equals(hit.getHit()));
        assertEquals(true, hit.getEvidence().get("hasNobleman"));
        assertEquals(true, hit.getEvidence().get("useGodWithNobleman"));
        assertEquals(true, hit.getEvidence().get("hasTravelHorse"));
        assertEquals(true, hit.getEvidence().get("movingWithTravelHorse"));
        assertEquals(true, hit.getEvidence().get("hasPeachBlossom"));
        assertTrue(String.valueOf(hit.getHitReason()).contains("贵人"));
    }

    @Test
    void shouldMarkExpandedShenShaFacts() {
        ChartSnapshot chart = new ChartSnapshot();
        chart.setUseGod("妻财");
        chart.setExt(new LinkedHashMap<>());
        chart.getExt().put("useGod", "妻财");
        chart.getExt().put("useGodLineIndex", 1);
        chart.setLines(List.of(
                line(1, "妻财", "巳", true),
                line(2, "兄弟", "子", false),
                line(3, "官鬼", "午", true),
                line(4, "父母", "亥", true)
        ));
        chart.setShenShaHits(new ShenShaResolver().resolve("甲子", chart.getLines()));

        RuleHit hit = new ShenShaRule().evaluate(chart);

        assertEquals(true, hit.getEvidence().get("hasWenChang"));
        assertEquals(true, hit.getEvidence().get("useGodWithWenChang"));
        assertEquals(true, hit.getEvidence().get("hasGeneralStar"));
        assertEquals(true, hit.getEvidence().get("hasDisasterSha"));
        assertEquals(true, hit.getEvidence().get("movingWithDisasterSha"));
        assertEquals(true, hit.getEvidence().get("hasJieSha"));
        assertEquals(true, hit.getEvidence().get("movingWithJieSha"));
    }

    private LineInfo line(int index, String liuQin, String branch, boolean moving) {
        LineInfo line = new LineInfo();
        line.setIndex(index);
        line.setLiuQin(liuQin);
        line.setBranch(branch);
        line.setMoving(moving);
        return line;
    }
}
