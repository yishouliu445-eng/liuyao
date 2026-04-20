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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                                           String mainUpperTrigram,
                                           String mainLowerTrigram,
                                           String changedUpperTrigram,
                                           String changedLowerTrigram,
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
        assertEquals(mainUpperTrigram, chartSnapshot.getMainUpperTrigram());
        assertEquals(mainLowerTrigram, chartSnapshot.getMainLowerTrigram());
        assertEquals(changedUpperTrigram, chartSnapshot.getChangedUpperTrigram());
        assertEquals(changedLowerTrigram, chartSnapshot.getChangedLowerTrigram());
        assertEquals(palace, chartSnapshot.getPalace());
        assertEquals(palaceWuXing, chartSnapshot.getPalaceWuXing());
        assertEquals("v1", chartSnapshot.getSnapshotVersion());
        assertEquals("v1", chartSnapshot.getCalendarVersion());
        assertEquals(palace, chartSnapshot.getExt().get("palace"));
        assertEquals(palaceWuXing, chartSnapshot.getExt().get("palaceWuXing"));
        assertEquals(mainUpperTrigram, chartSnapshot.getExt().get("mainUpperTrigram"));
        assertEquals(mainLowerTrigram, chartSnapshot.getExt().get("mainLowerTrigram"));
        assertEquals(shi, chartSnapshot.getShi());
        assertEquals(ying, chartSnapshot.getYing());
        assertEquals(line1Branch, chartSnapshot.getLines().get(0).getBranch());
        assertEquals(line1LiuQin, chartSnapshot.getLines().get(0).getLiuQin());
        assertEquals(line6Branch, chartSnapshot.getLines().get(5).getBranch());
        assertEquals(line6LiuQin, chartSnapshot.getLines().get(5).getLiuQin());
    }

    @org.junit.jupiter.api.Test
    void shouldExposeFuShenDataWhenVisibleSixKinIsMissing() {
        DivinationInput input = new DivinationInput();
        input.setQuestion("伏神回归");
        input.setQuestionCategory("测试");
        input.setDivinationTime(LocalDateTime.of(2026, 4, 12, 10, 0));
        input.setRawLines(List.of("老阳", "少阴", "少阳", "少阴", "老阴", "少阳"));
        input.setMovingLines(List.of(1, 5));

        ChartSnapshot chartSnapshot = service.buildChart(input);

        assertEquals("山火贲", chartSnapshot.getMainHexagram());
        assertEquals("艮", chartSnapshot.getPalace());
        assertNull(chartSnapshot.getLines().get(0).getFuShenLiuQin());
        assertEquals("父母", chartSnapshot.getLines().get(1).getFuShenLiuQin());
        assertEquals("午", chartSnapshot.getLines().get(1).getFuShenBranch());
        assertEquals("火", chartSnapshot.getLines().get(1).getFuShenWuXing());
        assertEquals("兄弟", chartSnapshot.getLines().get(1).getFlyShenLiuQin());
        assertEquals("子孙", chartSnapshot.getLines().get(2).getFuShenLiuQin());
        assertEquals("申", chartSnapshot.getLines().get(2).getFuShenBranch());
        assertEquals("金", chartSnapshot.getLines().get(2).getFuShenWuXing());
        assertEquals("妻财", chartSnapshot.getLines().get(2).getFlyShenLiuQin());
    }

    @org.junit.jupiter.api.Test
    void shouldExposeMutualOppositeAndReversedHexagrams() {
        DivinationInput input = new DivinationInput();
        input.setQuestion("派生卦回归");
        input.setQuestionCategory("测试");
        input.setDivinationTime(LocalDateTime.of(2026, 4, 12, 10, 0));
        input.setRawLines(List.of("老阳", "少阴", "少阳", "少阴", "老阴", "少阳"));
        input.setMovingLines(List.of(1, 5));

        ChartSnapshot chartSnapshot = service.buildChart(input);

        assertEquals("雷水解", chartSnapshot.getMutualHexagram());
        assertEquals("100010", chartSnapshot.getMutualHexagramCode());
        assertEquals("泽水困", chartSnapshot.getOppositeHexagram());
        assertEquals("110010", chartSnapshot.getOppositeHexagramCode());
        assertEquals("火雷噬嗑", chartSnapshot.getReversedHexagram());
        assertEquals("101100", chartSnapshot.getReversedHexagramCode());
        assertEquals("雷水解", chartSnapshot.getExt().get("mutualHexagram"));
        assertEquals("泽水困", chartSnapshot.getExt().get("oppositeHexagram"));
        assertEquals("火雷噬嗑", chartSnapshot.getExt().get("reversedHexagram"));
    }

    @org.junit.jupiter.api.Test
    void shouldExposeResolvedShenShaHits() {
        DivinationInput input = new DivinationInput();
        input.setQuestion("神煞回归");
        input.setQuestionCategory("测试");
        input.setDivinationTime(LocalDateTime.of(1986, 5, 29, 0, 0));
        input.setRawLines(List.of("少阳", "少阳", "少阳", "少阳", "少阳", "少阳"));

        ChartSnapshot chartSnapshot = service.buildChart(input);

        assertTrue(chartSnapshot.getShenShaHits().stream().anyMatch(hit ->
                "PEACH_BLOSSOM".equals(hit.getCode()) && "午".equals(hit.getBranch()) && hit.getLineIndexes().equals(List.of(4))));
        assertEquals(chartSnapshot.getShenShaHits(), chartSnapshot.getExt().get("shenShaHits"));
    }

    private static Stream<Arguments> cases() {
        return Stream.of(
                Arguments.of(
                        List.of("少阳", "少阳", "少阳", "少阳", "少阳", "少阳"),
                        "乾为天",
                        "乾为天",
                        "乾",
                        "乾",
                        "乾",
                        "乾",
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
                        "乾",
                        "乾",
                        "巽",
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
                        "坤",
                        "坤",
                        "坤",
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
                        "离",
                        "乾",
                        "离",
                        "乾",
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
