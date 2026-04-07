package com.yishou.liuyao.rule.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yishou.liuyao.rule.definition.RuleDefinition;
import com.yishou.liuyao.rule.domain.RuleDefinitionEntity;
import com.yishou.liuyao.rule.dto.RuleDefinitionDTO;
import com.yishou.liuyao.rule.dto.RuleDefinitionListResponse;
import com.yishou.liuyao.rule.repository.RuleDefinitionRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RuleDefinitionCatalogService {

    private static final Logger log = LoggerFactory.getLogger(RuleDefinitionCatalogService.class);

    private final RuleDefinitionRepository ruleDefinitionRepository;
    private final RuleDefinitionConfigLoader ruleDefinitionConfigLoader;
    private final ObjectMapper objectMapper;

    public RuleDefinitionCatalogService(RuleDefinitionRepository ruleDefinitionRepository,
                                        RuleDefinitionConfigLoader ruleDefinitionConfigLoader,
                                        ObjectMapper objectMapper) {
        this.ruleDefinitionRepository = ruleDefinitionRepository;
        this.ruleDefinitionConfigLoader = ruleDefinitionConfigLoader;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void syncDefinitionsFromConfig() {
        try {
            doSyncDefinitionsFromConfig();
            log.info("规则定义同步完成: version={}, total={}",
                    ruleDefinitionConfigLoader.getVersion(),
                    ruleDefinitionRepository.count());
        } catch (RuntimeException exception) {
            log.warn("跳过规则定义同步，通常是当前上下文未初始化规则定义表: {}", exception.getMessage());
        }
    }

    @Transactional
    protected void doSyncDefinitionsFromConfig() {
        for (RuleDefinition definition : ruleDefinitionConfigLoader.getEnabledRules()) {
            RuleDefinitionEntity entity = ruleDefinitionRepository.findByRuleCode(resolveRuleCode(definition))
                    .orElseGet(RuleDefinitionEntity::new);
            entity.setRuleId(definition.getId());
            entity.setRuleCode(resolveRuleCode(definition));
            entity.setName(definition.getName());
            entity.setCategory(definition.getCategory());
            entity.setPriority(definition.getPriority());
            entity.setEnabled(!Boolean.FALSE.equals(definition.getEnabled()));
            entity.setVersion(ruleDefinitionConfigLoader.getVersion());
            entity.setConditionJson(writeJson(definition.getCondition()));
            entity.setEffectJson(writeJson(definition.getEffect()));
            entity.setDescription(definition.getDescription());
            ruleDefinitionRepository.save(entity);
        }
    }

    @Transactional(readOnly = true)
    public RuleDefinitionListResponse listDefinitions() {
        List<RuleDefinitionDTO> definitions = ruleDefinitionRepository.findAllByOrderByPriorityAscRuleCodeAsc().stream()
                .map(this::toDto)
                .toList();
        RuleDefinitionListResponse response = new RuleDefinitionListResponse();
        response.setVersion(ruleDefinitionConfigLoader.getVersion());
        response.setTotal(definitions.size());
        response.setRules(definitions);
        return response;
    }

    private RuleDefinitionDTO toDto(RuleDefinitionEntity entity) {
        RuleDefinitionDTO dto = new RuleDefinitionDTO();
        dto.setRuleId(entity.getRuleId());
        dto.setRuleCode(entity.getRuleCode());
        dto.setName(entity.getName());
        dto.setCategory(entity.getCategory());
        dto.setPriority(entity.getPriority());
        dto.setEnabled(entity.getEnabled());
        dto.setVersion(entity.getVersion());
        dto.setConditionJson(entity.getConditionJson());
        dto.setEffectJson(entity.getEffectJson());
        dto.setDescription(entity.getDescription());
        return dto;
    }

    private String resolveRuleCode(RuleDefinition definition) {
        return definition.getRuleCode() == null || definition.getRuleCode().isBlank()
                ? definition.getId()
                : definition.getRuleCode();
    }

    private String writeJson(Object value) {
        if (value == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("序列化规则定义失败", exception);
        }
    }
}
