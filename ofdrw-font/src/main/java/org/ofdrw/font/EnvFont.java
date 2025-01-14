package org.ofdrw.font;

import java.awt.Font;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 环境变量中的字体
 *
 * @author 权观宇
 * @since 2023-3-2 22:05:45
 */
public final class EnvFont {

    /**
     * 是否初始化完成
     */
    private static volatile boolean isInitialized = false;

    /**
     * 字体缓存
     */
    private static Map<String, Font> fMap;


    /**
     * 字体渲染上线文
     */
    private static FontRenderContext frCtx;

    /**
     * 默认字体
     * <p>
     * 宋体 或 Serif、若都不存在则选择字体文件中出现的第一个
     */
    private static Font defaultFont;

    /**
     * 在当前环境中寻找指定名称的字体
     *
     * @param name 字体名
     * @return 指定名称字体，若不存在则返回空。
     */
    public static Font getFont(String name) {
        if (name == null || name.equals("")) {
            return null;
        }
        initialize();
        name = name.toLowerCase();
        return fMap.get(name);
    }


    /**
     * 字体加载初始化块，仅在首次执行时加载，防止由于并发读取字体造成的NPE。
     */
    private synchronized static void initialize() {
        if (!isInitialized) {
            defaultFont = null;
            // 静态初始化锁防止多线程初始化字体映射异常
            fMap = new HashMap<>();
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Font[] allFonts = ge.getAllFonts();
            Map<String, List<Font>> fontsFamilyMap = Arrays.stream(allFonts).collect(Collectors.groupingBy(Font::getFamily));
            loadFonts(fontsFamilyMap);
            if (fMap.get("宋体") != null) {
                defaultFont = fMap.get("宋体");
            } else if (fMap.get("simsun") != null) {
                defaultFont = fMap.get("simsun");
            } else if (fMap.get("microsoftyahei") != null) {
                defaultFont = fMap.get("microsoftyahei");
            } else if (fMap.get("stheiti-light") != null) {
                defaultFont = fMap.get("stheiti-light");
            } else if (fMap.get("times new roman") != null) {
                defaultFont = fMap.get("times new roman");
            } else if (fMap.get("serif") != null) {
                defaultFont = fMap.get("serif");
            } else if (!fMap.isEmpty()) {
                // 选择第一个字体
                defaultFont = fMap.values().iterator().next();
            }
            isInitialized = true;
        }
    }

    /**
     * 在当前环境中寻找指定名称的字体
     *
     * @param name   字体名
     * @param family 替换字体名
     * @return 指定名称字体，若不存在则返回空。
     */
    public static java.awt.Font getFont(String name, String family) {
        Font res = null;
        if (name != null) {
            res = getFont(name);
        }
        if (res != null) {
            return res;
        }
        if (family != null) {
            res = getFont(family);
        }
        return res;
    }

    /**
     * 设置自定义文字映射
     *
     * @param name 字体名
     * @param font 字体对象
     */
    public static synchronized void setMapping(String name, Font font) {
        if (name == null || name.equals("")) {
            return;
        }
        fMap.put(name, font);
    }


    /**
     * 从目录中加载字体，仅加载以 .otf 或 .ttf 结尾的字体文件，若字体无法加载则忽略并打印错误
     * <p>
     * 首次运行会加载环境变量中的字体，然后以目标目录中的字体文件覆盖环境变量中的字体。
     * <p>
     * 若需要指定默认字体，可以在加载字体后调用 {@link #setDefaultFont(Path)} 方法。
     *
     * @param dirPath 字体文件所有目录
     * @throws IOException IO读写异常
     */
    public static void load(Path dirPath) throws IOException {
        if (dirPath == null || !Files.isDirectory(dirPath)) {
            return;
        }
        initialize();
        // 遍历 dirPath 所有openType字体文件
        try (Stream<Path> walk = Files.walk(dirPath)) {
            Map<String, List<Font>> fontsFamilyMap = walk.filter(p -> {
                String fileName = p.getFileName().toString().toLowerCase();
                return fileName.endsWith(".otf") || fileName.endsWith(".ttf");
            }).map(path -> {
                Font font = null;
                try {
                    font = Font.createFont(Font.TRUETYPE_FONT, path.toFile());
                } catch (Exception e) {
                    // 加载字体失败，打印错误并继续
                    System.err.println("加载字体文件失败：" + path + "，错误：" + e.getMessage());
                }
                return font;
            }).filter(Objects::nonNull).collect(Collectors.groupingBy(Font::getFamily));
            loadFonts(fontsFamilyMap);

        }
    }


    /**
     * 分析字符串大小在指定字号下所占空间大小
     * <p>
     * 若无法找到字体则使用默认字体计算
     *
     * @param name   字体名
     * @param family 替换字体名
     * @param str    待分析字符串
     * @param size   字体大小
     * @return 字符所占区域大小
     */
    public static Rectangle2D strBounds(String name, String family, String str, double size) {
        Font font = getFont(name, family);
        if (font == null) {
            // 找不到字体时使用默认字体计算，防止NPE
            font = defaultFont;
        }
        font = font.deriveFont((float) size);
        return font.getStringBounds(str, FRCtx());
    }


    /**
     * 获取默认字体
     *
     * @return 默认字体
     */
    public static Font getDefaultFont() {
        return defaultFont;
    }

    /**
     * 设置默认字体
     *
     * @param defaultFont 默认字体
     */
    public static void setDefaultFont(Font defaultFont) {
        EnvFont.defaultFont = defaultFont;
    }


    /**
     * 设置默认字体
     *
     * @param path 字体文件路径
     * @throws IOException         IO读写异常
     * @throws FontFormatException 字体格式异常
     */
    public static void setDefaultFont(Path path) throws IOException, FontFormatException {
        if (path == null || !Files.exists(path)) {
            return;
        }
        defaultFont = Font.createFont(Font.TRUETYPE_FONT, path.toFile());
    }

    /**
     * 获取默认字体绘制上下文
     *
     * @return 上下文
     */
    public static FontRenderContext FRCtx() {
        if (frCtx == null) {
            synchronized (EnvFont.class) {
                frCtx = new FontRenderContext(new AffineTransform(), true, true);
            }
        }
        return frCtx;
    }

    /**
     * 加载字体
     *
     * @param fontsFamilyMap 包含字体数据的Map
     */
    private static void loadFonts(Map<String, List<Font>> fontsFamilyMap) {
        for (Map.Entry<String, List<Font>> fontEntry : fontsFamilyMap.entrySet()) {
            List<Font> fonts = fontEntry.getValue();
            // 常规字体中 FontName 等于 FamilyName
            boolean containsNormalFont = fonts.stream().anyMatch(font -> font.getFontName().equals(font.getFamily()));
            for (Font font : fonts) {
                fMap.put(font.getFontName().toLowerCase(), font);
                // Font Family 表示字体系列，如 Serif
                // Font Name 表示系列下的不同样式，如 Serif.bold、Serif.italic
                // 如果安装了常规字体就不再添加，防止正常字体被覆盖。
                if (!containsNormalFont && !fMap.containsKey(font.getFamily().toLowerCase())) {
                    fMap.put(font.getFamily().toLowerCase(), font);
                }
            }
        }
    }

}
