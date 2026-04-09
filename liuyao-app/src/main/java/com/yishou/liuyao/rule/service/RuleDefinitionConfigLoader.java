package com.yishou.liuyao.rule.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yishou.liuyao.rule.definition.RuleDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RuleDefinitionConfigLoader {

    private static final String DEFAULT_RULE_RESOURCE_VERSION = "v1";

    private final List<RuleDefinition> enabledRules;
    private final Map<String, RuleDefinition> ruleMap;
    private final Map<String, RuleDefinition> ruleCodeMap;
    private final String version;

    @Autowired
    public RuleDefinitionConfigLoader(ObjectMapper objectMapper,
                                      @Value("${liuyao.rules.version:" + DEFAULT_RULE_RESOURCE_VERSION + "}") String resourceVersion) {
        try {
            RuleDefinitionConfig config = readConfig(objectMapper, resolveResourcePath(resourceVersion));
            this.version = config.getVersion();
            this.enabledRules = resolveRules(objectMapper, resourceVersion, config).stream()
                    .filter(rule -> !Boolean.FALSE.equals(rule.getEnabled()))
                    .sorted(Comparator.comparing(RuleDefinition::getPriority))
                    .toList();
            this.ruleMap = enabledRules.stream()
                    .collect(Collectors.toMap(RuleDefinition::getId, Function.identity()));
            this.ruleCodeMap = enabledRules.stream()
                    .filter(rule -> rule.getRuleCode() != null && !rule.getRuleCode().isBlank())
                    .collect(Collectors.toMap(RuleDefinition::getRuleCode, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        } catch (Exception exception) {
            throw new IllegalStateException("加载规则定义配置失败", exception);
        }
    }

    public RuleDefinitionConfigLoader(ObjectMapper objectMapper) {
        this(objectMapper, DEFAULT_RULE_RESOURCE_VERSION);
    }

    public List<RuleDefinition> getEnabledRules() {
        return enabledRules;
    }

    public RuleDefinition getRule(String ruleId) {
        return ruleMap.get(ruleId);
    }

    public RuleDefinition getRuleByCode(String ruleCode) {
        return ruleCodeMap.get(ruleCode);
    }

    public String getVersion() {
        return version;
    }

    private List<RuleDefinition> resolveRules(ObjectMapper objectMapper,
                                              String resourceVersion,
                                              RuleDefinitionConfig config) {
        List<RuleDefinition> rules = new ArrayList<>(config.getRules());
        for (String file : config.getFiles()) {
            RuleDefinitionConfig nestedConfig = readConfig(objectMapper, resolveNestedResourcePath(resourceVersion, file));
            rules.addAll(nestedConfig.getRules());
        }
        return rules;
    }

    private RuleDefinitionConfig readConfig(ObjectMapper objectMapper, String resourcePath) {
        try (InputStream inputStream = new ClassPathResource(resourcePath).getInputStream()) {
            return objectMapper.readValue(inputStream, RuleDefinitionConfig.class);
        } catch (Exception exception) {
            throw new IllegalStateException("加载规则定义配置失败", exception);
        }
    }

    private static String resolveResourcePath(String resourceVersion) {
        String normalizedVersion = resourceVersion == null || resourceVersion.isBlank()
                ? DEFAULT_RULE_RESOURCE_VERSION
                : resourceVersion.trim();
        return "rules/" + normalizedVersion + "/rule_definitions.json";
    }

    private static String resolveNestedResourcePath(String resourceVersion, String file) {
        String normalizedVersion = resourceVersion == null || resourceVersion.isBlank()
                ? DEFAULT_RULE_RESOURCE_VERSION
                : resourceVersion.trim();
        return "rules/" + normalizedVersion + "/" + file;
    }
}
