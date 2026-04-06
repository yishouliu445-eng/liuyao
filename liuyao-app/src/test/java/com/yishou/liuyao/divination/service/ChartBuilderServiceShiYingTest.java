package com.yishou.liuyao.divination.service;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.DivinationInput;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartBuilderServiceShiYingTest {

    @Test
    void shouldPopulateShiYingForQianWeiTian() {
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
        input.setRawLines(List.of("少阳", "少阳", "少阳", "少阳", "少阳", "少阳"));

        ChartSnapshot chartSnapshot = service.buildChart(input);

        assertEquals(6, chartSnapshot.getShi());
        assertEquals(3, chartSnapshot.getYing());
        assertTrue(Boolean.TRUE.equals(chartSnapshot.getLines().get(5).getIsShi()));
        assertTrue(Boolean.TRUE.equals(chartSnapshot.getLines().get(2).getIsYing()));
    }
}
