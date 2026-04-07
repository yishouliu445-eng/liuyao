package com.yishou.liuyao.rule.definition;

import java.util.ArrayList;
import java.util.List;

public class RuleEffect {

    private Integer score;
    private List<String> tags = new ArrayList<>();
    private List<String> conclusionHints = new ArrayList<>();

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getConclusionHints() {
        return conclusionHints;
    }

    public void setConclusionHints(List<String> conclusionHints) {
        this.conclusionHints = conclusionHints;
    }
}
