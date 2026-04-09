package com.yishou.liuyao.rule.service;

import com.yishou.liuyao.rule.definition.RuleCondition;
import org.springframework.stereotype.Component;

@Component
public class RuleMatcher {

    public boolean matches(RuleCondition condition, RuleEvaluationContext context) {
        if (condition == null) {
            return false;
        }
        if (condition.getAllOf() != null && !condition.getAllOf().isEmpty()) {
            return condition.getAllOf().stream().allMatch(item -> matches(item, context));
        }
        if (condition.getAnyOf() != null && !condition.getAnyOf().isEmpty()) {
            return condition.getAnyOf().stream().anyMatch(item -> matches(item, context));
        }
        if (condition.getNoneOf() != null && !condition.getNoneOf().isEmpty()) {
            return condition.getNoneOf().stream().noneMatch(item -> matches(item, context));
        }
        return matchLeaf(condition, context);
    }

    private boolean matchLeaf(RuleCondition condition, RuleEvaluationContext context) {
        Object actual = readValue(condition.getTarget(), context);
        String operator = condition.getOperator();
        Object expected = condition.getValue();
        if (operator == null || operator.isBlank()) {
            return false;
        }
        return switch (operator) {
            case "EQUALS" -> actual != null && String.valueOf(actual).equals(String.valueOf(expected));
            case "NOT_EQUALS" -> actual == null ? expected != null : !String.valueOf(actual).equals(String.valueOf(expected));
            case "IN" -> inValues(actual, expected);
            case "NOT_IN" -> !inValues(actual, expected);
            case "GREATER_THAN" -> toInteger(actual) != null && toInteger(expected) != null && toInteger(actual) > toInteger(expected);
            case "GREATER_THAN_OR_EQUALS" -> toInteger(actual) != null && toInteger(expected) != null && toInteger(actual) >= toInteger(expected);
            case "LESS_THAN" -> toInteger(actual) != null && toInteger(expected) != null && toInteger(actual) < toInteger(expected);
            case "LESS_THAN_OR_EQUALS" -> toInteger(actual) != null && toInteger(expected) != null && toInteger(actual) <= toInteger(expected);
            case "CONTAINS" -> contains(actual, expected);
            case "HAS_RELATION" -> hasRelation(actual, expected);
            case "IS_EMPTY" -> isEmpty(actual);
            case "NOT_EMPTY" -> !isEmpty(actual);
            case "IS_TRUE" -> Boolean.TRUE.equals(toBoolean(actual));
            case "IS_FALSE" -> Boolean.FALSE.equals(toBoolean(actual));
            default -> false;
        };
    }

