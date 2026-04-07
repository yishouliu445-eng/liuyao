package com.yishou.liuyao.analysis.service;

import com.yishou.liuyao.analysis.dto.AnalysisContextDTO;
import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.rule.RuleHit;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnalysisService {

    public String analyze(String question, ChartSnapshot chartSnapshot, List<RuleHit> ruleHits) {
        AnalysisContextDTO context = buildContext(question, chartSnapshot, ruleHits);
        return analyze(context);
    }

    public String analyze(AnalysisContextDTO context) {
        if (context == null) {
            return "分析模块骨架已就绪，后续可接入受约束的 LLM 编排。";
        }
        String useGod = context.getUseGod() == null || context.getUseGod().isBlank() ? "未定用神" : context.getUseGod();
        String mainHexagram = context.getMainHexagram() == null || context.getMainHexagram().isBlank()
                ? "未知本卦"
                : context.getMainHexagram();
        int ruleCount = context.getRuleCount() == null ? 0 : context.getRuleCount();
        String summary = String.format("当前为骨架版分析结果：已基于%s、用神%s和%d条规则命中生成结构化上下文，后续可接入受约束的 LLM 编排。",
                mainHexagram,
                useGod,
                ruleCount);
        String knowledgeHint = firstKnowledgeHint(context.getKnowledgeSnippets());
        if (knowledgeHint == null) {
            return summary;
        }
        return summary + " 可参考资料：" + knowledgeHint;
    }

    private AnalysisContextDTO buildContext(String question, ChartSnapshot chartSnapshot, List<RuleHit> ruleHits) {
        AnalysisContextDTO context = new AnalysisContextDTO();
        context.setContextVersion("v1");
        context.setQuestion(question);
        if (chartSnapshot != null) {
            context.setQuestionCategory(chartSnapshot.getQuestionCategory());
            context.setUseGod(chartSnapshot.getUseGod());
            context.setMainHexagram(chartSnapshot.getMainHexagram());
            context.setChangedHexagram(chartSnapshot.getChangedHexagram());
        }
        context.setRuleCount(ruleHits == null ? 0 : ruleHits.size());
        context.setRuleCodes(ruleHits == null ? List.of() : ruleHits.stream().map(RuleHit::getRuleCode).toList());
        return context;
    }

    private String firstKnowledgeHint(List<String> knowledgeSnippets) {
        if (knowledgeSnippets == null || knowledgeSnippets.isEmpty()) {
            return null;
        }
        String first = knowledgeSnippets.get(0);
        if (first == null || first.isBlank()) {
            return null;
        }
        String normalized = first.replace('\n', ' ').trim();
        if (normalized.length() <= 48) {
            return normalized;
        }
        return normalized.substring(0, 48) + "...";
    }
}
