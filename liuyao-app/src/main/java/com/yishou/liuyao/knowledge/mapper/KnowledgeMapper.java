package com.yishou.liuyao.knowledge.mapper;

import com.yishou.liuyao.book.domain.Book;
import com.yishou.liuyao.knowledge.domain.BookChunk;
import com.yishou.liuyao.knowledge.dto.BookChunkDTO;
import com.yishou.liuyao.knowledge.domain.KnowledgeReference;
import com.yishou.liuyao.knowledge.dto.KnowledgeReferenceDTO;
import com.yishou.liuyao.knowledge.repository.BookChunkVectorSearchRow;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KnowledgeMapper {

    public KnowledgeReferenceDTO toDto(KnowledgeReference reference) {
        KnowledgeReferenceDTO dto = new KnowledgeReferenceDTO();
        dto.setId(reference.getId());
        dto.setBookId(reference.getBookId());
        dto.setTaskId(reference.getTaskId());
        dto.setTitle(reference.getTitle());
        dto.setTopicTag(reference.getTopicTag());
        dto.setSourceType(reference.getSourceType());
        dto.setSourcePage(reference.getSourcePage());
        dto.setSegmentIndex(reference.getSegmentIndex());
        dto.setContent(reference.getContent());
        dto.setKeywordSummary(reference.getKeywordSummary());
        return dto;
    }

    public KnowledgeReferenceDTO toDto(BookChunk chunk, Book book, List<String> topicTags) {
        KnowledgeReferenceDTO dto = new KnowledgeReferenceDTO();
        dto.setId(chunk.getId());
        dto.setBookId(chunk.getBookId());
        dto.setTaskId(chunk.getTaskId());
        dto.setTitle(chunk.getChapterTitle() == null || chunk.getChapterTitle().isBlank()
                ? chunk.getFocusTopic()
                : chunk.getChapterTitle());
        dto.setTopicTag(chunk.getFocusTopic());
        dto.setSourceType(book == null ? null : book.getSourceType());
        dto.setSourcePage(null);
        dto.setSegmentIndex(chunk.getChunkIndex());
        dto.setContent(chunk.getContent());
        dto.setKeywordSummary(String.join("、", topicTags));
        return dto;
    }

    public BookChunkDTO toBookChunkDto(BookChunk chunk, List<String> topicTags, java.util.Map<String, Object> metadata) {
        BookChunkDTO dto = new BookChunkDTO();
        dto.setId(chunk.getId());
        dto.setBookId(chunk.getBookId());
        dto.setTaskId(chunk.getTaskId());
        dto.setChapterTitle(chunk.getChapterTitle());
        dto.setChunkIndex(chunk.getChunkIndex());
        dto.setContent(chunk.getContent());
        dto.setContentType(chunk.getContentType());
        dto.setFocusTopic(chunk.getFocusTopic());
        dto.setKeywordSummary(String.join("、", topicTags));
        dto.setCharCount(chunk.getCharCount());
        dto.setSentenceCount(chunk.getSentenceCount());
        dto.setEmbeddingModel(chunk.getEmbeddingModel());
        dto.setEmbeddingProvider(chunk.getEmbeddingProvider());
        dto.setTopicTags(topicTags);
        dto.setMetadata(metadata);
        return dto;
    }

    public BookChunkDTO toBookChunkDto(BookChunkVectorSearchRow row,
                                       List<String> topicTags,
                                       java.util.Map<String, Object> metadata) {
        BookChunkDTO dto = new BookChunkDTO();
        dto.setId(row.id());
        dto.setBookId(row.bookId());
        dto.setTaskId(row.taskId());
        dto.setChapterTitle(row.chapterTitle());
        dto.setChunkIndex(row.chunkIndex());
        dto.setContent(row.content());
        dto.setContentType(row.contentType());
        dto.setFocusTopic(row.focusTopic());
        dto.setKeywordSummary(String.join("、", topicTags));
        dto.setCharCount(row.charCount());
        dto.setSentenceCount(row.sentenceCount());
        dto.setEmbeddingModel(row.embeddingModel());
        dto.setEmbeddingProvider(row.embeddingProvider());
        dto.setSimilarityScore(row.similarityScore());
        dto.setTopicTags(topicTags);
        dto.setMetadata(metadata);
        return dto;
    }
}
