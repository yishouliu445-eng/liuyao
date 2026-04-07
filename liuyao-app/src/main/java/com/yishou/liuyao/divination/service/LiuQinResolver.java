package com.yishou.liuyao.divination.service;

import org.springframework.stereotype.Component;

@Component
public class LiuQinResolver {

    public String resolve(String palaceWuXing, String branch) {
        String lineWuXing = WuXingSupport.branchToWuXing(branch);
        if (palaceWuXing == null || lineWuXing == null) {
            return "待定";
        }
        // 这里以“宫五行为我”来判断同我、生我、我生、我克、克我。
        if (palaceWuXing.equals(lineWuXing)) {
            return "兄弟";
        }
        if (WuXingSupport.generates(palaceWuXing, lineWuXing)) {
            return "子孙";
        }
        if (WuXingSupport.generates(lineWuXing, palaceWuXing)) {
            return "父母";
        }
        if (WuXingSupport.controls(palaceWuXing, lineWuXing)) {
            return "妻财";
        }
        if (WuXingSupport.controls(lineWuXing, palaceWuXing)) {
            return "官鬼";
        }
        return "待定";
    }
}
