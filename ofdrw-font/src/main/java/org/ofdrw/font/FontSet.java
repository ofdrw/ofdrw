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
            Path path = loadAndCacheFont(fileName);
            switch (fontName) {
                case NotoSerif:
                    return new Font("NotoSerifCJKsc", "Medium", path);
                case NotoSerifBold:
                    return new Font("NotoSerifCJKsc", "Bold", path);
                case NotoSans:
                    return new Font("NotoSansCJKsc", "Medium", path);
                case NotoSansBold:
                    return new Font("NotoSansCJKsc", "Bold", path);
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
