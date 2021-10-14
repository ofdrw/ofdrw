package org.ofdrw.converter.utils;

public class StringUtils {
    public static boolean isBlank(String str) {
        return str == null || str.length() == 0;
    }

    /**
     * 移除字符串前后的换行符
     *
     * @param str 字符串
     * @return 移除后的字符串
     */
    public static String removeNewline(String str) {
        if (str == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\n' || c == '\u0000'){
                continue;
            }
            sb.append(c);
        }
       return sb.toString();
    }
}
