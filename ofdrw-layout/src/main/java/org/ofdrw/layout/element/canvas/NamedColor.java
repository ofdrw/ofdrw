package org.ofdrw.layout.element.canvas;

import java.awt.*;

/**
 * 命名的颜色
 *
 * @author 权观宇
 * @since 2023-4-3 22:52:32
 */
public final class NamedColor {

    /**
     * 解析颜色名 或 16进制颜色 RGB颜色 字符串为RGB参数
     * <p>
     * 若颜色无法解析则返回null
     * <p>
     * 若颜色参数包含透明通道，那个返回参数最后一个元素为透明度值 [0,255] 0表示完全透明，255表示完全不透明。
     *
     * @param hex 颜色16进制值、颜色名称
     * @return RGB颜色数组
     */
    public static int[] rgb(String hex) {
        if (hex == null || hex.length() == 0) {
            return null;
        }
        hex = hex.toLowerCase().trim();
        switch (hex) {
            case "aliceblue":
                return new int[]{240, 248, 255};
            case "antiquewhite":
                return new int[]{250, 235, 215};
            case "aqua":
                return new int[]{0, 255, 255};
            case "aquamarine":
                return new int[]{127, 255, 212};
            case "azure":
                return new int[]{240, 255, 255};
            case "beige":
                return new int[]{245, 245, 220};
            case "bisque":
                return new int[]{255, 228, 196};
            case "black":
                return new int[]{0, 0, 0};
            case "blanchedalmond":
                return new int[]{255, 235, 205};
            case "blue":
                return new int[]{0, 0, 255};
            case "blueviolet":
                return new int[]{138, 43, 226};
            case "brown":
                return new int[]{165, 42, 42};
            case "burlywood":
                return new int[]{222, 184, 135};
            case "cadetblue":
                return new int[]{95, 158, 160};
            case "chartreuse":
                return new int[]{127, 255, 0};
            case "chocolate":
                return new int[]{210, 105, 30};
            case "coral":
                return new int[]{255, 127, 80};
            case "cornflowerblue":
                return new int[]{100, 149, 237};
            case "cornsilk":
                return new int[]{255, 248, 220};
            case "crimson":
                return new int[]{220, 20, 60};
            case "cyan":
                return new int[]{0, 255, 255};
            case "darkblue":
                return new int[]{0, 0, 139};
            case "darkcyan":
                return new int[]{0, 139, 139};
            case "darkgoldenrod":
                return new int[]{184, 134, 11};
            case "darkgray":
                return new int[]{169, 169, 169};
            case "darkgreen":
                return new int[]{0, 100, 0};
            case "darkkhaki":
                return new int[]{189, 183, 107};
            case "darkmagenta":
                return new int[]{139, 0, 139};
            case "darkolivegreen":
                return new int[]{85, 107, 47};
            case "darkorange":
                return new int[]{255, 140, 0};
            case "darkorchid":
                return new int[]{153, 50, 204};
            case "darkred":
                return new int[]{139, 0, 0};
            case "darksalmon":
                return new int[]{233, 150, 122};
            case "darkseagreen":
                return new int[]{143, 188, 143};
            case "darkslateblue":
                return new int[]{72, 61, 139};
            case "darkslategray":
                return new int[]{47, 79, 79};
            case "darkturquoise":
                return new int[]{0, 206, 209};
            case "darkviolet":
                return new int[]{148, 0, 211};
            case "deeppink":
                return new int[]{255, 20, 147};
            case "deepskyblue":
                return new int[]{0, 191, 255};
            case "dimgray":
                return new int[]{105, 105, 105};
            case "dodgerblue":
                return new int[]{30, 144, 255};
            case "feldspar":
                return new int[]{209, 146, 117};
            case "firebrick":
                return new int[]{178, 34, 34};
            case "floralwhite":
                return new int[]{255, 250, 240};
            case "forestgreen":
                return new int[]{34, 139, 34};
            case "fuchsia":
                return new int[]{255, 0, 255};
            case "gainsboro":
                return new int[]{220, 220, 220};
            case "ghostwhite":
                return new int[]{248, 248, 255};
            case "gold":
                return new int[]{255, 215, 0};
            case "goldenrod":
                return new int[]{218, 165, 32};
            case "gray":
                return new int[]{128, 128, 128};
            case "green":
                return new int[]{0, 128, 0};
            case "greenyellow":
                return new int[]{173, 255, 47};
            case "honeydew":
                return new int[]{240, 255, 240};
            case "hotpink":
                return new int[]{255, 105, 180};
            case "indianred":
                return new int[]{205, 92, 92};
            case "indigo":
                return new int[]{75, 0, 130};
            case "ivory":
                return new int[]{255, 255, 240};
            case "khaki":
                return new int[]{240, 230, 140};
            case "lavender":
                return new int[]{230, 230, 250};
            case "lavenderblush":
                return new int[]{255, 240, 245};
            case "lawngreen":
                return new int[]{124, 252, 0};
            case "lemonchiffon":
                return new int[]{255, 250, 205};
            case "lightblue":
                return new int[]{173, 216, 230};
            case "lightcoral":
                return new int[]{240, 128, 128};
            case "lightcyan":
                return new int[]{224, 255, 255};
            case "lightgoldenrodyellow":
                return new int[]{250, 250, 210};
            case "lightgrey":
                return new int[]{211, 211, 211};
            case "lightgreen":
                return new int[]{144, 238, 144};
            case "lightpink":
                return new int[]{255, 182, 193};
            case "lightsalmon":
                return new int[]{255, 160, 122};
            case "lightseagreen":
                return new int[]{32, 178, 170};
            case "lightskyblue":
                return new int[]{135, 206, 250};
            case "lightslateblue":
                return new int[]{132, 112, 255};
            case "lightslategray":
                return new int[]{119, 136, 153};
            case "lightsteelblue":
                return new int[]{176, 196, 222};
            case "lightyellow":
                return new int[]{255, 255, 224};
            case "lime":
                return new int[]{0, 255, 0};
            case "limegreen":
                return new int[]{50, 205, 50};
            case "linen":
                return new int[]{250, 240, 230};
            case "magenta":
                return new int[]{255, 0, 255};
            case "maroon":
                return new int[]{128, 0, 0};
            case "mediumaquamarine":
                return new int[]{102, 205, 170};
            case "mediumblue":
                return new int[]{0, 0, 205};
            case "mediumorchid":
                return new int[]{186, 85, 211};
            case "mediumpurple":
                return new int[]{147, 112, 216};
            case "mediumseagreen":
                return new int[]{60, 179, 113};
            case "mediumslateblue":
                return new int[]{123, 104, 238};
            case "mediumspringgreen":
                return new int[]{0, 250, 154};
            case "mediumturquoise":
                return new int[]{72, 209, 204};
            case "mediumvioletred":
                return new int[]{199, 21, 133};
            case "midnightblue":
                return new int[]{25, 25, 112};
            case "mintcream":
                return new int[]{245, 255, 250};
            case "mistyrose":
                return new int[]{255, 228, 225};
            case "moccasin":
                return new int[]{255, 228, 181};
            case "navajowhite":
                return new int[]{255, 222, 173};
            case "navy":
                return new int[]{0, 0, 128};
            case "oldlace":
                return new int[]{253, 245, 230};
            case "olive":
                return new int[]{128, 128, 0};
            case "olivedrab":
                return new int[]{107, 142, 35};
            case "orange":
                return new int[]{255, 165, 0};
            case "orangered":
                return new int[]{255, 69, 0};
            case "orchid":
                return new int[]{218, 112, 214};
            case "palegoldenrod":
                return new int[]{238, 232, 170};
            case "palegreen":
                return new int[]{152, 251, 152};
            case "paleturquoise":
                return new int[]{175, 238, 238};
            case "palevioletred":
                return new int[]{216, 112, 147};
            case "papayawhip":
                return new int[]{255, 239, 213};
            case "peachpuff":
                return new int[]{255, 218, 185};
            case "peru":
                return new int[]{205, 133, 63};
            case "pink":
                return new int[]{255, 192, 203};
            case "plum":
                return new int[]{221, 160, 221};
            case "powderblue":
                return new int[]{176, 224, 230};
            case "purple":
                return new int[]{128, 0, 128};
            case "red":
                return new int[]{255, 0, 0};
            case "rosybrown":
                return new int[]{188, 143, 143};
            case "royalblue":
                return new int[]{65, 105, 225};
            case "saddlebrown":
                return new int[]{139, 69, 19};
            case "salmon":
                return new int[]{250, 128, 114};
            case "sandybrown":
                return new int[]{244, 164, 96};
            case "seagreen":
                return new int[]{46, 139, 87};
            case "seashell":
                return new int[]{255, 245, 238};
            case "sienna":
                return new int[]{160, 82, 45};
            case "silver":
                return new int[]{192, 192, 192};
            case "skyblue":
                return new int[]{135, 206, 235};
            case "slateblue":
                return new int[]{106, 90, 205};
            case "slategray":
                return new int[]{112, 128, 144};
            case "snow":
                return new int[]{255, 250, 250};
            case "springgreen":
                return new int[]{0, 255, 127};
            case "steelblue":
                return new int[]{70, 130, 180};
            case "tan":
                return new int[]{210, 180, 140};
            case "teal":
                return new int[]{0, 128, 128};
            case "thistle":
                return new int[]{216, 191, 216};
            case "tomato":
                return new int[]{255, 99, 71};
            case "turquoise":
                return new int[]{64, 224, 208};
            case "violet":
                return new int[]{238, 130, 238};
            case "violetred":
                return new int[]{208, 32, 144};
            case "wheat":
                return new int[]{245, 222, 179};
            case "white":
                return new int[]{255, 255, 255};
            case "whitesmoke":
                return new int[]{245, 245, 245};
            case "yellow":
                return new int[]{255, 255, 0};
            case "yellowgreen":
                return new int[]{154, 205, 50};
        }

        // 如果是 #xxx 格式的颜色值，转换为 #xxxxxx 格式
        if (hex.charAt(0) == '#' && hex.length() == 4) {
            hex = String.format(
                    "#%c%c%c%c%c%c",
                    hex.charAt(1), hex.charAt(1),
                    hex.charAt(2), hex.charAt(2),
                    hex.charAt(3), hex.charAt(3));
        }
        // 若是 rgb(x,x,x) 格式的颜色值，返回对应的 int 数组
        if (hex.startsWith("rgb(") && hex.endsWith(")")) {
            String[] rgb = hex.substring(4, hex.length() - 1).split(",");
            return new int[]{
                    Integer.parseInt(rgb[0].trim()),
                    Integer.parseInt(rgb[1].trim()),
                    Integer.parseInt(rgb[2].trim())
            };
        }
        // 若是 rgba(x,x,x,x) 格式的颜色值，返回对应的 int 数组
        if (hex.startsWith("rgba(") && hex.endsWith(")")) {
            String[] rgba = hex.substring(5, hex.length() - 1).split(",");
            return new int[]{
                    Integer.parseInt(rgba[0].trim()),
                    Integer.parseInt(rgba[1].trim()),
                    Integer.parseInt(rgba[2].trim()),
                    (int) (255 * Double.parseDouble(rgba[3]))
            };
        }

        // 尝试解析 #xxxxxx 格式的颜色值
        try {
            Color c = Color.decode(hex);
            return new int[]{c.getRed(), c.getGreen(), c.getBlue()};
        } catch (Exception e) {
            // 无法解析时返回黑色
            return null;
        }
    }
}
