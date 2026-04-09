package com.yishou.liuyao.rule.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class RuleResourceMetadataLoader {

    private static final String DEFAULT_RULE_RESOURCE_VERSION = "v1";

    private final RuleResourceMetadata metadata;

    @Autowired
    public RuleResourceMetadataLoader(ObjectMapper objectMapper,
                                      @Value("${liuyao.rules.version:" + DEFAULT_RULE_RESOURCE_VERSION + "}") String resourceVersion) {
        this.metadata = loadMetadata(objectMapper, resourceVersion);
    }

    public RuleResourceMetadata getMetadata() {
        return metadata;
    }

    private RuleResourceMetadata loadMetadata(ObjectMapper objectMapper, String resourceVersion) {
        String normalizedVersion = resourceVersion == null || resourceVersion.isBlank()
                ? DEFAULT_RULE_RESOURCE_VERSION
                : resourceVersion.trim();
        String resourcePath = "rules/" + normalizedVersion + "/metadata.json";
        try (InputStream inputStream = new ClassPathResource(resourcePath).getInputStream()) {
            return objectMapper.readValue(inputStream, RuleResourceMetadata.class);
        } catch (Exception exception) {
            throw new IllegalStateException("加载规则资源元数据失败", exception);
        }
    }
}
