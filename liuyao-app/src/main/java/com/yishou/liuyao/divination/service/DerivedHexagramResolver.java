package com.yishou.liuyao.divination.service;

import org.springframework.stereotype.Component;

@Component
public class DerivedHexagramResolver {

    private final HexagramResolver hexagramResolver;

    public DerivedHexagramResolver(HexagramResolver hexagramResolver) {
        this.hexagramResolver = hexagramResolver;
    }

    public DerivedHexagramSet resolve(String mainHexagramCode) {
        if (mainHexagramCode == null || mainHexagramCode.length() != 6) {
            return DerivedHexagramSet.empty();
        }
        String mutualCode = buildMutualCode(mainHexagramCode);
        String oppositeCode = invertCode(mainHexagramCode);
        String reversedCode = reverseCode(mainHexagramCode);
        return new DerivedHexagramSet(
                hexagramResolver.describeCode(mutualCode),
                hexagramResolver.describeCode(oppositeCode),
                hexagramResolver.describeCode(reversedCode)
        );
    }

    static String buildMutualCode(String mainHexagramCode) {
        char[] lines = toBottomUpLines(mainHexagramCode);
        String lower = new String(new char[]{lines[1], lines[2], lines[3]});
        String upper = new String(new char[]{lines[2], lines[3], lines[4]});
        return upper + lower;
    }

    static String invertCode(String mainHexagramCode) {
        StringBuilder builder = new StringBuilder(mainHexagramCode.length());
        for (char value : mainHexagramCode.toCharArray()) {
            builder.append(value == '1' ? '0' : '1');
        }
        return builder.toString();
    }

    static String reverseCode(String mainHexagramCode) {
        char[] lines = toBottomUpLines(mainHexagramCode);
        char[] reversedLines = new char[6];
        for (int index = 0; index < lines.length; index++) {
            reversedLines[index] = lines[lines.length - 1 - index];
        }
        String lower = new String(reversedLines, 0, 3);
        String upper = new String(reversedLines, 3, 3);
        return upper + lower;
    }

    private static char[] toBottomUpLines(String mainHexagramCode) {
        return new char[]{
                mainHexagramCode.charAt(3),
                mainHexagramCode.charAt(4),
                mainHexagramCode.charAt(5),
                mainHexagramCode.charAt(0),
                mainHexagramCode.charAt(1),
                mainHexagramCode.charAt(2)
        };
    }

    public record DerivedHexagramSet(HexagramResolver.HexagramDescriptor mutual,
                                     HexagramResolver.HexagramDescriptor opposite,
                                     HexagramResolver.HexagramDescriptor reversed) {
        static DerivedHexagramSet empty() {
            return new DerivedHexagramSet(
                    new HexagramResolver.HexagramDescriptor("未知卦", "", "坤", "坤"),
                    new HexagramResolver.HexagramDescriptor("未知卦", "", "坤", "坤"),
                    new HexagramResolver.HexagramDescriptor("未知卦", "", "坤", "坤")
            );
        }
    }
}
