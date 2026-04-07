package com.yishou.liuyao.knowledge.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yishou.liuyao.book.domain.Book;
import com.yishou.liuyao.book.repository.BookRepository;
import com.yishou.liuyao.knowledge.domain.BookChunk;
import com.yishou.liuyao.knowledge.dto.BookChunkQueryResponse;
import com.yishou.liuyao.knowledge.dto.KnowledgeReferenceQueryResponse;
import com.yishou.liuyao.knowledge.dto.KnowledgeSearchResponse;
import com.yishou.liuyao.knowledge.mapper.KnowledgeMapper;
import com.yishou.liuyao.knowledge.repository.BookChunkRepository;
import com.yishou.liuyao.knowledge.repository.BookChunkVectorSearchRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class KnowledgeSearchService {

    private final BookChunkRepository bookChunkRepository;
    private final BookRepository bookRepository;
    private final KnowledgeImportService knowledgeImportService;
    private final KnowledgeMapper knowledgeMapper;
    private final KnowledgeQueryEmbeddingService knowledgeQueryEmbeddingService;
    private final BookChunkVectorSearchRepository bookChunkVectorSearchRepository;
    private final ObjectMapper objectMapper;

    public KnowledgeSearchService(BookChunkRepository bookChunkRepository,
                                  BookRepository bookRepository,
                                  KnowledgeImportService knowledgeImportService,
                                  KnowledgeMapper knowledgeMapper,
                                  KnowledgeQueryEmbeddingService knowledgeQueryEmbeddingService,
                                  BookChunkVectorSearchRepository bookChunkVectorSearchRepository,
                                  ObjectMapper objectMapper) {
        this.bookChunkRepository = bookChunkRepository;
        this.bookRepository = bookRepository;
        this.knowledgeImportService = knowledgeImportService;
        this.knowledgeMapper = knowledgeMapper;
        this.knowledgeQueryEmbeddingService = knowledgeQueryEmbeddingService;
        this.bookChunkVectorSearchRepository = bookChunkVectorSearchRepository;
        this.objectMapper = objectMapper;
    }

    public KnowledgeSearchResponse buildImportTopicsPreview() {
        // 当前先返回“首批最值得导入的知识主题”和模块职责，
        // 作为资料导入前的可视化准备结果，暂不提前接复杂检索。
        KnowledgeSearchResponse response = new KnowledgeSearchResponse();
        response.setTopics(List.of("用神", "世应", "六亲", "月破", "日破", "空亡", "动爻"));

        Map<String, String> responsibilities = new LinkedHashMap<>();
        responsibilities.put("book", "登记原始资料文件和解析状态，作为资料入口台账。");
        responsibilities.put("knowledge", "承接导入后的主题知识和后续检索结果。");
        responsibilities.put("task", "跟踪文档解析、切片和导入任务状态。");
        response.setModuleResponsibilities(responsibilities);
        return response;
    }

    public KnowledgeReferenceQueryResponse listReferences(String topicTag) {
        List<BookChunk> chunks = (topicTag == null || topicTag.isBlank())
                ? bookChunkRepository.findTop20ByOrderByIdDesc()
                : bookChunkRepository.findTop20ByFocusTopicOrderByIdDesc(topicTag);
        if (!chunks.isEmpty()) {
            KnowledgeReferenceQueryResponse response = new KnowledgeReferenceQueryResponse();
            Map<Long, Book> booksById = new HashMap<>();
            for (Book book : bookRepository.findAllById(chunks.stream().map(BookChunk::getBookId).distinct().toList())) {
                booksById.put(book.getId(), book);
            }
            response.setItems(chunks.stream()
                    .map(chunk -> knowledgeMapper.toDto(chunk, booksById.get(chunk.getBookId()), readTopicTags(chunk.getTopicTagsJson())))
                    .toList());
            return response;
        }
        return knowledgeImportService.listReferences(topicTag);
    }

    public BookChunkQueryResponse listChunks(Long bookId, String topicTag) {
        List<BookChunk> chunks;
        if (bookId != null && topicTag != null && !topicTag.isBlank()) {
            chunks = bookChunkRepository.findTop50ByBookIdAndFocusTopicOrderByChunkIndexAsc(bookId, topicTag);
        } else if (bookId != null) {
            chunks = bookChunkRepository.findTop50ByBookIdOrderByChunkIndexAsc(bookId);
        } else if (topicTag != null && !topicTag.isBlank()) {
            chunks = bookChunkRepository.findTop20ByFocusTopicOrderByIdDesc(topicTag);
        } else {
            chunks = bookChunkRepository.findTop20ByOrderByIdDesc();
        }
        BookChunkQueryResponse response = new BookChunkQueryResponse();
        response.setItems(chunks.stream()
                .map(chunk -> knowledgeMapper.toBookChunkDto(
                        chunk,
                        readTopicTags(chunk.getTopicTagsJson()),
                        readMetadata(chunk.getMetadataJson())))
                .toList());
        return response;
    }

    public BookChunkQueryResponse semanticSearchChunks(String queryText,
                                                       Long bookId,
                                                       String topicTag,
                                                       Integer limit) {
        BookChunkQueryResponse response = new BookChunkQueryResponse();
        if (!bookChunkVectorSearchRepository.supportsVectorSearch()) {
            return response;
        }
        int resolvedLimit = limit == null ? 10 : Math.max(1, Math.min(limit, 50));
        List<Double> queryEmbedding = knowledgeQueryEmbeddingService.embed(queryText);
        if (queryEmbedding.isEmpty()) {
            return response;
        }
        String vectorLiteral = toVectorLiteral(queryEmbedding);
        response.setItems(bookChunkVectorSearchRepository.search(
                        bookId,
                        topicTag,
                        vectorLiteral,
                        queryEmbedding.size(),
                        resolvedLimit)
                .stream()
                .map(row -> knowledgeMapper.toBookChunkDto(
                        row,
                        readTopicTags(row.topicTagsJson()),
                        readMetadata(row.metadataJson())))
                .toList());
        return response;
    }

    public List<String> suggestKnowledgeSnippets(String questionCategory,
                                                 String useGod,
                                                 List<String> ruleCodes,
                                                 int limit) {
        Set<String> candidateTopics = new LinkedHashSet<>();
        candidateTopics.add("用神");
        if ("出行".equals(questionCategory)) {
            candidateTopics.add("世应");
            candidateTopics.add("动爻");
            candidateTopics.add("空亡");
        } else if ("收入".equals(questionCategory)) {
            candidateTopics.add("动爻");
            candidateTopics.add("月破");
            candidateTopics.add("世应");
        } else if ("感情".equals(questionCategory) || "合作".equals(questionCategory)) {
            candidateTopics.add("世应");
            candidateTopics.add("动爻");
        }
        if (ruleCodes != null) {
            for (String ruleCode : ruleCodes) {
                candidateTopics.addAll(mapRuleCodeToTopics(ruleCode));
            }
        }
        int resolvedLimit = Math.max(1, Math.min(limit, 8));
        List<String> snippets = new java.util.ArrayList<>();
        for (String topic : candidateTopics) {
            for (BookChunk chunk : bookChunkRepository.findTop20ByFocusTopicOrderByIdDesc(topic)) {
                String snippet = buildKnowledgeSnippet(topic, chunk);
                if (!snippets.contains(snippet)) {
                    snippets.add(snippet);
                }
                if (snippets.size() >= resolvedLimit) {
                    return snippets;
                }
            }
        }
        return snippets;
    }

    private List<String> readTopicTags(String topicTagsJson) {
        try {
            return objectMapper.readValue(topicTagsJson, new TypeReference<>() {
            });
        } catch (Exception exception) {
            return List.of();
        }
    }

    private Map<String, Object> readMetadata(String metadataJson) {
        try {
            Map<String, Object> metadata = objectMapper.readValue(metadataJson, new TypeReference<>() {
            });
            return normalizeMetadataMap(metadata);
        } catch (Exception exception) {
            return Map.of();
        }
    }

    private Map<String, Object> normalizeMetadataMap(Map<String, Object> metadata) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            normalized.put(toCamelCase(entry.getKey()), normalizeMetadataValue(entry.getValue()));
        }
        return normalized;
    }

    private Object normalizeMetadataValue(Object value) {
        if (value instanceof Map<?, ?> nestedMap) {
            Map<String, Object> converted = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : nestedMap.entrySet()) {
                converted.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return normalizeMetadataMap(converted);
        }
        if (value instanceof List<?> items) {
            return items.stream().map(this::normalizeMetadataValue).toList();
        }
        return value;
    }

    private String toCamelCase(String value) {
        if (value == null || value.isBlank() || !value.contains("_")) {
            return value;
        }
        StringBuilder builder = new StringBuilder();
        boolean upperNext = false;
        for (char current : value.toCharArray()) {
            if (current == '_') {
                upperNext = true;
                continue;
            }
            builder.append(upperNext ? Character.toUpperCase(current) : current);
            upperNext = false;
        }
        return builder.toString();
    }

    private String toVectorLiteral(List<Double> values) {
        return "[" + values.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")) + "]";
    }

    private List<String> mapRuleCodeToTopics(String ruleCode) {
        if (ruleCode == null || ruleCode.isBlank()) {
            return List.of();
        }
        return switch (ruleCode) {
            case "USE_GOD_SELECTION", "USE_GOD_STRENGTH" -> List.of("用神");
            case "MOVING_LINE_EXISTS", "MOVING_LINE_AFFECT_USE_GOD" -> List.of("动爻");
            case "SHI_YING_EXISTS", "SHI_YING_RELATION" -> List.of("世应");
            case "USE_GOD_MONTH_BREAK" -> List.of("月破");
            case "USE_GOD_DAY_BREAK" -> List.of("日破");
            case "USE_GOD_EMPTY" -> List.of("空亡");
            default -> List.of();
        };
    }

    private String buildKnowledgeSnippet(String topic, BookChunk chunk) {
        String title = chunk.getChapterTitle() == null || chunk.getChapterTitle().isBlank()
                ? topic
                : chunk.getChapterTitle();
        return "[" + title + "] " + chunk.getContent();
    }
}
