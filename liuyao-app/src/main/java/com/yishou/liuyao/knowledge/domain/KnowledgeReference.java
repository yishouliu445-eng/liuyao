package com.yishou.liuyao.knowledge.domain;

import com.yishou.liuyao.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "knowledge_reference")
public class KnowledgeReference extends BaseEntity {

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    private String title;

    @Column(name = "topic_tag")
    private String topicTag;

    @Column(name = "source_type", nullable = false)
    private String sourceType;

    @Column(name = "source_page")
    private Integer sourcePage;

    @Column(name = "segment_index", nullable = false)
    private Integer segmentIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "keyword_summary")
    private String keywordSummary;

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTopicTag() {
        return topicTag;
    }

    public void setTopicTag(String topicTag) {
        this.topicTag = topicTag;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Integer getSourcePage() {
        return sourcePage;
    }

    public void setSourcePage(Integer sourcePage) {
        this.sourcePage = sourcePage;
    }

    public Integer getSegmentIndex() {
        return segmentIndex;
    }

    public void setSegmentIndex(Integer segmentIndex) {
        this.segmentIndex = segmentIndex;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getKeywordSummary() {
        return keywordSummary;
    }

    public void setKeywordSummary(String keywordSummary) {
        this.keywordSummary = keywordSummary;
    }
}
