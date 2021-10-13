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

       return str.replace('\n', Character.MIN_VALUE);
    }
}
