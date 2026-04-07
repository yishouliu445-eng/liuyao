package com.yishou.liuyao.rule.domain;

import com.yishou.liuyao.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "rule_definition")
public class RuleDefinitionEntity extends BaseEntity {

    @Column(name = "rule_id", nullable = false)
    private String ruleId;

    @Column(name = "rule_code", nullable = false)
    private String ruleCode;

    @Column(nullable = false)
    private String name;

    private String category;

    private Integer priority;

    @Column(nullable = false)
    private Boolean enabled;

    private String version;

    @Column(name = "condition_json", columnDefinition = "TEXT")
    private String conditionJson;

    @Column(name = "effect_json", columnDefinition = "TEXT")
    private String effectJson;

    @Column(columnDefinition = "TEXT")
    private String description;

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getConditionJson() {
        return conditionJson;
    }

    public void setConditionJson(String conditionJson) {
        this.conditionJson = conditionJson;
    }

    public String getEffectJson() {
        return effectJson;
    }

    public void setEffectJson(String effectJson) {
        this.effectJson = effectJson;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
