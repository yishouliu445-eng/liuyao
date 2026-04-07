package com.yishou.liuyao.knowledge.dto;

public class KnowledgeReferenceDTO {

    private Long id;
    private Long bookId;
    private Long taskId;
    private String title;
    private String topicTag;
    private String sourceType;
    private Integer sourcePage;
    private Integer segmentIndex;
    private String content;
    private String keywordSummary;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
