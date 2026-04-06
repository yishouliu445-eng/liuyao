package com.yishou.liuyao.divination.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NaJiaResolverTest {

    private final NaJiaResolver resolver = new NaJiaResolver();

    @Test
    void shouldResolveBranchesForQianWeiTian() {
        assertEquals(
                List.of("子", "寅", "辰", "午", "申", "戌"),
                resolver.resolve("乾为天", "111111")
        );
    }

    @Test
    void shouldResolveBranchesForTianFengGou() {
        assertEquals(
                List.of("丑", "亥", "酉", "午", "申", "戌"),
                resolver.resolve("天风姤", "111011")
        );
    }
}
