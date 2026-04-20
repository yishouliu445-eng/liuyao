package com.yishou.liuyao.divination.service;

import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.divination.domain.ShenShaHit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShenShaResolverTest {

    private final ShenShaResolver resolver = new ShenShaResolver();

    @Test
    void shouldResolveClassicShenShaHitsFromRiChen() {
        List<ShenShaHit> hits = resolver.resolve("甲子", List.of(
                line(1, "丑", false),
                line(2, "寅", true),
                line(3, "酉", false),
                line(4, "辰", false),
                line(5, "子", false),
                line(6, "巳", false),
                line(7, "午", false)
        ));

        assertEquals(8, hits.size());
        assertTrue(hits.stream().anyMatch(hit -> "NOBLEMAN".equals(hit.getCode()) && "丑".equals(hit.getBranch()) && hit.getLineIndexes().equals(List.of(1))));
        assertTrue(hits.stream().anyMatch(hit -> "TRAVEL_HORSE".equals(hit.getCode()) && "寅".equals(hit.getBranch()) && hit.getLineIndexes().equals(List.of(2))));
        assertTrue(hits.stream().anyMatch(hit -> "PEACH_BLOSSOM".equals(hit.getCode()) && "酉".equals(hit.getBranch()) && hit.getLineIndexes().equals(List.of(3))));
        assertTrue(hits.stream().anyMatch(hit -> "HUA_GAI".equals(hit.getCode()) && "辰".equals(hit.getBranch()) && hit.getLineIndexes().equals(List.of(4))));
        assertTrue(hits.stream().anyMatch(hit -> "GENERAL_STAR".equals(hit.getCode()) && "子".equals(hit.getBranch()) && hit.getLineIndexes().equals(List.of(5))));
        assertTrue(hits.stream().anyMatch(hit -> "WEN_CHANG".equals(hit.getCode()) && "巳".equals(hit.getBranch()) && hit.getLineIndexes().equals(List.of(6))));
        assertTrue(hits.stream().anyMatch(hit -> "DISASTER_SHA".equals(hit.getCode()) && "午".equals(hit.getBranch()) && hit.getLineIndexes().equals(List.of(7))));
    }

    private LineInfo line(int index, String branch, boolean moving) {
        LineInfo line = new LineInfo();
        line.setIndex(index);
        line.setBranch(branch);
        line.setMoving(moving);
        return line;
    }
}
