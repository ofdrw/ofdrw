package org.ofdrw.sign;

import java.util.regex.Matcher;

/**
 * 签名ID解析器
 *
 * @author 权观宇
 * @since 2020-10-13 19:38:50
 */
public class SignIdParser {

    /**
     * 解析出电子签名的ID数字
     * <p>
     * 支持标准推荐样式s'NNN'、sN、N三种类型签名ID的解析
     *
     * @param id ID字符串
     * @return ID数字
     */
    public static int parseIndex(String id) {
        Matcher m = SignIDProvider.IDPattern.matcher(id);
        if (m.find()) {
            String idNumStr = m.group(1);
            return Integer.parseInt(idNumStr);
        } else {
            return Integer.parseInt(id);
        }
    }
}
