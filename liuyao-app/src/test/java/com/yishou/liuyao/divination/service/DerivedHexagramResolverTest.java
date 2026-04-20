package com.yishou.liuyao.divination.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DerivedHexagramResolverTest {

    private final DerivedHexagramResolver resolver = new DerivedHexagramResolver(new HexagramResolver());

    @Test
    void shouldResolveDerivedHexagramsForPureQian() {
        DerivedHexagramResolver.DerivedHexagramSet result = resolver.resolve("111111");

        assertEquals("乾为天", result.mutual().name());
        assertEquals("111111", result.mutual().code());
        assertEquals("坤为地", result.opposite().name());
        assertEquals("000000", result.opposite().code());
        assertEquals("乾为天", result.reversed().name());
        assertEquals("111111", result.reversed().code());
    }

    @Test
    void shouldResolveDerivedHexagramsForMountainFireBi() {
        DerivedHexagramResolver.DerivedHexagramSet result = resolver.resolve("001101");

        assertEquals("雷水解", result.mutual().name());
        assertEquals("100010", result.mutual().code());
        assertEquals("泽水困", result.opposite().name());
        assertEquals("110010", result.opposite().code());
        assertEquals("火雷噬嗑", result.reversed().name());
        assertEquals("101100", result.reversed().code());
    }
}
