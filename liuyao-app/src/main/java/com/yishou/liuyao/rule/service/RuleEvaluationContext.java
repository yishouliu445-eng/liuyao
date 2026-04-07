package com.yishou.liuyao.rule.service;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.rule.RuleHit;
import com.yishou.liuyao.rule.batch.UseGodLineLocator;

import java.util.List;

public class RuleEvaluationContext {

    private String questionType;
    private String useGod;
    private Boolean useGodFound;
    private Integer useGodLineIndex;
    private String yongshenState;
    private String useGodToShiRelation;
    private Boolean useGodHeShi;
    private Boolean useGodRetreat;
    private String shiState;
    private Boolean shiMoving;
    private Boolean shiEmpty;
    private String shiYingRelation;
    private Integer movingCount;
    private List<String> kongWangBranches = List.of();

    public static RuleEvaluationContext from(ChartSnapshot chartSnapshot, List<RuleHit> hits) {
        RuleEvaluationContext context = new RuleEvaluationContext();
        context.setQuestionType(chartSnapshot == null ? null : chartSnapshot.getQuestionCategory());
        context.setUseGod(UseGodLineLocator.extractUseGod(chartSnapshot));
        context.setUseGodFound(context.getUseGod() != null && !context.getUseGod().isBlank());
        context.setUseGodLineIndex(resolveUseGodLineIndex(chartSnapshot, context.getUseGod()));
        context.setYongshenState(resolveUseGodState(hits));
        context.setUseGodToShiRelation(resolveUseGodToShiRelation(chartSnapshot, context.getUseGod()));
        context.setUseGodHeShi(resolveUseGodHeShi(chartSnapshot, context.getUseGod()));
        context.setUseGodRetreat(resolveUseGodRetreat(chartSnapshot, context.getUseGod()));
        context.setShiState(resolveShiState(chartSnapshot));
        context.setShiMoving(resolveShiMoving(chartSnapshot));
        context.setShiEmpty(resolveShiEmpty(chartSnapshot));
        context.setShiYingRelation(resolveShiYingRelation(hits));
        context.setMovingCount(resolveMovingCount(chartSnapshot));
        context.setKongWangBranches(chartSnapshot == null || chartSnapshot.getKongWang() == null ? List.of() : chartSnapshot.getKongWang());
        return context;
    }

    private static Integer resolveUseGodLineIndex(ChartSnapshot chartSnapshot, String useGod) {
        if (chartSnapshot == null || useGod == null || useGod.isBlank()) {
            return null;
        }
        return chartSnapshot.getLines().stream()
                .filter(line -> useGod.equals(line.getLiuQin()))
                .map(LineInfo::getIndex)
                .findFirst()
                .orElse(null);
    }

