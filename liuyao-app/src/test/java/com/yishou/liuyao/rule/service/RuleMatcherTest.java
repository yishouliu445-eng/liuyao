package com.yishou.liuyao.rule.service;

import com.yishou.liuyao.rule.definition.RuleCondition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleMatcherTest {

    private final RuleMatcher matcher = new RuleMatcher();

    @Test
    void shouldMatchMovingCountGreaterThanCondition() {
        RuleEvaluationContext context = new RuleEvaluationContext();
        context.setMovingCount(4);

        RuleCondition condition = new RuleCondition();
        condition.setTarget("MOVING_COUNT");
        condition.setOperator("GREATER_THAN");
        condition.setValue(3);

        assertTrue(matcher.matches(condition, context));
    }

    @Test
    void shouldMatchAllOfCompositeCondition() {
        RuleEvaluationContext context = new RuleEvaluationContext();
        context.setYongshenState("STRONG");
        context.setShiState("STRONG");

        RuleCondition first = new RuleCondition();
        first.setTarget("YONGSHEN_STATE");
        first.setOperator("EQUALS");
        first.setValue("STRONG");

        RuleCondition second = new RuleCondition();
        second.setTarget("SHI_STATE");
        second.setOperator("EQUALS");
        second.setValue("STRONG");

        RuleCondition root = new RuleCondition();
        root.setAllOf(List.of(first, second));

        assertTrue(matcher.matches(root, context));
    }

    @Test
    void shouldRejectCompositeConditionWhenOneBranchDoesNotMatch() {
        RuleEvaluationContext context = new RuleEvaluationContext();
        context.setYongshenState("WEAK");
        context.setShiState("STRONG");

        RuleCondition first = new RuleCondition();
        first.setTarget("YONGSHEN_STATE");
        first.setOperator("EQUALS");
        first.setValue("STRONG");

        RuleCondition second = new RuleCondition();
        second.setTarget("SHI_STATE");
        second.setOperator("EQUALS");
        second.setValue("STRONG");

        RuleCondition root = new RuleCondition();
        root.setAllOf(List.of(first, second));

        assertFalse(matcher.matches(root, context));
    }

    @Test
    void shouldMatchInAndNotEqualsOperators() {
        RuleEvaluationContext context = new RuleEvaluationContext();
        context.setQuestionType("收入");
        context.setShiState("MEDIUM");

        RuleCondition inCondition = new RuleCondition();
        inCondition.setTarget("QUESTION_TYPE");
        inCondition.setOperator("IN");
        inCondition.setValue(List.of("收入", "工作"));

        RuleCondition notEqualsCondition = new RuleCondition();
        notEqualsCondition.setTarget("SHI_STATE");
        notEqualsCondition.setOperator("NOT_EQUALS");
        notEqualsCondition.setValue("WEAK");

        assertTrue(matcher.matches(inCondition, context));
        assertTrue(matcher.matches(notEqualsCondition, context));
    }

    @Test
    void shouldMatchEmptyAndBooleanOperators() {
        RuleEvaluationContext context = new RuleEvaluationContext();
        context.setUseGod("");
        context.setUseGodFound(false);
        context.setShiMoving(true);

        RuleCondition emptyCondition = new RuleCondition();
        emptyCondition.setTarget("USE_GOD");
        emptyCondition.setOperator("IS_EMPTY");

        RuleCondition falseCondition = new RuleCondition();
        falseCondition.setTarget("USE_GOD_FOUND");
        falseCondition.setOperator("IS_FALSE");

        RuleCondition trueCondition = new RuleCondition();
        trueCondition.setTarget("SHI_MOVING");
        trueCondition.setOperator("IS_TRUE");

        assertTrue(matcher.matches(emptyCondition, context));
        assertTrue(matcher.matches(falseCondition, context));
        assertTrue(matcher.matches(trueCondition, context));
    }

    @Test
    void shouldMatchNotInAndLessThanOperators() {
        RuleEvaluationContext context = new RuleEvaluationContext();
        context.setQuestionType("收入");
        context.setMovingCount(2);

        RuleCondition notInCondition = new RuleCondition();
        notInCondition.setTarget("QUESTION_TYPE");
        notInCondition.setOperator("NOT_IN");
        notInCondition.setValue(List.of("感情", "合作"));

        RuleCondition lessThanCondition = new RuleCondition();
        lessThanCondition.setTarget("MOVING_COUNT");
        lessThanCondition.setOperator("LESS_THAN");
        lessThanCondition.setValue(3);

        assertTrue(matcher.matches(notInCondition, context));
        assertTrue(matcher.matches(lessThanCondition, context));
    }

    @Test
    void shouldMatchHasRelationOperator() {
        RuleEvaluationContext context = new RuleEvaluationContext();
        context.setShiYingRelation("应克世");
        context.setUseGodToShiRelation("生");
        context.setKongWangBranches(List.of("子", "丑"));

        RuleCondition keCondition = new RuleCondition();
        keCondition.setTarget("SHI_YING_RELATION");
        keCondition.setOperator("HAS_RELATION");
        keCondition.setValue("KE");

        RuleCondition shengCondition = new RuleCondition();
        shengCondition.setTarget("USE_GOD_TO_SHI_RELATION");
        shengCondition.setOperator("HAS_RELATION");
        shengCondition.setValue("SHENG");

        RuleCondition inKongWangCondition = new RuleCondition();
        inKongWangCondition.setTarget("KONG_WANG");
        inKongWangCondition.setOperator("HAS_RELATION");
        inKongWangCondition.setValue("子");

        assertTrue(matcher.matches(keCondition, context));
        assertTrue(matcher.matches(shengCondition, context));
        assertTrue(matcher.matches(inKongWangCondition, context));
    }

    @Test
    void shouldMatchExtendedUseGodTargets() {
        RuleEvaluationContext context = new RuleEvaluationContext();
        context.setUseGodMoving(true);
        context.setUseGodEmpty(false);
        context.setUseGodLineCount(2);
        context.setUseGodBestScore(8);
        context.setUseGodDistanceToShi(1);

        RuleCondition movingCondition = new RuleCondition();
        movingCondition.setTarget("USE_GOD_MOVING");
        movingCondition.setOperator("IS_TRUE");

        RuleCondition lineCountCondition = new RuleCondition();
        lineCountCondition.setTarget("USE_GOD_LINE_COUNT");
        lineCountCondition.setOperator("GREATER_THAN_OR_EQUALS");
        lineCountCondition.setValue(2);

        RuleCondition scoreCondition = new RuleCondition();
        scoreCondition.setTarget("USE_GOD_BEST_SCORE");
        scoreCondition.setOperator("GREATER_THAN");
        scoreCondition.setValue(5);

        RuleCondition distanceCondition = new RuleCondition();
        distanceCondition.setTarget("USE_GOD_DISTANCE_TO_SHI");
        distanceCondition.setOperator("LESS_THAN_OR_EQUALS");
        distanceCondition.setValue(1);

        assertTrue(matcher.matches(movingCondition, context));
        assertTrue(matcher.matches(lineCountCondition, context));
        assertTrue(matcher.matches(scoreCondition, context));
        assertTrue(matcher.matches(distanceCondition, context));
    }

    @Test
    void shouldMatchContainsOperator() {
        RuleEvaluationContext context = new RuleEvaluationContext();
        context.setUseGod("应爻");

        RuleCondition containsCondition = new RuleCondition();
        containsCondition.setTarget("USE_GOD");
        containsCondition.setOperator("CONTAINS");
        containsCondition.setValue("应");

        assertTrue(matcher.matches(containsCondition, context));
    }

    @Test
    void shouldMatchBreakAndShiYingStructureTargets() {
        RuleEvaluationContext context = new RuleEvaluationContext();
        context.setUseGodMonthBreak(true);
        context.setUseGodDayBreak(true);
        context.setShiYingExists(true);
        context.setShiYingDistance(3);

        RuleCondition monthBreakCondition = new RuleCondition();
        monthBreakCondition.setTarget("USE_GOD_MONTH_BREAK");
        monthBreakCondition.setOperator("IS_TRUE");

        RuleCondition dayBreakCondition = new RuleCondition();
        dayBreakCondition.setTarget("USE_GOD_DAY_BREAK");
        dayBreakCondition.setOperator("IS_TRUE");

        RuleCondition shiYingExistsCondition = new RuleCondition();
        shiYingExistsCondition.setTarget("SHI_YING_EXISTS");
        shiYingExistsCondition.setOperator("IS_TRUE");

        RuleCondition shiYingDistanceCondition = new RuleCondition();
        shiYingDistanceCondition.setTarget("SHI_YING_DISTANCE");
        shiYingDistanceCondition.setOperator("GREATER_THAN_OR_EQUALS");
        shiYingDistanceCondition.setValue(3);

        assertTrue(matcher.matches(monthBreakCondition, context));
        assertTrue(matcher.matches(dayBreakCondition, context));
        assertTrue(matcher.matches(shiYingExistsCondition, context));
        assertTrue(matcher.matches(shiYingDistanceCondition, context));
    }

    @Test
    void shouldMatchMovingAffectTargets() {
        RuleEvaluationContext context = new RuleEvaluationContext();
        context.setHasMovingShengUseGod(true);
        context.setHasChangedKeUseGod(true);
        context.setHasMovingChongUseGod(true);
        context.setHasMovingChongShi(true);

        RuleCondition movingShengUseGod = new RuleCondition();
        movingShengUseGod.setTarget("HAS_MOVING_SHENG_USE_GOD");
        movingShengUseGod.setOperator("IS_TRUE");

        RuleCondition changedKeUseGod = new RuleCondition();
        changedKeUseGod.setTarget("HAS_CHANGED_KE_USE_GOD");
        changedKeUseGod.setOperator("IS_TRUE");

        RuleCondition movingChongUseGod = new RuleCondition();
        movingChongUseGod.setTarget("HAS_MOVING_CHONG_USE_GOD");
        movingChongUseGod.setOperator("IS_TRUE");

        RuleCondition movingChongShi = new RuleCondition();
        movingChongShi.setTarget("HAS_MOVING_CHONG_SHI");
        movingChongShi.setOperator("IS_TRUE");

        assertTrue(matcher.matches(movingShengUseGod, context));
        assertTrue(matcher.matches(changedKeUseGod, context));
        assertTrue(matcher.matches(movingChongUseGod, context));
        assertTrue(matcher.matches(movingChongShi, context));
    }

    @Test
    void shouldMatchUseGodRuMuTarget() {
        RuleEvaluationContext context = new RuleEvaluationContext();
        context.setUseGodRuMu(true);

        RuleCondition ruMuCondition = new RuleCondition();
        ruMuCondition.setTarget("USE_GOD_RU_MU");
        ruMuCondition.setOperator("IS_TRUE");

        assertTrue(matcher.matches(ruMuCondition, context));
    }

    @Test
    void shouldMatchUseGodChongDetailTargets() {
        RuleEvaluationContext context = new RuleEvaluationContext();
        context.setUseGodChongKai(true);
        context.setUseGodChongSan(true);

        RuleCondition chongKaiCondition = new RuleCondition();
        chongKaiCondition.setTarget("USE_GOD_CHONG_KAI");
        chongKaiCondition.setOperator("IS_TRUE");

        RuleCondition chongSanCondition = new RuleCondition();
        chongSanCondition.setTarget("USE_GOD_CHONG_SAN");
        chongSanCondition.setOperator("IS_TRUE");

        assertTrue(matcher.matches(chongKaiCondition, context));
        assertTrue(matcher.matches(chongSanCondition, context));
    }
}
