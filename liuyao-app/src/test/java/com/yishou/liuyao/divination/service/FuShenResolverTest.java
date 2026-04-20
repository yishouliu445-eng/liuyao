package com.yishou.liuyao.divination.service;

import com.yishou.liuyao.divination.domain.LineInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FuShenResolverTest {

    private final FuShenResolver resolver = new FuShenResolver(new NaJiaResolver(), new LiuQinResolver());

    @Test
    void shouldBorrowMissingUseGodFromPurePalaceHexagram() {
        List<LineInfo> resolved = resolver.resolve("乾", "金", List.of(
                line(1, "丑", "土", "父母"),
                line(2, "亥", "水", "子孙"),
                line(3, "酉", "金", "兄弟"),
                line(4, "午", "火", "官鬼"),
                line(5, "申", "金", "兄弟"),
                line(6, "戌", "土", "父母")
        ));

        assertNull(resolved.get(0).getFuShenLiuQin());
        assertEquals("妻财", resolved.get(1).getFuShenLiuQin());
        assertEquals("寅", resolved.get(1).getFuShenBranch());
        assertEquals("木", resolved.get(1).getFuShenWuXing());
        assertEquals("子孙", resolved.get(1).getFlyShenLiuQin());
    }

    private LineInfo line(int index, String branch, String wuXing, String liuQin) {
        LineInfo line = new LineInfo();
        line.setIndex(index);
        line.setBranch(branch);
        line.setWuXing(wuXing);
        line.setLiuQin(liuQin);
        return line;
    }
}
