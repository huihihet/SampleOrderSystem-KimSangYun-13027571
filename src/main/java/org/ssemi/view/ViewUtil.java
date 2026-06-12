package org.ssemi.view;

public class ViewUtil {

    private ViewUtil() {}

    /** 한글(2칸) 등 와이드 문자를 고려해 표시 폭 기준으로 우측 패딩 */
    public static String padRight(String s, int targetDisplayWidth) {
        int displayW = 0;
        for (char c : s.toCharArray()) {
            displayW += (c >= 0x1100 && c <= 0x11FF)
                     || (c >= 0xAC00 && c <= 0xD7A3)
                     || (c >= 0x3000 && c <= 0x9FFF) ? 2 : 1;
        }
        int pad = targetDisplayWidth - displayW;
        return pad > 0 ? s + " ".repeat(pad) : s;
    }
}
