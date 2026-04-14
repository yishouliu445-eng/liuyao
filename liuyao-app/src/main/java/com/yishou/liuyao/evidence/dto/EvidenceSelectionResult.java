package com.yishou.liuyao.evidence.dto;

import java.util.ArrayList;
import java.util.List;

public class EvidenceSelectionResult {

    private List<EvidenceHit> hits = new ArrayList<>();

    public List<EvidenceHit> getHits() {
        return hits;
    }

    public void setHits(List<EvidenceHit> hits) {
        this.hits = hits;
    }

    public List<String> toPromptSnippets() {
        return hits.stream().map(this::toPromptSnippet).toList();
    }

    private String toPromptSnippet(EvidenceHit hit) {
        String sourceName = hit.getSourceTitle() == null || hit.getSourceTitle().isBlank()
                ? "未命名资料"
                : "《" + hit.getSourceTitle() + "》";
        String chapterTitle = hit.getChapterTitle() == null || hit.getChapterTitle().isBlank()
                ? "未命名章节"
                : hit.getChapterTitle();
        return "[" + sourceName + "·" + chapterTitle + "] " + hit.getContent();
    }
}
