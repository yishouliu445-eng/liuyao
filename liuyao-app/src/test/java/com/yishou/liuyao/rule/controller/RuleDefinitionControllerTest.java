package com.yishou.liuyao.rule.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.flyway.enabled=true")
class RuleDefinitionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposePersistedRuleDefinitions() throws Exception {
        mockMvc.perform(get("/api/rules/definitions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.version").isNotEmpty())
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(10)))
                .andExpect(jsonPath("$.data.rules[*].ruleCode").value(hasItem("R019")))
                .andExpect(jsonPath("$.data.rules[?(@.ruleCode=='R019')].name").value(hasItem("用神旺且世旺")))
                .andExpect(jsonPath("$.data.rules[?(@.ruleCode=='R019')].conditionJson").isNotEmpty())
                .andExpect(jsonPath("$.data.rules[?(@.ruleCode=='R019')].effectJson").isNotEmpty())
                .andExpect(jsonPath("$.data.rules[?(@.ruleCode=='R019')].enabled").value(hasItem(true)));
    }
}
