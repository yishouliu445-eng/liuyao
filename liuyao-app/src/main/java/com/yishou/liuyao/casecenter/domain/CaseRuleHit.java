package com.yishou.liuyao.casecenter.domain;

import com.yishou.liuyao.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "case_rule_hit")
public class CaseRuleHit extends BaseEntity {

    @Column(name = "case_id")
    private Long caseId;

    @Column(name = "rule_code")
    private String ruleCode;

    @Column(name = "rule_name")
    private String ruleName;

    @Column(name = "hit_reason")
    private String hitReason;

    @Column(name = "impact_level")
    private String impactLevel;

    @Column(name = "evidence_json")
    private String evidenceJson;

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

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

    public String getEvidenceJson() {
        return evidenceJson;
    }

    public void setEvidenceJson(String evidenceJson) {
        this.evidenceJson = evidenceJson;
    }
}
