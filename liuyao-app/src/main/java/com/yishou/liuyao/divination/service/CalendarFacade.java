package com.yishou.liuyao.divination.service;

import com.nlf.calendar.Lunar;
import com.nlf.calendar.Solar;
import com.yishou.liuyao.divination.domain.CalendarSnapshot;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
public class CalendarFacade {

    public CalendarSnapshot resolve(LocalDateTime divinationTime) {
        // 第三方历法库负责把公历时间转换成六爻常用的日辰、月建、旬空。
        Solar solar = Solar.fromYmdHms(
                divinationTime.getYear(),
                divinationTime.getMonthValue(),
                divinationTime.getDayOfMonth(),
                divinationTime.getHour(),
                divinationTime.getMinute(),
                divinationTime.getSecond()
        );
        Lunar lunar = solar.getLunar();
        return new CalendarSnapshot(
                lunar.getDayInGanZhiExact(),
                lunar.getMonthZhiExact(),
                splitXunKong(lunar.getDayXunKongExact())
        );
    }

    private List<String> splitXunKong(String xunKong) {
        if (xunKong == null || xunKong.isBlank()) {
            return List.of();
        }
        // 例如“戌亥”会被拆成 ["戌", "亥"]，方便规则层直接 contains 判断。
        return Arrays.stream(xunKong.split("(?<=.)(?=.)"))
                .filter(part -> !part.isBlank())
                .toList();
    }
}
