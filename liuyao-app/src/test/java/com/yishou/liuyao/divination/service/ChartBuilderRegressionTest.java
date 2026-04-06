package com.yishou.liuyao.divination.service;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.DivinationInput;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChartBuilderRegressionTest {

    private final ChartBuilderService service = new ChartBuilderService(
            new CalendarFacade(),
            new HexagramResolver(),
            new ShiYingResolver(),
            new LiuShenResolver(),
            new PalaceResolver(),
            new NaJiaResolver(),
            new LiuQinResolver()
    );

    @ParameterizedTest
    @MethodSource("cases")
    void shouldKeepKeySnapshotFieldsStable(List<String> rawLines,
                                           String mainHexagram,
                                           String changedHexagram,
                                           String palace,
                                           String palaceWuXing,
                                           int shi,
                                           int ying,
                                           String line1Branch,
                                           String line1LiuQin,
                                           String line6Branch,
                                           String line6LiuQin) {
        DivinationInput input = new DivinationInput();
        input.setQuestion("回归测试");
        input.setQuestionCategory("测试");
        input.setDivinationTime(LocalDateTime.of(1986, 5, 29, 0, 0));
        input.setRawLines(rawLines);

        ChartSnapshot chartSnapshot = service.buildChart(input);

        assertEquals(mainHexagram, chartSnapshot.getMainHexagram());
        assertEquals(changedHexagram, chartSnapshot.getChangedHexagram());
        assertEquals(palace, chartSnapshot.getPalace());
        assertEquals(palaceWuXing, chartSnapshot.getPalaceWuXing());
        assertEquals(palace, chartSnapshot.getExt().get("palace"));
        assertEquals(palaceWuXing, chartSnapshot.getExt().get("palaceWuXing"));
        assertEquals(shi, chartSnapshot.getShi());
        assertEquals(ying, chartSnapshot.getYing());
        assertEquals(line1Branch, chartSnapshot.getLines().get(0).getBranch());
        assertEquals(line1LiuQin, chartSnapshot.getLines().get(0).getLiuQin());
        assertEquals(line6Branch, chartSnapshot.getLines().get(5).getBranch());
        assertEquals(line6LiuQin, chartSnapshot.getLines().get(5).getLiuQin());
    }

    private static Stream<Arguments> cases() {
        return Stream.of(
                Arguments.of(
                        List.of("少阳", "少阳", "少阳", "少阳", "少阳", "少阳"),
                        "乾为天",
                        "乾为天",
                        "乾",
                        "金",
                        6,
                        3,
                        "子",
                        "子孙",
                        "戌",
                        "父母"
                ),
                Arguments.of(
                        List.of("老阳", "少阳", "少阳", "少阳", "少阳", "少阳"),
                        "乾为天",
                        "天风姤",
                        "乾",
                        "金",
                        6,
                        3,
                        "子",
                        "子孙",
                        "戌",
                        "父母"
                ),
                Arguments.of(
                        List.of("少阴", "少阴", "少阴", "少阴", "少阴", "少阴"),
                        "坤为地",
                        "坤为地",
                        "坤",
                        "土",
                        6,
                        3,
                        "未",
                        "兄弟",
                        "酉",
                        "子孙"
                ),
                Arguments.of(
                        List.of("少阳", "少阳", "少阳", "少阳", "少阴", "少阳"),
                        "火天大有",
                        "火天大有",
                        "乾",
                        "金",
                        3,
                        6,
                        "子",
                        "子孙",
                        "巳",
                        "官鬼"
                )
        );
    }
}
