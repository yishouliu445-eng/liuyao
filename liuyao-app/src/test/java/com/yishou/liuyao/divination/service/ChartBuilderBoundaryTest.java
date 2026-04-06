package com.yishou.liuyao.divination.service;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.DivinationInput;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChartBuilderBoundaryTest {

    private final ChartBuilderService service = new ChartBuilderService(
            new CalendarFacade(),
            new HexagramResolver(),
            new ShiYingResolver(),
            new LiuShenResolver(),
            new PalaceResolver(),
            new NaJiaResolver(),
            new LiuQinResolver()
    );

    @Test
    void shouldFallbackToPlaceholderHexagramWhenRawLinesIncomplete() {
        DivinationInput input = new DivinationInput();
        input.setQuestion("边界测试");
        input.setQuestionCategory("测试");
        input.setDivinationTime(LocalDateTime.of(2026, 4, 11, 10, 0));
        input.setRawLines(List.of("少阳", "少阴", "少阳"));

        ChartSnapshot chartSnapshot = service.buildChart(input);

        assertEquals("待定本卦", chartSnapshot.getMainHexagram());
        assertEquals("待定变卦", chartSnapshot.getChangedHexagram());
        assertEquals("000000", chartSnapshot.getMainHexagramCode());
        assertEquals("000000", chartSnapshot.getChangedHexagramCode());
    }
}
