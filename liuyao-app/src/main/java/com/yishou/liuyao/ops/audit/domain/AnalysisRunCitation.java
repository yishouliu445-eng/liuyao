package com.yishou.liuyao.ops.audit.domain;

import com.yishou.liuyao.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "analysis_run_citation")
public class AnalysisRunCitation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_run_id", insertable = false, updatable = false)
    private AnalysisRun analysisRun;

    @Column(name = "analysis_run_id", nullable = false)
    private Long analysisRunId;

    @Column(name = "citation_id")
    private String citationId;

    @Column(name = "chunk_id")
    private Long chunkId;

    @Column(name = "book_id")
    private Long bookId;

    @Column(name = "source_title")
    private String sourceTitle;

    @Column(name = "chapter_title")
    private String chapterTitle;

    @Column(name = "reference_source")
    private String referenceSource;

    @Column(name = "reference_quote")
    private String referenceQuote;

    @Column(name = "reference_relevance")
    private String referenceRelevance;

    @Column(name = "matched_source_title")
    private String matchedSourceTitle;

    @Column(name = "matched_chapter_title")
    private String matchedChapterTitle;

    public Long getAnalysisRunId() {
        return analysisRunId;
    }

    public void setAnalysisRunId(Long analysisRunId) {
        this.analysisRunId = analysisRunId;
    }

    public AnalysisRun getAnalysisRun() {
        return analysisRun;
    }

    public void setAnalysisRun(AnalysisRun analysisRun) {
        this.analysisRun = analysisRun;
        this.analysisRunId = analysisRun == null ? null : analysisRun.getId();
    }

    public String getCitationId() {
        return citationId;
    }

    public void setCitationId(String citationId) {
        this.citationId = citationId;
    }

    public Long getChunkId() {
        return chunkId;
    }

    public void setChunkId(Long chunkId) {
        this.chunkId = chunkId;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getSourceTitle() {
        return sourceTitle;
    }

    public void setSourceTitle(String sourceTitle) {
        this.sourceTitle = sourceTitle;
    }

    public String getChapterTitle() {
        return chapterTitle;
    }

    public void setChapterTitle(String chapterTitle) {
        this.chapterTitle = chapterTitle;
    }

    public String getReferenceSource() {
        return referenceSource;
    }

    public void setReferenceSource(String referenceSource) {
        this.referenceSource = referenceSource;
    }

    public String getReferenceQuote() {
        return referenceQuote;
    }

    public void setReferenceQuote(String referenceQuote) {
        this.referenceQuote = referenceQuote;
    }

    public String getReferenceRelevance() {
        return referenceRelevance;
    }

    public void setReferenceRelevance(String referenceRelevance) {
        this.referenceRelevance = referenceRelevance;
    }

    public String getMatchedSourceTitle() {
        return matchedSourceTitle;
    }

    public void setMatchedSourceTitle(String matchedSourceTitle) {
        this.matchedSourceTitle = matchedSourceTitle;
    }

    public String getMatchedChapterTitle() {
        return matchedChapterTitle;
    }

    public void setMatchedChapterTitle(String matchedChapterTitle) {
        this.matchedChapterTitle = matchedChapterTitle;
    }
}
