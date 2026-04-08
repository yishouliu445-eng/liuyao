package com.yishou.liuyao.analysis.service;

import com.yishou.liuyao.analysis.dto.AnalysisContextDTO;
import com.yishou.liuyao.analysis.dto.RuleCategorySummaryDTO;
import com.yishou.liuyao.analysis.dto.StructuredAnalysisResultDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisServiceTest {

    @Test
    void shouldGenerateReadableStructuredAnalysisText() {
        AnalysisContextDTO context = new AnalysisContextDTO();
        context.setQuestion("我下个月工资会不会上涨");
        context.setQuestionCategory("收入");
        context.setUseGod("妻财");
        context.setMainHexagram("山火贲");
        context.setChangedHexagram("风山渐");
        context.setRuleCount(6);
        context.setRuleCodes(List.of("R003", "R010", "USE_GOD_STRENGTH"));
        context.setKnowledgeSnippets(List.of("《增删卜易》 用神宜旺相，不宜休囚。"));

        StructuredAnalysisResultDTO structuredResult = new StructuredAnalysisResultDTO();
        structuredResult.setSummary("本卦山火贲，围绕用神妻财共命中6条规则。");
        structuredResult.setResultLevel("NEUTRAL");
        structuredResult.setEffectiveResultLevel("GOOD");
        structuredResult.setEffectiveScore(3);
        structuredResult.setTags(List.of("有利", "主动"));
        structuredResult.setEffectiveRuleCodes(List.of("R003", "R010"));
        structuredResult.setSuppressedRuleCodes(List.of("R005"));
        RuleCategorySummaryDTO categorySummaryDTO = new RuleCategorySummaryDTO();
        categorySummaryDTO.setCategory("YONGSHEN_STATE");
        categorySummaryDTO.setHitCount(3);
        categorySummaryDTO.setScore(2);
        categorySummaryDTO.setEffectiveHitCount(2);
        categorySummaryDTO.setEffectiveScore(3);
        structuredResult.setCategorySummaries(List.of(categorySummaryDTO));
        context.setStructuredResult(structuredResult);

        String analysis = new AnalysisService().analyze(context);

        assertTrue(analysis.contains("问收入"));
        assertTrue(analysis.contains("卦象概览"));
        assertTrue(analysis.contains("用神判断"));
        assertTrue(analysis.contains("重点看财务回报是否能落袋"));
        assertTrue(analysis.contains("风险提示"));
        assertTrue(analysis.contains("结论建议"));
        assertTrue(analysis.contains("下一步建议"));
        assertTrue(analysis.contains("以妻财为用神"));
        assertTrue(analysis.contains("山火贲"));
        assertTrue(analysis.contains("风山渐"));
        assertTrue(analysis.contains("有效评分3"));
        assertTrue(analysis.contains("当前主导信号显示用神一侧更有承接力"));
        assertTrue(analysis.contains("《增删卜易》"));
        assertTrue(analysis.contains("资料片段中的“《增删卜易》"));
    }

    @Test
    void shouldRenderSecondBatchCategoriesWithSpecificHints() {
        AnalysisContextDTO context = new AnalysisContextDTO();
        context.setQuestion("这次投资能赚钱吗");
        context.setQuestionCategory("财运");
        context.setUseGod("妻财");
        context.setMainHexagram("山火贲");

        StructuredAnalysisResultDTO structuredResult = new StructuredAnalysisResultDTO();
        structuredResult.setEffectiveResultLevel("GOOD");
        structuredResult.setEffectiveScore(2);
        context.setStructuredResult(structuredResult);

        String analysis = new AnalysisService().analyze(context);

        assertTrue(analysis.contains("财运层面"));
        assertTrue(analysis.contains("投资回报"));
        assertTrue(analysis.contains("财务回报是否能落袋"));
    }

    @Test
    void shouldRenderDifferentHintsForRealEstateAndLawsuit() {
        AnalysisContextDTO realEstate = new AnalysisContextDTO();
        realEstate.setQuestion("这次买房手续能办下来吗");
        realEstate.setQuestionCategory("房产");
        realEstate.setUseGod("父母");
        realEstate.setMainHexagram("山火贲");
        StructuredAnalysisResultDTO realEstateResult = new StructuredAnalysisResultDTO();
        realEstateResult.setEffectiveResultLevel("GOOD");
        realEstateResult.setEffectiveScore(2);
        realEstate.setStructuredResult(realEstateResult);

        AnalysisContextDTO lawsuit = new AnalysisContextDTO();
        lawsuit.setQuestion("这场官司能不能顺一点");
        lawsuit.setQuestionCategory("官司");
        lawsuit.setUseGod("官鬼");
        lawsuit.setMainHexagram("山火贲");
        StructuredAnalysisResultDTO lawsuitResult = new StructuredAnalysisResultDTO();
        lawsuitResult.setEffectiveResultLevel("BAD");
        lawsuitResult.setEffectiveScore(-2);
        lawsuit.setStructuredResult(lawsuitResult);

        String realEstateAnalysis = new AnalysisService().analyze(realEstate);
        String lawsuitAnalysis = new AnalysisService().analyze(lawsuit);

        assertTrue(realEstateAnalysis.contains("重点看手续、文书、住处安排与成交推进"));
        assertTrue(realEstateAnalysis.contains("时间安排和文书细节"));
        assertTrue(realEstateAnalysis.contains("继续推进手续、过户和文书确认"));
        assertTrue(lawsuitAnalysis.contains("重点看牵制、压力来源和主动权归属"));
        assertTrue(lawsuitAnalysis.contains("稳住证据和节奏"));
        assertTrue(lawsuitAnalysis.contains("先补强证据与节奏控制"));
    }

    @Test
    void shouldRenderDifferentObservationLeadsForHighFrequencyCategories() {
        StructuredAnalysisResultDTO structuredResult = new StructuredAnalysisResultDTO();
        structuredResult.setEffectiveResultLevel("NEUTRAL");
        structuredResult.setEffectiveScore(1);

        AnalysisContextDTO job = buildCategoryContext("求职", "官鬼", structuredResult);
        AnalysisContextDTO emotion = buildCategoryContext("感情", "应爻", structuredResult);
        AnalysisContextDTO cooperation = buildCategoryContext("合作", "应爻", structuredResult);
        AnalysisContextDTO health = buildCategoryContext("健康", "官鬼", structuredResult);
        AnalysisContextDTO travel = buildCategoryContext("出行", "父母", structuredResult);

        String jobAnalysis = new AnalysisService().analyze(job);
        String emotionAnalysis = new AnalysisService().analyze(emotion);
        String cooperationAnalysis = new AnalysisService().analyze(cooperation);
        String healthAnalysis = new AnalysisService().analyze(health);
        String travelAnalysis = new AnalysisService().analyze(travel);

        assertTrue(jobAnalysis.contains("录用链条是否继续往前推"));
        assertTrue(jobAnalysis.contains("岗位事项已经开始推动"));
        assertTrue(emotionAnalysis.contains("互动与回应"));
        assertTrue(emotionAnalysis.contains("关系层已经被触动"));
        assertTrue(cooperationAnalysis.contains("对方配合度"));
        assertTrue(cooperationAnalysis.contains("合作条件和对方态度已经进入变化阶段"));
        assertTrue(healthAnalysis.contains("病象与恢复信号"));
        assertTrue(healthAnalysis.contains("恢复与病象都在变化"));
        assertTrue(travelAnalysis.contains("行程推进"));
        assertTrue(travelAnalysis.contains("行程与迁移事项已经动起来"));
    }

    @Test
    void shouldRenderSpecificRiskAndActionForExamAndRelationshipCategories() {
        AnalysisContextDTO exam = new AnalysisContextDTO();
        exam.setQuestion("这次考试能不能顺利通过");
        exam.setQuestionCategory("考试");
        exam.setUseGod("父母");
        exam.setMainHexagram("山火贲");
        exam.setChangedHexagram("风山渐");
        exam.setRuleCodes(List.of("USE_GOD_EMPTY", "R010"));
        exam.setKnowledgeSnippets(List.of("[增删卜易] 空亡之时，主心神不定，临场易失次序。"));
        StructuredAnalysisResultDTO examResult = new StructuredAnalysisResultDTO();
        examResult.setEffectiveResultLevel("BAD");
        examResult.setEffectiveScore(-1);
        examResult.setEffectiveRuleCodes(List.of("USE_GOD_EMPTY"));
        exam.setStructuredResult(examResult);

        AnalysisContextDTO relationship = new AnalysisContextDTO();
        relationship.setQuestion("这段关系还有没有缓和空间");
        relationship.setQuestionCategory("人际");
        relationship.setUseGod("应爻");
        relationship.setMainHexagram("山火贲");
        relationship.setChangedHexagram("风山渐");
        relationship.setRuleCodes(List.of("SHI_YING_RELATION", "R014"));
        relationship.setKnowledgeSnippets(List.of("[卜筮正宗] 世应相接，则看对方回应与来往节度。"));
        StructuredAnalysisResultDTO relationshipResult = new StructuredAnalysisResultDTO();
        relationshipResult.setEffectiveResultLevel("NEUTRAL");
        relationshipResult.setEffectiveScore(1);
        relationshipResult.setEffectiveRuleCodes(List.of("SHI_YING_RELATION", "R014"));
        relationship.setStructuredResult(relationshipResult);

        String examAnalysis = new AnalysisService().analyze(exam);
        String relationshipAnalysis = new AnalysisService().analyze(relationship);

        assertTrue(examAnalysis.contains("考试层面"));
        assertTrue(examAnalysis.contains("临场"));
        assertTrue(examAnalysis.contains("先把复习节奏和临场准备稳住"));
        assertTrue(relationshipAnalysis.contains("人际层面"));
        assertTrue(relationshipAnalysis.contains("对方态度"));
        assertTrue(relationshipAnalysis.contains("双方互动仍是关键变量"));
        assertTrue(relationshipAnalysis.contains("先看对方是否释放更明确的回应"));
    }

    @Test
    void shouldRenderSpecificRiskAndActionForRemainingTaxonomyCategories() {
        AnalysisContextDTO growth = buildSimpleContext("成长", "父母", "GOOD", 2);
        AnalysisContextDTO pressure = buildSimpleContext("压力", "官鬼", "BAD", -2);
        AnalysisContextDTO marriage = buildSimpleContext("婚姻", "应爻", "GOOD", 2);
        AnalysisContextDTO reunion = buildSimpleContext("复合", "应爻", "NEUTRAL", 1);
        AnalysisContextDTO promotion = buildSimpleContext("升职", "官鬼", "GOOD", 2);
        AnalysisContextDTO transfer = buildSimpleContext("调岗", "官鬼", "BAD", -1);

        String growthAnalysis = new AnalysisService().analyze(growth);
        String pressureAnalysis = new AnalysisService().analyze(pressure);
        String marriageAnalysis = new AnalysisService().analyze(marriage);
        String reunionAnalysis = new AnalysisService().analyze(reunion);
        String promotionAnalysis = new AnalysisService().analyze(promotion);
        String transferAnalysis = new AnalysisService().analyze(transfer);

        assertTrue(growthAnalysis.contains("成长层面"));
        assertTrue(growthAnalysis.contains("投入"));
        assertTrue(growthAnalysis.contains("继续把有效投入稳定下来"));

        assertTrue(pressureAnalysis.contains("压力层面"));
        assertTrue(pressureAnalysis.contains("牵制"));
        assertTrue(pressureAnalysis.contains("先把最重的牵制点拆开"));

        assertTrue(marriageAnalysis.contains("婚姻层面"));
        assertTrue(marriageAnalysis.contains("稳定"));
        assertTrue(marriageAnalysis.contains("继续看关系是否能往稳定和落定推进"));

        assertTrue(reunionAnalysis.contains("复合层面"));
        assertTrue(reunionAnalysis.contains("重新连接"));
        assertTrue(reunionAnalysis.contains("先看旧联系是否继续回温"));

        assertTrue(promotionAnalysis.contains("升职层面"));
        assertTrue(promotionAnalysis.contains("职位抬升"));
        assertTrue(promotionAnalysis.contains("继续推动关键汇报和成果呈现"));

        assertTrue(transferAnalysis.contains("调岗层面"));
        assertTrue(transferAnalysis.contains("岗位变动"));
        assertTrue(transferAnalysis.contains("先稳住当前节奏"));
    }

    @Test
    void shouldFallbackWhenKnowledgeSnippetsAreMissing() {
        AnalysisContextDTO context = new AnalysisContextDTO();
        context.setQuestion("这次合作能不能顺利");
        context.setQuestionCategory("合作");
        context.setUseGod("应爻");
        context.setMainHexagram("山火贲");
        context.setChangedHexagram("风山渐");
        context.setRuleCodes(List.of("SHI_YING_RELATION"));

        StructuredAnalysisResultDTO structuredResult = new StructuredAnalysisResultDTO();
        structuredResult.setEffectiveResultLevel("NEUTRAL");
        structuredResult.setEffectiveScore(1);
        context.setStructuredResult(structuredResult);

        String analysis = new AnalysisService().analyze(context);

        assertTrue(analysis.contains("下一步建议"));
        assertTrue(analysis.contains("当前暂未检索到直接匹配资料"));
        assertTrue(analysis.contains("先以盘面结构和规则信号为主"));
        assertTrue(analysis.contains("下一步建议"));
    }

    @Test
    void shouldPickDifferentKnowledgeSnippetsForRiskAndAction() {
        AnalysisContextDTO context = new AnalysisContextDTO();
        context.setQuestion("这次找工作能不能顺利拿到 offer");
        context.setQuestionCategory("求职");
        context.setUseGod("官鬼");
        context.setMainHexagram("山火贲");
        context.setChangedHexagram("风山渐");
        context.setRuleCodes(List.of("R010", "R011", "USE_GOD_EMPTY"));
        context.setKnowledgeSnippets(List.of(
                "[增删卜易] 用神空亡，主事体反复，宜防拖延与落空。",
                "[卜筮正宗] 求职之事宜继续推进文书与回访，主动跟进则更易见回应。"));

        StructuredAnalysisResultDTO structuredResult = new StructuredAnalysisResultDTO();
        structuredResult.setEffectiveResultLevel("BAD");
        structuredResult.setEffectiveScore(-2);
        context.setStructuredResult(structuredResult);

        String analysis = new AnalysisService().analyze(context);

        assertTrue(analysis.contains("风险提示："));
        assertTrue(analysis.contains("下一步建议："));
        assertTrue(analysis.contains("增删卜易中的“用神空亡"));
        assertTrue(analysis.contains("卜筮正宗中的“求职之事宜继续推进"));
    }

    @Test
    void shouldPreferRuleSpecificKnowledgeWhenBuildingActionSuggestion() {
        AnalysisContextDTO context = new AnalysisContextDTO();
        context.setQuestion("这次收入会不会涨");
        context.setQuestionCategory("收入");
        context.setUseGod("妻财");
        context.setMainHexagram("山火贲");
        context.setChangedHexagram("风山渐");
        context.setRuleCodes(List.of("USE_GOD_STRENGTH"));
        context.setKnowledgeSnippets(List.of(
                "[黄金策] 用神旺相则财易得，休囚则财难就。",
                "[增删卜易] 继续观察后续变化即可。"));

        StructuredAnalysisResultDTO structuredResult = new StructuredAnalysisResultDTO();
        structuredResult.setEffectiveResultLevel("GOOD");
        structuredResult.setEffectiveScore(2);
        context.setStructuredResult(structuredResult);

        String analysis = new AnalysisService().analyze(context);

        assertTrue(analysis.contains("下一步建议："));
        assertTrue(analysis.contains("黄金策中的“用神旺相则财易得"));
    }

    @Test
    void shouldPreferEffectiveRuleCodesOverRawRuleCodesWhenSelectingKnowledge() {
        AnalysisContextDTO context = new AnalysisContextDTO();
        context.setQuestion("这次收入后面到底偏强还是偏空");
        context.setQuestionCategory("收入");
        context.setUseGod("妻财");
        context.setMainHexagram("山火贲");
        context.setChangedHexagram("风山渐");
        context.setRuleCodes(List.of("USE_GOD_EMPTY", "USE_GOD_STRENGTH"));
        context.setKnowledgeSnippets(List.of(
                "[增删卜易] 用神空亡，多主事体拖延反复。",
                "[黄金策] 用神旺相则财易得，休囚则财难就。"));

        StructuredAnalysisResultDTO structuredResult = new StructuredAnalysisResultDTO();
        structuredResult.setEffectiveResultLevel("GOOD");
        structuredResult.setEffectiveScore(2);
        structuredResult.setEffectiveRuleCodes(List.of("USE_GOD_STRENGTH"));
        structuredResult.setSuppressedRuleCodes(List.of("USE_GOD_EMPTY"));
        context.setStructuredResult(structuredResult);

        String analysis = new AnalysisService().analyze(context);

        assertTrue(analysis.contains("下一步建议："));
        assertTrue(analysis.contains("黄金策中的“用神旺相则财易得"));
    }

    private AnalysisContextDTO buildCategoryContext(String questionCategory,
                                                    String useGod,
                                                    StructuredAnalysisResultDTO structuredResult) {
        AnalysisContextDTO context = new AnalysisContextDTO();
        context.setQuestion("测试问题");
        context.setQuestionCategory(questionCategory);
        context.setUseGod(useGod);
        context.setMainHexagram("山火贲");
        context.setChangedHexagram("风山渐");
        context.setRuleCount(3);
        context.setRuleCodes(List.of("R010", "SHI_YING_RELATION"));
        context.setStructuredResult(structuredResult);
        return context;
    }

    private AnalysisContextDTO buildSimpleContext(String questionCategory,
                                                  String useGod,
                                                  String effectiveResultLevel,
                                                  int effectiveScore) {
        AnalysisContextDTO context = new AnalysisContextDTO();
        context.setQuestion("测试问题");
        context.setQuestionCategory(questionCategory);
        context.setUseGod(useGod);
        context.setMainHexagram("山火贲");
        context.setChangedHexagram("风山渐");
        StructuredAnalysisResultDTO structuredResult = new StructuredAnalysisResultDTO();
        structuredResult.setEffectiveResultLevel(effectiveResultLevel);
        structuredResult.setEffectiveScore(effectiveScore);
        context.setStructuredResult(structuredResult);
        return context;
    }
}
