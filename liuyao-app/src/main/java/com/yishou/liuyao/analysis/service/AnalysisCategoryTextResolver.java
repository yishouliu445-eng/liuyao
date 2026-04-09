package com.yishou.liuyao.analysis.service;

import com.yishou.liuyao.analysis.dto.AnalysisContextDTO;

import java.util.List;

public class AnalysisCategoryTextResolver {

    public String renderUseGodFocus(String questionCategory, String useGod) {
        return switch (questionCategory == null ? "" : questionCategory) {
            case "收入", "财运" -> "重点看财务回报是否能落袋，以及中途有没有折损。";
            case "求职" -> "重点看机会是否成形，以及录用流程能否真正推进。";
            case "工作", "升职", "调岗" -> "重点看岗位推进、外部反馈与最终落实。";
            case "感情", "婚姻", "复合" -> "重点看双方回应、关系牵引和后续是否继续靠近。";
            case "健康" -> "重点看病象、恢复节奏和缓解信号。";
            case "出行", "搬家" -> "重点看安排是否顺、途中是否有阻滞。";
            case "合作" -> "重点看对方配合度、条件变化和最终履约。";
            case "房产" -> "重点看手续、文书、住处安排与成交推进。";
            case "官司" -> "重点看牵制、压力来源和主动权归属。";
            case "寻物" -> "重点看物件线索是否回拢，以及能否重新定位。";
            default -> String.format("当前先围绕%s的旺衰与动变来判断主线。", useGod);
        };
    }

    public String renderCategoryObservationLead(String questionCategory) {
        return switch (questionCategory == null ? "" : questionCategory) {
            case "求职" -> "求职层面先看机会有没有成形，再看录用链条是否继续往前推";
            case "工作" -> "工作层面先看岗位是否稳得住，再看外部反馈和现实推进";
            case "收入" -> "收入层面先看财务信号是否真实落地，再看兑现节奏";
            case "财运" -> "财运层面先看回报方向，再看资金是否能真正回流";
            case "压力" -> "压力层面先看牵制来源，再看能否逐步减轻";
            case "人际" -> "人际层面先看对方态度，再看关系是否出现缓和空间";
            case "成长" -> "成长层面先看投入是否有效，再看能否稳定转成提升";
            case "考试" -> "考试层面先看发挥和临场状态，再看结果是否兑现";
            case "感情" -> "感情层面先看互动与回应，再看关系有没有继续靠近";
            case "婚姻" -> "婚姻层面先看关系是否稳定，再看后续能否真正落定";
            case "复合" -> "复合层面先看旧关系有没有重新连接，再看对方是否愿意回头";
            case "健康" -> "健康层面先看病象与恢复信号，再看反复点会不会压下来";
            case "出行" -> "出行层面先看行程推进，再看途中阻滞和临时变化";
            case "搬家" -> "搬家层面先看迁移安排，再看住处、手续和时间是否衔接";
            case "合作" -> "合作层面先看对方配合度，再看条件和履约是否稳定";
            case "房产" -> "房产层面先看手续和文书，再看成交推进是否受阻";
            case "官司" -> "官司层面先看压力和对抗来源，再看主动权是否能慢慢转向己方";
            case "寻物" -> "寻物层面先看线索是否回拢，再看是否能借重复排查重新定位";
            default -> renderQuestionCategoryHint(questionCategory) + "当前仍以综合信号为主";
        };
    }

    public String renderQuestionCategoryHint(String questionCategory) {
        return switch (questionCategory) {
            case "求职" -> "求职层面";
            case "收入" -> "收入层面";
            case "财运" -> "财运层面";
            case "压力" -> "压力层面";
            case "人际" -> "人际层面";
            case "成长" -> "成长层面";
            case "考试" -> "考试层面";
            case "感情" -> "感情层面";
            case "婚姻" -> "婚姻层面";
            case "复合" -> "复合层面";
            case "工作" -> "工作层面";
            case "升职" -> "升职层面";
            case "调岗" -> "调岗层面";
            case "健康" -> "健康层面";
            case "出行" -> "出行层面";
            case "搬家" -> "搬家层面";
            case "合作" -> "合作层面";
            case "房产" -> "房产层面";
            case "官司" -> "官司层面";
            case "寻物" -> "寻物层面";
            default -> "综合层面";
        };
    }

