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
}
