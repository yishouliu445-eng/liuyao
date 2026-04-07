package com.yishou.liuyao.rule.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yishou.liuyao.rule.definition.RuleDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleDefinitionConfigLoaderTest {

    @Test
    void shouldLoadCompositeRuleDefinitionsFromJson() {
        RuleDefinitionConfigLoader loader = new RuleDefinitionConfigLoader(new ObjectMapper());

        List<RuleDefinition> definitions = loader.getEnabledRules();

        assertFalse(definitions.isEmpty());
        assertTrue(definitions.stream().map(RuleDefinition::getId).toList()
                .containsAll(List.of("R003", "R004", "R009", "R013", "R014", "R015", "R016", "R018", "R019", "R020")));

        RuleDefinition compositeRule = loader.getRule("R019");
        assertNotNull(compositeRule);
        assertEquals("用神旺且世旺", compositeRule.getName());
        assertNotNull(compositeRule.getCondition());
        assertEquals(2, compositeRule.getCondition().getAllOf().size());
        assertEquals(3, compositeRule.getEffect().getScore());
        assertTrue(compositeRule.getEffect().getTags().contains("双强"));
    }
}
