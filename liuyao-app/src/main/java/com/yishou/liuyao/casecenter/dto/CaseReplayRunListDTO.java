package com.yishou.liuyao.casecenter.dto;

import java.util.ArrayList;
import java.util.List;

public class CaseReplayRunListDTO {

    private int page;
    private int size;
    private long total;
    private List<CaseReplayRunDTO> items = new ArrayList<>();

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

    public List<CaseReplayRunDTO> getItems() {
        return items;
    }

    public void setItems(List<CaseReplayRunDTO> items) {
        this.items = items;
    }
}
