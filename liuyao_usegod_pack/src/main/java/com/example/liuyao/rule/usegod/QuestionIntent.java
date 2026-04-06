package com.example.liuyao.rule.usegod;

/**
 * 问题意图。
 * 不要把“工作”这种大类直接映射成用神，而是先落到更细的意图层。
 */
public enum QuestionIntent {
    JOB_OPPORTUNITY,   // 求职/录用/offer/入职/岗位机会
    JOB_STABILITY,     // 工作能否稳定、是否会失业、岗位是否保住
    INCOME,            // 工资、收入、奖金、提成、财务回报
    PRESSURE,          // 压力、负担、是否很累
    RELATION,          // 人际、同事、上下级关系
    GROWTH,            // 学习、成长、能力提升
    EXAM,              // 考试、考核、资格证
    HEALTH,            // 健康
    EMOTION,           // 感情
    COOPERATION,       // 合作、签约、项目
    UNKNOWN            // 暂时无法识别
}
