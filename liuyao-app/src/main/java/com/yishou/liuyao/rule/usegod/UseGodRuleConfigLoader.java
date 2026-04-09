package com.yishou.liuyao.rule.usegod;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;

@Component
public class UseGodRuleConfigLoader {

    private static final String DEFAULT_RULE_RESOURCE_VERSION = "v1";

    private final Map<QuestionIntent, UseGodRuleItem> ruleMap = new EnumMap<>(QuestionIntent.class);
    private String version = "unknown";

    @Autowired
    public UseGodRuleConfigLoader(ObjectMapper objectMapper,
                                  @Value("${liuyao.rules.version:" + DEFAULT_RULE_RESOURCE_VERSION + "}") String resourceVersion) {
        load(objectMapper, resourceVersion);
    }

    public UseGodRuleConfigLoader(ObjectMapper objectMapper) {
        this(objectMapper, DEFAULT_RULE_RESOURCE_VERSION);
    }

    private void load(ObjectMapper objectMapper, String resourceVersion) {
        try (InputStream inputStream = new ClassPathResource(resolveResourcePath(resourceVersion)).getInputStream()) {
            UseGodRuleConfig config = objectMapper.readValue(inputStream, UseGodRuleConfig.class);
            version = config.getVersion();
            config.getRules().stream()
                    .sorted(Comparator.comparing(UseGodRuleItem::getPriority))
                    .forEach(item -> ruleMap.put(QuestionIntent.valueOf(item.getIntent()), item));
        } catch (Exception exception) {
            throw new IllegalStateException("加载用神规则配置失败", exception);
        }
    }

    public UseGodRuleItem getRule(QuestionIntent intent) {
        return ruleMap.get(intent);
    }

    public String getVersion() {
        return version;
    }

    private static String resolveResourcePath(String resourceVersion) {
        String normalizedVersion = resourceVersion == null || resourceVersion.isBlank()
                ? DEFAULT_RULE_RESOURCE_VERSION
                : resourceVersion.trim();
        return "rules/" + normalizedVersion + "/use_god_rules.json";
    }
}
