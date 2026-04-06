package com.yishou.liuyao.divination.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HexagramResolverTest {

    private final HexagramResolver resolver = new HexagramResolver();

    @Test
    void shouldResolveQianForSixYoungYangLines() {
        HexagramResult result = resolver.resolve(List.of("少阳", "少阳", "少阳", "少阳", "少阳", "少阳"));

        assertEquals("乾为天", result.getMainHexagramName());
        assertEquals("乾为天", result.getChangedHexagramName());
        assertEquals("111111", result.getMainHexagramCode());
        assertEquals("111111", result.getChangedHexagramCode());
    }

    @Test
    void shouldResolveGouWhenBottomLineChangesFromQian() {
        HexagramResult result = resolver.resolve(List.of("老阳", "少阳", "少阳", "少阳", "少阳", "少阳"));

        assertEquals("乾为天", result.getMainHexagramName());
        assertEquals("天风姤", result.getChangedHexagramName());
        assertEquals("111011", result.getChangedHexagramCode());
    }

    @Test
    void shouldResolveKunForSixYoungYinLines() {
        HexagramResult result = resolver.resolve(List.of("少阴", "少阴", "少阴", "少阴", "少阴", "少阴"));

        assertEquals("坤为地", result.getMainHexagramName());
        assertEquals("坤为地", result.getChangedHexagramName());
    }
}
