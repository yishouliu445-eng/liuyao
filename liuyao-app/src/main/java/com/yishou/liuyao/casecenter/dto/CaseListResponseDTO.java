package com.yishou.liuyao.casecenter.dto;

import java.util.ArrayList;
import java.util.List;

public class CaseListResponseDTO {

    // 简单分页响应，先满足联调和后台筛选，不引入更重的分页抽象。
    private int page;
    private int size;
    private long total;
    private List<CaseSummaryDTO> items = new ArrayList<>();

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

    public List<CaseSummaryDTO> getItems() {
        return items;
    }

    public void setItems(List<CaseSummaryDTO> items) {
        this.items = items;
    }
}
