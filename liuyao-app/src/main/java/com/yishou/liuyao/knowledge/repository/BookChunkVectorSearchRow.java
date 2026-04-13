package com.yishou.liuyao.knowledge.repository;

public record BookChunkVectorSearchRow(
        Long id,
        Long bookId,
        Long taskId,
        String chapterTitle,
        Integer chunkIndex,
        String content,
        String contentType,
        String focusTopic,
        String knowledgeType,
        Boolean hasTimingPrediction,
        String topicTagsJson,
        String metadataJson,
        Integer charCount,
        Integer sentenceCount,
        String embeddingModel,
        String embeddingProvider,
        Double similarityScore
) {
}
