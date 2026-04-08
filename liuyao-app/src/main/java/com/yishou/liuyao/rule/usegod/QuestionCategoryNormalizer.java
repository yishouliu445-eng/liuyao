package com.yishou.liuyao.rule.usegod;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class QuestionCategoryNormalizer {

    private static final Map<String, String> CATEGORY_ALIASES = createCategoryAliases();

    /**
     * 将外部传入的问类收敛成系统内部使用的标准中文问类。
     * 这一层只做轻量规范化，不做复杂 NLP，未知分类保持原值返回。
     */
    public String normalize(String questionCategory) {
        if (questionCategory == null || questionCategory.isBlank()) {
            return questionCategory;
        }
        String trimmed = questionCategory.trim();
        String directMatch = CATEGORY_ALIASES.get(trimmed.toLowerCase(Locale.ROOT));
        if (directMatch != null) {
            return directMatch;
        }
        return trimmed;
    }

    private static Map<String, String> createCategoryAliases() {
        Map<String, String> aliases = new LinkedHashMap<>();
        aliases.put("求职", "求职");
        aliases.put("工作机会", "求职");
        aliases.put("面试", "求职");
        aliases.put("录用", "求职");
        aliases.put("入职", "求职");
        aliases.put("offer", "求职");
        aliases.put("job_opportunity", "求职");

        aliases.put("工作", "工作");
        aliases.put("工作稳定", "工作");
        aliases.put("稳定性", "工作");
        aliases.put("保工作", "工作");
        aliases.put("job_stability", "工作");

        aliases.put("收入", "收入");
        aliases.put("工资", "收入");
        aliases.put("薪资", "收入");
        aliases.put("奖金", "收入");
        aliases.put("提成", "收入");
        aliases.put("财运", "收入");
        aliases.put("income", "收入");

        aliases.put("财运", "财运");
        aliases.put("投资", "财运");
        aliases.put("理财", "财运");
        aliases.put("回报", "财运");

        aliases.put("压力", "压力");
        aliases.put("工作压力", "压力");
        aliases.put("累不累", "压力");
        aliases.put("pressure", "压力");

        aliases.put("人际", "人际");
        aliases.put("关系", "人际");
        aliases.put("同事", "人际");
        aliases.put("上下级", "人际");
        aliases.put("relation", "人际");

        aliases.put("成长", "成长");
        aliases.put("学习", "成长");
        aliases.put("提升", "成长");
        aliases.put("growth", "成长");

        aliases.put("考试", "考试");
        aliases.put("考证", "考试");
        aliases.put("考核", "考试");
        aliases.put("exam", "考试");

        aliases.put("感情", "感情");
        aliases.put("恋爱", "感情");
        aliases.put("婚姻", "感情");
        aliases.put("复合", "感情");
        aliases.put("emotion", "感情");

        aliases.put("婚姻", "婚姻");
        aliases.put("结婚", "婚姻");
        aliases.put("复婚", "婚姻");
        aliases.put("复合", "复合");
        aliases.put("和好", "复合");

        aliases.put("健康", "健康");
        aliases.put("health", "健康");

        aliases.put("出行", "出行");
        aliases.put("旅行", "出行");
        aliases.put("差旅", "出行");
        aliases.put("travel", "出行");

        aliases.put("合作", "合作");
        aliases.put("签约", "合作");
        aliases.put("合同", "合作");
        aliases.put("cooperation", "合作");

        aliases.put("升职", "升职");
        aliases.put("晋升", "升职");
        aliases.put("提拔", "升职");
        aliases.put("调岗", "调岗");
        aliases.put("调动", "调岗");

        aliases.put("房产", "房产");
        aliases.put("买房", "房产");
        aliases.put("卖房", "房产");
        aliases.put("搬家", "搬家");
        aliases.put("迁居", "搬家");

        aliases.put("官司", "官司");
        aliases.put("诉讼", "官司");
        aliases.put("纠纷", "官司");
        aliases.put("仲裁", "官司");

        aliases.put("寻物", "寻物");
        aliases.put("失物", "寻物");
        aliases.put("找东西", "寻物");
        return aliases;
    }
}
