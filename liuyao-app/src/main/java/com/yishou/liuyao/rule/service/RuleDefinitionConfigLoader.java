package com.yishou.liuyao.rule.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yishou.liuyao.rule.definition.RuleDefinition;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RuleDefinitionConfigLoader {

    private final List<RuleDefinition> enabledRules;
    private final Map<String, RuleDefinition> ruleMap;
    private final Map<String, RuleDefinition> ruleCodeMap;
    private final String version;

    public RuleDefinitionConfigLoader(ObjectMapper objectMapper) {
        try (InputStream inputStream = new ClassPathResource("rules/rule_definitions.json").getInputStream()) {
            RuleDefinitionConfig config = objectMapper.readValue(inputStream, RuleDefinitionConfig.class);
            this.version = config.getVersion();
            this.enabledRules = config.getRules().stream()
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
}
