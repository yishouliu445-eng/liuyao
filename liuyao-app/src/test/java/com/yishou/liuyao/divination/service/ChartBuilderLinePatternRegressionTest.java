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

class ChartBuilderLinePatternRegressionTest {

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
    void shouldKeepMovingLinePatternsStable(String question,
                                            List<String> rawLines,
                                            List<Integer> movingLines,
                                            String mainHexagram,
                                            String changedHexagram,
                                            String mainUpperTrigram,
                                            String changedUpperTrigram,
                                            int expectedShi,
                                            int expectedYing,
                                            int expectedMovingCount) {
        DivinationInput input = new DivinationInput();
        input.setQuestion(question);
        input.setQuestionCategory("测试");
        input.setDivinationTime(LocalDateTime.of(2026, 4, 12, 10, 0));
        input.setRawLines(rawLines);
        input.setMovingLines(movingLines);

        ChartSnapshot chartSnapshot = service.buildChart(input);

        assertEquals(mainHexagram, chartSnapshot.getMainHexagram());
        assertEquals(changedHexagram, chartSnapshot.getChangedHexagram());
        assertEquals(mainUpperTrigram, chartSnapshot.getMainUpperTrigram());
        assertEquals(changedUpperTrigram, chartSnapshot.getChangedUpperTrigram());
        assertEquals(expectedShi, chartSnapshot.getShi());
        assertEquals(expectedYing, chartSnapshot.getYing());
        assertEquals(expectedMovingCount, chartSnapshot.getLines().stream().filter(line -> Boolean.TRUE.equals(line.getMoving())).count());
    }

    @ParameterizedTest
    @MethodSource("changedLineCases")
    void shouldKeepChangedLineDetailsStable(String question,
                                            List<String> rawLines,
                                            List<Integer> movingLines,
                                            int lineIndex,
                                            String expectedChangeTo,
                                            String expectedChangeBranch,
                                            String expectedChangeWuXing,
                                            String expectedChangeLiuQin) {
        DivinationInput input = new DivinationInput();
        input.setQuestion(question);
        input.setQuestionCategory("测试");
        input.setDivinationTime(LocalDateTime.of(2026, 4, 12, 10, 0));
        input.setRawLines(rawLines);
        input.setMovingLines(movingLines);

        ChartSnapshot chartSnapshot = service.buildChart(input);

        assertEquals(expectedChangeTo, chartSnapshot.getLines().get(lineIndex - 1).getChangeTo());
        assertEquals(expectedChangeBranch, chartSnapshot.getLines().get(lineIndex - 1).getChangeBranch());
        assertEquals(expectedChangeWuXing, chartSnapshot.getLines().get(lineIndex - 1).getChangeWuXing());
        assertEquals(expectedChangeLiuQin, chartSnapshot.getLines().get(lineIndex - 1).getChangeLiuQin());
    }

    @ParameterizedTest
    @MethodSource("staticLineCases")
    void shouldKeepStaticLineDetailsEmpty(String question,
                                          List<String> rawLines,
                                          List<Integer> movingLines,
                                          int lineIndex) {
        DivinationInput input = new DivinationInput();
        input.setQuestion(question);
        input.setQuestionCategory("测试");
        input.setDivinationTime(LocalDateTime.of(2026, 4, 12, 10, 0));
        input.setRawLines(rawLines);
        input.setMovingLines(movingLines);

        ChartSnapshot chartSnapshot = service.buildChart(input);

        assertNull(chartSnapshot.getLines().get(lineIndex - 1).getChangeTo());
        assertNull(chartSnapshot.getLines().get(lineIndex - 1).getChangeBranch());
        assertNull(chartSnapshot.getLines().get(lineIndex - 1).getChangeWuXing());
        assertNull(chartSnapshot.getLines().get(lineIndex - 1).getChangeLiuQin());
    }

    private static Stream<Arguments> cases() {
        return Stream.of(
                Arguments.of("静卦样例", List.of("少阳", "少阳", "少阳", "少阳", "少阳", "少阳"), List.of(), "乾为天", "乾为天", "乾", "乾", 6, 3, 0),
                Arguments.of("单动爻样例", List.of("老阳", "少阳", "少阳", "少阳", "少阳", "少阳"), List.of(1), "乾为天", "天风姤", "乾", "乾", 6, 3, 1),
                Arguments.of("双动爻样例", List.of("老阳", "少阴", "少阳", "少阴", "老阴", "少阳"), List.of(1, 5), "山火贲", "风山渐", "艮", "巽", 1, 4, 2),
                Arguments.of("多动爻样例", List.of("老阳", "老阴", "少阳", "少阴", "老阴", "少阳"), List.of(1, 2, 5), "山火贲", "巽为风", "艮", "巽", 1, 4, 3)
        );
    }

    private static Stream<Arguments> changedLineCases() {
        return Stream.of(
                Arguments.of("单动爻样例", List.of("老阳", "少阳", "少阳", "少阳", "少阳", "少阳"), List.of(1), 1, "阴", "丑", "土", "父母"),
                Arguments.of("双动爻样例-初爻", List.of("老阳", "少阴", "少阳", "少阴", "老阴", "少阳"), List.of(1, 5), 1, "阴", "辰", "土", "兄弟"),
                Arguments.of("双动爻样例-五爻", List.of("老阳", "少阴", "少阳", "少阴", "老阴", "少阳"), List.of(1, 5), 5, "阳", "巳", "火", "父母")
        );
    }

    private static Stream<Arguments> staticLineCases() {
        return Stream.of(
                Arguments.of("静卦样例", List.of("少阳", "少阳", "少阳", "少阳", "少阳", "少阳"), List.of(), 1),
                Arguments.of("双动爻样例", List.of("老阳", "少阴", "少阳", "少阴", "老阴", "少阳"), List.of(1, 5), 3)
        );
    }
}
