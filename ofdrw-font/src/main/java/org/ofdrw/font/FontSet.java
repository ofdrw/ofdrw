package org.ofdrw.font;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 字体集合
 *
 * @author 权观宇
 * @since 2020-03-18 18:45:15
 */
final public class FontSet {

    /**
     * 可打印的ASCII表字母宽度所占用百分比
     * <p>
     * ASCII区间: [32,126]
     * <p>
     * 其中空格特殊处理，默认为半个字符宽度也就是 0.5
     */
    public static final double[] NOTO_PRINTABLE_ASCII_WIDTH_MAP = {
            0.5, 0.3125, 0.435546875, 0.63818359375, 0.58642578125, 0.8896484375, 0.8701171875, 0.25634765625, 0.333984375, 0.333984375, 0.455078125, 0.74169921875, 0.24072265625, 0.4326171875, 0.24072265625, 0.42724609375, 0.58642578125, 0.58642578125, 0.58642578125, 0.58642578125, 0.58642578125, 0.58642578125, 0.58642578125, 0.58642578125, 0.58642578125, 0.58642578125, 0.24072265625, 0.24072265625, 0.74169921875, 0.74169921875, 0.74169921875, 0.48291015625, 1.03125, 0.70361328125, 0.62744140625, 0.6689453125, 0.76171875, 0.5498046875, 0.53125, 0.74365234375, 0.7734375, 0.2939453125, 0.39599609375, 0.634765625, 0.51318359375, 0.97705078125, 0.81298828125, 0.81494140625, 0.61181640625, 0.81494140625, 0.65283203125, 0.5771484375, 0.5732421875, 0.74658203125, 0.67626953125, 1.017578125, 0.64501953125, 0.603515625, 0.6201171875, 0.333984375, 0.416015625, 0.333984375, 0.74169921875, 0.4482421875, 0.294921875, 0.552734375, 0.638671875, 0.50146484375, 0.6396484375, 0.5673828125, 0.3466796875, 0.6396484375, 0.61572265625, 0.26611328125, 0.26708984375, 0.54443359375, 0.26611328125, 0.93701171875, 0.6162109375, 0.6357421875, 0.638671875, 0.6396484375, 0.3818359375, 0.462890625, 0.37255859375, 0.6162109375, 0.52490234375, 0.78955078125, 0.5068359375, 0.529296875, 0.49169921875, 0.333984375, 0.26904296875, 0.333984375, 0.74169921875
    };

    /**
     * Time New Roman 字体宽度比例
     */
    public static final double[] TIMES_NEW_ROMAN_PRINTABLE_ASCII_MAP = {
            0.25, 0.3330078125, 0.408203125, 0.5, 0.5, 0.8330078125, 0.77783203125, 0.18017578125, 0.3330078125, 0.3330078125, 0.5, 0.56396484375, 0.25, 0.3330078125, 0.25, 0.27783203125, 0.5, 0.46326171875, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.27783203125, 0.27783203125, 0.56396484375, 0.56396484375, 0.56396484375, 0.44384765625, 0.9208984375, 0.72216796875, 0.6669921875, 0.6669921875, 0.72216796875, 0.61083984375, 0.55615234375, 0.72216796875, 0.72216796875, 0.3330078125, 0.38916015625, 0.72216796875, 0.61083984375, 0.88916015625, 0.72216796875, 0.72216796875, 0.55615234375, 0.72216796875, 0.6669921875, 0.55615234375, 0.61083984375, 0.72216796875, 0.72216796875, 0.94384765625, 0.72216796875, 0.72216796875, 0.61083984375, 0.3330078125, 0.27783203125, 0.3330078125, 0.46923828125, 0.5, 0.3330078125, 0.44384765625, 0.5, 0.44384765625, 0.5, 0.44384765625, 0.3151220703125, 0.5, 0.5, 0.27783203125, 0.27783203125, 0.5, 0.27783203125, 0.77783203125, 0.5, 0.5, 0.5, 0.5, 0.3330078125, 0.38916015625, 0.27783203125, 0.5, 0.5, 0.72216796875, 0.5, 0.5, 0.44384765625, 0.47998046875, 0.2001953125, 0.47998046875, 0.541015625,
    };

    /**
     * 通过文件名获取字体
     *
     * @param fontName 字体名称
     * @return 字体对象
     */
    public static Font get(FontName fontName) {
        try {
            if (fontName == null) {
                // 默认使用思源宋体字体
                fontName = FontName.NotoSerif;
            }
            String fileName = fontName.getFilename();

            Path path = null;
            if (fileName != null && !fileName.trim().isEmpty()) {
                // 从jar包中加载字体
                path = loadAndCacheFont(fileName);
            }

            switch (fontName) {
                case NotoSerif:
                    return new Font("Noto Serif CJK SC", null, path, NOTO_PRINTABLE_ASCII_WIDTH_MAP);
                case NotoSerifBold:
                    return new Font("Noto Serif CJK SC Bold", "Bold", path, NOTO_PRINTABLE_ASCII_WIDTH_MAP);
                case NotoSans:
                    return new Font("Noto Sans Mono CJK SC Regular", null, path, NOTO_PRINTABLE_ASCII_WIDTH_MAP);
                case NotoSansBold:
                    return new Font("Noto Sans Mono CJK SC Bold", "Bold", path, NOTO_PRINTABLE_ASCII_WIDTH_MAP);
                case SimSun:
                    return new Font("宋体", "宋体");
                case SimHei:
                    return new Font("黑体", "黑体");
                case KaiTi:
                    return new Font("楷体", "楷体");
                case MSYahei:
                    return new Font("微软雅黑", "微软雅黑");
                case FangSong:
                    return new Font("仿宋", "仿宋");
                case TimesNewRoman:
                    return new Font("Times New Roman", "Times New Roman", null, TIMES_NEW_ROMAN_PRINTABLE_ASCII_MAP);
            }
            throw new IllegalArgumentException("不支持字体：" + fontName);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 加载并缓存字体文件
     *
     * @param fileName 字体文件名称
     * @return 缓存的字体文件路径
     * @throws IOException 流转移时IO异常
     */
    private static Path loadAndCacheFont(String fileName) throws IOException {
        String tempPath = System.getProperty("java.io.tmpdir") + File.separator;
        Path path = Paths.get(tempPath, fileName);
        if (Files.notExists(path)) {
            // 文件不存在那么创建文件
            Files.createFile(path);
            // 缓存到临时文件中
            try (InputStream in = FontSet.class.getResourceAsStream("/" + fileName);
                 OutputStream out = Files.newOutputStream(path)) {
                IOUtils.copy(in, out);
            }
        }
        return path;
    }
}
