package org.ofdrw.converter.ofdconverter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ofdrw.reader.OFDReader;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link PDFGraphicsConverter}（图形化 PDF → OFD）的单元测试
 * <p>
 * 验证要点：
 * <ol>
 *     <li>转换成功并生成合法的 OFD 包；</li>
 *     <li>产物<b>不含</b> TextObject、<b>不嵌入</b>字体，文字全部展开为路径（“当作图片”）；</li>
 *     <li>页面尺寸与 PDF 页面一致（含 /Rotate 90 的横向页）；</li>
 *     <li>与 {@link PDFConverter}（文字对象化）并存、互不影响。</li>
 * </ol>
 *
 * @author ofdrw
 */
class PDFGraphicsConverterTest {

    /**
     * 页面尺寸换算常数：每毫米对应的 PDF 用户单元数（25.4/72），单位：user unit/mm
     */
    private static final double UU_PMM = 2.8346d;

    /**
     * 尺寸比较容差，单位：毫米
     */
    private static final double TOL_MM = 0.5d;

    /**
     * 转换普通多页 PDF：应生成 OFD，且不含文字对象与字体
     *
     * @param tempDir 临时目录
     * @throws Exception 转换异常
     */
    @Test
    @DisplayName("转换普通PDF：产物不含文字对象与字体，文字全部展开为路径")
    void convertProducesPathsOnly(@TempDir Path tempDir) throws Exception {
        Path src = Paths.get("src/test/resources/Test.pdf");
        Path dst = tempDir.resolve("graphics-test.ofd");
        try (PDFGraphicsConverter converter = new PDFGraphicsConverter(dst)) {
            converter.convert(src);
        }
        assertTrue(Files.exists(dst), "应当生成 OFD 文件");
        assertTrue(Files.size(dst) > 0, "OFD 文件不应为空");

        assertEquals(0, countElementsInFile(dst, "TextObject"),
                "图形化转换不应产生 TextObject");
        assertTrue(countElementsInFile(dst, "PathObject") > 0,
                "图形化转换应产生 PathObject");
        assertEquals(0, countEmbeddedFonts(dst), "图形化转换不应嵌入字体");
    }

    /**
     * 页面尺寸应与 PDF 页面一致（A4 → 约 210 × 297 毫米）
     *
     * @param tempDir 临时目录
     * @throws Exception 转换异常
     */
    @Test
    @DisplayName("页面尺寸与PDF一致：A4页面转换为约210×297毫米")
    void pageSizeMatchesPdf(@TempDir Path tempDir) throws Exception {
        Path src = Paths.get("src/test/resources/Test.pdf");
        Path dst = tempDir.resolve("graphics-size.ofd");
        try (PDFGraphicsConverter converter = new PDFGraphicsConverter(dst)) {
            converter.convert(src);
        }
        double[] size = firstPageSizeMm(dst);
        assertTrue(size[0] > 0 && size[1] > 0, "页面尺寸应为正数");
        // A4 的两个边长（宽高可能互换，取决于页面方向）
        assertTrue(near(size[0], 210d) || near(size[0], 297d),
                "页面宽度应接近 A4 边长，实际=" + size[0]);
        assertTrue(near(size[1], 210d) || near(size[1], 297d),
                "页面高度应接近 A4 边长，实际=" + size[1]);
    }

    /**
     * /Rotate 90 的纵向页应转换为横向 OFD 页面（宽 > 高）
     * <p>
     * 对应旧版本缺陷：旋转页被按纵向生成（commit 57c65e32 修复）。
     *
     * @param tempDir 临时目录
     * @throws Exception 转换异常
     */
    @Test
    @DisplayName("页面旋转：/Rotate 90 的纵向页正确生成横向OFD页面")
    void rotatedPageBecomesLandscape(@TempDir Path tempDir) throws Exception {
        Path src = Paths.get("src/test/resources/xbrl-001.pdf");
        Path dst = tempDir.resolve("graphics-rotate.ofd");
        try (PDFGraphicsConverter converter = new PDFGraphicsConverter(dst)) {
            converter.convert(src);
        }
        double[] size = firstPageSizeMm(dst);
        assertTrue(size[0] > size[1],
                "旋转页应转换为横向（宽 > 高），实际 " + size[0] + " × " + size[1] + " mm");
    }

    /**
     * 指定页码转换：只输出被选中的页，且支持乱序与重复
     *
     * @param tempDir 临时目录
     * @throws Exception 转换异常
     */
    @Test
    @DisplayName("指定页码转换：只输出选中页，支持乱序与重复")
    void convertSelectedPages(@TempDir Path tempDir) throws Exception {
        Path src = Paths.get("src/test/resources/pdf-textobj/dense-text-perf.pdf");
        Path dst = tempDir.resolve("graphics-pages.ofd");
        try (PDFGraphicsConverter converter = new PDFGraphicsConverter(dst)) {
            converter.convert(src, 1, 0, 0);
        }
        try (OFDReader reader = new OFDReader(dst)) {
            assertEquals(3, reader.getNumberOfPages(), "应按页码序列输出 3 页");
        }
    }

