package com.yishou.liuyao.analysis.dto;

import com.yishou.liuyao.divination.dto.ChartSnapshotDTO;
import com.yishou.liuyao.rule.dto.RuleHitDTO;

import java.util.ArrayList;
import java.util.List;

public class AnalysisContextDTO {

    // 后续分析模块统一从这个结构取上下文，避免直接绑定领域对象和规则实体。
    // 这层 DTO 既服务当前 stub，也服务后续接入知识检索和真正分析服务。
    private String contextVersion;
    private String question;
    private String questionCategory;
    private String useGod;
    private String mainHexagram;
    private String changedHexagram;
    private ChartSnapshotDTO chartSnapshot;
    private Integer ruleCount;
    private List<String> ruleCodes = new ArrayList<>();
    private List<String> knowledgeSnippets = new ArrayList<>();
    private List<RuleHitDTO> ruleHits = new ArrayList<>();
    private StructuredAnalysisResultDTO structuredResult;

    public String getContextVersion() {
        return contextVersion;
    }

    public void setContextVersion(String contextVersion) {
        this.contextVersion = contextVersion;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getQuestionCategory() {
        return questionCategory;
    }

    public void setQuestionCategory(String questionCategory) {
        this.questionCategory = questionCategory;
    }

    public String getUseGod() {
        return useGod;
    }

    public void setUseGod(String useGod) {
        this.useGod = useGod;
    }

    public String getMainHexagram() {
        return mainHexagram;
    }

    public void setMainHexagram(String mainHexagram) {
        this.mainHexagram = mainHexagram;
    }

    public String getChangedHexagram() {
        return changedHexagram;
    }

    public void setChangedHexagram(String changedHexagram) {
        this.changedHexagram = changedHexagram;
    }

    public ChartSnapshotDTO getChartSnapshot() {
        return chartSnapshot;
    }

    public void setChartSnapshot(ChartSnapshotDTO chartSnapshot) {
        this.chartSnapshot = chartSnapshot;
    }

    public Integer getRuleCount() {
        return ruleCount;
    }

    public void setRuleCount(Integer ruleCount) {
        this.ruleCount = ruleCount;
    }

    public List<String> getRuleCodes() {
        return ruleCodes;
    }

    public void setRuleCodes(List<String> ruleCodes) {
        this.ruleCodes = ruleCodes;
    }

    public List<String> getKnowledgeSnippets() {
        return knowledgeSnippets;
    }

    public void setKnowledgeSnippets(List<String> knowledgeSnippets) {
        this.knowledgeSnippets = knowledgeSnippets;
    }

    public List<RuleHitDTO> getRuleHits() {
        return ruleHits;
    }

    public void setRuleHits(List<RuleHitDTO> ruleHits) {
        this.ruleHits = ruleHits;
    }

    public StructuredAnalysisResultDTO getStructuredResult() {
        return structuredResult;
    }

    public void setStructuredResult(StructuredAnalysisResultDTO structuredResult) {
        this.structuredResult = structuredResult;
    }
}
