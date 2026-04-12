package com.yishou.liuyao.analysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.yishou.liuyao.analysis.config.LlmProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldParseValidJsonResponse() throws Exception {
        server = createServer("""
                {
                  "choices": [{
                    "message": {
                      "content": "```json\\n{\\"analysis\\":{\\"conclusion\\":\\"测试结论\\"}}\\n```"
                    }
                  }],
                  "usage": {
                    "prompt_tokens": 123,
                    "completion_tokens": 45
                  }
                }
                """, 0);

        LlmClient client = new LlmClient(buildProperties(server, 500, true), new RestTemplateBuilder(), new ObjectMapper());
        LlmClient.LlmResponse response = client.chat(List.of(LlmClient.ChatMessage.user("你好")), null, true);

        assertTrue(response.success());
        assertEquals("qwen-test", response.model());
        assertEquals(168, response.totalTokens());
        assertEquals("测试结论", response.parsedJson().path("analysis").path("conclusion").asText());
    }

    @Test
    void shouldFallbackOnInvalidJsonContent() throws Exception {
        server = createServer("""
                {
                  "choices": [{
                    "message": {
                      "content": "not-a-json-object"
                    }
                  }],
                  "usage": {
                    "prompt_tokens": 1,
                    "completion_tokens": 1
                  }
                }
                """, 0);

        LlmClient client = new LlmClient(buildProperties(server, 500, true), new RestTemplateBuilder(), new ObjectMapper());
        LlmClient.LlmResponse response = client.chat(List.of(LlmClient.ChatMessage.user("你好")), null, true);

        assertFalse(response.success());
        assertNotNull(response.errorMessage());
        assertTrue(response.errorMessage().contains("响应解析失败"));
    }

    @Test
    void shouldReturnFailureOnTimeout() throws Exception {
        server = createServer("""
                {
                  "choices": [{
                    "message": {
                      "content": "{\\"analysis\\":{\\"conclusion\\":\\"延迟返回\\"}}"
                    }
                  }]
                }
                """, 200);

        LlmClient client = new LlmClient(buildProperties(server, 50, true), new RestTemplateBuilder(), new ObjectMapper());
        LlmClient.LlmResponse response = client.chat(List.of(LlmClient.ChatMessage.user("你好")), null, true);

        assertFalse(response.success());
        assertNotNull(response.errorMessage());
    }

    private LlmProperties buildProperties(HttpServer server, int timeoutMs, boolean enabled) {
        LlmProperties properties = new LlmProperties();
        properties.setEnabled(enabled);
        properties.setApiKey("test-key");
        properties.setModel("qwen-test");
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setTimeoutMs(timeoutMs);
        return properties;
    }

    private HttpServer createServer(String responseBody, long delayMs) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.createContext("/chat/completions", exchange -> writeResponse(exchange, responseBody, delayMs));
        httpServer.start();
        return httpServer;
    }

    private void writeResponse(HttpExchange exchange, String responseBody, long delayMs) throws IOException {
        try {
            if (delayMs > 0) {
                Thread.sleep(delayMs);
            }
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(bytes);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted", exception);
        } finally {
            exchange.close();
        }
    }
}
