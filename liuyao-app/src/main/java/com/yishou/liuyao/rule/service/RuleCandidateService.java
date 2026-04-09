package com.yishou.liuyao.rule.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yishou.liuyao.rule.definition.RuleDefinition;
import com.yishou.liuyao.rule.domain.RuleCandidate;
import com.yishou.liuyao.rule.dto.RuleCandidateDTO;
import com.yishou.liuyao.rule.dto.RuleCandidateSearchRequest;
import com.yishou.liuyao.rule.repository.RuleCandidateRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class RuleCandidateService {

    private final RuleCandidateRepository ruleCandidateRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public RuleCandidateService(RuleCandidateRepository ruleCandidateRepository, JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.ruleCandidateRepository = ruleCandidateRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public Page<RuleCandidateDTO> searchCandidates(RuleCandidateSearchRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Specification<RuleCandidate> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (request.getStatus() != null && !request.getStatus().isBlank()) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            }
            if (request.getCategory() != null && !request.getCategory().isBlank()) {
                predicates.add(cb.equal(root.get("category"), request.getCategory()));
            }
            if (request.getSourceBook() != null && !request.getSourceBook().isBlank()) {
                predicates.add(cb.equal(root.get("sourceBook"), request.getSourceBook()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return ruleCandidateRepository.findAll(spec, pageable).map(this::toDTO);
    }

    @Transactional
    public RuleCandidateDTO reviewCandidate(Long candidateId, String action) {
        RuleCandidate candidate = ruleCandidateRepository.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found: " + candidateId));
                
        if (!"PENDING".equals(candidate.getStatus())) {
            throw new IllegalStateException("Candidate is not in PENDING state: " + candidate.getStatus());
        }

        if ("REJECT".equalsIgnoreCase(action)) {
            candidate.setStatus("REJECTED");
            candidate.setUpdatedAt(LocalDateTime.now());
            return toDTO(ruleCandidateRepository.save(candidate));
        } else if ("PROMOTE".equalsIgnoreCase(action)) {
            candidate.setStatus("PROMOTED");
            candidate.setUpdatedAt(LocalDateTime.now());
            promoteToDefinition(candidate);
            return toDTO(ruleCandidateRepository.save(candidate));
        } else {
            throw new IllegalArgumentException("Invalid action: " + action);
        }
    }

    private void promoteToDefinition(RuleCandidate candidate) {
        // T2-2 Goal: 复用现有 rule_definition 能力，不另建平行正式规则库
        // We write to DB rule_definition as requested in the plan
        String ruleId = "RC_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String ruleCode = "EXTRACTED_" + ruleId;
        
        ObjectNode condition = objectMapper.createObjectNode();
        condition.put("target", "EXTRACTED_" + ruleCode);
        condition.put("operator", "IS_TRUE");
        
        ObjectNode effect = objectMapper.createObjectNode();
        effect.put("score", 0);
        effect.putArray("tags").add("FromBook");
        effect.putArray("conclusionHints").add(candidate.getConditionDesc() + " -> " + candidate.getEffectDirection());
        
        String conditionJson;
        String effectJson;
        try {
            conditionJson = objectMapper.writeValueAsString(condition);
            effectJson = objectMapper.writeValueAsString(effect);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize json", e);
        }

        String sql = "INSERT INTO rule_definition (rule_id, rule_code, name, category, priority, enabled, " +
                     "condition_json, effect_json, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                     
        jdbcTemplate.update(sql, 
                ruleId, 
                ruleCode, 
                candidate.getRuleTitle(), 
                candidate.getCategory(), 
                50, // default priority
                false, // start as disabled since it needs manual tweak in code
                conditionJson,
                effectJson,
                "Extracted from " + candidate.getSourceBook() + ". Evidence: " + candidate.getEvidenceText());
    }

    private RuleCandidateDTO toDTO(RuleCandidate entity) {
        RuleCandidateDTO dto = new RuleCandidateDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
}
