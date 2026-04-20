package com.yishou.liuyao.rule.usegod;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class QuestionIntentResolver {

    private final QuestionCategoryNormalizer questionCategoryNormalizer;

    public QuestionIntentResolver(QuestionCategoryNormalizer questionCategoryNormalizer) {
        this.questionCategoryNormalizer = questionCategoryNormalizer;
    }

    public QuestionIntent resolve(String questionText, String questionCategory) {
        // 先信任结构化分类，再退回到文本关键词，尽量让外部接入方有可控入口。
        QuestionIntent byCategory = resolveFromCategory(questionCategory);
        if (byCategory != QuestionIntent.UNKNOWN) {
            return byCategory;
        }
        return resolveFromText(questionText);
    }

    public QuestionIntent resolveFromCategory(String questionCategory) {
        return fromCategory(questionCategoryNormalizer.normalize(questionCategory));
    }

    public QuestionIntent resolveFromText(String questionText) {
        return fromText(questionText);
    }

    public String detectDirectionFromText(String questionText) {
        return toCanonicalDirection(resolveFromText(questionText));
    }

    private QuestionIntent fromCategory(String questionCategory) {
        if (questionCategory == null || questionCategory.isBlank()) {
            return QuestionIntent.UNKNOWN;
        }

        String category = questionCategory.trim().toLowerCase(Locale.ROOT);
        return switch (category) {
            case "求职", "工作机会", "面试", "录用", "offer", "job_opportunity" -> QuestionIntent.JOB_OPPORTUNITY;
            case "工作", "工作稳定", "稳定性", "保工作", "job_stability" -> QuestionIntent.JOB_STABILITY;
            case "收入", "工资", "奖金", "财运", "income", "投资", "理财" -> QuestionIntent.INCOME;
            case "压力", "工作压力", "累不累", "pressure" -> QuestionIntent.PRESSURE;
            case "关系", "人际", "同事", "上下级", "relation" -> QuestionIntent.RELATION;
            case "成长", "学习", "提升", "growth", "升职", "调岗" -> QuestionIntent.GROWTH;
            case "考试", "考证", "exam" -> QuestionIntent.EXAM;
            case "健康", "health" -> QuestionIntent.HEALTH;
            case "出行", "旅行", "差旅", "travel" -> QuestionIntent.TRAVEL;
            case "感情", "emotion", "婚姻", "复合" -> QuestionIntent.EMOTION;
            case "合作", "签约", "cooperation" -> QuestionIntent.COOPERATION;
            case "房产", "买房", "卖房", "real_estate" -> QuestionIntent.REAL_ESTATE;
            case "搬家", "迁居", "relocation" -> QuestionIntent.RELOCATION;
            case "官司", "诉讼", "纠纷", "仲裁", "lawsuit" -> QuestionIntent.LAWSUIT;
            case "寻物", "失物", "找东西", "lost_item" -> QuestionIntent.LOST_ITEM;
            default -> QuestionIntent.UNKNOWN;
        };
    }

    private QuestionIntent fromText(String questionText) {
        if (questionText == null || questionText.isBlank()) {
            return QuestionIntent.UNKNOWN;
        }

        // 当前阶段采用轻量关键词匹配，后续可替换成分类器，但返回枚举尽量保持稳定。
        String text = questionText.trim();
        if (containsAny(text, "面试", "录用", "入职", "offer", "找工作", "求职", "岗位", "应聘")) {
            return QuestionIntent.JOB_OPPORTUNITY;
        }
        if (containsAny(text, "稳不稳定", "失业", "裁员", "保住工作", "会不会离职")) {
            return QuestionIntent.JOB_STABILITY;
        }
        if (containsAny(text, "工资", "收入", "奖金", "提成", "赚多少钱", "薪资")) {
            return QuestionIntent.INCOME;
        }
        if (containsAny(text, "压力", "累", "辛苦", "负担")) {
            return QuestionIntent.PRESSURE;
        }
        if (containsAny(text, "同事", "领导", "上级", "关系", "相处")) {
            return QuestionIntent.RELATION;
        }
        if (containsAny(text, "成长", "学习", "学到", "提升", "进步")) {
            return QuestionIntent.GROWTH;
        }
        if (containsAny(text, "考试", "考证", "考核", "上岸")) {
            return QuestionIntent.EXAM;
        }
        if (containsAny(text, "身体", "疾病", "康复", "健康")) {
            return QuestionIntent.HEALTH;
        }
        if (containsAny(text, "出行", "旅行", "出差", "路上", "行程", "航班")) {
            return QuestionIntent.TRAVEL;
        }
        if (containsAny(text, "感情", "对象", "恋爱", "复合", "婚姻")) {
            return QuestionIntent.EMOTION;
        }
        if (containsAny(text, "合作", "签约", "合同", "项目")) {
            return QuestionIntent.COOPERATION;
        }
        if (containsAny(text, "买房", "卖房", "房产", "房子", "房屋", "过户")) {
            return QuestionIntent.REAL_ESTATE;
        }
        if (containsAny(text, "搬家", "迁居", "迁移", "搬迁")) {
            return QuestionIntent.RELOCATION;
        }
        if (containsAny(text, "官司", "诉讼", "纠纷", "仲裁")) {
            return QuestionIntent.LAWSUIT;
        }
        if (containsAny(text, "寻物", "失物", "找东西", "丢了", "丢失")) {
            return QuestionIntent.LOST_ITEM;
        }
        return QuestionIntent.UNKNOWN;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String toCanonicalDirection(QuestionIntent intent) {
        return switch (intent) {
            case JOB_OPPORTUNITY -> "求职";
            case JOB_STABILITY -> "工作";
            case INCOME -> "收入";
            case PRESSURE -> "压力";
            case RELATION -> "人际";
            case GROWTH -> "成长";
            case EXAM -> "考试";
            case HEALTH -> "健康";
            case TRAVEL -> "出行";
            case EMOTION -> "感情";
            case COOPERATION -> "合作";
            case REAL_ESTATE -> "房产";
            case RELOCATION -> "搬家";
            case LAWSUIT -> "官司";
            case LOST_ITEM -> "寻物";
            case UNKNOWN -> "";
        };
    }
}
