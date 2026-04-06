package com.yishou.liuyao.divination.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LiuQinResolverTest {

    private final LiuQinResolver resolver = new LiuQinResolver();

    @Test
    void shouldResolveLiuQinAgainstQianPalaceElement() {
        assertEquals("子孙", resolver.resolve("金", "子"));
        assertEquals("妻财", resolver.resolve("金", "寅"));
        assertEquals("父母", resolver.resolve("金", "辰"));
        assertEquals("官鬼", resolver.resolve("金", "午"));
        assertEquals("兄弟", resolver.resolve("金", "申"));
    }
}
