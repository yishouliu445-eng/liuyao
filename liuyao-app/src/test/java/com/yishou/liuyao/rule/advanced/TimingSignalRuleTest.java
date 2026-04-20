package com.yishou.liuyao.rule.advanced;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.rule.RuleHit;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimingSignalRuleTest {

    @Test
    void shouldMarkMovingVisibleUseGodAsShortTerm() {
        LineInfo line = new LineInfo();
        line.setIndex(4);
        line.setLiuQin("妻财");
        line.setMoving(true);
        line.setBranch("寅");
        line.setWuXing("木");
        line.setChangeBranch("卯");
        line.setChangeWuXing("木");

        ChartSnapshot chartSnapshot = new ChartSnapshot();
        chartSnapshot.setUseGod("妻财");
        chartSnapshot.setYueJian("寅");
        chartSnapshot.setLines(List.of(line));
        chartSnapshot.setExt(new LinkedHashMap<>());
        chartSnapshot.getExt().put("useGod", "妻财");
        chartSnapshot.getExt().put("useGodLineIndex", 4);

        RuleHit hit = new TimingSignalRule().evaluate(chartSnapshot);

        assertTrue(Boolean.TRUE.equals(hit.getHit()));
        assertEquals("SHORT_TERM", hit.getEvidence().get("timingBucket"));
        assertEquals(false, hit.getEvidence().get("resolvedFromHiddenUseGod"));
        assertTrue(String.valueOf(hit.getEvidence().get("timingHint")).contains("近期"));
    }

    @Test
    void shouldDelayTimingWhenHiddenUseGodIsSuppressed() {
        LineInfo line = new LineInfo();
        line.setIndex(2);
        line.setLiuQin("子孙");
        line.setBranch("申");
        line.setWuXing("金");
        line.setFuShenLiuQin("妻财");
        line.setFuShenBranch("寅");
        line.setFuShenWuXing("木");
        line.setFlyShenLiuQin("子孙");
        line.setFlyShenBranch("申");
        line.setFlyShenWuXing("金");

        ChartSnapshot chartSnapshot = new ChartSnapshot();
        chartSnapshot.setUseGod("妻财");
        chartSnapshot.setYueJian("辰");
        chartSnapshot.setLines(List.of(line));
        chartSnapshot.setExt(new LinkedHashMap<>());
        chartSnapshot.getExt().put("useGod", "妻财");

        RuleHit hit = new TimingSignalRule().evaluate(chartSnapshot);

        assertTrue(Boolean.TRUE.equals(hit.getHit()));
        assertEquals("DELAYED", hit.getEvidence().get("timingBucket"));
        assertEquals(true, hit.getEvidence().get("resolvedFromHiddenUseGod"));
        assertTrue(String.valueOf(hit.getEvidence().get("timingHint")).contains("偏后"));
    }
}
