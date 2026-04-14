package com.yishou.liuyao.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yishou.liuyao.analysis.dto.AnalysisOutputDTO;
import com.yishou.liuyao.evaluation.dto.EvaluationScoreCard;
import com.yishou.liuyao.evaluation.service.EvaluationRunService;
import com.yishou.liuyao.analysis.service.ContextWindowBuilder;
import com.yishou.liuyao.analysis.service.LlmClient;
import com.yishou.liuyao.analysis.service.OrchestratedAnalysisService;
import com.yishou.liuyao.analysis.service.PromptTemplateEngine;
import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.rule.RuleHit;
import com.yishou.liuyao.session.domain.ChatMessage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("prompt-test")
@Tag("prompt-regression")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PromptRegressionTest {

    private static final ObjectMapper CASE_MAPPER = new ObjectMapper();
    private static final Set<String> KNOWN_CLASSICS = Set.of("增删卜易", "卜筮正宗", "易林", "黄金策", "周易");
    private static final String SCHEMA_PATH = "golden-dataset/schema/analysis_output_schema.json";

    @Autowired
    private OrchestratedAnalysisService analysisService;

    @Autowired
    private PromptTemplateEngine promptTemplateEngine;

    @Autowired
    private ContextWindowBuilder contextWindowBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EvaluationRunService evaluationRunService;

    static Stream<Arguments> loadGoldenCases() throws IOException {
        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:golden-dataset/cases/*.json");
        List<Resource> sortedResources = Stream.of(resources)
                .sorted(Comparator.comparing(resource -> {
                    try {
                        return resource.getFilename() == null ? "" : resource.getFilename();
                    } catch (Exception ignored) {
                        return "";
                    }
                }))
                .toList();

        List<Arguments> arguments = new ArrayList<>();
        for (Resource resource : sortedResources) {
            try (InputStream inputStream = resource.getInputStream()) {
                JsonNode node = CASE_MAPPER.readTree(inputStream);
                arguments.add(Arguments.of(resource.getFilename(), node));
            }
        }
        return arguments.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("loadGoldenCases")
    void shouldMatchGoldenCase(String fileName, JsonNode goldenCase) throws Exception {
        JsonNode schema = loadSchema();
        ScenarioData scenario = ScenarioData.from(goldenCase);

        List<LlmClient.ChatMessage> promptMessages = buildPromptMessages(scenario);
        assertPromptStructure(fileName, scenario, promptMessages);

        AnalysisOutputDTO output = runAnalysis(scenario);
        validateSchemaLike(schema, output);
        assertCoreRegressionAssertions(fileName, scenario, output);
        EvaluationScoreCard scoreCard = evaluationRunService.evaluatePromptRegression(
                fileName,
                scenario.chart().getQuestionCategory(),
                output
        );
        assertNotNull(scoreCard.getSummary());
        assertEquals("PROMPT_REGRESSION", scoreCard.getDatasetType());
    }

    private List<LlmClient.ChatMessage> buildPromptMessages(ScenarioData scenario) {
        String systemTemplate = promptTemplateEngine.load(PromptTemplateEngine.SYSTEM_ANALYST);
        if (scenario.followUp()) {
            return contextWindowBuilder.buildFollowUpContext(
                    systemTemplate,
                    scenario.chart(),
                    scenario.ruleHits(),
                    scenario.effectiveScore(),
                    scenario.resultLevel(),
                    scenario.history(),
                    scenario.knowledgeSnippets(),
                    scenario.followUpQuestion()
            );
        }
        return contextWindowBuilder.buildInitialContext(
                systemTemplate,
                scenario.chart(),
                scenario.ruleHits(),
                scenario.effectiveScore(),
                scenario.resultLevel(),
                scenario.knowledgeSnippets(),
                scenario.chart().getQuestion()
        );
    }

    private AnalysisOutputDTO runAnalysis(ScenarioData scenario) {
        if (scenario.followUp()) {
            return analysisService.analyzeFollowUp(
                    scenario.chart(),
                    scenario.ruleHits(),
                    scenario.effectiveScore(),
                    scenario.resultLevel(),
                    scenario.history(),
                    scenario.knowledgeSnippets(),
                    scenario.followUpQuestion()
            );
        }
        return analysisService.analyzeInitial(
                scenario.chart(),
                scenario.ruleHits(),
                scenario.effectiveScore(),
                scenario.resultLevel(),
                scenario.knowledgeSnippets()
        );
    }

    private JsonNode loadSchema() throws IOException {
        ClassPathResource resource = new ClassPathResource(SCHEMA_PATH);
        assertTrue(resource.exists(), "缺少 JSON Schema 文件: " + SCHEMA_PATH);
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readTree(inputStream);
        }
    }

    private void assertPromptStructure(String fileName,
                                       ScenarioData scenario,
                                       List<LlmClient.ChatMessage> promptMessages) {
        assertFalse(promptMessages.isEmpty(), "Prompt 不能为空: " + fileName);

        String systemContent = promptMessages.get(0).content();
        assertAll("system prompt sections",
                () -> assertTrue(systemContent.contains("<chart_data>"), "缺少 chart_data 段: " + fileName),
                () -> assertTrue(systemContent.contains("<rule_hits>"), "缺少 rule_hits 段: " + fileName),
                () -> assertTrue(systemContent.contains("问题：" + scenario.chart().getQuestion()),
                        "缺少问题上下文: " + fileName),
                () -> assertTrue(systemContent.contains("用神：" + nullSafe(scenario.chart().getUseGod())),
                        "缺少用神上下文: " + fileName));

        if (scenario.followUp()) {
            assertTrue(promptMessages.size() > 2, "追问上下文应包含历史消息: " + fileName);
            String lastUserMessage = promptMessages.get(promptMessages.size() - 1).content();
            assertTrue(lastUserMessage.contains(scenario.followUpQuestion()),
                    "追问消息未包含 followUpQuestion: " + fileName);

            if (scenario.knowledgeSnippets().isEmpty()) {
                assertFalse(lastUserMessage.contains("## 本轮相关古籍片段"),
                        "无知识片段时不应生成 knowledge section: " + fileName);
            } else {
                assertTrue(lastUserMessage.contains("## 本轮相关古籍片段"),
                        "追问消息应包含 knowledge section: " + fileName);
                assertTrue(lastUserMessage.contains(scenario.knowledgeSnippets().get(0)),
                        "追问消息应包含古籍片段内容: " + fileName);
            }

            if (scenario.historyRoundCount() > 5) {
                boolean hasSummary = promptMessages.stream()
                        .skip(1)
                        .filter(message -> "user".equals(message.role()))
                        .findFirst()
                        .map(message -> message.content().startsWith("[历史摘要]"))
                        .orElse(false);
                assertTrue(hasSummary, "超过 5 轮历史时应压缩为摘要: " + fileName);
            }
        } else {
            assertEquals(2, promptMessages.size(), "首次分析应只包含 system + user 两条消息: " + fileName);
            assertEquals(scenario.chart().getQuestion(), promptMessages.get(1).content(),
                    "首次分析 user 消息应与原始问题一致: " + fileName);

            if (scenario.knowledgeSnippets().isEmpty()) {
                assertFalse(systemContent.contains("</knowledge_snippets>"),
                        "无知识片段时不应生成 knowledge section: " + fileName);
            } else {
                assertTrue(systemContent.contains("<knowledge_snippets>"),
                        "有知识片段时应包含 knowledge section: " + fileName);
                assertTrue(systemContent.contains("</knowledge_snippets>"),
                        "有知识片段时应包含 knowledge section 结束标记: " + fileName);
            }
        }
    }

    private void validateSchemaLike(JsonNode schema, AnalysisOutputDTO output) {
        JsonNode actual = objectMapper.valueToTree(output);
        assertSchemaNode(schema, actual, "root");
    }

    private void assertSchemaNode(JsonNode schemaNode, JsonNode actualNode, String path) {
        assertNotNull(actualNode, "Schema 校验对象为空: " + path);
        String expectedType = schemaNode.path("type").asText("");
        if ("object".equals(expectedType)) {
            assertTrue(actualNode.isObject(), "类型不匹配: " + path);
            JsonNode required = schemaNode.path("required");
            if (required.isArray()) {
                for (JsonNode item : required) {
                    assertTrue(actualNode.has(item.asText()), "缺少必填字段: " + path + "." + item.asText());
                }
            }

            JsonNode properties = schemaNode.path("properties");
            if (properties.isObject()) {
                properties.fields().forEachRemaining(entry -> {
                    JsonNode childSchema = entry.getValue();
                    JsonNode childActual = actualNode.get(entry.getKey());
                    if (childActual == null || childActual.isNull()) {
                        return;
                    }
                    assertSchemaNode(childSchema, childActual, path + "." + entry.getKey());
                });
            }
            return;
        }

        if ("array".equals(expectedType)) {
            assertTrue(actualNode.isArray(), "类型不匹配: " + path);
            JsonNode minItems = schemaNode.get("minItems");
            if (minItems != null) {
                assertTrue(actualNode.size() >= minItems.asInt(), "数组长度不足: " + path);
            }
            JsonNode itemSchema = schemaNode.get("items");
            if (itemSchema != null) {
                for (int index = 0; index < actualNode.size(); index++) {
                    assertSchemaNode(itemSchema, actualNode.get(index), path + "[" + index + "]");
                }
            }
            return;
        }

        if ("string".equals(expectedType)) {
            assertTrue(actualNode.isTextual(), "类型不匹配: " + path);
            return;
        }

        if ("integer".equals(expectedType)) {
            assertTrue(actualNode.isIntegralNumber(), "类型不匹配: " + path);
            return;
        }

        if ("number".equals(expectedType)) {
            assertTrue(actualNode.isNumber(), "类型不匹配: " + path);
            return;
        }

        if ("boolean".equals(expectedType)) {
            assertTrue(actualNode.isBoolean(), "类型不匹配: " + path);
        }
    }

    private void assertCoreRegressionAssertions(String fileName,
                                                ScenarioData scenario,
                                                AnalysisOutputDTO output) {
        JsonNode assertions = scenario.assertions();
        String conclusion = nullSafe(output.getAnalysis() == null ? null : output.getAnalysis().getConclusion());
        String fullText = buildFullText(output);

        String expectedDirection = assertions.path("conclusionDirection").asText("NEUTRAL");
        assertEquals(expectedDirection, inferDirection(conclusion), "结论方向不一致: " + fileName);

        for (String forbidden : readStringArray(assertions.path("mustNotContain"))) {
            assertFalse(conclusion.contains(forbidden), "结论包含禁用词 '" + forbidden + "': " + fileName);
        }

        for (String required : readStringArray(assertions.path("mustContain"))) {
            assertTrue(fullText.contains(required), "结果缺少必含词 '" + required + "': " + fileName);
        }

        int minActionPlanLength = assertions.path("actionPlanMinLength").asInt(0);
        assertTrue(output.getAnalysis() != null
                        && output.getAnalysis().getActionPlan() != null
                        && output.getAnalysis().getActionPlan().size() >= minActionPlanLength,
                "actionPlan 数量不足: " + fileName);

        int maxFabricatedReferences = assertions.path("classicReferencesMaxFabricated").asInt(0);
        assertTrue(countFabricatedReferences(output) <= maxFabricatedReferences,
                "疑似捏造古籍引用: " + fileName);

        int smartPromptsLength = assertions.path("smartPromptsLength").asInt(3);
        assertTrue(output.getSmartPrompts() != null, "smartPrompts 不能为空: " + fileName);
        assertEquals(smartPromptsLength, output.getSmartPrompts().size(),
                "smartPrompts 数量不一致: " + fileName);
    }

    private int countFabricatedReferences(AnalysisOutputDTO output) {
        if (output.getAnalysis() == null || output.getAnalysis().getClassicReferences() == null) {
            return 0;
        }
        int fabricated = 0;
        for (AnalysisOutputDTO.ClassicReference reference : output.getAnalysis().getClassicReferences()) {
            if (reference == null) {
                fabricated++;
                continue;
            }
            String source = nullSafe(reference.getSource());
            String quote = nullSafe(reference.getQuote());
            String relevance = nullSafe(reference.getRelevance());
            if (source.isBlank() || quote.isBlank() || relevance.isBlank()) {
                fabricated++;
                continue;
            }
            if (!isKnownClassicSource(source)) {
                fabricated++;
                continue;
            }
            if (hasPartialCitationMetadata(reference)) {
                fabricated++;
            }
        }
        return fabricated;
    }

    private boolean hasPartialCitationMetadata(AnalysisOutputDTO.ClassicReference reference) {
        boolean hasCitationId = reference.getCitationId() != null && !reference.getCitationId().isBlank();
        boolean hasChunkId = reference.getChunkId() != null;
        boolean hasBookId = reference.getBookId() != null;
        boolean hasAny = hasCitationId || hasChunkId || hasBookId;
        boolean hasAll = hasCitationId && hasChunkId && hasBookId;
        return hasAny && !hasAll;
    }

    private boolean isKnownClassicSource(String source) {
        String normalized = normalizeClassicSource(source);
        return KNOWN_CLASSICS.stream().anyMatch(normalized::contains);
    }

    private String normalizeClassicSource(String source) {
        String text = nullSafe(source).replace('《', ' ').replace('》', ' ').trim();
        int separatorIndex = text.indexOf('·');
        if (separatorIndex > 0) {
            return text.substring(0, separatorIndex).trim();
        }
        return text;
    }

    private String buildFullText(AnalysisOutputDTO output) {
        if (output == null || output.getAnalysis() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        appendIfPresent(sb, output.getAnalysis().getHexagramOverview());
        appendIfPresent(sb, output.getAnalysis().getUseGodAnalysis());
        appendIfPresent(sb, output.getAnalysis().getDetailedReasoning());
        appendIfPresent(sb, output.getAnalysis().getConclusion());
        if (output.getAnalysis().getActionPlan() != null) {
            output.getAnalysis().getActionPlan().forEach(item -> appendIfPresent(sb, item));
        }
        if (output.getAnalysis().getClassicReferences() != null) {
            output.getAnalysis().getClassicReferences().forEach(reference -> {
                if (reference != null) {
                    appendIfPresent(sb, reference.getSource());
                    appendIfPresent(sb, reference.getQuote());
                    appendIfPresent(sb, reference.getRelevance());
                }
            });
        }
        if (output.getSmartPrompts() != null) {
            output.getSmartPrompts().forEach(item -> appendIfPresent(sb, item));
        }
        return sb.toString();
    }

    private void appendIfPresent(StringBuilder sb, String value) {
        String normalized = nullSafe(value);
        if (!normalized.isBlank()) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(normalized);
        }
    }

    private static List<String> readStringArray(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            String value = item.asText("");
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    private String inferDirection(String conclusion) {
        String normalized = nullSafe(conclusion);
        if (normalized.contains("阻力偏大") || normalized.contains("稳住节奏")
                || normalized.contains("风险信号偏重") || normalized.contains("当前使用基础推算引擎")) {
            return "NEGATIVE";
        }
        if (normalized.contains("走势仍偏积极") || normalized.contains("把握时机")
                || normalized.contains("建议") || normalized.contains("有机会")) {
            return "POSITIVE";
        }
        return "NEUTRAL";
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private record ScenarioData(
            String caseId,
            String description,
            boolean followUp,
            ChartSnapshot chart,
            List<RuleHit> ruleHits,
            int effectiveScore,
            String resultLevel,
            List<String> knowledgeSnippets,
            List<ChatMessage> history,
            String followUpQuestion,
            JsonNode assertions,
            int historyRoundCount
    ) {

        static ScenarioData from(JsonNode goldenCase) {
            JsonNode input = goldenCase.path("input");
            ChartSnapshot chart = toChart(input);
            List<RuleHit> ruleHits = toRuleHits(input.path("ruleHits"));
            JsonNode structuredResult = input.path("structuredResult");
            int effectiveScore = structuredResult.path("effectiveScore").asInt(0);
            String resultLevel = structuredResult.path("effectiveResultLevel").asText("NEUTRAL");
            List<String> knowledgeSnippets = readStringList(input.path("knowledgeSnippets"));
            boolean followUp = input.path("followUpQuestion").isTextual() || input.path("historyRounds").isIntegralNumber();
            List<ChatMessage> history = followUp
                    ? buildHistory(
                            input.path("historyRounds").asInt(0),
                            input.path("historyRepeatLength").asInt(24))
                    : List.of();
            return new ScenarioData(
                    goldenCase.path("caseId").asText("unknown_case"),
                    goldenCase.path("description").asText(""),
                    followUp,
                    chart,
                    ruleHits,
                    effectiveScore,
                    resultLevel,
                    knowledgeSnippets,
                    history,
                    input.path("followUpQuestion").asText(""),
                    goldenCase.path("assertions"),
                    input.path("historyRounds").asInt(0)
            );
        }

        private static ChartSnapshot toChart(JsonNode input) {
            JsonNode chartNode = input.path("chartSnapshot");
            ChartSnapshot chart = new ChartSnapshot();
            chart.setQuestion(input.path("question").asText(""));
            chart.setQuestionCategory(input.path("questionCategory").asText(""));
            chart.setDivinationTime(parseDateTime(input.path("divinationTime").asText(null)));
            chart.setMainHexagram(chartNode.path("mainHexagram").asText(""));
            chart.setChangedHexagram(chartNode.path("changedHexagram").asText(""));
            chart.setPalace(chartNode.path("palace").asText(""));
            chart.setPalaceWuXing(chartNode.path("palaceWuXing").asText(""));
            chart.setShi(chartNode.path("shi").isMissingNode() ? null : chartNode.path("shi").asInt());
            chart.setYing(chartNode.path("ying").isMissingNode() ? null : chartNode.path("ying").asInt());
            chart.setUseGod(chartNode.path("useGod").asText(""));
            chart.setRiChen(chartNode.path("riChen").asText(""));
            chart.setYueJian(chartNode.path("yueJian").asText(""));
            chart.setKongWang(readStringList(chartNode.path("kongWang")));
            chart.setLines(buildDefaultLines());
            return chart;
        }

        private static List<RuleHit> toRuleHits(JsonNode ruleHitsNode) {
            if (ruleHitsNode == null || !ruleHitsNode.isArray()) {
                return List.of();
            }
            List<RuleHit> ruleHits = new ArrayList<>();
            for (JsonNode item : ruleHitsNode) {
                RuleHit hit = new RuleHit();
                hit.setHit(true);
                hit.setRuleCode(item.path("ruleCode").asText(""));
                hit.setImpactLevel(item.path("impactLevel").asText(""));
                hit.setScoreDelta(item.path("scoreDelta").isMissingNode() ? null : item.path("scoreDelta").asInt());
                hit.setHitReason(item.path("hitReason").asText(""));
                ruleHits.add(hit);
            }
            return ruleHits;
        }

        private static List<String> readStringList(JsonNode node) {
            if (node == null || !node.isArray()) {
                return List.of();
            }
            List<String> values = new ArrayList<>();
            for (JsonNode item : node) {
                String text = item.asText("");
                if (!text.isBlank()) {
                    values.add(text);
                }
            }
            return values;
        }

        private static List<ChatMessage> buildHistory(int rounds, int repeatLength) {
            if (rounds <= 0) {
                return List.of();
            }
            List<ChatMessage> history = new ArrayList<>();
            UUID sessionId = UUID.randomUUID();
            for (int round = 1; round <= rounds; round++) {
                ChatMessage user = ChatMessage.userMessage(sessionId, "第" + round + "轮用户提问-" + "x".repeat(repeatLength));
                ChatMessage assistant = ChatMessage.assistantMessage(sessionId, "第" + round + "轮AI回答-" + "y".repeat(repeatLength), null, "mock", 10, 10);
                history.add(user);
                history.add(assistant);
            }
            return history;
        }

        private static List<LineInfo> buildDefaultLines() {
            List<LineInfo> lines = new ArrayList<>();
            for (int index = 1; index <= 6; index++) {
                LineInfo line = new LineInfo();
                line.setIndex(index);
                line.setYinYang(index % 2 == 0 ? "阴" : "阳");
                line.setBranch(index % 2 == 0 ? "阴爻" : "阳爻");
                line.setLiuQin(index == 1 ? "兄弟" : "父母");
                line.setLiuShen(index % 2 == 0 ? "玄武" : "青龙");
                line.setMoving(index == 2 || index == 5);
                line.setChangeBranch(index % 2 == 0 ? "亥" : "子");
                lines.add(line);
            }
            return lines;
        }

        private static LocalDateTime parseDateTime(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            return LocalDateTime.parse(value);
        }
    }
}
