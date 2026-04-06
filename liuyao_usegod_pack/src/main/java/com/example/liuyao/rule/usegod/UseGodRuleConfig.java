package com.example.liuyao.rule.usegod;

import java.util.List;

public class UseGodRuleConfig {

    private String version;
    private List<UseGodRuleItem> rules;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<UseGodRuleItem> getRules() {
        return rules;
    }

    public void setRules(List<UseGodRuleItem> rules) {
        this.rules = rules;
    }
}
