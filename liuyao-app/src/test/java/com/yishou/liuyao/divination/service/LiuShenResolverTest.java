package com.yishou.liuyao.divination.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LiuShenResolverTest {

    private final LiuShenResolver resolver = new LiuShenResolver();

    @Test
    void shouldStartFromQingLongOnJiaYiDay() {
        assertEquals("青龙", resolver.resolve("甲子", 1));
        assertEquals("玄武", resolver.resolve("乙丑", 6));
    }

    @Test
    void shouldStartFromXuanWuOnRenGuiDay() {
        assertEquals("玄武", resolver.resolve("癸酉", 1));
        assertEquals("白虎", resolver.resolve("壬申", 6));
    }
}
