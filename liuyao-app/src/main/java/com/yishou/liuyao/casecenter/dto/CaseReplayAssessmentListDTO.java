package com.yishou.liuyao.casecenter.dto;

import java.util.ArrayList;
import java.util.List;

public class CaseReplayAssessmentListDTO {

    private int page;
    private int size;
    private long total;
    private List<CaseReplayAssessmentDTO> items = new ArrayList<>();

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<CaseReplayAssessmentDTO> getItems() {
        return items;
    }

    public void setItems(List<CaseReplayAssessmentDTO> items) {
        this.items = items;
    }
}
