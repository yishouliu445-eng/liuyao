package com.yishou.liuyao.rule.definition;

import java.util.ArrayList;
import java.util.List;

public class RuleCondition {

    private String target;
    private String operator;
    private Object value;
    private List<RuleCondition> allOf = new ArrayList<>();
    private List<RuleCondition> anyOf = new ArrayList<>();
    private List<RuleCondition> noneOf = new ArrayList<>();

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public List<RuleCondition> getAllOf() {
        return allOf;
    }

    public void setAllOf(List<RuleCondition> allOf) {
        this.allOf = allOf;
    }

    public List<RuleCondition> getAnyOf() {
        return anyOf;
    }

    public void setAnyOf(List<RuleCondition> anyOf) {
        this.anyOf = anyOf;
    }

    public List<RuleCondition> getNoneOf() {
        return noneOf;
    }

    public void setNoneOf(List<RuleCondition> noneOf) {
        this.noneOf = noneOf;
    }
}
