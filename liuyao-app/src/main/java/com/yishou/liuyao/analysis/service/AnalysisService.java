package com.yishou.liuyao.analysis.service;

import com.yishou.liuyao.analysis.dto.AnalysisContextDTO;
import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.rule.RuleHit;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnalysisService {

    public String analyze(String question, ChartSnapshot chartSnapshot, List<RuleHit> ruleHits) {
        AnalysisContextDTO context = buildContext(question, chartSnapshot, ruleHits);
        return analyze(context);
    }

    public String analyze(AnalysisContextDTO context) {
        if (context == null) {
            return "分析模块骨架已就绪，后续可接入受约束的 LLM 编排。";
        }
        int ruleCount = context.getRuleCount() == null ? 0 : context.getRuleCount();
        return String.join(" ", List.of(
                        buildHexagramOverview(context),
                        buildUseGodSection(context, ruleCount),
                        buildCategoryObservation(context),
                        buildMovingObservation(context),
                        buildRiskObservation(context),
                        buildConclusion(context),
                        buildActionSuggestion(context),
                        buildKnowledgeHint(context.getKnowledgeSnippets()))
                .stream()
                .filter(item -> item != null && !item.isBlank())
                .toList());
    }

    private AnalysisContextDTO buildContext(String question, ChartSnapshot chartSnapshot, List<RuleHit> ruleHits) {
        AnalysisContextDTO context = new AnalysisContextDTO();
        context.setContextVersion("v1");
        context.setQuestion(question);
        if (chartSnapshot != null) {
            context.setQuestionCategory(chartSnapshot.getQuestionCategory());
            context.setUseGod(chartSnapshot.getUseGod());
            context.setMainHexagram(chartSnapshot.getMainHexagram());
            context.setChangedHexagram(chartSnapshot.getChangedHexagram());
        }
        context.setRuleCount(ruleHits == null ? 0 : ruleHits.size());
        context.setRuleCodes(ruleHits == null ? List.of() : ruleHits.stream().map(RuleHit::getRuleCode).toList());
        return context;
    }

    private String firstKnowledgeHint(List<String> knowledgeSnippets) {
        if (knowledgeSnippets == null || knowledgeSnippets.isEmpty()) {
            return null;
        }
        String first = knowledgeSnippets.get(0);
        if (first == null || first.isBlank()) {
            return null;
        }
        String normalized = first.replace('\n', ' ').trim();
        if (normalized.length() <= 48) {
            return normalized;
        }
        return normalized.substring(0, 48) + "...";
    }

    private String resolveQuestionLead(String questionCategory) {
        if (questionCategory == null || questionCategory.isBlank()) {
            return "本次所问";
        }
        return "问" + questionCategory;
    }

    private String buildHexagramOverview(AnalysisContextDTO context) {
        String questionLead = resolveQuestionLead(context.getQuestionCategory());
        String mainHexagram = context.getMainHexagram() == null || context.getMainHexagram().isBlank()
                ? "未知本卦"
                : context.getMainHexagram();
        String changedHexagram = context.getChangedHexagram() == null || context.getChangedHexagram().isBlank()
                ? "未见明显变卦"
                : context.getChangedHexagram();
        return String.format("卦象概览：%s，本卦%s，变卦%s。", questionLead, mainHexagram, changedHexagram);
    }

    private String buildUseGodSection(AnalysisContextDTO context, int ruleCount) {
        String useGod = context.getUseGod() == null || context.getUseGod().isBlank() ? "未定用神" : context.getUseGod();
        return String.format("用神判断：本次以%s为用神。%s %s",
                useGod,
                renderUseGodFocus(context.getQuestionCategory(), useGod),
                buildRuleSummary(context, ruleCount));
    }

    private String buildRuleSummary(AnalysisContextDTO context, int ruleCount) {
        if (context.getStructuredResult() == null) {
            return String.format("当前共命中%d条规则。", ruleCount);
        }
        Integer effectiveScore = context.getStructuredResult().getEffectiveScore();
        String effectiveResultLevel = context.getStructuredResult().getEffectiveResultLevel();
        String summary = context.getStructuredResult().getSummary();
        String effectiveText = effectiveScore == null
                ? ""
                : String.format("冲突裁剪后有效评分%d，%s。", effectiveScore, renderLevelText(effectiveResultLevel));
        String summaryText = summary == null || summary.isBlank() ? "" : summary;
        return String.format("当前共命中%d条规则。%s%s", ruleCount, effectiveText, summaryText);
    }

    private String buildCategoryObservation(AnalysisContextDTO context) {
        if (context.getStructuredResult() == null) {
            return "";
        }
        String questionCategory = context.getQuestionCategory() == null ? "" : context.getQuestionCategory();
        List<String> tags = context.getStructuredResult().getTags();
        List<String> effectiveRuleCodes = context.getStructuredResult().getEffectiveRuleCodes();
        List<String> suppressedRuleCodes = context.getStructuredResult().getSuppressedRuleCodes();
        String categoryText = resolveCategorySummaryText(context);
        String tagText = tags == null || tags.isEmpty() ? "暂无明显标签" : String.join("、", tags);
        String effectiveRuleText = effectiveRuleCodes == null || effectiveRuleCodes.isEmpty()
                ? "当前没有明确保留下来的主导规则"
                : "当前主导规则为" + String.join("、", effectiveRuleCodes);
        String suppressedText = suppressedRuleCodes == null || suppressedRuleCodes.isEmpty()
                ? ""
                : "，被压制规则为" + String.join("、", suppressedRuleCodes);
        return String.format("关系判断：%s。当前标签为%s；%s%s。", renderCategoryObservationLead(questionCategory), tagText, effectiveRuleText, suppressedText)
                + (categoryText.isBlank() ? "" : " " + categoryText);
    }

    private String buildMovingObservation(AnalysisContextDTO context) {
        if (context.getStructuredResult() == null || context.getStructuredResult().getConflictSummaries() == null) {
            return "";
        }
        long movingConflicts = context.getStructuredResult().getConflictSummaries().stream()
                .filter(item -> "MOVING_CHANGE".equals(item.getCategory()))
                .count();
        String movingRuleText = context.getRuleCodes() != null && context.getRuleCodes().contains("R010")
                ? renderMovingSignal(context.getQuestionCategory(), true)
                : renderMovingSignal(context.getQuestionCategory(), false);
        if (movingConflicts > 0) {
            return "动爻影响：" + movingRuleText + "，但动变层也存在相互牵制，节奏上会有反复。";
        }
        return "动爻影响：" + movingRuleText + "。";
    }

    private String buildRiskObservation(AnalysisContextDTO context) {
        if (context.getStructuredResult() == null) {
            return "";
        }
        String level = context.getStructuredResult().getEffectiveResultLevel();
        String questionCategory = context.getQuestionCategory() == null ? "" : context.getQuestionCategory();
        String riskText;
        if ("GOOD".equals(level)) {
            riskText = switch (questionCategory) {
                case "收入", "财运" -> "当前更要留意兑现节奏，避免看对方向却高估落袋速度";
                case "求职", "工作", "升职", "调岗" -> "当前更要留意流程推进，避免因为沟通和手续拖慢落地";
                case "成长" -> "当前更要留意投入是否持续，避免三分钟热度消耗掉已有积累";
                case "压力" -> "当前虽有缓冲空间，仍要留意旧牵制是否反扑回来";
                case "考试" -> "当前更要留意临场节奏，避免准备充分却在细节上失分";
                case "感情", "婚姻", "复合" -> "当前更要留意情绪反复，避免因为一时回应就过早下结论";
                case "人际" -> "当前更要留意沟通分寸，避免误判对方态度而把关系推远";
                case "出行", "搬家", "房产" -> "当前更要留意时间安排和文书细节，避免小阻滞放大";
                case "官司" -> "当前更要留意证据和节奏，避免因为对方动作变化而掉以轻心";
                case "寻物" -> "当前更要留意回头查找和线索复盘，线索往往不是一次就出现";
                default -> "当前虽不算弱，但仍要留意节奏上的反复与细节偏差";
            };
            return "风险提示：" + appendKnowledgeEvidence(riskText, context, "risk") + "。";
        }
        if ("BAD".equals(level)) {
            riskText = switch (questionCategory) {
                case "收入", "财运" -> "财务层面容易出现拖延、折损或预期落空，先防守再谈扩大";
                case "求职", "工作", "升职", "调岗" -> "岗位层面容易出现流程受阻或外部反馈偏慢，要准备备选方案";
                case "成长" -> "投入容易散、收效容易慢，先防忙了很多却没沉淀下来";
                case "压力" -> "牵制和消耗更重，先防旧问题叠着新问题一起压上来";
                case "考试" -> "临场和准备之间容易脱节，先防心态波动和步骤失序";
                case "感情", "婚姻", "复合" -> "关系层面容易出现冷热不定或对方回应不足，不宜只凭主观期待推进";
                case "人际" -> "人际互动里容易出现误读和卡顿，先防话到嘴边却越说越偏";
                case "健康" -> "恢复节奏未必线性，先控制反复点，再看后续改善";
                case "出行", "搬家", "房产" -> "行程与手续层面容易反复，要提前预留时间与替代安排";
                case "官司" -> "压力与牵制信号更重，宜先稳住证据和节奏，不宜贸然乐观";
                case "寻物" -> "线索暂时分散，宜从最近接触地点和文书物件关联处反向排查";
                default -> "当前风险信号偏重，宜先收缩预期，再观察后续转机";
            };
            return "风险提示：" + appendKnowledgeEvidence(riskText, context, "risk") + "。";
        }
        riskText = switch (questionCategory) {
            case "收入", "财运" -> "当前成与不成并存，重点看兑现时点与中途波动";
            case "求职", "工作", "升职", "调岗" -> "当前进展与阻力并存，重点看外部反馈和流程节点";
            case "成长" -> "当前积累和波动并存，重点看投入能否稳步转化为提升";
            case "压力" -> "当前压力与缓冲并存，重点看牵制能否一层层拆开";
            case "考试" -> "当前发挥空间与波动并存，重点看临场状态能否稳住";
            case "感情", "婚姻", "复合" -> "当前互动有空间，但也容易反复，重点看后续实际回应";
            case "人际" -> "当前关系有缓和空间，但也容易因为表达不准而反复";
            case "健康" -> "当前恢复与反复并见，重点看后续调养是否持续";
            case "出行", "搬家", "房产" -> "当前安排能推得动，但中途仍可能有手续或时间上的折返";
            case "官司" -> "当前压力和转机并存，重点看谁先掌握主动节奏";
            case "寻物" -> "当前线索未完全断开，重点看回溯和重复排查";
            default -> "当前信号并不单一，重点看后续关键节点能否兑现";
        };
        return "风险提示：" + appendKnowledgeEvidence(riskText, context, "risk") + "。";
    }

    private String buildConclusion(AnalysisContextDTO context) {
        if (context.getStructuredResult() == null) {
            return "";
        }
        Integer effectiveScore = context.getStructuredResult().getEffectiveScore();
        String levelText = renderLevelText(context.getStructuredResult().getEffectiveResultLevel());
        String dominantSignal = renderDominantSignalText(context);
        String direction = switch (context.getQuestionCategory() == null ? "" : context.getQuestionCategory()) {
            case "求职" -> "求职层面先看机会是否落地与录用节奏";
            case "收入" -> "财务层面先看落实和兑现节奏";
            case "财运" -> "财运层面先看投资回报与资金回流节奏";
            case "压力" -> "压力层面先看约束是否减轻与节奏能否放缓";
            case "人际" -> "人际层面先看对方态度与关系缓和空间";
            case "成长" -> "成长层面先看投入能否转成稳定提升";
            case "考试" -> "考试层面先看发挥与结果兑现";
            case "感情" -> "关系层面先看互动与回应";
            case "婚姻" -> "婚姻层面先看关系是否能稳定落定";
            case "复合" -> "复合层面先看旧关系是否有重新连接的机会";
            case "工作" -> "工作层面先看岗位推进和外部反馈";
            case "升职" -> "升职层面先看职位抬升与外部认可";
            case "调岗" -> "调岗层面先看岗位变动是否真正落地";
            case "健康" -> "健康层面先看恢复节奏与反复点";
            case "出行" -> "出行层面先看行程是否受阻";
            case "搬家" -> "搬家层面先看迁移安排是否顺畅";
            case "合作" -> "合作层面先看对方配合度与条件变动";
            case "房产" -> "房产层面先看手续进度与成交阻力";
            case "官司" -> "官司层面先看压力来源与结果走向";
            case "寻物" -> "寻物层面先看线索是否逐步收拢";
            default -> "后续以关键节点是否兑现为主";
        };
        return String.format("结论建议：当前有效评分%s，%s；%s%s。",
                effectiveScore == null ? "未定" : String.valueOf(effectiveScore),
                levelText,
                dominantSignal,
                direction);
    }

    private String buildKnowledgeHint(List<String> knowledgeSnippets) {
        String knowledgeHint = firstKnowledgeHint(knowledgeSnippets);
        if (knowledgeHint == null) {
            return "可参考资料：当前暂未检索到直接匹配资料，先以盘面结构和规则信号为主。";
        }
        return "可参考资料：" + knowledgeHint;
    }

    private String buildActionSuggestion(AnalysisContextDTO context) {
        if (context.getStructuredResult() == null) {
            return "";
        }
        String level = context.getStructuredResult().getEffectiveResultLevel();
        String category = context.getQuestionCategory() == null ? "" : context.getQuestionCategory();
        String suggestion = switch (category) {
            case "求职" -> switch (level) {
                case "GOOD" -> "继续盯紧面试反馈、录用通知和入职节奏，不要只停留在口头好感。";
                case "BAD" -> "同步准备备选岗位和第二方案，避免把希望压在单一机会点上。";
                default -> "继续观察外部反馈是否变明确，尤其是时间节点和流程推进。";
            };
            case "工作" -> switch (level) {
                case "GOOD" -> "继续推动关键沟通，把模糊态度尽量转成明确安排。";
                case "BAD" -> "先稳住现有位置，再观察是否需要调整节奏或另做准备。";
                default -> "盯住岗位安排和外部反馈，先看谁先给出明确信号。";
            };
            case "升职" -> switch (level) {
                case "GOOD" -> "继续推动关键汇报和成果呈现，让职位抬升信号尽快转成明确信息。";
                case "BAD" -> "先稳住现有表现，别急着正面硬顶，等认可度再积一层。";
                default -> "继续看上层反馈和岗位信号，先确认抬升机会是否在变清楚。";
            };
            case "调岗" -> switch (level) {
                case "GOOD" -> "继续把岗位变动的时间点和安排细节确认下来，避免临门一脚再反复。";
                case "BAD" -> "先稳住当前节奏，再看调岗信号是真推进还是暂时风声。";
                default -> "继续盯住岗位安排和变动节点，先看这次调整会不会真正落地。";
            };
            case "成长" -> switch (level) {
                case "GOOD" -> "继续把有效投入稳定下来，让已经见效的方法持续累积。";
                case "BAD" -> "先收束分散投入，保留最有效的一两条路径再持续推进。";
                default -> "继续观察哪些投入真正有回响，再决定资源往哪里压。";
            };
            case "压力" -> switch (level) {
                case "GOOD" -> "继续按轻重缓急拆解牵制点，别在已经变轻的环节上反复耗力。";
                case "BAD" -> "先把最重的牵制点拆开，不要试图同时硬扛所有压力。";
                default -> "继续分层处理压力来源，先看哪一处最先松动。";
            };
            case "收入", "财运" -> switch (level) {
                case "GOOD" -> "先看兑现节点，再看是否值得追加投入，不宜只看账面想象。";
                case "BAD" -> "先控制风险和支出，回避激进追加，等信号更明确再动作。";
                default -> "先看回款、发放或回流时点，避免只根据情绪判断结果。";
            };
            case "考试" -> switch (level) {
                case "GOOD" -> "继续把答题节奏和重点内容再过一遍，临场越稳越容易把优势兑现。";
                case "BAD" -> "先把复习节奏和临场准备稳住，避免在会做的地方因为慌乱失分。";
                default -> "继续盯住复习顺序和临场状态，先把容易波动的点压下来。";
            };
            case "感情" -> switch (level) {
                case "GOOD" -> "继续看真实互动频率和回应质量，不要只凭一次积极信号定结论。";
                case "BAD" -> "先降低主观预期，观察对方是否真的愿意持续投入。";
                default -> "先看后续回应是否稳定，再判断关系走向。";
            };
            case "婚姻" -> switch (level) {
                case "GOOD" -> "继续看关系是否能往稳定和落定推进，不要只看一时气氛。";
                case "BAD" -> "先看现实承诺和相处节奏，别急着把期待提前落到结果上。";
                default -> "先看双方是否能持续朝稳定方向靠近，再谈后续落定。";
            };
            case "复合" -> switch (level) {
                case "GOOD" -> "继续顺着已有回暖信号观察，但别急着一次性把旧问题全部翻开。";
                case "BAD" -> "先看旧联系有没有真实恢复，再决定是否继续投入感情成本。";
                default -> "先看旧联系是否继续回温，再判断这段关系有没有重启空间。";
            };
            case "人际" -> switch (level) {
                case "GOOD" -> "继续顺着对方已经给出的善意信号往前沟通，但别急着一次谈太满。";
                case "BAD" -> "先收住情绪和表达强度，避免在误会没解开前继续硬推。";
                default -> "先看对方是否释放更明确的回应，再决定把话说到什么程度。";
            };
            case "健康" -> switch (level) {
                case "GOOD" -> "顺着当前恢复节奏继续调养，同时留意是否有阶段性反复。";
                case "BAD" -> "优先控制反复点和消耗点，不宜对恢复速度过度乐观。";
                default -> "继续看恢复是否持续，重点记录反复发生的节点。";
            };
            case "出行", "搬家" -> switch (level) {
                case "GOOD" -> "提前把时间、路线和文书再核一遍，能明显减少中途折返。";
                case "BAD" -> "预留更多缓冲时间，并提前准备替代方案或备用安排。";
                default -> "先把行程和手续逐项确认，避免临时变化打乱节奏。";
            };
            case "合作" -> switch (level) {
                case "GOOD" -> "尽快把口头态度落成明确条件，别让合作停留在含糊空间。";
                case "BAD" -> "先压实边界和条款，再决定是否继续推进。";
                default -> "继续看对方是否给出实质回应，再决定投入深度。";
            };
            case "房产" -> switch (level) {
                case "GOOD" -> "继续推进手续、过户和文书确认，别让细节拖住整体进度。";
                case "BAD" -> "先核对手续、资金和时间节点，避免在关键步骤上失误。";
                default -> "先把文书与成交条件对齐，再判断后续是否顺推。";
            };
            case "官司" -> switch (level) {
                case "GOOD" -> "继续稳住证据、节奏和主动表达，不要在占优时掉以轻心。";
                case "BAD" -> "先补强证据与节奏控制，避免情绪化推进。";
                default -> "继续看谁先掌握实质主动权，尤其留意对方动作。";
            };
            case "寻物" -> switch (level) {
                case "GOOD" -> "优先回查最近接触点、收纳点和关联物件，线索通常会从近处回拢。";
                case "BAD" -> "先缩小范围分段排查，不要漫无目的地重复翻找。";
                default -> "继续按时间线倒推，把最近接触路径再过一遍。";
            };
            default -> "继续观察关键节点是否兑现，再决定下一步动作。";
        };
        return "下一步建议：" + appendKnowledgeEvidence(suggestion, context, "action");
    }

    private String appendKnowledgeEvidence(String baseText, AnalysisContextDTO context, String purpose) {
        if (baseText == null || baseText.isBlank()) {
            return "";
        }
        String evidence = summarizeKnowledgeEvidence(selectKnowledgeSnippet(context, purpose));
        if (evidence == null) {
            return baseText;
        }
        return baseText + " 可结合" + evidence + "继续判断";
    }

    private String selectKnowledgeSnippet(AnalysisContextDTO context, String purpose) {
        if (context == null || context.getKnowledgeSnippets() == null || context.getKnowledgeSnippets().isEmpty()) {
            return null;
        }
        String bestSnippet = null;
        int bestScore = Integer.MIN_VALUE;
        for (String snippet : context.getKnowledgeSnippets()) {
            int score = scoreKnowledgeSnippet(snippet, context, purpose);
            if (score > bestScore) {
                bestScore = score;
                bestSnippet = snippet;
            }
        }
        return bestSnippet;
    }

    private int scoreKnowledgeSnippet(String snippet, AnalysisContextDTO context, String purpose) {
        if (snippet == null || snippet.isBlank()) {
            return Integer.MIN_VALUE;
        }
        String normalized = snippet.replace(" ", "");
        String category = context.getQuestionCategory() == null ? "" : context.getQuestionCategory();
        List<String> ruleCodes = resolvePreferredRuleCodes(context);
        int score = "risk".equals(purpose)
                ? (containsAny(normalized, "风险", "空亡", "月破", "日破", "受克", "拖延", "阻", "反复") ? 6 : 0)
                : (containsAny(normalized, "宜", "可", "利", "回查", "继续", "推进", "准备", "观察") ? 6 : 0);
        score += switch (category) {
            case "收入", "财运" -> containsAny(normalized, "财", "回款", "收益", "用神") ? 4 : 0;
            case "求职", "工作", "升职", "调岗" -> containsAny(normalized, "官", "岗位", "录用", "机会", "求职", "文书", "回访", "回应") ? 4 : 0;
            case "感情", "婚姻", "复合", "合作", "人际" -> containsAny(normalized, "世应", "应爻", "关系", "对方") ? 4 : 0;
            case "健康" -> containsAny(normalized, "病", "官鬼", "恢复", "子孙") ? 4 : 0;
            case "出行", "搬家", "房产" -> containsAny(normalized, "父母", "文书", "手续", "行程", "住处") ? 4 : 0;
            case "官司" -> containsAny(normalized, "官司", "诉讼", "证据", "官鬼", "牵制") ? 4 : 0;
            case "寻物" -> containsAny(normalized, "失物", "寻物", "回查", "线索", "妻财") ? 4 : 0;
            default -> 0;
        };
        score += scoreByRuleCodes(normalized, ruleCodes);
        if ("action".equals(purpose) && containsAny(normalized, "空亡", "月破", "日破", "拖延", "落空", "反复")) {
            score -= 8;
        }
        if (ruleCodes.contains("R011") || ruleCodes.contains("USE_GOD_EMPTY")) {
            score += containsAny(normalized, "空亡", "旬空") ? 3 : 0;
        }
        if (ruleCodes.contains("USE_GOD_MONTH_BREAK")) {
            score += containsAny(normalized, "月破") ? 3 : 0;
        }
        if (ruleCodes.contains("USE_GOD_DAY_BREAK") || ruleCodes.contains("R009")) {
            score += containsAny(normalized, "日破", "受克") ? 3 : 0;
        }
        if (ruleCodes.contains("R010") || ruleCodes.contains("MOVING_LINE_EXISTS") || ruleCodes.contains("MOVING_LINE_AFFECT_USE_GOD")) {
            score += containsAny(normalized, "动爻", "发动", "动化", "变化") ? 3 : 0;
        }
        return score;
    }

    private List<String> resolvePreferredRuleCodes(AnalysisContextDTO context) {
        if (context == null) {
            return List.of();
        }
        if (context.getStructuredResult() != null
                && context.getStructuredResult().getEffectiveRuleCodes() != null
                && !context.getStructuredResult().getEffectiveRuleCodes().isEmpty()) {
            return context.getStructuredResult().getEffectiveRuleCodes();
        }
        return context.getRuleCodes() == null ? List.of() : context.getRuleCodes();
    }

    private int scoreByRuleCodes(String normalizedSnippet, List<String> ruleCodes) {
        int score = 0;
        for (String ruleCode : ruleCodes) {
            score += switch (ruleCode) {
                case "USE_GOD_STRENGTH", "R003", "R004", "R007", "R008", "R018" ->
                        containsAny(normalizedSnippet, "旺相", "休囚", "生扶", "合", "强弱", "化退") ? 3 : 0;
                case "USE_GOD_EMPTY", "R011" ->
                        containsAny(normalizedSnippet, "空亡", "旬空") ? 3 : 0;
                case "USE_GOD_MONTH_BREAK" ->
                        containsAny(normalizedSnippet, "月破") ? 3 : 0;
                case "USE_GOD_DAY_BREAK", "R009" ->
                        containsAny(normalizedSnippet, "日破", "受克") ? 3 : 0;
                case "MOVING_LINE_EXISTS", "MOVING_LINE_AFFECT_USE_GOD", "R010" ->
                        containsAny(normalizedSnippet, "动爻", "发动", "动化", "变化") ? 3 : 0;
                case "SHI_YING_RELATION", "R013", "R014", "R015" ->
                        containsAny(normalizedSnippet, "世应", "应爻", "对方", "关系") ? 3 : 0;
                default -> 0;
            };
        }
        return score;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String summarizeKnowledgeEvidence(String snippet) {
        if (snippet == null || snippet.isBlank()) {
            return null;
        }
        if (snippet.startsWith("[") && snippet.contains("]")) {
            int closingIndex = snippet.indexOf(']');
            String source = snippet.substring(1, closingIndex).trim();
            String content = snippet.substring(closingIndex + 1).trim();
            if (!source.isBlank() && !content.isBlank()) {
                return source + "中的“" + trimKnowledgeSentence(content) + "”";
            }
        }
        return "资料片段中的“" + trimKnowledgeSentence(snippet) + "”";
    }

    private String trimKnowledgeSentence(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.replace('\n', ' ').trim();
        if (normalized.length() <= 24) {
            return normalized;
        }
        return normalized.substring(0, 24) + "...";
    }

    private String renderUseGodFocus(String questionCategory, String useGod) {
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

    private String renderCategoryObservationLead(String questionCategory) {
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

    private String resolveCategorySummaryText(AnalysisContextDTO context) {
        if (context.getStructuredResult() == null || context.getStructuredResult().getCategorySummaries() == null) {
            return "";
        }
        return context.getStructuredResult().getCategorySummaries().stream()
                .filter(item -> item.getEffectiveHitCount() != null && item.getEffectiveHitCount() > 0)
                .sorted((left, right) -> Integer.compare(
                        right.getEffectiveScore() == null ? 0 : right.getEffectiveScore(),
                        left.getEffectiveScore() == null ? 0 : left.getEffectiveScore()))
                .limit(2)
                .map(item -> String.format("%s阶段保留%d条有效信号，合计%d分",
                        item.getCategory(),
                        item.getEffectiveHitCount(),
                        item.getEffectiveScore() == null ? 0 : item.getEffectiveScore()))
                .reduce((left, right) -> left + "；" + right)
                .orElse("");
    }

    private String renderQuestionCategoryHint(String questionCategory) {
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

    private String renderMovingSignal(String questionCategory, boolean active) {
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

    private String renderDominantSignalText(AnalysisContextDTO context) {
        List<String> effectiveRuleCodes = context.getStructuredResult() == null
                ? List.of()
                : context.getStructuredResult().getEffectiveRuleCodes();
        if (effectiveRuleCodes == null || effectiveRuleCodes.isEmpty()) {
            return "";
        }
        if (effectiveRuleCodes.stream().anyMatch(code -> List.of("USE_GOD_STRENGTH", "R003", "R004", "R007").contains(code))) {
            return "当前主导信号显示用神一侧更有承接力，";
        }
        if (effectiveRuleCodes.stream().anyMatch(code -> List.of("USE_GOD_EMPTY", "USE_GOD_MONTH_BREAK", "USE_GOD_DAY_BREAK", "R009", "R011").contains(code))) {
            return "当前主导信号显示阻滞与牵制仍偏明显，";
        }
        if (effectiveRuleCodes.stream().anyMatch(code -> List.of("R010", "MOVING_LINE_EXISTS", "MOVING_LINE_AFFECT_USE_GOD", "R018").contains(code))) {
            return "当前主导信号显示事情仍在变化途中，";
        }
        if (effectiveRuleCodes.stream().anyMatch(code -> List.of("SHI_YING_RELATION", "R013", "R014", "R015").contains(code))) {
            return "当前主导信号显示双方互动仍是关键变量，";
        }
        return "";
    }

    private String renderLevelText(String resultLevel) {
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
