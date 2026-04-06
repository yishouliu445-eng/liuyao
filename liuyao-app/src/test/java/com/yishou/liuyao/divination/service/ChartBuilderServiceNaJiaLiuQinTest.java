package com.yishou.liuyao.divination.service;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.DivinationInput;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChartBuilderServiceNaJiaLiuQinTest {

    @Test
    void shouldPopulateBranchAndLiuQinFromHexagram() {
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

        assertEquals("乾为天", chartSnapshot.getMainHexagram());
        assertEquals("子", chartSnapshot.getLines().get(0).getBranch());
        assertEquals("子孙", chartSnapshot.getLines().get(0).getLiuQin());
        assertEquals("申", chartSnapshot.getLines().get(4).getBranch());
        assertEquals("兄弟", chartSnapshot.getLines().get(4).getLiuQin());
    }
}
