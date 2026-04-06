package com.yishou.liuyao.analysis.service;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.rule.RuleHit;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnalysisService {

    public String analyze(String question, ChartSnapshot chartSnapshot, List<RuleHit> ruleHits) {
        return "分析模块骨架已就绪，后续可接入受约束的 LLM 编排。";
    }
}
