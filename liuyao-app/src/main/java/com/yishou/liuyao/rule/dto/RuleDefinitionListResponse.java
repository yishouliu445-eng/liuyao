package com.yishou.liuyao.rule.dto;

import java.util.ArrayList;
import java.util.List;

public class RuleDefinitionListResponse {

    private String version;
    private Integer total;
    private List<RuleDefinitionDTO> rules = new ArrayList<>();

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public List<RuleDefinitionDTO> getRules() {
        return rules;
    }

    public void setRules(List<RuleDefinitionDTO> rules) {
        this.rules = rules;
    }
}
