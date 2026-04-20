package com.yishou.liuyao.divination.service;

import com.yishou.liuyao.divination.domain.CalendarSnapshot;
import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.DivinationInput;
import com.yishou.liuyao.divination.domain.LineInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChartBuilderService {

    private static final Logger log = LoggerFactory.getLogger(ChartBuilderService.class);

    private final CalendarFacade calendarFacade;
    private final HexagramResolver hexagramResolver;
    private final ShiYingResolver shiYingResolver;
    private final LiuShenResolver liuShenResolver;
    private final PalaceResolver palaceResolver;
    private final NaJiaResolver naJiaResolver;
    private final LiuQinResolver liuQinResolver;
    private final FuShenResolver fuShenResolver;
    private final DerivedHexagramResolver derivedHexagramResolver;
    private final ShenShaResolver shenShaResolver;

    public ChartBuilderService(CalendarFacade calendarFacade,
                               HexagramResolver hexagramResolver,
                               ShiYingResolver shiYingResolver,
                               LiuShenResolver liuShenResolver,
                               PalaceResolver palaceResolver,
                               NaJiaResolver naJiaResolver,
                               LiuQinResolver liuQinResolver) {
        this(calendarFacade,
                hexagramResolver,
                shiYingResolver,
                liuShenResolver,
                palaceResolver,
                naJiaResolver,
                liuQinResolver,
                new FuShenResolver(naJiaResolver, liuQinResolver),
                new DerivedHexagramResolver(hexagramResolver),
                new ShenShaResolver());
    }

    @Autowired
    public ChartBuilderService(CalendarFacade calendarFacade,
                               HexagramResolver hexagramResolver,
                               ShiYingResolver shiYingResolver,
                               LiuShenResolver liuShenResolver,
                               PalaceResolver palaceResolver,
                               NaJiaResolver naJiaResolver,
                               LiuQinResolver liuQinResolver,
                               FuShenResolver fuShenResolver,
                               DerivedHexagramResolver derivedHexagramResolver,
                               ShenShaResolver shenShaResolver) {
        this.calendarFacade = calendarFacade;
        this.hexagramResolver = hexagramResolver;
        this.shiYingResolver = shiYingResolver;
        this.liuShenResolver = liuShenResolver;
        this.palaceResolver = palaceResolver;
        this.naJiaResolver = naJiaResolver;
        this.liuQinResolver = liuQinResolver;
        this.fuShenResolver = fuShenResolver;
        this.derivedHexagramResolver = derivedHexagramResolver;
        this.shenShaResolver = shenShaResolver;
    }

    public ChartSnapshot buildChart(DivinationInput input) {
        // 先把时间对应的历法信息算准，再基于爻象生成整张盘。
        CalendarSnapshot calendarSnapshot = calendarFacade.resolve(input.getDivinationTime());
        HexagramResult hexagramResult = hexagramResolver.resolve(input.getRawLines());
        ShiYingPosition shiYingPosition = shiYingResolver.resolve(hexagramResult.getMainHexagramName());
        PalaceInfo palaceInfo = palaceResolver.resolve(hexagramResult.getMainHexagramName());
        List<String> naJiaBranches = naJiaResolver.resolve(hexagramResult.getMainHexagramName(), hexagramResult.getMainHexagramCode());
        List<String> changedNaJiaBranches = naJiaResolver.resolve(hexagramResult.getChangedHexagramName(), hexagramResult.getChangedHexagramCode());
        ChartSnapshot chartSnapshot = new ChartSnapshot();
        chartSnapshot.setQuestion(input.getQuestion());
        chartSnapshot.setQuestionCategory(input.getQuestionCategory());
        chartSnapshot.setDivinationTime(input.getDivinationTime());
        chartSnapshot.setDivinationMethod("MANUAL_PLACEHOLDER");
        chartSnapshot.setMainHexagram(hexagramResult.getMainHexagramName());
        chartSnapshot.setChangedHexagram(hexagramResult.getChangedHexagramName());
        chartSnapshot.setMainHexagramCode(hexagramResult.getMainHexagramCode());
        chartSnapshot.setChangedHexagramCode(hexagramResult.getChangedHexagramCode());
        chartSnapshot.setMainUpperTrigram(hexagramResult.getMainUpperTrigram());
        chartSnapshot.setMainLowerTrigram(hexagramResult.getMainLowerTrigram());
        chartSnapshot.setChangedUpperTrigram(hexagramResult.getChangedUpperTrigram());
        chartSnapshot.setChangedLowerTrigram(hexagramResult.getChangedLowerTrigram());
        DerivedHexagramResolver.DerivedHexagramSet derivedHexagrams = derivedHexagramResolver.resolve(hexagramResult.getMainHexagramCode());
        chartSnapshot.setMutualHexagram(derivedHexagrams.mutual().name());
        chartSnapshot.setMutualHexagramCode(derivedHexagrams.mutual().code());
        chartSnapshot.setOppositeHexagram(derivedHexagrams.opposite().name());
        chartSnapshot.setOppositeHexagramCode(derivedHexagrams.opposite().code());
        chartSnapshot.setReversedHexagram(derivedHexagrams.reversed().name());
        chartSnapshot.setReversedHexagramCode(derivedHexagrams.reversed().code());
        chartSnapshot.setPalace(palaceInfo.getPalace());
        chartSnapshot.setPalaceWuXing(palaceInfo.getWuXing());
        chartSnapshot.setShi(shiYingPosition.getShiIndex());
        chartSnapshot.setYing(shiYingPosition.getYingIndex());
        chartSnapshot.setRiChen(calendarSnapshot.getRiChen());
        chartSnapshot.setYueJian(calendarSnapshot.getYueJian());
        chartSnapshot.setKongWang(calendarSnapshot.getKongWang());
        chartSnapshot.setSnapshotVersion("v1");
        chartSnapshot.setCalendarVersion("v1");
        chartSnapshot.setLines(buildLines(
                input.getRawLines(),
                input.getMovingLines(),
                shiYingPosition,
                calendarSnapshot,
                naJiaBranches,
                changedNaJiaBranches,
                palaceInfo.getPalace(),
                palaceInfo.getWuXing()
        ));
        chartSnapshot.setShenShaHits(shenShaResolver.resolve(chartSnapshot.getRiChen(), chartSnapshot.getLines()));
        chartSnapshot.getExt().put("rawLines", input.getRawLines());
        chartSnapshot.getExt().put("palace", palaceInfo.getPalace());
        chartSnapshot.getExt().put("palaceWuXing", palaceInfo.getWuXing());
        chartSnapshot.getExt().put("mainHexagramCode", hexagramResult.getMainHexagramCode());
        chartSnapshot.getExt().put("changedHexagramCode", hexagramResult.getChangedHexagramCode());
        chartSnapshot.getExt().put("mutualHexagram", chartSnapshot.getMutualHexagram());
        chartSnapshot.getExt().put("mutualHexagramCode", chartSnapshot.getMutualHexagramCode());
        chartSnapshot.getExt().put("oppositeHexagram", chartSnapshot.getOppositeHexagram());
        chartSnapshot.getExt().put("oppositeHexagramCode", chartSnapshot.getOppositeHexagramCode());
        chartSnapshot.getExt().put("reversedHexagram", chartSnapshot.getReversedHexagram());
        chartSnapshot.getExt().put("reversedHexagramCode", chartSnapshot.getReversedHexagramCode());
        chartSnapshot.getExt().put("mainUpperTrigram", hexagramResult.getMainUpperTrigram());
        chartSnapshot.getExt().put("mainLowerTrigram", hexagramResult.getMainLowerTrigram());
        chartSnapshot.getExt().put("changedUpperTrigram", hexagramResult.getChangedUpperTrigram());
        chartSnapshot.getExt().put("changedLowerTrigram", hexagramResult.getChangedLowerTrigram());
        chartSnapshot.getExt().put("shenShaHits", chartSnapshot.getShenShaHits());
        log.info("排盘完成: category={}, mainHexagram={}({}/{}) changedHexagram={}({}/{}), shi={}, ying={}, movingLineCount={}",
                input.getQuestionCategory(),
                chartSnapshot.getMainHexagram(),
                chartSnapshot.getMainUpperTrigram(),
                chartSnapshot.getMainLowerTrigram(),
                chartSnapshot.getChangedHexagram(),
                chartSnapshot.getChangedUpperTrigram(),
                chartSnapshot.getChangedLowerTrigram(),
                chartSnapshot.getShi(),
                chartSnapshot.getYing(),
                chartSnapshot.getLines().stream().filter(line -> Boolean.TRUE.equals(line.getMoving())).count());
        return chartSnapshot;
    }

    private List<LineInfo> buildLines(List<String> rawLines,
                                      List<Integer> movingLines,
                                      ShiYingPosition shiYingPosition,
                                      CalendarSnapshot calendarSnapshot,
                                      List<String> naJiaBranches,
                                      List<String> changedNaJiaBranches,
                                      String palace,
                                      String palaceWuXing) {
        List<LineInfo> lines = new ArrayList<>();
        for (int index = 1; index <= 6; index++) {
            LineInfo lineInfo = new LineInfo();
            lineInfo.setIndex(index);
            String rawLine = rawLines != null && rawLines.size() >= index ? rawLines.get(index - 1) : null;
            lineInfo.setYinYang(resolveYinYang(rawLine, index));
            lineInfo.setMoving(resolveMoving(rawLine, movingLines, index));
            lineInfo.setChangeTo(resolveChangeTo(rawLine));
            lineInfo.setLiuShen(liuShenResolver.resolve(calendarSnapshot.getRiChen(), index));
            String branch = naJiaBranches.get(index - 1);
            lineInfo.setBranch(branch);
            lineInfo.setWuXing(WuXingSupport.branchToWuXing(branch));
            // 六亲以卦宫五行为“我”，结合本爻地支五行推得。
            lineInfo.setLiuQin(liuQinResolver.resolve(palaceWuXing, branch));
            if (Boolean.TRUE.equals(lineInfo.getIsMoving())) {
                // 动爻需要补齐变后地支、五行、六亲，方便规则直接读取。
                String changeBranch = changedNaJiaBranches.get(index - 1);
                lineInfo.setChangeBranch(changeBranch);
                lineInfo.setChangeWuXing(WuXingSupport.branchToWuXing(changeBranch));
                lineInfo.setChangeLiuQin(liuQinResolver.resolve(palaceWuXing, changeBranch));
            }
            lineInfo.setShi(index == shiYingPosition.getShiIndex());
            lineInfo.setYing(index == shiYingPosition.getYingIndex());
            lines.add(lineInfo);
        }
        return fuShenResolver.resolve(palace, palaceWuXing, lines);
    }

    private String resolveYinYang(String rawLine, int index) {
        String normalized = rawLine == null ? "" : rawLine.trim();
        return switch (normalized) {
            case "少阳", "老阳" -> "阳";
            case "少阴", "老阴" -> "阴";
            default -> index % 2 == 0 ? "阴" : "阳";
        };
    }

    private boolean resolveMoving(String rawLine, List<Integer> movingLines, int index) {
        String normalized = rawLine == null ? "" : rawLine.trim();
        if ("老阳".equals(normalized) || "老阴".equals(normalized)) {
            return true;
        }
        return movingLines != null && movingLines.contains(index);
    }

    private String resolveChangeTo(String rawLine) {
        String normalized = rawLine == null ? "" : rawLine.trim();
        return switch (normalized) {
            case "老阳" -> "阴";
            case "老阴" -> "阳";
            default -> null;
        };
    }

}
