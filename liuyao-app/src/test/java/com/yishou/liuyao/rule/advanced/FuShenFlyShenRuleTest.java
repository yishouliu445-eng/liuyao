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

class FuShenFlyShenRuleTest {

    @Test
    void shouldTreatHiddenLineAsUseGodWhenSurfaceLineIsMissing() {
        LineInfo line = new LineInfo();
        line.setIndex(2);
        line.setBranch("亥");
        line.setWuXing("水");
        line.setLiuQin("子孙");
        line.setFuShenBranch("寅");
        line.setFuShenWuXing("木");
        line.setFuShenLiuQin("妻财");
        line.setFlyShenBranch("亥");
        line.setFlyShenWuXing("水");
        line.setFlyShenLiuQin("子孙");

        ChartSnapshot chartSnapshot = new ChartSnapshot();
        chartSnapshot.setRiChen("甲子日");
        chartSnapshot.setYueJian("寅");
        chartSnapshot.setKongWang(List.of("戌", "亥"));
        chartSnapshot.setLines(List.of(line));
        chartSnapshot.setExt(new LinkedHashMap<>());
        chartSnapshot.getExt().put("useGod", "妻财");

        RuleHit hit = new FuShenFlyShenRule().evaluate(chartSnapshot);

        assertTrue(Boolean.TRUE.equals(hit.getHit()));
        assertEquals(2, hit.getEvidence().get("hiddenUseGodLineIndex"));
        assertEquals("妻财", hit.getEvidence().get("resolvedUseGodSource"));
        assertEquals(true, hit.getEvidence().get("hiddenUseGodFound"));
        assertEquals(true, hit.getEvidence().get("hiddenUseGodSupported"));
        assertEquals(false, hit.getEvidence().get("flyShenSuppress"));
        assertEquals(false, hit.getEvidence().get("hiddenUseGodBroken"));
        @SuppressWarnings("unchecked")
        Map<String, Object> hiddenLine = (Map<String, Object>) hit.getEvidence().get("hiddenLine");
        assertEquals("寅", hiddenLine.get("branch"));
        assertEquals("飞神生伏", hit.getEvidence().get("flyShenRelation"));
    }

    @Test
    void shouldExposeSuppressedAndBrokenHiddenUseGodFlags() {
        LineInfo line = new LineInfo();
        line.setIndex(5);
        line.setBranch("申");
        line.setWuXing("金");
        line.setLiuQin("官鬼");
        line.setFuShenBranch("寅");
        line.setFuShenWuXing("木");
        line.setFuShenLiuQin("妻财");
        line.setFlyShenBranch("申");
        line.setFlyShenWuXing("金");
        line.setFlyShenLiuQin("官鬼");

        ChartSnapshot chartSnapshot = new ChartSnapshot();
        chartSnapshot.setRiChen("庚申日");
        chartSnapshot.setYueJian("申");
        chartSnapshot.setLines(List.of(line));
        chartSnapshot.setExt(new LinkedHashMap<>());
        chartSnapshot.getExt().put("useGod", "妻财");

        RuleHit hit = new FuShenFlyShenRule().evaluate(chartSnapshot);

        assertTrue(Boolean.TRUE.equals(hit.getHit()));
        assertEquals("飞神克伏", hit.getEvidence().get("flyShenRelation"));
        assertEquals(true, hit.getEvidence().get("flyShenSuppress"));
        assertEquals(true, hit.getEvidence().get("hiddenUseGodBroken"));
        assertEquals(false, hit.getEvidence().get("hiddenUseGodSupported"));
    }
}
