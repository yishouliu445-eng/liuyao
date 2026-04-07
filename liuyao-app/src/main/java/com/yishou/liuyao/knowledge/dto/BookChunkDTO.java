package com.yishou.liuyao.knowledge.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BookChunkDTO {

    private Long id;
    private Long bookId;
    private Long taskId;
    private String chapterTitle;
    private Integer chunkIndex;
    private String content;
    private String contentType;
    private String focusTopic;
    private String keywordSummary;
    private Integer charCount;
    private Integer sentenceCount;
    private String embeddingModel;
    private String embeddingProvider;
    private Double similarityScore;
    private List<String> topicTags = new ArrayList<>();
    private Map<String, Object> metadata = new LinkedHashMap<>();

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

    public String getChapterTitle() {
        return chapterTitle;
    }

    public void setChapterTitle(String chapterTitle) {
        this.chapterTitle = chapterTitle;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getFocusTopic() {
        return focusTopic;
    }

    public void setFocusTopic(String focusTopic) {
        this.focusTopic = focusTopic;
    }

    public String getKeywordSummary() {
        return keywordSummary;
    }

    public void setKeywordSummary(String keywordSummary) {
        this.keywordSummary = keywordSummary;
    }

    public Integer getCharCount() {
        return charCount;
    }

    public void setCharCount(Integer charCount) {
        this.charCount = charCount;
    }

    public Integer getSentenceCount() {
        return sentenceCount;
    }

    public void setSentenceCount(Integer sentenceCount) {
        this.sentenceCount = sentenceCount;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public String getEmbeddingProvider() {
        return embeddingProvider;
    }

    public void setEmbeddingProvider(String embeddingProvider) {
        this.embeddingProvider = embeddingProvider;
    }

    public Double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(Double similarityScore) {
        this.similarityScore = similarityScore;
    }

    public List<String> getTopicTags() {
        return topicTags;
    }

    public void setTopicTags(List<String> topicTags) {
        this.topicTags = topicTags;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