    private Object readValue(String target, RuleEvaluationContext context) {
        if (target == null || context == null) {
            return null;
        }
        return switch (target) {
            case "QUESTION_TYPE" -> context.getQuestionType();
            case "USE_GOD" -> context.getUseGod();
            case "USE_GOD_FOUND" -> context.getUseGodFound();
            case "USE_GOD_LINE_INDEX" -> context.getUseGodLineIndex();
            case "USE_GOD_MOVING" -> context.getUseGodMoving();
            case "USE_GOD_LINE_COUNT" -> context.getUseGodLineCount();
            case "USE_GOD_EMPTY" -> context.getUseGodEmpty();
            case "USE_GOD_MONTH_BREAK" -> context.getUseGodMonthBreak();
            case "USE_GOD_DAY_BREAK" -> context.getUseGodDayBreak();
            case "USE_GOD_RU_MU" -> context.getUseGodRuMu();
            case "USE_GOD_CHONG_KAI" -> context.getUseGodChongKai();
            case "USE_GOD_CHONG_SAN" -> context.getUseGodChongSan();
            case "HAS_MOVING_SHENG_USE_GOD" -> context.getHasMovingShengUseGod();
            case "HAS_MOVING_KE_USE_GOD" -> context.getHasMovingKeUseGod();
            case "HAS_CHANGED_SHENG_USE_GOD" -> context.getHasChangedShengUseGod();
            case "HAS_CHANGED_KE_USE_GOD" -> context.getHasChangedKeUseGod();
            case "HAS_MOVING_CHONG_USE_GOD" -> context.getHasMovingChongUseGod();
            case "HAS_MOVING_CHONG_SHI" -> context.getHasMovingChongShi();
            case "USE_GOD_BEST_SCORE" -> context.getUseGodBestScore();
            case "USE_GOD_DISTANCE_TO_SHI" -> context.getUseGodDistanceToShi();
            case "YONGSHEN_STATE" -> context.getYongshenState();
            case "USE_GOD_TO_SHI_RELATION" -> context.getUseGodToShiRelation();
            case "USE_GOD_HE_SHI" -> context.getUseGodHeShi();
            case "USE_GOD_RETREAT" -> context.getUseGodRetreat();
            case "SHI_STATE" -> context.getShiState();
            case "SHI_MOVING" -> context.getShiMoving();
            case "SHI_EMPTY" -> context.getShiEmpty();
            case "SHI_YING_EXISTS" -> context.getShiYingExists();
            case "SHI_YING_DISTANCE" -> context.getShiYingDistance();
            case "SHI_YING_RELATION" -> context.getShiYingRelation();
            case "MOVING_COUNT" -> context.getMovingCount();
            case "KONG_WANG" -> context.getKongWangBranches();
            default -> null;
        };
    }

    private boolean contains(Object actual, Object expected) {
        if (actual == null || expected == null) {
            return false;
        }
        if (actual instanceof Iterable<?> values) {
            for (Object item : values) {
                if (item != null && String.valueOf(item).contains(String.valueOf(expected))) {
                    return true;
                }
            }
            return false;
        }
        return String.valueOf(actual).contains(String.valueOf(expected));
    }

    private boolean inValues(Object actual, Object expected) {
        if (expected instanceof Iterable<?> values) {
            for (Object item : values) {
                if (actual != null && String.valueOf(actual).equals(String.valueOf(item))) {
                    return true;
                }
            }
            return false;
        }
        return actual != null && expected != null && String.valueOf(actual).equals(String.valueOf(expected));
    }

    private boolean isEmpty(Object actual) {
        if (actual == null) {
            return true;
        }
        if (actual instanceof String text) {
            return text.isBlank();
        }
        if (actual instanceof Iterable<?> values) {
            return !values.iterator().hasNext();
        }
        return false;
    }

    private boolean hasRelation(Object actual, Object expected) {
        if (actual == null || expected == null) {
            return false;
        }
        String expectedText = String.valueOf(expected).trim().toUpperCase();
        if (expectedText.isEmpty()) {
            return false;
        }
        if (actual instanceof Iterable<?> values) {
            for (Object item : values) {
                if (relationMatches(item, expectedText)) {
                    return true;
                }
            }
            return false;
        }
        return relationMatches(actual, expectedText);
    }

    private boolean relationMatches(Object actual, String expectedText) {
        String actualText = String.valueOf(actual).trim();
        if (actualText.isEmpty()) {
            return false;
        }
        return switch (expectedText) {
            case "SHENG" -> actualText.contains("生");
            case "KE" -> actualText.contains("克");
            case "HE" -> actualText.contains("合") || "比和".equals(actualText) || "同五行".equals(actualText);
            case "CHONG" -> actualText.contains("冲");
            case "EMPTY", "KONG_WANG" -> actualText.contains("空") || actualText.contains("亡");
            default -> actualText.equalsIgnoreCase(expectedText);
        };
    }

    private Boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text && !text.isBlank()) {
            return Boolean.parseBoolean(text);
        }
        return null;
    }

    private Integer toInteger(Object value) {
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Integer.parseInt(text);
        }
        return null;
    }
}
