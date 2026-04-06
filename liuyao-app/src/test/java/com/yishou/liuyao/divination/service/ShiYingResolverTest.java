package com.yishou.liuyao.divination.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShiYingResolverTest {

    private final ShiYingResolver resolver = new ShiYingResolver();

    @Test
    void shouldResolveShiYingForQianWeiTian() {
        ShiYingPosition position = resolver.resolve("乾为天");

        assertEquals(6, position.getShiIndex());
        assertEquals(3, position.getYingIndex());
    }

    @Test
    void shouldResolveShiYingForTianFengGou() {
        ShiYingPosition position = resolver.resolve("天风姤");

        assertEquals(1, position.getShiIndex());
        assertEquals(4, position.getYingIndex());
    }
}