    private static String resolveUseGodState(List<RuleHit> hits) {
        if (hits == null) {
            return null;
        }
        return hits.stream()
                .filter(hit -> "USE_GOD_STRENGTH".equals(hit.getRuleCode()))
                .map(RuleHit::getEvidence)
                .filter(evidence -> evidence != null)
                .map(evidence -> evidence.get("bestLevel"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .findFirst()
                .orElse(null);
    }

    private static String resolveUseGodToShiRelation(ChartSnapshot chartSnapshot, String useGod) {
        if (chartSnapshot == null || chartSnapshot.getShi() == null || chartSnapshot.getLines() == null || useGod == null || useGod.isBlank()) {
            return null;
        }
        LineInfo shiLine = chartSnapshot.getLines().stream()
                .filter(line -> chartSnapshot.getShi().equals(line.getIndex()))
                .findFirst()
                .orElse(null);
        if (shiLine == null || shiLine.getWuXing() == null || shiLine.getWuXing().isBlank()) {
            return null;
        }
        return chartSnapshot.getLines().stream()
                .filter(line -> useGod.equals(line.getLiuQin()))
                .map(LineInfo::getWuXing)
                .filter(value -> value != null && !value.isBlank())
                .map(value -> UseGodLineLocator.relationOf(value, shiLine.getWuXing()))
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private static Boolean resolveUseGodHeShi(ChartSnapshot chartSnapshot, String useGod) {
        if (chartSnapshot == null || chartSnapshot.getShi() == null || chartSnapshot.getLines() == null || useGod == null || useGod.isBlank()) {
            return false;
        }
        LineInfo shiLine = chartSnapshot.getLines().stream()
                .filter(line -> chartSnapshot.getShi().equals(line.getIndex()))
                .findFirst()
                .orElse(null);
        if (shiLine == null || shiLine.getBranch() == null || shiLine.getBranch().isBlank()) {
            return false;
        }
        return chartSnapshot.getLines().stream()
                .filter(line -> useGod.equals(line.getLiuQin()))
                .map(LineInfo::getBranch)
                .filter(value -> value != null && !value.isBlank())
                .anyMatch(value -> isHe(value, shiLine.getBranch()));
    }

    private static Boolean resolveUseGodRetreat(ChartSnapshot chartSnapshot, String useGod) {
        if (chartSnapshot == null || chartSnapshot.getLines() == null || useGod == null || useGod.isBlank()) {
            return false;
        }
        return chartSnapshot.getLines().stream()
                .filter(line -> useGod.equals(line.getLiuQin()))
                .filter(line -> Boolean.TRUE.equals(line.getIsMoving()))
                .anyMatch(line -> (line.getChangeLiuQin() != null && !useGod.equals(line.getChangeLiuQin()))
                        || "被克".equals(UseGodLineLocator.relationOf(line.getChangeWuXing(), line.getWuXing())));
    }

    private static String resolveShiState(ChartSnapshot chartSnapshot) {
        if (chartSnapshot == null || chartSnapshot.getShi() == null || chartSnapshot.getLines() == null) {
            return null;
        }
        return chartSnapshot.getLines().stream()
                .filter(line -> chartSnapshot.getShi().equals(line.getIndex()))
                .findFirst()
                .map(RuleEvaluationContext::scoreLineStrength)
                .orElse(null);
    }

    private static Boolean resolveShiMoving(ChartSnapshot chartSnapshot) {
        if (chartSnapshot == null || chartSnapshot.getShi() == null || chartSnapshot.getLines() == null) {
            return false;
        }
        return chartSnapshot.getLines().stream()
                .filter(line -> chartSnapshot.getShi().equals(line.getIndex()))
                .findFirst()
                .map(line -> Boolean.TRUE.equals(line.getIsMoving()))
                .orElse(false);
    }

    private static Boolean resolveShiEmpty(ChartSnapshot chartSnapshot) {
        if (chartSnapshot == null
                || chartSnapshot.getShi() == null
                || chartSnapshot.getLines() == null
                || chartSnapshot.getKongWang() == null
                || chartSnapshot.getKongWang().isEmpty()) {
            return false;
        }
        return chartSnapshot.getLines().stream()
                .filter(line -> chartSnapshot.getShi().equals(line.getIndex()))
                .findFirst()
                .map(line -> line.getBranch() != null && chartSnapshot.getKongWang().contains(line.getBranch()))
                .orElse(false);
    }

    private static String scoreLineStrength(LineInfo line) {
        if (line == null || line.getWuXing() == null || line.getWuXing().isBlank()) {
            return null;
        }
        int score = Boolean.TRUE.equals(line.getIsMoving()) ? 1 : 0;
        if (line.getWuXing().equals(line.getChangeWuXing())) {
            score += 1;
        }
        if (score >= 2) {
            return "STRONG";
        }
        if (score <= 0) {
            return "WEAK";
        }
        return "MEDIUM";
    }

    private static boolean isHe(String a, String b) {
        return ("子".equals(a) && "丑".equals(b)) || ("丑".equals(a) && "子".equals(b))
                || ("寅".equals(a) && "亥".equals(b)) || ("亥".equals(a) && "寅".equals(b))
                || ("卯".equals(a) && "戌".equals(b)) || ("戌".equals(a) && "卯".equals(b))
                || ("辰".equals(a) && "酉".equals(b)) || ("酉".equals(a) && "辰".equals(b))
                || ("巳".equals(a) && "申".equals(b)) || ("申".equals(a) && "巳".equals(b))
                || ("午".equals(a) && "未".equals(b)) || ("未".equals(a) && "午".equals(b));
    }

    private static String resolveShiYingRelation(List<RuleHit> hits) {
        if (hits == null) {
            return null;
        }
        return hits.stream()
                .filter(hit -> "SHI_YING_RELATION".equals(hit.getRuleCode()))
                .map(RuleHit::getEvidence)
                .filter(evidence -> evidence != null)
                .map(evidence -> evidence.get("relation"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .findFirst()
                .orElse(null);
    }

    private static Integer resolveMovingCount(ChartSnapshot chartSnapshot) {
        if (chartSnapshot == null || chartSnapshot.getLines() == null) {
            return 0;
        }
        return Math.toIntExact(chartSnapshot.getLines().stream().filter(line -> Boolean.TRUE.equals(line.getIsMoving())).count());
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getUseGod() {
        return useGod;
    }

    public void setUseGod(String useGod) {
        this.useGod = useGod;
    }

    public Integer getUseGodLineIndex() {
        return useGodLineIndex;
    }

    public void setUseGodLineIndex(Integer useGodLineIndex) {
        this.useGodLineIndex = useGodLineIndex;
    }

    public Boolean getUseGodFound() {
        return useGodFound;
    }

    public void setUseGodFound(Boolean useGodFound) {
        this.useGodFound = useGodFound;
    }

    public String getYongshenState() {
        return yongshenState;
    }

    public void setYongshenState(String yongshenState) {
        this.yongshenState = yongshenState;
    }

    public String getUseGodToShiRelation() {
        return useGodToShiRelation;
    }

    public void setUseGodToShiRelation(String useGodToShiRelation) {
        this.useGodToShiRelation = useGodToShiRelation;
    }

    public Boolean getUseGodHeShi() {
        return useGodHeShi;
    }

    public void setUseGodHeShi(Boolean useGodHeShi) {
        this.useGodHeShi = useGodHeShi;
    }

    public Boolean getUseGodRetreat() {
        return useGodRetreat;
    }

    public void setUseGodRetreat(Boolean useGodRetreat) {
        this.useGodRetreat = useGodRetreat;
    }

    public String getShiState() {
        return shiState;
    }

    public void setShiState(String shiState) {
        this.shiState = shiState;
    }

    public Boolean getShiMoving() {
        return shiMoving;
    }

    public void setShiMoving(Boolean shiMoving) {
        this.shiMoving = shiMoving;
    }

    public Boolean getShiEmpty() {
        return shiEmpty;
    }

    public void setShiEmpty(Boolean shiEmpty) {
        this.shiEmpty = shiEmpty;
    }

    public String getShiYingRelation() {
        return shiYingRelation;
    }

    public void setShiYingRelation(String shiYingRelation) {
        this.shiYingRelation = shiYingRelation;
    }

    public Integer getMovingCount() {
        return movingCount;
    }

    public void setMovingCount(Integer movingCount) {
        this.movingCount = movingCount;
    }

    public List<String> getKongWangBranches() {
        return kongWangBranches;
    }

    public void setKongWangBranches(List<String> kongWangBranches) {
        this.kongWangBranches = kongWangBranches;
    }
}
