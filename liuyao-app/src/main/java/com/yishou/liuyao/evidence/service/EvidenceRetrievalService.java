package com.yishou.liuyao.evidence.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yishou.liuyao.book.domain.Book;
import com.yishou.liuyao.book.repository.BookRepository;
import com.yishou.liuyao.evidence.dto.EvidenceHit;
import com.yishou.liuyao.evidence.dto.EvidenceSelectionResult;
import com.yishou.liuyao.knowledge.domain.BookChunk;
import com.yishou.liuyao.knowledge.repository.BookChunkHybridSearchRepository;
import com.yishou.liuyao.knowledge.repository.BookChunkRepository;
import com.yishou.liuyao.knowledge.repository.BookChunkVectorSearchRepository;
import com.yishou.liuyao.knowledge.repository.BookChunkVectorSearchRow;
import com.yishou.liuyao.knowledge.service.KnowledgeQueryEmbeddingService;
import com.yishou.liuyao.rule.usegod.QuestionCategoryNormalizer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class EvidenceRetrievalService {

    private final BookChunkRepository bookChunkRepository;
    private final BookRepository bookRepository;
    private final KnowledgeQueryEmbeddingService knowledgeQueryEmbeddingService;
    private final BookChunkVectorSearchRepository bookChunkVectorSearchRepository;
    private final BookChunkHybridSearchRepository bookChunkHybridSearchRepository;
    private final QuestionCategoryNormalizer questionCategoryNormalizer;

    public EvidenceRetrievalService(BookChunkRepository bookChunkRepository,
                                    BookRepository bookRepository,
                                    KnowledgeQueryEmbeddingService knowledgeQueryEmbeddingService,
                                    BookChunkVectorSearchRepository bookChunkVectorSearchRepository,
                                    BookChunkHybridSearchRepository bookChunkHybridSearchRepository,
                                    QuestionCategoryNormalizer questionCategoryNormalizer,
                                    ObjectMapper objectMapper) {
        this.bookChunkRepository = bookChunkRepository;
        this.bookRepository = bookRepository;
        this.knowledgeQueryEmbeddingService = knowledgeQueryEmbeddingService;
        this.bookChunkVectorSearchRepository = bookChunkVectorSearchRepository;
        this.bookChunkHybridSearchRepository = bookChunkHybridSearchRepository;
        this.questionCategoryNormalizer = questionCategoryNormalizer;
    }

    public EvidenceSelectionResult retrieveInitial(String questionCategory,
                                                   String useGod,
                                                   List<String> ruleCodes,
                                                   int limit) {
        String normalizedCategory = questionCategoryNormalizer.normalize(questionCategory);
        return retrieve(
                buildInitialQuery(normalizedCategory, useGod, ruleCodes),
                buildCandidateTopics(normalizedCategory, useGod, ruleCodes),
                null,
                limit
        );
    }

    public EvidenceSelectionResult retrieveFollowUp(String questionCategory,
                                                    String useGod,
                                                    String followUpQuestion,
                                                    int limit) {
        String normalizedCategory = questionCategoryNormalizer.normalize(questionCategory);
        return retrieve(
                buildFollowUpQuery(normalizedCategory, useGod, followUpQuestion),
                buildCandidateTopics(normalizedCategory, useGod, List.of()),
                followUpQuestion,
                limit
        );
    }

    private EvidenceSelectionResult retrieve(String queryText,
                                            Set<String> candidateTopics,
                                            String lexicalQuery,
                                            int limit) {
        EvidenceSelectionResult result = new EvidenceSelectionResult();
        if (!bookChunkVectorSearchRepository.supportsVectorSearch()) {
            result.setHits(fallbackHits(candidateTopics, lexicalQuery, limit));
            return result;
        }
        List<Double> queryEmbedding = knowledgeQueryEmbeddingService.embed(queryText);
        if (queryEmbedding.isEmpty()) {
            result.setHits(fallbackHits(candidateTopics, lexicalQuery, limit));
            return result;
        }

        List<BookChunkVectorSearchRow> rows = bookChunkHybridSearchRepository.hybridSearch(
                queryText,
                toVectorLiteral(queryEmbedding),
                queryEmbedding.size(),
                null,
                null,
                Math.max(1, Math.min(limit, 8))
        );
        Map<Long, Book> booksById = loadBooks(rows);
        List<EvidenceHit> hits = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            BookChunkVectorSearchRow row = rows.get(index);
            Book book = booksById.get(row.bookId());
            EvidenceHit hit = new EvidenceHit();
            hit.setChunkId(row.id());
            hit.setBookId(row.bookId());
            hit.setCitationId("chunk:" + row.id());
            hit.setSourceTitle(book == null ? null : book.getTitle());
            hit.setChapterTitle(row.chapterTitle());
            hit.setKnowledgeType(row.knowledgeType());
            hit.setContent(row.content());
            hit.setScore(row.similarityScore());
            hit.setRank(index + 1);
            hits.add(hit);
        }
        if (hits.isEmpty()) {
            hits = fallbackHits(candidateTopics, lexicalQuery, limit);
        }
        result.setHits(hits);
        return result;
    }

    private Map<Long, Book> loadBooks(List<BookChunkVectorSearchRow> rows) {
        Map<Long, Book> booksById = new HashMap<>();
        List<Long> bookIds = rows.stream().map(BookChunkVectorSearchRow::bookId).distinct().toList();
        for (Book book : bookRepository.findAllById(bookIds)) {
            booksById.put(book.getId(), book);
        }
        return booksById;
    }

    private Set<String> buildCandidateTopics(String questionCategory, String useGod, List<String> ruleCodes) {
        Set<String> candidateTopics = new LinkedHashSet<>();
        candidateTopics.add("用神");
        candidateTopics.addAll(mapUseGodToTopics(useGod));
        if ("出行".equals(questionCategory) || "搬家".equals(questionCategory)) {
            candidateTopics.add("世应");
            candidateTopics.add("动爻");
            candidateTopics.add("空亡");
        } else if ("收入".equals(questionCategory) || "财运".equals(questionCategory)
                || "求职".equals(questionCategory) || "工作".equals(questionCategory) || "升职".equals(questionCategory)) {
            candidateTopics.add("动爻");
            candidateTopics.add("月破");
            candidateTopics.add("世应");
        } else if ("感情".equals(questionCategory) || "婚姻".equals(questionCategory) || "复合".equals(questionCategory)
                || "合作".equals(questionCategory) || "人际".equals(questionCategory)) {
            candidateTopics.add("世应");
            candidateTopics.add("动爻");
        } else if ("考试".equals(questionCategory) || "成长".equals(questionCategory) || "调岗".equals(questionCategory)) {
            candidateTopics.add("用神");
            candidateTopics.add("世应");
        } else if ("压力".equals(questionCategory) || "健康".equals(questionCategory) || "官司".equals(questionCategory)) {
            candidateTopics.add("空亡");
            candidateTopics.add("动爻");
        } else if ("房产".equals(questionCategory) || "寻物".equals(questionCategory)) {
            candidateTopics.add("用神");
            candidateTopics.add("世应");
        }
        if (ruleCodes != null) {
            for (String ruleCode : ruleCodes) {
                candidateTopics.addAll(mapRuleCodeToTopics(ruleCode));
            }
        }
        return candidateTopics;
    }

    private List<EvidenceHit> fallbackHits(Set<String> candidateTopics, String lexicalQuery, int limit) {
        Map<Long, BookChunk> uniqueChunks = new LinkedHashMap<>();
        for (String topic : candidateTopics) {
            for (BookChunk chunk : bookChunkRepository.findTop20ByFocusTopicOrderByIdDesc(topic)) {
                uniqueChunks.putIfAbsent(chunk.getId(), chunk);
            }
        }
        if (uniqueChunks.isEmpty()) {
            for (BookChunk chunk : bookChunkRepository.findTop20ByOrderByIdDesc()) {
                uniqueChunks.putIfAbsent(chunk.getId(), chunk);
            }
        }
        Map<Long, Book> booksById = loadBooksByChunkIds(uniqueChunks.values().stream().map(BookChunk::getBookId).distinct().toList());
        List<BookChunk> rankedChunks = uniqueChunks.values().stream()
                .sorted((left, right) -> compareFallbackChunk(left, right, booksById, lexicalQuery))
                .limit(Math.max(1, Math.min(limit, 8)))
                .toList();
        List<EvidenceHit> hits = new ArrayList<>();
        for (int index = 0; index < rankedChunks.size(); index++) {
            BookChunk chunk = rankedChunks.get(index);
            Book book = booksById.get(chunk.getBookId());
            EvidenceHit hit = new EvidenceHit();
            hit.setChunkId(chunk.getId());
            hit.setBookId(chunk.getBookId());
            hit.setCitationId("chunk:" + chunk.getId());
            hit.setSourceTitle(book == null ? null : book.getTitle());
            hit.setChapterTitle(chunk.getChapterTitle());
            hit.setKnowledgeType(chunk.getContentType());
            hit.setContent(chunk.getContent());
            hit.setRank(index + 1);
            hits.add(hit);
        }
        return hits;
    }

    private Map<Long, Book> loadBooksByChunkIds(List<Long> bookIds) {
        Map<Long, Book> booksById = new HashMap<>();
        for (Book book : bookRepository.findAllById(bookIds)) {
            booksById.put(book.getId(), book);
        }
        return booksById;
    }

    private int compareFallbackChunk(BookChunk left,
                                     BookChunk right,
                                     Map<Long, Book> booksById,
                                     String lexicalQuery) {
        int lexicalDiff = Integer.compare(keywordScore(right, lexicalQuery), keywordScore(left, lexicalQuery));
        if (lexicalDiff != 0) {
            return lexicalDiff;
        }
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
    }

    private int keywordScore(BookChunk chunk, String lexicalQuery) {
        if (chunk == null || lexicalQuery == null || lexicalQuery.isBlank()) {
            return 0;
        }
        String haystack = ((chunk.getChapterTitle() == null ? "" : chunk.getChapterTitle()) + " "
                + (chunk.getContent() == null ? "" : chunk.getContent())).replaceAll("\\s+", "");
        int score = 0;
        for (String token : tokenizeLexicalQuery(lexicalQuery)) {
            if (!token.isBlank() && haystack.contains(token)) {
                score++;
            }
        }
        return score;
    }

    private List<String> tokenizeLexicalQuery(String lexicalQuery) {
        String normalized = lexicalQuery == null ? "" : lexicalQuery.replaceAll("[，。！？、；：,.!?;:]", " ").trim();
        if (normalized.isBlank()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        for (String part : normalized.split("\\s+")) {
            String token = part.trim();
            if (token.length() >= 2) {
                tokens.add(token);
            }
        }
        return tokens;
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

    private String buildInitialQuery(String questionCategory, String useGod, List<String> ruleCodes) {
        List<String> parts = new ArrayList<>();
        if (questionCategory != null && !questionCategory.isBlank()) {
            parts.add("问类:" + questionCategory);
            String hint = renderCategorySemanticHint(questionCategory);
            if (!hint.isBlank()) {
                parts.add("关注:" + hint);
            }
        }
        if (useGod != null && !useGod.isBlank()) {
            parts.add("用神:" + useGod);
        }
        if (ruleCodes != null && !ruleCodes.isEmpty()) {
            parts.add("规则:" + String.join(" ", ruleCodes));
        }
        return String.join(" ", parts);
    }

    private String buildFollowUpQuery(String questionCategory, String useGod, String followUpQuestion) {
        List<String> parts = new ArrayList<>();
        if (questionCategory != null && !questionCategory.isBlank()) {
            parts.add("问类:" + questionCategory);
            String hint = renderCategorySemanticHint(questionCategory);
            if (!hint.isBlank()) {
                parts.add("关注:" + hint);
            }
        }
        if (useGod != null && !useGod.isBlank()) {
            parts.add("用神:" + useGod);
        }
        if (followUpQuestion != null && !followUpQuestion.isBlank()) {
            parts.add("追问:" + followUpQuestion);
        }
        return String.join(" ", parts);
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

    private String toVectorLiteral(List<Double> values) {
        return "[" + values.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")) + "]";
    }
}
