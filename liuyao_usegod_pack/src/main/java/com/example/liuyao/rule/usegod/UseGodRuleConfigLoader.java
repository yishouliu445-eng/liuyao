package com.example.liuyao.rule.usegod;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;

@Component
public class UseGodRuleConfigLoader {

    private final Map<QuestionIntent, UseGodRuleItem> ruleMap = new EnumMap<>(QuestionIntent.class);
    private String version = "unknown";

    public UseGodRuleConfigLoader(ObjectMapper objectMapper) {
        load(objectMapper);
    }

    private void load(ObjectMapper objectMapper) {
        try (InputStream inputStream =
                     new ClassPathResource("rules/use_god_rules.json").getInputStream()) {
            UseGodRuleConfig config = objectMapper.readValue(inputStream, UseGodRuleConfig.class);
            this.version = config.getVersion();

            config.getRules().stream()
                    .sorted(Comparator.comparing(UseGodRuleItem::getPriority))
                    .forEach(item -> {
                        QuestionIntent intent = QuestionIntent.valueOf(item.getIntent());
                        ruleMap.put(intent, item);
                    });
        } catch (Exception e) {
            throw new IllegalStateException("加载用神规则配置失败", e);
        }
    }

    public UseGodRuleItem getRule(QuestionIntent intent) {
        return ruleMap.get(intent);
    }

    public String getVersion() {
        return version;
    }
}
