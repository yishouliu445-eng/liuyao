package com.yishou.liuyao.rule.definition;

public class RuleDefinition {

    private String id;
    private String ruleCode;
    private String name;
    private String category;
    private Integer priority;
    private Boolean enabled;
    private String version;
    private RuleCondition condition;
    private RuleEffect effect;
    private String description;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
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

    public RuleCondition getCondition() {
        return condition;
    }

    public void setCondition(RuleCondition condition) {
        this.condition = condition;
    }

    public RuleEffect getEffect() {
        return effect;
    }

    public void setEffect(RuleEffect effect) {
        this.effect = effect;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
