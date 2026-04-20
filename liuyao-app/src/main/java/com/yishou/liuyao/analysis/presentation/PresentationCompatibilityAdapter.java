package com.yishou.liuyao.analysis.presentation;

import com.yishou.liuyao.analysis.dto.AnalysisContextDTO;
import com.yishou.liuyao.analysis.dto.AnalysisOutputDTO;
import com.yishou.liuyao.analysis.dto.StructuredAnalysisResultDTO;
import com.yishou.liuyao.analysis.service.AnalysisPhaseTwoSignalFormatter;
import com.yishou.liuyao.divination.dto.ChartSnapshotDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PresentationCompatibilityAdapter {

    private final AnalysisPhaseTwoSignalFormatter phaseTwoSignalFormatter;

    public PresentationCompatibilityAdapter(AnalysisPhaseTwoSignalFormatter phaseTwoSignalFormatter) {
        this.phaseTwoSignalFormatter = phaseTwoSignalFormatter;
    }

    public String render(AnalysisContextDTO analysisContext,
                         StructuredAnalysisResultDTO structuredResult,
                         AnalysisOutputDTO analysisOutput) {
        List<String> lines = new ArrayList<>();
        ChartSnapshotDTO chartSnapshot = analysisContext == null ? null : analysisContext.getChartSnapshot();
        String questionCategory = firstNonBlank(
                analysisContext == null ? null : analysisContext.getQuestionCategory(),
                chartSnapshot != null
                        ? chartSnapshot.getQuestionCategory()
                        : null
        );
        String mainHexagram = firstNonBlank(
                analysisContext == null ? null : analysisContext.getMainHexagram(),
                chartSnapshot != null
                        ? chartSnapshot.getMainHexagram()
                        : null
        );
        String changedHexagram = firstNonBlank(
                analysisContext == null ? null : analysisContext.getChangedHexagram(),
                chartSnapshot != null
                        ? chartSnapshot.getChangedHexagram()
                        : null
        );
        String useGod = analysisContext == null ? null : analysisContext.getUseGod();

        lines.add("卦象概览");
        if (questionCategory != null) {
            lines.add("问" + questionCategory + "，主卦为" + defaultValue(mainHexagram) + "，变卦为" + defaultValue(changedHexagram) + "。");
        }
        appendIfPresent(lines, renderDerivedHexagramLine(chartSnapshot));
        appendIfPresent(lines, analysisOutput == null || analysisOutput.getAnalysis() == null
                ? null
                : analysisOutput.getAnalysis().getHexagramOverview());

        lines.add("用神判断");
        if (useGod != null && !useGod.isBlank()) {
            lines.add("以" + useGod + "为用神。");
        }
        appendIfPresent(lines, analysisOutput == null || analysisOutput.getAnalysis() == null
                ? null
                : analysisOutput.getAnalysis().getUseGodAnalysis());

        lines.add("综合推演");
        if (structuredResult != null && structuredResult.getEffectiveScore() != null) {
            lines.add("有效评分：" + structuredResult.getEffectiveScore()
                    + "（" + defaultValue(structuredResult.getEffectiveResultLevel()) + "）");
        }
        appendIfPresent(lines, renderPhaseTwoLine(analysisContext));
        appendIfPresent(lines, analysisOutput == null || analysisOutput.getAnalysis() == null
                ? null
                : analysisOutput.getAnalysis().getDetailedReasoning());

        lines.add("综合结论");
        appendIfPresent(lines, analysisOutput == null || analysisOutput.getAnalysis() == null
                ? null
                : analysisOutput.getAnalysis().getConclusion());

        if (analysisOutput != null
                && analysisOutput.getAnalysis() != null
                && analysisOutput.getAnalysis().getActionPlan() != null
                && !analysisOutput.getAnalysis().getActionPlan().isEmpty()) {
            lines.add("行动建议");
            for (String action : analysisOutput.getAnalysis().getActionPlan()) {
                appendIfPresent(lines, "- " + action);
            }
        }

        if (analysisContext != null
                && analysisContext.getKnowledgeSnippets() != null
                && !analysisContext.getKnowledgeSnippets().isEmpty()) {
            lines.add("参考摘录");
            analysisContext.getKnowledgeSnippets().stream()
                    .limit(3)
                    .forEach(snippet -> appendIfPresent(lines, "- " + snippet));
        }

        return lines.stream()
                .filter(line -> line != null && !line.isBlank())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }

    private void appendIfPresent(List<String> lines, String content) {
        if (content != null && !content.isBlank()) {
            lines.add(content);
        }
    }

    private String defaultValue(String value) {
        return value == null ? "" : value;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }

    private String renderDerivedHexagramLine(ChartSnapshotDTO chartSnapshot) {
        String derivedHexagrams = phaseTwoSignalFormatter.renderDerivedHexagrams(chartSnapshot);
        if (derivedHexagrams.isBlank()) {
            return null;
        }
        return derivedHexagrams + "。";
    }

    private String renderPhaseTwoLine(AnalysisContextDTO analysisContext) {
        String phaseTwoSignals = phaseTwoSignalFormatter.renderPhaseTwoSignals(analysisContext);
        if (phaseTwoSignals.isBlank()) {
            return null;
        }
        return phaseTwoSignals + "。";
    }
}
