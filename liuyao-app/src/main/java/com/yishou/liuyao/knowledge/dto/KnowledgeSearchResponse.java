package com.yishou.liuyao.knowledge.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class KnowledgeSearchResponse {

    private List<String> topics = new ArrayList<>();
    private Map<String, String> moduleResponsibilities = new LinkedHashMap<>();

    public List<String> getTopics() {
        return topics;
    }

    public void setTopics(List<String> topics) {
        this.topics = topics;
    }

    public Map<String, String> getModuleResponsibilities() {
        return moduleResponsibilities;
    }

    public void setModuleResponsibilities(Map<String, String> moduleResponsibilities) {
        this.moduleResponsibilities = moduleResponsibilities;
    }
}
