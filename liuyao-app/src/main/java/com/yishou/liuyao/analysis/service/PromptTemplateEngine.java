package com.yishou.liuyao.analysis.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prompt 模板引擎。
 *
 * <p>从 resources/prompts/{version}/*.md 加载模板文件，支持：
 * <ul>
 *   <li>变量插值（{variableName} 语法）</li>
 *   <li>版本化切换（v1 / v2 / ...）</li>
 *   <li>懒加载 + 内存缓存（避免频繁 IO）</li>
 * </ul>
 * </p>
 */
@Component
public class PromptTemplateEngine {

    private static final Logger log = LoggerFactory.getLogger(PromptTemplateEngine.class);

    @Value("${liuyao.prompt.version:v1}")
    private String currentVersion;

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    // ---- Template Keys ----

    public static final String SYSTEM_ANALYST = "system/orchestrated_analyst";
    public static final String CTX_CHART     = "context/chart_context";
    public static final String CTX_RULE      = "context/rule_context";
    public static final String CTX_KNOWLEDGE = "context/knowledge_context";
    public static final String USER_INITIAL  = "user/initial_analysis";
    public static final String USER_FOLLOWUP = "user/follow_up";

    // ---- Public API ----

    /**
     * 加载模板并替换变量。
     *
     * @param templateKey 模板键（如 "system/orchestrated_analyst"）
     * @param variables   变量映射，key 对应模板中的 {key}
     * @return 替换后的完整 Prompt 字符串
     */
    public String render(String templateKey, Map<String, String> variables) {
        String template = loadTemplate(currentVersion, templateKey);
        return interpolate(template, variables);
    }

    /**
     * 加载模板，不做变量替换。
     */
    public String load(String templateKey) {
        return loadTemplate(currentVersion, templateKey);
    }

    // ---- Private ----

    private String loadTemplate(String version, String key) {
        String cacheKey = version + "/" + key;
        return cache.computeIfAbsent(cacheKey, k -> {
            String path = "prompts/" + version + "/" + key + ".md";
            try {
                ClassPathResource resource = new ClassPathResource(path);
                byte[] bytes = resource.getInputStream().readAllBytes();
                return new String(bytes, StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.error("Prompt 模板文件不存在: {}", path);
                throw new IllegalStateException("Prompt模板加载失败: " + path, e);
            }
        });
    }

    private String interpolate(String template, Map<String, String> vars) {
        if (vars == null || vars.isEmpty()) return template;
        String result = template;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }

    /** 用于测试时重置缓存（强制重新加载模板文件） */
    public void clearCache() {
        cache.clear();
    }
}
