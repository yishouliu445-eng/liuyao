package com.yishou.liuyao.divination.service;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.DivinationInput;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChartBuilderServiceCalendarTest {

    @Test
    void shouldUseCalendarFacadeToPopulateRiChenAndYueJian() {
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
        input.setQuestion("这次机会能不能成");
        input.setQuestionCategory("求职");
        input.setDivinationTime(LocalDateTime.of(1986, 5, 29, 0, 0));
        input.setRawLines(java.util.List.of("少阳", "少阳", "少阳", "少阳", "少阳", "少阳"));

        ChartSnapshot chartSnapshot = service.buildChart(input);

        assertEquals("癸酉", chartSnapshot.getRiChen());
        assertEquals("巳", chartSnapshot.getYueJian());
        assertEquals(List.of("戌", "亥"), chartSnapshot.getKongWang());
        assertEquals("乾为天", chartSnapshot.getMainHexagram());
        assertEquals("玄武", chartSnapshot.getLines().get(0).getLiuShen());
    }
}
