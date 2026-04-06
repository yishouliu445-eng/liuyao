package com.yishou.liuyao.divination.service;

import com.yishou.liuyao.divination.domain.CalendarSnapshot;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalendarFacadeTest {

    @Test
    void shouldResolveGanzhiAndMonthBranchFromSolarTime() {
        CalendarFacade facade = new CalendarFacade();

        CalendarSnapshot snapshot = facade.resolve(LocalDateTime.of(1986, 5, 29, 0, 0));

        assertEquals("癸酉", snapshot.getRiChen());
        assertEquals("巳", snapshot.getYueJian());
        assertEquals(List.of("戌", "亥"), snapshot.getKongWang());
    }
}
