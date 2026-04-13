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
import com.yishou.liuyao.knowledge.repository.BookChunkHybridSearchRepository;
import com.yishou.liuyao.knowledge.repository.BookChunkVectorSearchRow;
import com.yishou.liuyao.rule.usegod.QuestionCategoryNormalizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
    private final BookChunkHybridSearchRepository bookChunkHybridSearchRepository;
    private final QuestionCategoryNormalizer questionCategoryNormalizer;
    private final ObjectMapper objectMapper;

    @Value("${liuyao.knowledge.similarity-threshold:0.65}")
    private double similarityThreshold = 0.65D;

    public KnowledgeSearchService(BookChunkRepository bookChunkRepository,
                                  BookRepository bookRepository,
                                  KnowledgeImportService knowledgeImportService,
                                  KnowledgeMapper knowledgeMapper,
                                  KnowledgeQueryEmbeddingService knowledgeQueryEmbeddingService,
                                  BookChunkVectorSearchRepository bookChunkVectorSearchRepository,
                                  BookChunkHybridSearchRepository bookChunkHybridSearchRepository,
                                  QuestionCategoryNormalizer questionCategoryNormalizer,
                                  ObjectMapper objectMapper) {
        this.bookChunkRepository = bookChunkRepository;
        this.bookRepository = bookRepository;
        this.knowledgeImportService = knowledgeImportService;
        this.knowledgeMapper = knowledgeMapper;
        this.knowledgeQueryEmbeddingService = knowledgeQueryEmbeddingService;
        this.bookChunkVectorSearchRepository = bookChunkVectorSearchRepository;
        this.bookChunkHybridSearchRepository = bookChunkHybridSearchRepository;
        this.questionCategoryNormalizer = questionCategoryNormalizer;
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
                .filter(this::passesSimilarityThreshold)
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
        String normalizedCategory = questionCategoryNormalizer.normalize(questionCategory);
        candidateTopics.add("用神");
        candidateTopics.addAll(mapUseGodToTopics(useGod));
        if ("出行".equals(normalizedCategory) || "搬家".equals(normalizedCategory)) {
            candidateTopics.add("世应");
            candidateTopics.add("动爻");
            candidateTopics.add("空亡");
        } else if ("收入".equals(normalizedCategory) || "财运".equals(normalizedCategory)
                || "求职".equals(normalizedCategory) || "工作".equals(normalizedCategory) || "升职".equals(normalizedCategory)) {
            candidateTopics.add("动爻");
            candidateTopics.add("月破");
            candidateTopics.add("世应");
        } else if ("感情".equals(normalizedCategory) || "婚姻".equals(normalizedCategory) || "复合".equals(normalizedCategory)
                || "合作".equals(normalizedCategory) || "人际".equals(normalizedCategory)) {
            candidateTopics.add("世应");
            candidateTopics.add("动爻");
        } else if ("考试".equals(normalizedCategory) || "成长".equals(normalizedCategory) || "调岗".equals(normalizedCategory)) {
            candidateTopics.add("用神");
            candidateTopics.add("世应");
        } else if ("压力".equals(normalizedCategory) || "健康".equals(normalizedCategory) || "官司".equals(normalizedCategory)) {
            candidateTopics.add("空亡");
            candidateTopics.add("动爻");
        } else if ("房产".equals(normalizedCategory)) {
            candidateTopics.add("用神");
            candidateTopics.add("世应");
        } else if ("寻物".equals(normalizedCategory)) {
            candidateTopics.add("用神");
            candidateTopics.add("动爻");
        }
        if (ruleCodes != null) {
            for (String ruleCode : ruleCodes) {
                candidateTopics.addAll(mapRuleCodeToTopics(ruleCode));
            }
        }
        int resolvedLimit = Math.max(1, Math.min(limit, 8));
        List<String> snippets = new ArrayList<>();
        appendHybridSnippets(snippets, resolvedLimit, normalizedCategory, useGod, ruleCodes);
        Map<Long, Book> booksById = loadBooksByTopicCandidates(candidateTopics);
        for (String topic : candidateTopics) {
            for (BookChunk chunk : sortChunks(bookChunkRepository.findTop20ByFocusTopicOrderByIdDesc(topic), booksById)) {
                String snippet = buildKnowledgeSnippet(topic, chunk, booksById.get(chunk.getBookId()));
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

    private void appendHybridSnippets(List<String> snippets,
                                      int resolvedLimit,
                                      String questionCategory,
                                      String useGod,
                                      List<String> ruleCodes) {
        if (!bookChunkVectorSearchRepository.supportsVectorSearch() || snippets.size() >= resolvedLimit) {
            return;
        }
        try {
            String queryText = buildSemanticQueryText(questionCategory, useGod, ruleCodes);
            List<BookChunkVectorSearchRow> rows = hybridSearch(queryText, null, resolvedLimit);
            if (rows.isEmpty()) {
                return;
            }
            Map<Long, Book> booksById = new HashMap<>();
            for (Book book : bookRepository.findAllById(rows.stream().map(BookChunkVectorSearchRow::bookId).distinct().toList())) {
                booksById.put(book.getId(), book);
            }
            for (BookChunkVectorSearchRow row : rows) {
                if (!passesSimilarityThreshold(row)) {
                    continue;
                }
                String snippet = buildKnowledgeSnippet(row.focusTopic(), row.content(), row.chapterTitle(), booksById.get(row.bookId()));
                if (!snippets.contains(snippet)) {
                    snippets.add(snippet);
                }
                if (snippets.size() >= resolvedLimit) {
                    return;
                }
            }
        } catch (RuntimeException exception) {
            // 检索失败时回退到标签召回
        }
    }

    List<BookChunkVectorSearchRow> hybridSearch(String queryText,
                                                String knowledgeType,
                                                int limit) {
        List<Double> queryEmbedding = knowledgeQueryEmbeddingService.embed(queryText);
        if (queryEmbedding.isEmpty()) {
            return List.of();
        }
        return bookChunkHybridSearchRepository.hybridSearch(
                queryText,
                toVectorLiteral(queryEmbedding),
                queryEmbedding.size(),
                null,
                knowledgeType,
                limit
        );
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

    private boolean passesSimilarityThreshold(BookChunkVectorSearchRow row) {
        Double similarity = row == null ? null : row.similarityScore();
        if (similarity == null) {
            return false;
        }
        return similarityThreshold <= 0 || similarity >= similarityThreshold;
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

    private String buildSemanticQueryText(String questionCategory, String useGod, List<String> ruleCodes) {
        List<String> parts = new ArrayList<>();
        if (questionCategory != null && !questionCategory.isBlank()) {
            parts.add("问类:" + questionCategory);
            String categoryHint = renderCategorySemanticHint(questionCategory);
            if (!categoryHint.isBlank()) {
                parts.add("关注:" + categoryHint);
            }
        }
        if (useGod != null && !useGod.isBlank()) {
            parts.add("用神:" + useGod);
        }
        if (ruleCodes != null && !ruleCodes.isEmpty()) {
            parts.add("规则:" + String.join(" ", ruleCodes));
        }
        return parts.isEmpty() ? "六爻知识" : String.join(" ", parts);
    }

    private String renderCategorySemanticHint(String questionCategory) {
        return switch (questionCategory) {
            case "收入" -> "工资 收益 回款";
            case "财运" -> "投资 回报 资金回流";
            case "求职" -> "面试 录用 岗位 机会";
            case "工作" -> "岗位 稳定 外部反馈";
            case "升职" -> "晋升 提拔 认可";
            case "调岗" -> "岗位变动 调动 安排";
            case "感情" -> "互动 态度 回应";
            case "婚姻" -> "关系稳定 落定";
            case "复合" -> "旧关系 重新连接";
            case "健康" -> "病象 恢复 反复";
            case "出行" -> "行程 路途 阻滞";
            case "搬家" -> "迁移 住处 安排";
            case "合作" -> "对方 配合 履约";
            case "房产" -> "房屋 手续 文书 成交";
            case "官司" -> "诉讼 证据 纠纷 主动权";
            case "寻物" -> "失物 线索 定位 回找";
            default -> "";
        };
    }

    private List<String> mapRuleCodeToTopics(String ruleCode) {
        if (ruleCode == null || ruleCode.isBlank()) {
            return List.of();
        }
        return switch (ruleCode) {
            case "USE_GOD_SELECTION", "USE_GOD_STRENGTH", "R003", "R004", "R018" -> List.of("用神");
            case "MOVING_LINE_EXISTS", "MOVING_LINE_AFFECT_USE_GOD", "R010" -> List.of("动爻");
            case "SHI_YING_EXISTS", "SHI_YING_RELATION", "R013", "R014", "R015" -> List.of("世应");
            case "USE_GOD_MONTH_BREAK" -> List.of("月破");
            case "USE_GOD_DAY_BREAK", "R009" -> List.of("日破");
            case "USE_GOD_EMPTY", "R011" -> List.of("空亡");
            default -> List.of();
        };
    }

    private List<String> mapUseGodToTopics(String useGod) {
        if (useGod == null || useGod.isBlank()) {
            return List.of();
        }
        return switch (useGod) {
            case "妻财", "官鬼", "父母", "兄弟", "子孙" -> List.of("用神");
            default -> List.of();
        };
    }

    private Map<Long, Book> loadBooksByTopicCandidates(Set<String> candidateTopics) {
        List<BookChunk> chunks = candidateTopics.stream()
                .flatMap(topic -> bookChunkRepository.findTop20ByFocusTopicOrderByIdDesc(topic).stream())
                .toList();
        Map<Long, Book> booksById = new LinkedHashMap<>();
        for (Book book : bookRepository.findAllById(chunks.stream().map(BookChunk::getBookId).distinct().toList())) {
            booksById.put(book.getId(), book);
        }
        return booksById;
    }

    private List<BookChunk> sortChunks(List<BookChunk> chunks, Map<Long, Book> booksById) {
        return chunks.stream()
                .sorted((left, right) -> {
                    int sourceRankDiff = Integer.compare(sourceRank(booksById.get(left.getBookId())), sourceRank(booksById.get(right.getBookId())));
                    if (sourceRankDiff != 0) {
                        return sourceRankDiff;
                    }
                    int charCountDiff = Integer.compare(
                            right.getCharCount() == null ? 0 : right.getCharCount(),
                            left.getCharCount() == null ? 0 : left.getCharCount());
                    if (charCountDiff != 0) {
                        return charCountDiff;
                    }
                    return Long.compare(right.getId(), left.getId());
                })
                .toList();
    }

    private int sourceRank(Book book) {
        if (book == null || book.getSourceType() == null) {
            return 9;
        }
        return switch (book.getSourceType()) {
            case "TXT" -> 1;
            case "MD" -> 2;
            case "PDF" -> 3;
            default -> 9;
        };
    }

    private String buildKnowledgeSnippet(String topic, BookChunk chunk, Book book) {
        return buildKnowledgeSnippet(topic, chunk.getContent(), chunk.getChapterTitle(), book);
    }

    private String buildKnowledgeSnippet(String topic, String content, String chapterTitle, Book book) {
        String title = chapterTitle == null || chapterTitle.isBlank() ? topic : chapterTitle;
        String sourceName = book == null || book.getTitle() == null || book.getTitle().isBlank()
                ? "未命名资料"
                : "《" + book.getTitle() + "》";
        return "[" + sourceName + "·" + title + "] " + content;
    }
}
