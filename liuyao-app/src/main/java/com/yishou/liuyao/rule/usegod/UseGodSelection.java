package com.yishou.liuyao.rule.usegod;

public class UseGodSelection {

    private final QuestionIntent intent;
    private final UseGodType useGod;
    private final Integer priority;
    private final String scenario;
    private final String note;
    private final String configVersion;

    public UseGodSelection(QuestionIntent intent,
                           UseGodType useGod,
                           Integer priority,
                           String scenario,
                           String note,
                           String configVersion) {
        this.intent = intent;
        this.useGod = useGod;
        this.priority = priority;
        this.scenario = scenario;
        this.note = note;
        this.configVersion = configVersion;
    }

    public QuestionIntent getIntent() {
        return intent;
    }

    public UseGodType getUseGod() {
        return useGod;
    }

    public Integer getPriority() {
        return priority;
    }

    public String getScenario() {
        return scenario;
    }

    public String getNote() {
        return note;
    }

    public String getConfigVersion() {
        return configVersion;
    }
}
