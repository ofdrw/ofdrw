package org.ofdrw.core.text.font;

/**
 * 字形适用的字符分类
 * <p>
 * 用于匹配替代字形
 * <p>
 * 11.1 表 44
 * <p>
 * 附录 A.5 CT_Font
 *
 * @author 权观宇
 * @since 2019-10-18 08:38:14
 */
public enum Charset {
    /**
     * 符号
     */
    symbol,
    prc,
    big5,
    shift_jis,
    wansung,
    johab,
    /**
     * 默认值
     */
    unicode;

    public static Charset getInstance(String name) {
        name = (name == null) ? "" : name.trim();
        // switch (name) {
        //     case "symbol":
        //         return symbol;
        //     case "prc":
        //         return prc;
        //     case "big5":
        //         return big5;
        //     case "shift-jis":
        //         return shift_jis;
        //     case "wansung":
        //         return wansung;
        //     case "johab":
        //         return johab;
        //     case "":
        //     case "unicode":
        //         return unicode;
        //     default:
        //         throw new IllegalArgumentException("未知的字形适用的字符分类：" + name);
        // }
        if (name.equalsIgnoreCase("symbol")) {
            return symbol;
        } else if (name.equalsIgnoreCase("prc")) {
            return prc;
        } else if (name.equalsIgnoreCase("big5")) {
            return big5;
        } else if (name.equalsIgnoreCase("shift-jis")) {
            return shift_jis;
        } else if (name.equalsIgnoreCase("wansung")) {
            return wansung;
        } else if (name.equalsIgnoreCase("johab")) {
            return johab;
        } else if (name.equals("") || name.equalsIgnoreCase("unicode")) {
            return unicode;
        } else {
            throw new IllegalArgumentException("未知的字形适用的字符分类：" + name);
        }
    }
    
    @Override
    public String toString() {
        if (this == shift_jis) {
            return "shift-jis";
        }
        return super.toString();
    }
}
