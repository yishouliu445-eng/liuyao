package com.yishou.liuyao.knowledge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yishou.liuyao.book.domain.Book;
import com.yishou.liuyao.book.dto.BookImportRequest;
import com.yishou.liuyao.infrastructure.util.JsonUtils;
import com.yishou.liuyao.knowledge.domain.KnowledgeReference;
import com.yishou.liuyao.knowledge.dto.KnowledgeReferenceQueryResponse;
import com.yishou.liuyao.knowledge.mapper.KnowledgeMapper;
import com.yishou.liuyao.knowledge.repository.KnowledgeReferenceRepository;
import com.yishou.liuyao.task.domain.DocProcessTask;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class KnowledgeImportService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeImportService.class);
    private static final List<String> DEFAULT_TOPICS = List.of(
            "用神", "世应", "六亲", "月破", "日破", "空亡", "旬空", "动爻",
            "伏神", "飞神", "入墓", "冲开", "化进", "化退", "伏吟", "反吟", "应期",
            "神煞", "驿马", "桃花", "贵人", "文昌", "将星", "劫煞", "灾煞"
    );
    private static final int MAX_HEADING_LENGTH = 32;

    private final KnowledgeReferenceRepository knowledgeReferenceRepository;
    private final KnowledgeMapper knowledgeMapper;
    private final ObjectMapper objectMapper;

    public KnowledgeImportService(KnowledgeReferenceRepository knowledgeReferenceRepository,
                                  KnowledgeMapper knowledgeMapper,
                                  ObjectMapper objectMapper) {
        this.knowledgeReferenceRepository = knowledgeReferenceRepository;
        this.knowledgeMapper = knowledgeMapper;
        this.objectMapper = objectMapper;
    }

    public int importBook(Book book, DocProcessTask task) {
        BookImportRequest request = readImportRequest(task.getPayloadJson());
        List<ExtractedSegment> segments = extractSegments(book, request);

        knowledgeReferenceRepository.deleteByBookId(book.getId());
        int index = 1;
        for (ExtractedSegment segment : segments) {
            KnowledgeReference reference = new KnowledgeReference();
            reference.setBookId(book.getId());
            reference.setTaskId(task.getId());
            reference.setTitle(segment.title());
            reference.setTopicTag(segment.topicTag());
            reference.setSourceType(book.getSourceType());
            reference.setSourcePage(segment.sourcePage());
            reference.setSegmentIndex(index++);
            reference.setContent(segment.content());
            reference.setKeywordSummary(segment.keywordSummary());
            knowledgeReferenceRepository.save(reference);
        }
        log.info("知识切片导入完成: bookId={}, taskId={}, sourceType={}, referenceCount={}",
                book.getId(),
                task.getId(),
                book.getSourceType(),
                segments.size());
        return segments.size();
    }

    public KnowledgeReferenceQueryResponse listReferences(String topicTag) {
        KnowledgeReferenceQueryResponse response = new KnowledgeReferenceQueryResponse();
        response.setItems((topicTag == null || topicTag.isBlank()
                ? knowledgeReferenceRepository.findTop20ByOrderByIdDesc()
                : knowledgeReferenceRepository.findTop20ByTopicTagOrderByIdDesc(topicTag))
                .stream()
                .map(knowledgeMapper::toDto)
                .toList());
        return response;
    }

    private BookImportRequest readImportRequest(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, BookImportRequest.class);
        } catch (Exception exception) {
            throw new IllegalStateException("导入任务载荷解析失败", exception);
        }
    }

    private List<ExtractedSegment> extractSegments(Book book, BookImportRequest request) {
        String sourceType = normalizeSourceType(book.getSourceType(), book.getFilePath());
        return switch (sourceType) {
            case "TXT" -> extractFromTxt(book.getFilePath(), request.getTopicTags());
            case "PDF" -> extractFromPdf(book.getFilePath(), request.getTopicTags());
            default -> throw new IllegalStateException("暂不支持的资料类型: " + sourceType);
        };
    }

    private List<ExtractedSegment> extractFromTxt(String filePath, List<String> topicTags) {
        try {
            String content = Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
            return toSegments(content, topicTags, null);
        } catch (IOException exception) {
            throw new IllegalStateException("TXT 读取失败: " + filePath, exception);
        }
    }

    private List<ExtractedSegment> extractFromPdf(String filePath, List<String> topicTags) {
        try (PDDocument document = PDDocument.load(Path.of(filePath).toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            List<ExtractedSegment> segments = new ArrayList<>();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                segments.addAll(toSegments(stripper.getText(document), topicTags, page));
            }
            return segments;
        } catch (IOException exception) {
            throw new IllegalStateException("PDF 文本抽取失败: " + filePath, exception);
        }
    }

    private List<ExtractedSegment> toSegments(String text, List<String> topicTags, Integer page) {
        List<String> mergedParagraphs = mergeHeadingParagraphs(splitParagraphs(text));
        List<ExtractedSegment> segments = new ArrayList<>();
        for (String paragraph : mergedParagraphs) {
            if (paragraph.isBlank()) {
                continue;
            }
            String normalized = paragraph.trim();
            String topicTag = detectTopicTag(normalized, topicTags);
            if (topicTag == null) {
                continue;
            }
            String title = resolveTitle(normalized, topicTag);
            segments.add(new ExtractedSegment(
                    title,
                    topicTag,
                    page,
                    normalized,
                    buildKeywordSummary(normalized, topicTags)
            ));
        }
        return segments;
    }

    private List<String> splitParagraphs(String text) {
        String normalized = text.replace("\r\n", "\n");
        String[] rawParagraphs = normalized.split("\\n\\s*\\n");
        List<String> paragraphs = new ArrayList<>();
        for (String rawParagraph : rawParagraphs) {
            String compact = rawParagraph.lines()
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .reduce((left, right) -> left + "\n" + right)
                    .orElse("");
            if (!compact.isBlank()) {
                paragraphs.add(compact);
            }
        }
        return paragraphs;
    }

    private List<String> mergeHeadingParagraphs(List<String> paragraphs) {
        List<String> merged = new ArrayList<>();
        int index = 0;
        while (index < paragraphs.size()) {
            String current = paragraphs.get(index);
            if (current.length() <= 12 && index + 1 < paragraphs.size()) {
                merged.add(current + "\n" + paragraphs.get(index + 1));
                index += 2;
                continue;
            }
            merged.add(current);
            index++;
        }
        return merged;
    }

    private String resolveTitle(String content, String topicTag) {
        if (!content.contains("\n")) {
            return topicTag;
        }
        String heading = content.substring(0, content.indexOf('\n')).trim();
        return heading.length() <= MAX_HEADING_LENGTH ? heading : topicTag;
    }

    private String detectTopicTag(String content, List<String> topicTags) {
        for (String topic : resolveTopics(topicTags)) {
            if (content.contains(topic)) {
                return topic;
            }
        }
        return null;
    }

    private String buildKeywordSummary(String content, List<String> topicTags) {
        Set<String> keywords = new LinkedHashSet<>();
        for (String topic : resolveTopics(topicTags)) {
            if (content.contains(topic)) {
                keywords.add(topic);
            }
        }
        return String.join("、", keywords);
    }

    private List<String> resolveTopics(List<String> topicTags) {
        return topicTags == null || topicTags.isEmpty() ? DEFAULT_TOPICS : topicTags;
    }

    private String normalizeSourceType(String sourceType, String filePath) {
        if (sourceType != null && !sourceType.isBlank()) {
            return sourceType.trim().toUpperCase(Locale.ROOT);
        }
        String lowerPath = filePath.toLowerCase(Locale.ROOT);
        if (lowerPath.endsWith(".txt")) {
            return "TXT";
        }
        if (lowerPath.endsWith(".pdf")) {
            return "PDF";
        }
        return "UNKNOWN";
    }

    private record ExtractedSegment(String title, String topicTag, Integer sourcePage, String content, String keywordSummary) {
    }
}
