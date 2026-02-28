import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.ofdrw.converter.FontLoader;
import org.ofdrw.converter.SVGMaker;
import org.ofdrw.reader.OFDReader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 测试字体加载和 fontconfig 集成
 * <p>
 * 注意：因为火车票包含个人敏感信息，本测试需要用户自行从12306下载一个电子火车票OFD文件进行测试。
 * <p>
 * 使用说明：
 * 1. 从12306下载一个电子火车票OFD文件
 * 2. 修改下面的 TEST_FILE_PATH 变量为实际的文件路径
 * 3. 移除 @Disabled 注解
 * 4. 运行测试
 */
@Disabled("需要用户提供测试文件路径")
public class OFD2SVGFontTest {

    /**
     * 测试文件路径，请修改为实际的OFD文件路径
     * 示例: /home/user/ticket.ofd
     */
    private static final String TEST_FILE_PATH = null;

    @Test
    public void testUserOFD() throws IOException {
        if (TEST_FILE_PATH == null || TEST_FILE_PATH.isEmpty()) {
            System.out.println("请先设置 TEST_FILE_PATH 为有效的OFD文件路径");
            return;
        }

        // 启用字体加载调试日志（可选）
        // FontLoader.DEBUG = true;

        // 手动指定字体映射 - 直接映射到系统字体文件
        // 楷体 -> AR PL UKai CN (文鼎楷体)
        // 宋体 -> Noto Serif CJK SC (衬线体)
        // 黑体/SimHei -> Noto Sans CJK SC Regular (思源黑体 简体中文 常规体)
        // 等线 -> Noto Sans CJK SC DemiLight (细体)
        FontLoader.getInstance()
                .addSystemFontMapping("楷体", "/usr/share/fonts/truetype/arphic/ukai.ttc")
                .addSystemFontMapping("宋体", "/usr/share/fonts/opentype/noto/NotoSerifCJK-Regular.ttc")
                .addSystemFontMapping("黑体", "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc")
                .addSystemFontMapping("微软雅黑", "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc")
                .addSystemFontMapping("等线", "/usr/share/fonts/opentype/noto/NotoSansCJK-DemiLight.ttc");

        // 扫描项目内字体目录（如果存在）
        File fontDir = new File("src/main/resources/fonts");
        if (fontDir.exists()) {
            FontLoader.getInstance().scanFontDir(fontDir);
        }
        FontLoader.setSimilarFontReplace(true);

        // 输出目录
        String outputDir = "target/font-test-output";

        System.out.println("=== 开始转换 OFD 文件 ===");
        System.out.println("源文件: " + TEST_FILE_PATH);
        System.out.println("输出目录: " + outputDir);
        System.out.println("当前操作系统: " + System.getProperty("os.name"));
        System.out.println("是否为 Linux: " + org.ofdrw.converter.utils.OSinfo.isLinux());

        long start = System.currentTimeMillis();
        toSVG(TEST_FILE_PATH, outputDir);
        long elapsed = System.currentTimeMillis() - start;
        System.out.printf(">>> 转换完成，耗时 %d ms\n", elapsed);
    }

    private static void toSVG(String filename, String dirPath) throws IOException {
        Files.createDirectories(Paths.get(dirPath));
        Path src = Paths.get(filename);
        if (!Files.exists(src)) {
            throw new IOException("OFD 文件不存在: " + filename);
        }
        try (OFDReader reader = new OFDReader(src)) {
            SVGMaker svgMaker = new SVGMaker(reader, 5d);
            svgMaker.config.setDrawBoundary(false);
            svgMaker.config.setClip(false);
            int pageCount = svgMaker.pageSize();
            System.out.println("文档页数: " + pageCount);
            for (int i = 0; i < pageCount; i++) {
                System.out.println("正在转换第 " + (i+1) + " 页...");
                String svg = svgMaker.makePage(i);
                Path dist = Paths.get(dirPath, i + ".svg");
                Files.write(dist, svg.getBytes());
                System.out.println("已生成: " + dist.toAbsolutePath());
                // 简单检查 SVG 内容
                if (svg != null && svg.length() > 0) {
                    System.out.println("SVG 大小: " + svg.length() + " 字符");
                    // 可选：检查是否包含中文字符（基本验证）
                    if (svg.contains("中文") || svg.contains("字体") || svg.contains("glyph")) {
                        System.out.println("SVG 包含字形或中文相关标记");
                    }
                }
            }
        }
    }
}