package com.yishou.liuyao.rule.service;

import com.yishou.liuyao.rule.definition.RuleDefinition;

import java.util.ArrayList;
import java.util.List;

public class RuleDefinitionConfig {

    private String version = "unknown";
    private List<String> files = new ArrayList<>();
    private List<RuleDefinition> rules = new ArrayList<>();

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<RuleDefinition> getRules() {
        return rules;
    }

    public void setRules(List<RuleDefinition> rules) {
        this.rules = rules;
    }

    public List<String> getFiles() {
        return files;
    }

    public void setFiles(List<String> files) {
        this.files = files;
    }
}
