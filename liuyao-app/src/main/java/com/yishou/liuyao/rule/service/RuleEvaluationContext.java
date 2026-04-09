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
    private Boolean useGodMoving;
    private Integer useGodLineCount;
    private Boolean useGodEmpty;
    private Boolean useGodMonthBreak;
    private Boolean useGodDayBreak;
    private Boolean useGodRuMu;
    private Boolean useGodChongKai;
    private Boolean useGodChongSan;
    private Boolean hasMovingShengUseGod;
    private Boolean hasMovingKeUseGod;
    private Boolean hasChangedShengUseGod;
    private Boolean hasChangedKeUseGod;
    private Boolean hasMovingChongUseGod;
    private Boolean hasMovingChongShi;
    private Integer useGodBestScore;
    private Integer useGodDistanceToShi;
    private String yongshenState;
    private String useGodToShiRelation;
    private Boolean useGodHeShi;
    private Boolean useGodRetreat;
    private String shiState;
    private Boolean shiMoving;
    private Boolean shiEmpty;
    private Boolean shiYingExists;
    private Integer shiYingDistance;
    private String shiYingRelation;
    private Integer movingCount;
    private List<String> kongWangBranches = List.of();

    public static RuleEvaluationContext from(ChartSnapshot chartSnapshot, List<RuleHit> hits) {
        RuleEvaluationContext context = new RuleEvaluationContext();
        context.setQuestionType(chartSnapshot == null ? null : chartSnapshot.getQuestionCategory());
        context.setUseGod(UseGodLineLocator.extractUseGod(chartSnapshot));
        context.setUseGodFound(context.getUseGod() != null && !context.getUseGod().isBlank());
        context.setUseGodLineIndex(resolveUseGodLineIndex(chartSnapshot, context.getUseGod()));
        context.setUseGodLineCount(resolveUseGodLineCount(chartSnapshot, context.getUseGod()));
        context.setUseGodMoving(resolveUseGodMoving(chartSnapshot, context.getUseGodLineIndex()));
        context.setUseGodEmpty(resolveUseGodEmpty(chartSnapshot, context.getUseGodLineIndex()));
        context.setUseGodMonthBreak(resolveRuleHitFlag(hits, "USE_GOD_MONTH_BREAK"));
        context.setUseGodDayBreak(resolveRuleHitFlag(hits, "USE_GOD_DAY_BREAK"));
        context.setUseGodRuMu(resolveUseGodRuMu(chartSnapshot, context.getUseGodLineIndex()));
        context.setUseGodChongKai(resolveUseGodChongKai(context));
        context.setUseGodChongSan(resolveUseGodChongSan(context));
        context.setHasMovingShengUseGod(resolveMovingEffectFlag(hits, "relation", "动爻生用神"));
        context.setHasMovingKeUseGod(resolveMovingEffectFlag(hits, "relation", "动爻克用神"));
        context.setHasChangedShengUseGod(resolveMovingEffectFlag(hits, "changeRelation", "变爻生用神"));
        context.setHasChangedKeUseGod(resolveMovingEffectFlag(hits, "changeRelation", "变爻克用神"));
        context.setHasMovingChongUseGod(resolveMovingChongUseGod(chartSnapshot, context.getUseGodLineIndex()));
        context.setHasMovingChongShi(resolveMovingChongShi(chartSnapshot));
        context.setUseGodBestScore(resolveUseGodBestScore(hits));
        context.setUseGodDistanceToShi(resolveUseGodDistanceToShi(chartSnapshot, context.getUseGodLineIndex()));
        context.setYongshenState(resolveUseGodState(hits));
        context.setUseGodToShiRelation(resolveUseGodToShiRelation(chartSnapshot, context.getUseGod()));
        context.setUseGodHeShi(resolveUseGodHeShi(chartSnapshot, context.getUseGod()));
        context.setUseGodRetreat(resolveUseGodRetreat(chartSnapshot, context.getUseGod()));
        context.setShiState(resolveShiState(chartSnapshot));
        context.setShiMoving(resolveShiMoving(chartSnapshot));
        context.setShiEmpty(resolveShiEmpty(chartSnapshot));
        context.setShiYingExists(resolveShiYingExists(chartSnapshot));
        context.setShiYingDistance(resolveShiYingDistance(chartSnapshot));
        context.setShiYingRelation(resolveShiYingRelation(hits));
        context.setMovingCount(resolveMovingCount(chartSnapshot));
        context.setKongWangBranches(chartSnapshot == null || chartSnapshot.getKongWang() == null ? List.of() : chartSnapshot.getKongWang());
        return context;
    }

    private static Integer resolveUseGodLineIndex(ChartSnapshot chartSnapshot, String useGod) {
        if (chartSnapshot == null || useGod == null || useGod.isBlank()) {
            return null;
        }
        Object explicitLineIndex = chartSnapshot.getExt() == null ? null : chartSnapshot.getExt().get("useGodLineIndex");
        if (explicitLineIndex instanceof Number number) {
            return number.intValue();
        }
        return resolveUseGodLines(chartSnapshot, useGod).stream()
                .map(LineInfo::getIndex)
                .findFirst()
                .orElse(null);
    }

    private static Integer resolveUseGodLineCount(ChartSnapshot chartSnapshot, String useGod) {
        return resolveUseGodLines(chartSnapshot, useGod).size();
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
        LineInfo useGodLine = resolveSelectedUseGodLine(chartSnapshot, useGod);
        if (useGodLine == null || useGodLine.getWuXing() == null || useGodLine.getWuXing().isBlank()) {
            return null;
        }
        String relation = UseGodLineLocator.relationOf(useGodLine.getWuXing(), shiLine.getWuXing());
        return relation == null || relation.isBlank() ? null : relation;
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
        LineInfo useGodLine = resolveSelectedUseGodLine(chartSnapshot, useGod);
        return useGodLine != null
                && useGodLine.getBranch() != null
                && !useGodLine.getBranch().isBlank()
                && isHe(useGodLine.getBranch(), shiLine.getBranch());
    }

    private static Boolean resolveUseGodRetreat(ChartSnapshot chartSnapshot, String useGod) {
        if (chartSnapshot == null || chartSnapshot.getLines() == null || useGod == null || useGod.isBlank()) {
            return false;
        }
        LineInfo useGodLine = resolveSelectedUseGodLine(chartSnapshot, useGod);
        if (useGodLine == null || !Boolean.TRUE.equals(useGodLine.getIsMoving())) {
            return false;
        }
        return (useGodLine.getChangeLiuQin() != null && !useGod.equals(useGodLine.getChangeLiuQin()))
                || "被克".equals(UseGodLineLocator.relationOf(useGodLine.getChangeWuXing(), useGodLine.getWuXing()));
    }

    private static Boolean resolveUseGodMoving(ChartSnapshot chartSnapshot, Integer useGodLineIndex) {
        return resolveLineByIndex(chartSnapshot, useGodLineIndex)
                .map(line -> Boolean.TRUE.equals(line.getIsMoving()))
                .orElse(false);
    }

    private static Boolean resolveUseGodEmpty(ChartSnapshot chartSnapshot, Integer useGodLineIndex) {
        if (chartSnapshot == null || chartSnapshot.getKongWang() == null || chartSnapshot.getKongWang().isEmpty()) {
            return false;
        }
        return resolveLineByIndex(chartSnapshot, useGodLineIndex)
                .map(line -> line.getBranch() != null && chartSnapshot.getKongWang().contains(line.getBranch()))
                .orElse(false);
    }

    private static Integer resolveUseGodBestScore(List<RuleHit> hits) {
        if (hits == null) {
            return null;
        }
        return hits.stream()
                .filter(hit -> "USE_GOD_STRENGTH".equals(hit.getRuleCode()))
                .map(RuleHit::getEvidence)
                .filter(evidence -> evidence != null)
                .map(evidence -> evidence.get("bestScore"))
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .map(Number::intValue)
                .findFirst()
                .orElse(null);
    }

    private static Boolean resolveMovingEffectFlag(List<RuleHit> hits, String field, String expectedValue) {
        if (hits == null || field == null || expectedValue == null) {
            return false;
        }
        for (RuleHit hit : hits) {
            if (!"MOVING_LINE_AFFECT_USE_GOD".equals(hit.getRuleCode()) || !Boolean.TRUE.equals(hit.getHit())) {
                continue;
            }
            Object effects = hit.getEvidence() == null ? null : hit.getEvidence().get("effects");
            if (!(effects instanceof List<?> effectList)) {
                continue;
            }
            for (Object item : effectList) {
                if (!(item instanceof java.util.Map<?, ?> effect)) {
                    continue;
                }
                if (expectedValue.equals(effect.get(field))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Boolean resolveRuleHitFlag(List<RuleHit> hits, String ruleCode) {
        if (hits == null || ruleCode == null || ruleCode.isBlank()) {
            return false;
        }
        return hits.stream()
                .anyMatch(hit -> ruleCode.equals(hit.getRuleCode()) && Boolean.TRUE.equals(hit.getHit()));
    }

    private static Integer resolveUseGodDistanceToShi(ChartSnapshot chartSnapshot, Integer useGodLineIndex) {
        if (chartSnapshot == null || chartSnapshot.getShi() == null || useGodLineIndex == null) {
            return null;
        }
        return Math.abs(useGodLineIndex - chartSnapshot.getShi());
    }

    private static Boolean resolveUseGodRuMu(ChartSnapshot chartSnapshot, Integer useGodLineIndex) {
        LineInfo useGodLine = resolveLineByIndex(chartSnapshot, useGodLineIndex).orElse(null);
        if (useGodLine == null || useGodLine.getWuXing() == null || useGodLine.getBranch() == null) {
            return false;
        }
        String tombBranch = resolveTombBranch(useGodLine.getWuXing());
        return tombBranch != null && tombBranch.equals(useGodLine.getBranch());
    }

    private static String resolveTombBranch(String wuXing) {
        if (wuXing == null || wuXing.isBlank()) {
            return null;
        }
        return switch (wuXing) {
            case "木" -> "未";
            case "火" -> "戌";
            case "金" -> "丑";
            case "水", "土" -> "辰";
            default -> null;
        };
    }

    private static Boolean resolveUseGodChongKai(RuleEvaluationContext context) {
        if (context == null) {
            return false;
        }
        boolean hasBreak = Boolean.TRUE.equals(context.getUseGodMonthBreak())
                || Boolean.TRUE.equals(context.getUseGodDayBreak())
                || Boolean.TRUE.equals(context.getHasMovingChongUseGod());
        boolean constrained = Boolean.TRUE.equals(context.getUseGodEmpty()) || Boolean.TRUE.equals(context.getUseGodRuMu());
        return hasBreak && constrained;
    }

    private static Boolean resolveUseGodChongSan(RuleEvaluationContext context) {
        if (context == null) {
            return false;
        }
        boolean hasBreak = Boolean.TRUE.equals(context.getUseGodMonthBreak())
                || Boolean.TRUE.equals(context.getUseGodDayBreak())
                || Boolean.TRUE.equals(context.getHasMovingChongUseGod());
        boolean constrained = Boolean.TRUE.equals(context.getUseGodEmpty()) || Boolean.TRUE.equals(context.getUseGodRuMu());
        return hasBreak && !constrained;
    }

    private static Boolean resolveMovingChongUseGod(ChartSnapshot chartSnapshot, Integer useGodLineIndex) {
        LineInfo useGodLine = resolveLineByIndex(chartSnapshot, useGodLineIndex).orElse(null);
        if (useGodLine == null || useGodLine.getBranch() == null || useGodLine.getBranch().isBlank()) {
            return false;
        }
        return chartSnapshot != null && chartSnapshot.getLines() != null && chartSnapshot.getLines().stream()
                .filter(line -> Boolean.TRUE.equals(line.getIsMoving()))
                .filter(line -> line.getIndex() == null || !line.getIndex().equals(useGodLineIndex))
                .anyMatch(line -> line.getBranch() != null && UseGodLineLocator.isChong(line.getBranch(), useGodLine.getBranch()));
    }

    private static Boolean resolveMovingChongShi(ChartSnapshot chartSnapshot) {
        if (chartSnapshot == null || chartSnapshot.getShi() == null) {
            return false;
        }
        LineInfo shiLine = resolveLineByIndex(chartSnapshot, chartSnapshot.getShi()).orElse(null);
        if (shiLine == null || shiLine.getBranch() == null || shiLine.getBranch().isBlank()) {
            return false;
        }
        return chartSnapshot.getLines() != null && chartSnapshot.getLines().stream()
                .filter(line -> Boolean.TRUE.equals(line.getIsMoving()))
                .filter(line -> line.getIndex() == null || !line.getIndex().equals(chartSnapshot.getShi()))
                .anyMatch(line -> line.getBranch() != null && UseGodLineLocator.isChong(line.getBranch(), shiLine.getBranch()));
    }

    private static LineInfo resolveSelectedUseGodLine(ChartSnapshot chartSnapshot, String useGod) {
        Integer useGodLineIndex = resolveUseGodLineIndex(chartSnapshot, useGod);
        return resolveLineByIndex(chartSnapshot, useGodLineIndex).orElse(null);
    }

    private static java.util.Optional<LineInfo> resolveLineByIndex(ChartSnapshot chartSnapshot, Integer lineIndex) {
        if (chartSnapshot == null || chartSnapshot.getLines() == null || lineIndex == null) {
            return java.util.Optional.empty();
        }
        return chartSnapshot.getLines().stream()
                .filter(line -> lineIndex.equals(line.getIndex()))
                .findFirst();
    }

    private static List<LineInfo> resolveUseGodLines(ChartSnapshot chartSnapshot, String useGod) {
        if (chartSnapshot == null || chartSnapshot.getLines() == null || useGod == null || useGod.isBlank()) {
            return List.of();
        }
        return switch (useGod) {
            case "世爻" -> chartSnapshot.getLines().stream().filter(line -> Boolean.TRUE.equals(line.getIsShi())).toList();
            case "应爻" -> chartSnapshot.getLines().stream().filter(line -> Boolean.TRUE.equals(line.getIsYing())).toList();
            default -> chartSnapshot.getLines().stream().filter(line -> useGod.equals(line.getLiuQin())).toList();
        };
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

    private static Boolean resolveShiYingExists(ChartSnapshot chartSnapshot) {
        if (chartSnapshot == null || chartSnapshot.getLines() == null) {
            return false;
        }
        boolean hasShi = chartSnapshot.getLines().stream().anyMatch(line -> Boolean.TRUE.equals(line.getIsShi()));
        boolean hasYing = chartSnapshot.getLines().stream().anyMatch(line -> Boolean.TRUE.equals(line.getIsYing()));
        return hasShi && hasYing;
    }

    private static Integer resolveShiYingDistance(ChartSnapshot chartSnapshot) {
        if (chartSnapshot == null || chartSnapshot.getShi() == null || chartSnapshot.getYing() == null) {
            return null;
        }
        return Math.abs(chartSnapshot.getShi() - chartSnapshot.getYing());
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

    public Boolean getUseGodMoving() {
        return useGodMoving;
    }

    public void setUseGodMoving(Boolean useGodMoving) {
        this.useGodMoving = useGodMoving;
    }

    public Integer getUseGodLineCount() {
        return useGodLineCount;
    }

    public void setUseGodLineCount(Integer useGodLineCount) {
        this.useGodLineCount = useGodLineCount;
    }

    public Boolean getUseGodEmpty() {
        return useGodEmpty;
    }

    public void setUseGodEmpty(Boolean useGodEmpty) {
        this.useGodEmpty = useGodEmpty;
    }

    public Boolean getUseGodMonthBreak() {
        return useGodMonthBreak;
    }

    public void setUseGodMonthBreak(Boolean useGodMonthBreak) {
        this.useGodMonthBreak = useGodMonthBreak;
    }

    public Boolean getUseGodDayBreak() {
        return useGodDayBreak;
    }

    public void setUseGodDayBreak(Boolean useGodDayBreak) {
        this.useGodDayBreak = useGodDayBreak;
    }

    public Boolean getUseGodRuMu() {
        return useGodRuMu;
    }

    public void setUseGodRuMu(Boolean useGodRuMu) {
        this.useGodRuMu = useGodRuMu;
    }

    public Boolean getUseGodChongKai() {
        return useGodChongKai;
    }

    public void setUseGodChongKai(Boolean useGodChongKai) {
        this.useGodChongKai = useGodChongKai;
    }

    public Boolean getUseGodChongSan() {
        return useGodChongSan;
    }

    public void setUseGodChongSan(Boolean useGodChongSan) {
        this.useGodChongSan = useGodChongSan;
    }

    public Boolean getHasMovingShengUseGod() {
        return hasMovingShengUseGod;
    }

    public void setHasMovingShengUseGod(Boolean hasMovingShengUseGod) {
        this.hasMovingShengUseGod = hasMovingShengUseGod;
    }

    public Boolean getHasMovingKeUseGod() {
        return hasMovingKeUseGod;
    }

    public void setHasMovingKeUseGod(Boolean hasMovingKeUseGod) {
        this.hasMovingKeUseGod = hasMovingKeUseGod;
    }

    public Boolean getHasChangedShengUseGod() {
        return hasChangedShengUseGod;
    }

    public void setHasChangedShengUseGod(Boolean hasChangedShengUseGod) {
        this.hasChangedShengUseGod = hasChangedShengUseGod;
    }

    public Boolean getHasChangedKeUseGod() {
        return hasChangedKeUseGod;
    }

    public void setHasChangedKeUseGod(Boolean hasChangedKeUseGod) {
        this.hasChangedKeUseGod = hasChangedKeUseGod;
    }

    public Boolean getHasMovingChongUseGod() {
        return hasMovingChongUseGod;
    }

    public void setHasMovingChongUseGod(Boolean hasMovingChongUseGod) {
        this.hasMovingChongUseGod = hasMovingChongUseGod;
    }

    public Boolean getHasMovingChongShi() {
        return hasMovingChongShi;
    }

    public void setHasMovingChongShi(Boolean hasMovingChongShi) {
        this.hasMovingChongShi = hasMovingChongShi;
    }

    public Integer getUseGodBestScore() {
        return useGodBestScore;
    }

    public void setUseGodBestScore(Integer useGodBestScore) {
        this.useGodBestScore = useGodBestScore;
    }

    public Integer getUseGodDistanceToShi() {
        return useGodDistanceToShi;
    }

    public void setUseGodDistanceToShi(Integer useGodDistanceToShi) {
        this.useGodDistanceToShi = useGodDistanceToShi;
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

    public Boolean getShiYingExists() {
        return shiYingExists;
    }

    public void setShiYingExists(Boolean shiYingExists) {
        this.shiYingExists = shiYingExists;
    }

    public Integer getShiYingDistance() {
        return shiYingDistance;
    }

    public void setShiYingDistance(Integer shiYingDistance) {
        this.shiYingDistance = shiYingDistance;
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
