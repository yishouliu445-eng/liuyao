package com.yishou.liuyao.divination.service;

import com.yishou.liuyao.divination.domain.LineInfo;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class FuShenResolver {

    private static final Map<String, String> PURE_PALACE_HEXAGRAMS = Map.of(
            "乾", "乾为天",
            "兑", "兑为泽",
            "离", "离为火",
            "震", "震为雷",
            "巽", "巽为风",
            "坎", "坎为水",
            "艮", "艮为山",
            "坤", "坤为地"
    );

    private static final Map<String, String> PURE_PALACE_CODES = Map.of(
            "乾", "111111",
            "兑", "110110",
            "离", "101101",
            "震", "100100",
            "巽", "011011",
            "坎", "010010",
            "艮", "001001",
            "坤", "000000"
    );

    private final NaJiaResolver naJiaResolver;
    private final LiuQinResolver liuQinResolver;

    public FuShenResolver(NaJiaResolver naJiaResolver, LiuQinResolver liuQinResolver) {
        this.naJiaResolver = naJiaResolver;
        this.liuQinResolver = liuQinResolver;
    }

    public List<LineInfo> resolve(String palace, String palaceWuXing, List<LineInfo> lines) {
        if (lines == null || lines.isEmpty() || palace == null || palace.isBlank() || palaceWuXing == null || palaceWuXing.isBlank()) {
            return lines == null ? List.of() : lines;
        }
        String pureHexagramName = PURE_PALACE_HEXAGRAMS.get(palace);
        String pureHexagramCode = PURE_PALACE_CODES.get(palace);
        if (pureHexagramName == null || pureHexagramCode == null) {
            return lines;
        }

        List<String> pureBranches = naJiaResolver.resolve(pureHexagramName, pureHexagramCode);
        Set<String> visibleLiuQin = new HashSet<>();
        for (LineInfo line : lines) {
            if (line.getLiuQin() != null && !line.getLiuQin().isBlank()) {
                visibleLiuQin.add(line.getLiuQin());
            }
        }

        for (int index = 0; index < Math.min(lines.size(), pureBranches.size()); index++) {
            LineInfo line = lines.get(index);
            String pureBranch = pureBranches.get(index);
            String pureLiuQin = liuQinResolver.resolve(palaceWuXing, pureBranch);
            if (visibleLiuQin.contains(pureLiuQin)) {
                continue;
            }
            line.setFuShenBranch(pureBranch);
            line.setFuShenWuXing(WuXingSupport.branchToWuXing(pureBranch));
            line.setFuShenLiuQin(pureLiuQin);
            line.setFlyShenBranch(line.getBranch());
            line.setFlyShenWuXing(line.getWuXing());
            line.setFlyShenLiuQin(line.getLiuQin());
        }
        return lines;
    }
}
