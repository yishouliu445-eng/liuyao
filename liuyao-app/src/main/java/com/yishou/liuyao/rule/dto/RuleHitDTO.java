package com.yishou.liuyao.rule.dto;

import java.util.Map;

public class RuleHitDTO {

    private String ruleCode;
    private String ruleName;
    private String hitReason;
    private String impactLevel;
    private Map<String, Object> evidence;

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getHitReason() {
        return hitReason;
    }

    public void setHitReason(String hitReason) {
        this.hitReason = hitReason;
    }

    public String getImpactLevel() {
        return impactLevel;
    }

    public void setImpactLevel(String impactLevel) {
        this.impactLevel = impactLevel;
    }

    public Map<String, Object> getEvidence() {
        return evidence;
    }

    public void setEvidence(Map<String, Object> evidence) {
        this.evidence = evidence;
    }
}
