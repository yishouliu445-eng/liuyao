package com.yishou.liuyao.divination.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PalaceResolverTest {

    private final PalaceResolver resolver = new PalaceResolver();

    @Test
    void shouldResolveQianPalaceAndMetalForGou() {
        PalaceInfo info = resolver.resolve("天风姤");

        assertEquals("乾", info.getPalace());
        assertEquals("金", info.getWuXing());
    }

    @Test
    void shouldResolveKunPalaceAndEarthForTai() {
        PalaceInfo info = resolver.resolve("地天泰");

        assertEquals("坤", info.getPalace());
        assertEquals("土", info.getWuXing());
    }
}
