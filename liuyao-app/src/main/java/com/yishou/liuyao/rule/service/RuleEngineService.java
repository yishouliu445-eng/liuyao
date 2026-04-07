package com.yishou.liuyao.rule.service;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.rule.Rule;
import com.yishou.liuyao.rule.RuleHit;
import com.yishou.liuyao.rule.definition.RuleDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RuleEngineService {

    private static final Logger log = LoggerFactory.getLogger(RuleEngineService.class);

    private final List<Rule> rules;
    private final RuleDefinitionConfigLoader ruleDefinitionConfigLoader;
    private final RuleMatcher ruleMatcher;
    private final RuleReasoningService ruleReasoningService;

    public RuleEngineService(List<Rule> rules) {
        this(rules, null, null, new RuleReasoningService());
    }

    @Autowired
    public RuleEngineService(List<Rule> rules,
                             RuleDefinitionConfigLoader ruleDefinitionConfigLoader,
                             RuleMatcher ruleMatcher,
                             RuleReasoningService ruleReasoningService) {
        this.rules = rules;
        this.ruleDefinitionConfigLoader = ruleDefinitionConfigLoader;
        this.ruleMatcher = ruleMatcher;
        this.ruleReasoningService = ruleReasoningService;
    }

    public List<RuleHit> evaluate(ChartSnapshot chartSnapshot) {
        return evaluateResult(chartSnapshot).getHits();
    }

    public RuleEvaluationResult evaluateResult(ChartSnapshot chartSnapshot) {
        // 规则引擎统一只返回命中的规则，未命中的规则交由各自测试覆盖。
        List<RuleHit> hits = rules.stream()
                .map(rule -> enrichHit(rule, rule.evaluate(chartSnapshot)))
                .filter(ruleHit -> Boolean.TRUE.equals(ruleHit.getHit()))
                .toList();
        List<RuleHit> configuredHits = evaluateConfiguredRules(chartSnapshot, hits);
        List<RuleHit> allHits = new ArrayList<>(hits);
        allHits.addAll(configuredHits);
        allHits = allHits.stream()
                .sorted(Comparator
                        .comparing((RuleHit hit) -> resolveStageOrder(hit.getCategory()))
                        .thenComparing(hit -> hit.getPriority() == null ? Integer.MAX_VALUE : hit.getPriority()))
                .toList();
        RuleEvaluationResult result = ruleReasoningService.summarize(chartSnapshot, allHits);
        log.info("规则引擎完成: mainHexagram={}, useGod={}, hitCount={}, hitRules={}",
                chartSnapshot == null ? "" : chartSnapshot.getMainHexagram(),
                chartSnapshot == null ? "" : chartSnapshot.getUseGod(),
                allHits.size(),
                allHits.stream().map(RuleHit::getRuleCode).toList());
        if (log.isDebugEnabled()) {
            allHits.forEach(hit -> log.debug("规则命中详情: ruleCode={}, impactLevel={}, evidenceKeys={}",
                    hit.getRuleCode(),
                    hit.getImpactLevel(),
                    hit.getEvidence() == null ? List.of() : hit.getEvidence().keySet()));
        }
        return result;
    }

    private List<RuleHit> evaluateConfiguredRules(ChartSnapshot chartSnapshot, List<RuleHit> baseHits) {
        if (ruleDefinitionConfigLoader == null || ruleMatcher == null) {
            return List.of();
        }
        RuleEvaluationContext context = RuleEvaluationContext.from(chartSnapshot, baseHits);
        return ruleDefinitionConfigLoader.getEnabledRules().stream()
                .filter(rule -> ruleMatcher.matches(rule.getCondition(), context))
                .map(rule -> toConfiguredHit(rule, context))
                .toList();
    }

    private RuleHit toConfiguredHit(RuleDefinition rule, RuleEvaluationContext context) {
        RuleHit hit = new RuleHit();
        hit.setRuleId(rule.getId());
        hit.setRuleCode(rule.getRuleCode() == null || rule.getRuleCode().isBlank() ? rule.getId() : rule.getRuleCode());
        hit.setRuleName(rule.getName());
        hit.setCategory(rule.getCategory());
        hit.setPriority(rule.getPriority());
        hit.setHit(true);
        hit.setImpactLevel(resolveImpactLevel(rule.getEffect() == null ? null : rule.getEffect().getScore()));
        hit.setScoreDelta(rule.getEffect() == null ? 0 : rule.getEffect().getScore());
        hit.setTags(rule.getEffect() == null || rule.getEffect().getTags() == null ? List.of() : rule.getEffect().getTags());
        hit.setHitReason(resolveConfiguredHitReason(rule));
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("questionType", context.getQuestionType() == null ? "" : context.getQuestionType());
        evidence.put("useGod", context.getUseGod() == null ? "" : context.getUseGod());
        evidence.put("useGodLineIndex", context.getUseGodLineIndex());
        evidence.put("yongshenState", context.getYongshenState() == null ? "" : context.getYongshenState());
        evidence.put("useGodToShiRelation", context.getUseGodToShiRelation() == null ? "" : context.getUseGodToShiRelation());
        evidence.put("useGodHeShi", Boolean.TRUE.equals(context.getUseGodHeShi()));
        evidence.put("useGodRetreat", Boolean.TRUE.equals(context.getUseGodRetreat()));
        evidence.put("shiState", context.getShiState() == null ? "" : context.getShiState());
        evidence.put("shiYingRelation", context.getShiYingRelation() == null ? "" : context.getShiYingRelation());
        evidence.put("shiMoving", Boolean.TRUE.equals(context.getShiMoving()));
        evidence.put("shiEmpty", Boolean.TRUE.equals(context.getShiEmpty()));
        evidence.put("movingCount", context.getMovingCount());
        evidence.put("kongWangBranches", context.getKongWangBranches() == null ? List.of() : context.getKongWangBranches());
        evidence.put("ruleVersion", ruleDefinitionConfigLoader.getVersion());
        hit.setEvidence(evidence);
        return hit;
    }

    private String resolveConfiguredHitReason(RuleDefinition rule) {
        if (rule.getEffect() != null
                && rule.getEffect().getConclusionHints() != null
                && !rule.getEffect().getConclusionHints().isEmpty()) {
            return rule.getEffect().getConclusionHints().get(0);
        }
        return rule.getDescription() == null ? "命中配置规则。" : rule.getDescription();
    }

    private String resolveImpactLevel(Integer scoreDelta) {
        if (scoreDelta == null) {
            return "LOW";
        }
        if (Math.abs(scoreDelta) >= 3) {
            return "HIGH";
        }
        if (Math.abs(scoreDelta) >= 2) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private RuleHit enrichHit(Rule rule, RuleHit hit) {
        if (hit == null) {
            return null;
        }
        RuleDefinition definition = hit.getRuleCode() == null || ruleDefinitionConfigLoader == null
                ? null
                : ruleDefinitionConfigLoader.getRuleByCode(hit.getRuleCode());
        if ((hit.getRuleId() == null || hit.getRuleId().isBlank()) && definition != null) {
            hit.setRuleId(definition.getId());
        }
        if (hit.getRuleId() == null || hit.getRuleId().isBlank()) {
            hit.setRuleId(hit.getRuleCode());
        }
        if ((hit.getRuleName() == null || hit.getRuleName().isBlank()) && definition != null && definition.getName() != null) {
            hit.setRuleName(definition.getName());
        }
        if (hit.getPriority() == null) {
            hit.setPriority(resolvePriority(rule, definition));
        }
        if (hit.getCategory() == null || hit.getCategory().isBlank()) {
            hit.setCategory(resolveCategory(hit.getRuleCode(), definition));
        }
        if (hit.getScoreDelta() == null) {
            hit.setScoreDelta(resolveScoreDelta(hit, definition));
        }
        if (hit.getTags() == null || hit.getTags().isEmpty()) {
            hit.setTags(resolveTags(hit, definition));
        }
        return hit;
    }

    private Integer resolvePriority(Rule rule, RuleDefinition definition) {
        if (definition != null && definition.getPriority() != null) {
            return definition.getPriority();
        }
        Order order = AnnotationUtils.findAnnotation(rule.getClass(), Order.class);
        return order == null ? 1000 : order.value();
    }

    private String resolveCategory(String ruleCode, RuleDefinition definition) {
        if (definition != null && definition.getCategory() != null && !definition.getCategory().isBlank()) {
            return definition.getCategory();
        }
        if (ruleCode == null || ruleCode.isBlank()) {
            return "GENERAL";
        }
        return switch (ruleCode) {
            case "USE_GOD_SELECTION", "USE_GOD_STRENGTH", "USE_GOD_EMPTY", "USE_GOD_MONTH_BREAK", "USE_GOD_DAY_BREAK" -> "YONGSHEN_STATE";
            case "SHI_YING_EXISTS", "SHI_YING_RELATION" -> "SHI_YING";
            case "MOVING_LINE_EXISTS", "MOVING_LINE_AFFECT_USE_GOD" -> "MOVING_CHANGE";
            default -> "GENERAL";
        };
    }

    private Integer resolveScoreDelta(RuleHit hit, RuleDefinition definition) {
        if (usesStaticScore(hit.getRuleCode())
                && definition != null
                && definition.getEffect() != null
                && definition.getEffect().getScore() != null) {
            return definition.getEffect().getScore();
        }
        String ruleCode = hit.getRuleCode();
        if (ruleCode == null || ruleCode.isBlank()) {
            return 0;
        }
        return switch (ruleCode) {
            case "USE_GOD_EMPTY", "USE_GOD_MONTH_BREAK", "USE_GOD_DAY_BREAK" -> -2;
            case "USE_GOD_SELECTION", "SHI_YING_EXISTS", "MOVING_LINE_EXISTS" -> 0;
            case "USE_GOD_STRENGTH" -> resolveUseGodStrengthScore(hit);
            case "SHI_YING_RELATION" -> resolveShiYingScore(hit);
            case "MOVING_LINE_AFFECT_USE_GOD" -> resolveMovingEffectScore(hit);
            default -> 0;
        };
    }

    private Integer resolveUseGodStrengthScore(RuleHit hit) {
        Object bestLevel = hit.getEvidence() == null ? null : hit.getEvidence().get("bestLevel");
        if ("STRONG".equals(bestLevel)) {
            return 2;
        }
        if ("WEAK".equals(bestLevel)) {
            return -2;
        }
        return 0;
    }

    private Integer resolveShiYingScore(RuleHit hit) {
        Object relation = hit.getEvidence() == null ? null : hit.getEvidence().get("relation");
        if ("应生世".equals(relation) || "世生应".equals(relation)) {
            return 2;
        }
        if ("应克世".equals(relation) || "世克应".equals(relation)) {
            return -2;
        }
        if ("比和".equals(relation)) {
            return 1;
        }
        return 0;
    }

    private Integer resolveMovingEffectScore(RuleHit hit) {
        if (hit.getEvidence() == null) {
            return 0;
        }
        Object effects = hit.getEvidence().get("effects");
        if (effects instanceof List<?> effectItems) {
            for (Object item : effectItems) {
                if (item instanceof Map<?, ?> effectMap) {
                    Object relation = effectMap.get("relation");
                    Object selfTransform = effectMap.get("selfTransform");
                    if (relation != null && String.valueOf(relation).contains("生")) {
                        return 2;
                    }
                    if (selfTransform != null && String.valueOf(selfTransform).contains("克")) {
                        return -2;
                    }
                }
            }
        }
        return 0;
    }

    private List<String> resolveTags(RuleHit hit, RuleDefinition definition) {
        if (usesStaticTags(hit.getRuleCode())
                && definition != null
                && definition.getEffect() != null
                && definition.getEffect().getTags() != null
                && !definition.getEffect().getTags().isEmpty()) {
            return definition.getEffect().getTags();
        }
        String ruleCode = hit.getRuleCode();
        if (ruleCode == null || ruleCode.isBlank()) {
            return List.of();
        }
        return switch (ruleCode) {
            case "USE_GOD_EMPTY" -> List.of("落空", "虚");
            case "USE_GOD_MONTH_BREAK", "USE_GOD_DAY_BREAK" -> List.of("受制", "不稳");
            case "USE_GOD_STRENGTH" -> resolveUseGodStrengthTags(hit);
            case "SHI_YING_RELATION" -> resolveShiYingTags(hit);
            case "MOVING_LINE_EXISTS" -> List.of("有变化");
            case "MOVING_LINE_AFFECT_USE_GOD" -> List.of("动变影响");
            case "USE_GOD_SELECTION" -> List.of("确定用神");
            default -> List.of();
        };
    }

    private List<String> resolveUseGodStrengthTags(RuleHit hit) {
        Object bestLevel = hit.getEvidence() == null ? null : hit.getEvidence().get("bestLevel");
        if ("STRONG".equals(bestLevel)) {
            return List.of("用神有力");
        }
        if ("WEAK".equals(bestLevel)) {
            return List.of("用神偏弱");
        }
        return List.of("用神中平");
    }

    private List<String> resolveShiYingTags(RuleHit hit) {
        Object relation = hit.getEvidence() == null ? null : hit.getEvidence().get("relation");
        if ("应生世".equals(relation)) {
            return List.of("外部助力");
        }
        if ("世生应".equals(relation)) {
            return List.of("主动争取");
        }
        if ("应克世".equals(relation)) {
            return List.of("外部压制");
        }
        if ("世克应".equals(relation)) {
            return List.of("对立消耗");
        }
        if ("比和".equals(relation)) {
            return List.of("互动平衡");
        }
        return List.of();
    }

    private boolean usesStaticScore(String ruleCode) {
        return switch (ruleCode) {
            case "USE_GOD_SELECTION", "SHI_YING_EXISTS", "MOVING_LINE_EXISTS", "USE_GOD_EMPTY", "USE_GOD_MONTH_BREAK", "USE_GOD_DAY_BREAK" -> true;
            default -> false;
        };
    }

    private boolean usesStaticTags(String ruleCode) {
        return switch (ruleCode) {
            case "USE_GOD_SELECTION", "MOVING_LINE_EXISTS", "USE_GOD_EMPTY", "USE_GOD_MONTH_BREAK", "USE_GOD_DAY_BREAK" -> true;
            default -> false;
        };
    }

    private int resolveStageOrder(String category) {
        if (category == null || category.isBlank()) {
            return 99;
        }
        return switch (category) {
            case "YONGSHEN_STATE" -> 1;
            case "SHI_STATE" -> 2;
            case "SHI_YING", "SHI_YING_STATE" -> 3;
            case "MOVING_CHANGE" -> 4;
            case "EMPTY_STATE" -> 5;
            case "WANG_SHUAI" -> 6;
            case "COMPOSITE" -> 7;
            case "SCENARIO_WEIGHT" -> 8;
            default -> 50;
        };
    }
}
