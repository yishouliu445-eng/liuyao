package com.yishou.liuyao.rule.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yishou.liuyao.rule.definition.RuleDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleDefinitionConfigLoaderTest {

    @Test
    void shouldLoadSplitRuleDefinitionFilesFromVersionDirectory() {
        assertTrue(new ClassPathResource("rules/v1/yongshen_rules.json").exists());
        assertTrue(new ClassPathResource("rules/v1/shi_rules.json").exists());
        assertTrue(new ClassPathResource("rules/v1/moving_rules.json").exists());
        assertTrue(new ClassPathResource("rules/v1/composite_rules.json").exists());

        RuleDefinitionConfigLoader loader = new RuleDefinitionConfigLoader(new ObjectMapper());

        assertNotNull(loader.getRule("R005"));
        assertNotNull(loader.getRule("R014"));
        assertNotNull(loader.getRule("R028"));
        assertNotNull(loader.getRule("R019"));
    }

    @Test
    void shouldLoadCompositeRuleDefinitionsFromJson() {
        RuleDefinitionConfigLoader loader = new RuleDefinitionConfigLoader(new ObjectMapper());

        List<RuleDefinition> definitions = loader.getEnabledRules();

        assertEquals("v1", loader.getVersion());
        assertFalse(definitions.isEmpty());
        assertTrue(definitions.stream().map(RuleDefinition::getId).toList()
                .containsAll(List.of("R003", "R004", "R009", "R013", "R014", "R015", "R016", "R018", "R019", "R020", "R021", "R022", "R023", "R024", "R025", "R026", "R027", "R028", "R029", "R030", "R031", "R032")));

        RuleDefinition compositeRule = loader.getRule("R019");
        assertNotNull(compositeRule);
        assertEquals("用神旺且世旺", compositeRule.getName());
        assertNotNull(compositeRule.getCondition());
        assertEquals(2, compositeRule.getCondition().getAllOf().size());
        assertEquals(3, compositeRule.getEffect().getScore());
        assertTrue(compositeRule.getEffect().getTags().contains("双强"));

        RuleDefinition useGodMovingRule = loader.getRule("R021");
        assertNotNull(useGodMovingRule);
        assertEquals("USE_GOD_MOVING", useGodMovingRule.getCondition().getTarget());

        RuleDefinition nearShiRule = loader.getRule("R022");
        assertNotNull(nearShiRule);
        assertEquals("LESS_THAN_OR_EQUALS", nearShiRule.getCondition().getOperator());

        RuleDefinition monthBreakRule = loader.getRule("R025");
        assertNotNull(monthBreakRule);
        assertEquals("USE_GOD_MONTH_BREAK", monthBreakRule.getCondition().getTarget());

        RuleDefinition dayBreakRule = loader.getRule("R026");
        assertNotNull(dayBreakRule);
        assertEquals("USE_GOD_DAY_BREAK", dayBreakRule.getCondition().getTarget());

        RuleDefinition shiYingDistanceRule = loader.getRule("R027");
        assertNotNull(shiYingDistanceRule);
        assertEquals("SHI_YING_DISTANCE", shiYingDistanceRule.getCondition().getTarget());
        assertEquals("GREATER_THAN_OR_EQUALS", shiYingDistanceRule.getCondition().getOperator());

        RuleDefinition movingChongShiRule = loader.getRule("R028");
        assertNotNull(movingChongShiRule);
        assertEquals("HAS_MOVING_CHONG_SHI", movingChongShiRule.getCondition().getTarget());

        RuleDefinition movingChongUseGodRule = loader.getRule("R029");
        assertNotNull(movingChongUseGodRule);
        assertEquals("HAS_MOVING_CHONG_USE_GOD", movingChongUseGodRule.getCondition().getTarget());

        RuleDefinition ruMuRule = loader.getRule("R030");
        assertNotNull(ruMuRule);
        assertEquals("USE_GOD_RU_MU", ruMuRule.getCondition().getTarget());

        RuleDefinition chongKaiRule = loader.getRule("R031");
        assertNotNull(chongKaiRule);
        assertEquals("USE_GOD_CHONG_KAI", chongKaiRule.getCondition().getTarget());

        RuleDefinition chongSanRule = loader.getRule("R032");
        assertNotNull(chongSanRule);
        assertEquals("USE_GOD_CHONG_SAN", chongSanRule.getCondition().getTarget());
    }
}
