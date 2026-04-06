package com.yishou.liuyao.casecenter.domain;

import com.yishou.liuyao.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "case_chart_snapshot")
public class CaseChartSnapshot extends BaseEntity {

    @Column(name = "case_id")
    private Long caseId;

    // 冗余保存关键索引字段，便于后续列表检索和统计，不必每次反序列化整张盘。
    @Column(name = "main_hexagram")
    private String mainHexagram;

    @Column(name = "changed_hexagram")
    private String changedHexagram;

    @Column(name = "main_hexagram_code")
    private String mainHexagramCode;

    @Column(name = "changed_hexagram_code")
    private String changedHexagramCode;

    @Column(name = "palace")
    private String palace;

    @Column(name = "palace_wu_xing")
    private String palaceWuXing;

    @Column(name = "use_god")
    private String useGod;

    @Column(name = "chart_json")
    private String chartJson;

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public String getMainHexagram() {
        return mainHexagram;
    }

    public void setMainHexagram(String mainHexagram) {
        this.mainHexagram = mainHexagram;
    }

    public String getChangedHexagram() {
        return changedHexagram;
    }

    public void setChangedHexagram(String changedHexagram) {
        this.changedHexagram = changedHexagram;
    }

    public String getMainHexagramCode() {
        return mainHexagramCode;
    }

    public void setMainHexagramCode(String mainHexagramCode) {
        this.mainHexagramCode = mainHexagramCode;
    }

    public String getChangedHexagramCode() {
        return changedHexagramCode;
    }

    public void setChangedHexagramCode(String changedHexagramCode) {
        this.changedHexagramCode = changedHexagramCode;
    }

    public String getPalace() {
        return palace;
    }

    public void setPalace(String palace) {
        this.palace = palace;
    }

    public String getPalaceWuXing() {
        return palaceWuXing;
    }

    public void setPalaceWuXing(String palaceWuXing) {
        this.palaceWuXing = palaceWuXing;
    }

    public String getUseGod() {
        return useGod;
    }

    public void setUseGod(String useGod) {
        this.useGod = useGod;
    }

    public String getChartJson() {
        return chartJson;
    }

    public void setChartJson(String chartJson) {
        this.chartJson = chartJson;
    }
}