    public String renderMovingSignal(String questionCategory, boolean active) {
        String category = questionCategory == null ? "" : questionCategory;
        if (active) {
            return switch (category) {
                case "收入", "财运" -> "财务事项已经开始变化，收入兑现或资金回流不会停在原地";
                case "求职", "工作", "升职", "调岗" -> "岗位事项已经开始推动，流程、反馈或人事动作还会继续发展";
                case "感情", "婚姻", "复合" -> "关系层已经被触动，后续互动不会完全停在原状";
                case "健康" -> "恢复与病象都在变化，后续体感和节奏还会继续波动";
                case "出行", "搬家" -> "行程与迁移事项已经动起来，后续安排还会继续调整";
                case "合作" -> "合作条件和对方态度已经进入变化阶段，后续还会继续谈动";
                case "房产" -> "房产手续或成交节奏已经动起来，后续流程不会停在原地";
                case "官司" -> "对抗和牵制已经出现动作，后续进展仍会继续拉扯";
                case "寻物" -> "寻物线索已经被带动，后续还会有新的回溯和定位机会";
                default -> "世爻或关键爻有发动信号，事情不会完全静止";
            };
        }
        return switch (category) {
            case "收入", "财运" -> "财务信号暂时偏稳，短期更像慢慢落地而不是突然大起大落";
            case "求职", "工作", "升职", "调岗" -> "岗位信号暂时偏稳，更多看外部反馈何时明确";
            case "感情", "婚姻", "复合" -> "关系信号暂时偏稳，短期更看态度延续而非突发变化";
            case "健康" -> "健康信号暂时偏稳，重点看恢复是否持续";
            case "出行", "搬家" -> "行程与迁移信号暂时偏稳，更多看细节是否顺畅";
            case "合作" -> "合作信号暂时偏稳，更多看后续条件能否落实";
            case "房产" -> "房产流程暂时偏稳，重点看手续推进而不是突然转折";
            case "官司" -> "官司信号暂时偏稳，但对抗并未完全消失";
            case "寻物" -> "线索暂时偏稳，更多看重复排查能否带来突破";
            default -> "当前动爻信号不算特别强烈";
        };
    }

    public String renderDominantSignalText(AnalysisContextDTO context) {
        List<String> effectiveRuleCodes = context.getStructuredResult() == null
                ? List.of()
                : context.getStructuredResult().getEffectiveRuleCodes();
        if (effectiveRuleCodes == null || effectiveRuleCodes.isEmpty()) {
            return "";
        }
        if (effectiveRuleCodes.stream().anyMatch(code -> List.of("R031").contains(code))) {
            return "当前主导信号显示原先受困之处正在被冲开，后续还有继续松动的空间，";
        }
        if (effectiveRuleCodes.stream().anyMatch(code -> List.of("USE_GOD_STRENGTH", "R003", "R004", "R007").contains(code))) {
            return "当前主导信号显示用神一侧更有承接力，";
        }
        if (effectiveRuleCodes.stream().anyMatch(code -> List.of(
                "USE_GOD_EMPTY", "USE_GOD_MONTH_BREAK", "USE_GOD_DAY_BREAK", "R009", "R011",
                "R028", "R029", "R030", "R032").contains(code))) {
            return "当前主导信号显示用神受制较深，入墓、受冲或外部牵制仍偏明显，";
        }
        if (effectiveRuleCodes.stream().anyMatch(code -> List.of("R010", "MOVING_LINE_EXISTS", "MOVING_LINE_AFFECT_USE_GOD", "R018", "R021").contains(code))) {
            return "当前主导信号显示事情仍在变化途中，";
        }
        if (effectiveRuleCodes.stream().anyMatch(code -> List.of("SHI_YING_RELATION", "R013", "R014", "R015").contains(code))) {
            return "当前主导信号显示双方互动仍是关键变量，";
        }
        return "";
    }

    public String renderLevelText(String resultLevel) {
        if (resultLevel == null || resultLevel.isBlank()) {
            return "整体仍需继续观察";
        }
        return switch (resultLevel) {
            case "GOOD" -> "整体偏吉";
            case "BAD" -> "整体偏弱";
            default -> "整体中性，存在反复";
        };
    }
}
