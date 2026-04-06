package com.yishou.liuyao.divination.service;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.DivinationInput;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartBuilderServiceChangedLineTest {

    @Test
    void shouldPopulateChangedLineDetailsForMovingLine() {
        ChartBuilderService service = new ChartBuilderService(
                new CalendarFacade(),
                new HexagramResolver(),
                new ShiYingResolver(),
                new LiuShenResolver(),
                new PalaceResolver(),
                new NaJiaResolver(),
                new LiuQinResolver()
        );

        DivinationInput input = new DivinationInput();
        input.setQuestion("测试");
        input.setQuestionCategory("测试");
        input.setDivinationTime(LocalDateTime.of(1986, 5, 29, 0, 0));
        input.setRawLines(List.of("老阳", "少阳", "少阳", "少阳", "少阳", "少阳"));

        ChartSnapshot chartSnapshot = service.buildChart(input);

        assertEquals("乾为天", chartSnapshot.getMainHexagram());
        assertEquals("天风姤", chartSnapshot.getChangedHexagram());

        var firstLine = chartSnapshot.getLines().get(0);
        assertTrue(Boolean.TRUE.equals(firstLine.getIsMoving()));
        assertEquals("阴", firstLine.getChangeTo());
        assertEquals("丑", firstLine.getChangeBranch());
        assertEquals("土", firstLine.getChangeWuXing());
        assertEquals("父母", firstLine.getChangeLiuQin());

        var secondLine = chartSnapshot.getLines().get(1);
        assertNull(secondLine.getChangeBranch());
        assertNull(secondLine.getChangeWuXing());
        assertNull(secondLine.getChangeLiuQin());
    }
}
