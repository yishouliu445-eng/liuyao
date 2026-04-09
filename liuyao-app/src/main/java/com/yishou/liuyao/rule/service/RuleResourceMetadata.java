package com.yishou.liuyao.rule.service;

public class RuleResourceMetadata {

    private String bundleVersion = "unknown";
    private String ruleDefinitionsVersion = "unknown";
    private String useGodRulesVersion = "unknown";
    private String sourceOfTruth = "";

    public String getBundleVersion() {
        return bundleVersion;
    }

    public void setBundleVersion(String bundleVersion) {
        this.bundleVersion = bundleVersion;
    }

    public String getRuleDefinitionsVersion() {
        return ruleDefinitionsVersion;
    }

    public void setRuleDefinitionsVersion(String ruleDefinitionsVersion) {
        this.ruleDefinitionsVersion = ruleDefinitionsVersion;
    }

    public String getUseGodRulesVersion() {
        return useGodRulesVersion;
    }

    public void setUseGodRulesVersion(String useGodRulesVersion) {
        this.useGodRulesVersion = useGodRulesVersion;
    }

    public String getSourceOfTruth() {
        return sourceOfTruth;
    }

    public void setSourceOfTruth(String sourceOfTruth) {
        this.sourceOfTruth = sourceOfTruth;
    }
}
