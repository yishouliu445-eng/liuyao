package com.yishou.liuyao.knowledge.dto;

import java.util.ArrayList;
import java.util.List;

public class BookChunkQueryResponse {

    private List<BookChunkDTO> items = new ArrayList<>();

    public List<BookChunkDTO> getItems() {
        return items;
    }

    public void setItems(List<BookChunkDTO> items) {
        this.items = items;
    }
}
