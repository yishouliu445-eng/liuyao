package com.yishou.liuyao.knowledge.domain;

import com.yishou.liuyao.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "book_chunk")
public class BookChunk extends BaseEntity {

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "chapter_title")
    private String chapterTitle;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "focus_topic")
    private String focusTopic;

    @Column(name = "topic_tags_json", nullable = false, columnDefinition = "TEXT")
    private String topicTagsJson;

    @Column(name = "metadata_json", nullable = false, columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "char_count", nullable = false)
    private Integer charCount;

    @Column(name = "sentence_count", nullable = false)
    private Integer sentenceCount;

    @Column(name = "embedding_json", columnDefinition = "TEXT")
    private String embeddingJson;

    @Column(name = "embedding_model")
    private String embeddingModel;

    @Column(name = "embedding_provider")
    private String embeddingProvider;

    @Column(name = "embedding_dim")
    private Integer embeddingDim;

    @Column(name = "embedding_version")
    private String embeddingVersion;

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

    public String getTopicTagsJson() {
        return topicTagsJson;
    }

    public void setTopicTagsJson(String topicTagsJson) {
        this.topicTagsJson = topicTagsJson;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
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

    public String getEmbeddingJson() {
        return embeddingJson;
    }

    public void setEmbeddingJson(String embeddingJson) {
        this.embeddingJson = embeddingJson;
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

    public Integer getEmbeddingDim() {
        return embeddingDim;
    }

    public void setEmbeddingDim(Integer embeddingDim) {
        this.embeddingDim = embeddingDim;
    }

    public String getEmbeddingVersion() {
        return embeddingVersion;
    }

    public void setEmbeddingVersion(String embeddingVersion) {
        this.embeddingVersion = embeddingVersion;
    }
}
