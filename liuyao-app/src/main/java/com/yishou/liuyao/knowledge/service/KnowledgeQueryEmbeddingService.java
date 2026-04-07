package com.yishou.liuyao.knowledge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class KnowledgeQueryEmbeddingService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final Duration timeout;

    public KnowledgeQueryEmbeddingService(ObjectMapper objectMapper,
                                          @Value("${KNOWLEDGE_EMBEDDING_API_KEY:${DASHSCOPE_API_KEY:}}") String apiKey,
                                          @Value("${KNOWLEDGE_EMBEDDING_BASE_URL:https://dashscope.aliyuncs.com/compatible-mode/v1}") String baseUrl,
                                          @Value("${KNOWLEDGE_EMBEDDING_MODEL:text-embedding-v4}") String model,
                                          @Value("${KNOWLEDGE_EMBEDDING_TIMEOUT_SECONDS:30}") int timeoutSeconds) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        this.model = model == null ? "" : model.trim();
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    public List<Double> embed(String text) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("Knowledge embedding API key is not configured");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("queryText must not be blank");
        }
        try {
            String requestBody = objectMapper.writeValueAsString(
                    java.util.Map.of("model", model, "input", text)
            );
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl.replaceAll("/+$", "") + "/embeddings"))
                    .timeout(timeout)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Embedding request failed with status " + response.statusCode() + ": " + response.body());
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode embeddingNode = root.path("data").path(0).path("embedding");
            if (!embeddingNode.isArray() || embeddingNode.isEmpty()) {
                throw new IllegalStateException("Embedding response missing data[0].embedding");
            }
            List<Double> values = new ArrayList<>();
            for (JsonNode node : embeddingNode) {
                values.add(node.asDouble());
            }
            return values;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Embedding request interrupted", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Embedding request failed", exception);
        }
    }
}
