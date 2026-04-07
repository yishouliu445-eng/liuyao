package com.yishou.liuyao.knowledge.dto;

import java.util.ArrayList;
import java.util.List;

public class KnowledgeReferenceQueryResponse {

    private List<KnowledgeReferenceDTO> items = new ArrayList<>();

    public List<KnowledgeReferenceDTO> getItems() {
        return items;
    }

    public void setItems(List<KnowledgeReferenceDTO> items) {
        this.items = items;
    }
}