    /**
     * 页码越界应抛出异常
     *
     * @param tempDir 临时目录
     */
    @Test
    @DisplayName("页码越界：抛出异常并终止转换")
    void outOfRangePageRejected(@TempDir Path tempDir) {
        Path src = Paths.get("src/test/resources/Test.pdf");
        Path dst = tempDir.resolve("graphics-bad.ofd");
        assertThrows(Exception.class, () -> {
            try (PDFGraphicsConverter converter = new PDFGraphicsConverter(dst)) {
                converter.convert(src, 9999);
            }
        }, "页码越界应抛出异常");
    }

    /**
     * 与现有实现并存：本类的图形化产物不含文字对象
     * <p>
     * 仅验证本类一侧的输出结构（不依赖 {@code PDFConverter} 的运行，避免与其内部演进耦合）；
     * 两套实现并存体现在它们是完全独立的类，可在同一工程内并行使用。
     *
     * @param tempDir 临时目录
     * @throws Exception 转换异常
     */
    @Test
    @DisplayName("图形化产物无文字对象：全部为路径，与文字对象化方案区分")
    void graphicsOutputHasNoTextObject(@TempDir Path tempDir) throws Exception {
        Path src = Paths.get("src/test/resources/pdf-textobj/dense-text-perf.pdf");
        Path graphicsOut = tempDir.resolve("graphics.ofd");

        try (PDFGraphicsConverter graphics = new PDFGraphicsConverter(graphicsOut)) {
            graphics.convert(src);
        }
        assertTrue(Files.exists(graphicsOut), "应产出 OFD");
        assertEquals(0, countElementsInFile(graphicsOut, "TextObject"),
                "图形化转换不应产生 TextObject");
        assertTrue(countElementsInFile(graphicsOut, "PathObject") > 0,
                "图形化转换应把文字展开为 PathObject");
    }

    /**
     * 统计 OFD 包内的字体文件数量
     *
     * @param ofd  OFD 文件路径
     * @return 字体文件数量
     */
    private static int countEmbeddedFonts(Path ofd) throws Exception {
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(ofd.toFile())) {
            int count = 0;
            java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName().toLowerCase();
                if (name.endsWith(".ttf") || name.endsWith(".otf") || name.endsWith(".ttc")) {
                    count++;
                }
            }
            return count;
        }
    }

    /**
     * 取首页的 PhysicalBox 尺寸
     *
     * @param ofd OFD 文件路径
     * @return 长度为 2 的数组：{宽度 mm, 高度 mm}
     * @throws Exception 读取失败
     */
    private static double[] firstPageSizeMm(Path ofd) throws Exception {
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(ofd.toFile())) {
            java.util.zip.ZipEntry entry = zip.getEntry("Doc_0/Pages/Page_1/Content.xml");
            if (entry == null) {
                entry = zip.getEntry("Doc_0/Pages/Page_0/Content.xml");
            }
            assertNotNull(entry, "应存在页面内容文件");
            String xml;
            try (InputStream in = zip.getInputStream(entry)) {
                xml = readFully(in);
            }
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("<ofd:PhysicalBox>([^<]+)</ofd:PhysicalBox>").matcher(xml);
            assertTrue(m.find(), "应存在 PhysicalBox");
            String[] parts = m.group(1).trim().split("\\s+");
            assertEquals(4, parts.length, "PhysicalBox 应包含 4 个数值");
            return new double[]{Double.parseDouble(parts[2]), Double.parseDouble(parts[3])};
        }
    }

    /**
     * 统计指定 OFD 文件全部页面内容中的图元出现次数
     *
     * @param ofd     OFD 文件路径
     * @param tagName OFD 元素名
     * @return 出现次数
     * @throws Exception 读取失败
     */
    private static int countElementsInFile(Path ofd, String tagName) throws Exception {
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(ofd.toFile())) {
            int count = 0;
            for (java.util.zip.ZipEntry entry : java.util.Collections.list(zip.entries())) {
                if (!entry.getName().endsWith("Content.xml")) {
                    continue;
                }
                try (InputStream in = zip.getInputStream(entry)) {
                    count += readFully(in).split(tagName, -1).length - 1;
                }
            }
            return count;
        }
    }

    /**
     * 读取整个输入流并解码为 UTF-8 字符串
     * <p>
     * JDK 8 兼容：{@code InputStream.readAllBytes()} 为 JDK 9 新增 API，此处手工读取。
     *
     * @param in 输入流，由调用方负责关闭
     * @return 输入流全部内容的 UTF-8 字符串
     * @throws java.io.IOException 读取失败
     */
    private static String readFully(InputStream in) throws java.io.IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) != -1) {
            out.write(buf, 0, len);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    /**
     * 浮点近似比较
     *
     * @param actual   实际值
     * @param expected 期望值
     * @return true 表示差值在容差内
     */
    private static boolean near(double actual, double expected) {
        return Math.abs(actual - expected) <= TOL_MM;
    }
}
