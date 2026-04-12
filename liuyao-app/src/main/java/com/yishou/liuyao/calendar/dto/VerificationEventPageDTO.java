package com.yishou.liuyao.calendar.dto;

import java.util.List;

public class VerificationEventPageDTO {

    private int page;
    private int size;
    private long total;
    private List<VerificationEventDTO> items;

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

    public List<VerificationEventDTO> getItems() {
        return items;
    }

    public void setItems(List<VerificationEventDTO> items) {
        this.items = items;
    }
}
