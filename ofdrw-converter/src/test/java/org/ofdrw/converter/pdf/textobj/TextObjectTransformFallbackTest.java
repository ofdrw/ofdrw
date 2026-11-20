package org.ofdrw.converter.pdf.textobj;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.util.Matrix;
import org.junit.jupiter.api.Test;
import org.ofdrw.converter.ofdconverter.PDFConverter;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicStructure.pageObj.layer.PageBlockType;
import org.ofdrw.core.basicStructure.pageObj.layer.block.CT_PageBlock;
import org.ofdrw.core.basicStructure.pageObj.layer.block.PathObject;
import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.core.pageDescription.CT_GraphicUnit;
import org.ofdrw.reader.OFDReader;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PDF → OFD 文字变换分流回归
 * <p>
 * 按文字变换形态决定走 {@code TextObject}（带 {@code CTM}）还是回退轮廓绘制：
 * <ul>
 *     <li>普通横排文字：不带 {@code CTM} 的 {@code TextObject}；</li>
 *     <li>仅轴向的非等比缩放（横向拉宽 / 压扁）：带 {@code CTM} 的 {@code TextObject}；</li>
 *     <li>旋转、镜像：回退为 {@code PathObject}（旋转的 X/Y 定位语义未明，镜像在参考查看器中渲染空白）；</li>
 *     <li>回退轮廓的坐标落在页面内、与 PDF 中的摆放位置一致，未贴到页面左上角。</li>
 * </ul>
 * 文字矩阵按 PDF 规则由 {@code Tf} 提供字号，{@code Tm} 只给出缩放/旋转，二者不重复相乘。
 *
 * @author 权观宇
 * @since 2.5.1
 */
class TextObjectTransformFallbackTest {

    /**
     * 测试用 TrueType 字体（仓库测试资源，随用例分发，不依赖操作系统字体）
     */
    private static final Path FONT = Paths.get("src/test/resources/font_13132_0_edit.ttf");

    /**
     * 页面尺寸换算：1 mm 对应的 PDF 用户空间单位（pt）
     */
    private static final double PT_PER_MM = 2.8346d;

    @Test
    void nonUniformScaledTextFallsBackToPath() throws Exception {
        Path dir = Paths.get("target", "textobj-transform-fallback");
        Files.createDirectories(dir);
        Path pdf = dir.resolve("non-uniform-text.pdf");
        Path ofd = dir.resolve("non-uniform-text.ofd");
        Files.deleteIfExists(ofd);

        // 1. 构造 PDF：同一字体，普通横排 + 拉宽 + 压扁 + 旋转，纵向依次排列
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            PDType0Font font = PDType0Font.load(doc, new FileInputStream(FONT.toFile()), false);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(font, 12);
                cs.newLineAtOffset(60, 760);
                cs.showText("NORMAL");
                cs.endText();
                // 横向拉宽 1.29 倍：字号由 Tf 提供，Tm 只给缩放
                cs.beginText();
                cs.setFont(font, 12);
                cs.setTextMatrix(new Matrix(1.29f, 0f, 0f, 1f, 120f, 680f));
                cs.showText("WIDE");
                cs.endText();
                // 压扁 0.5 倍
                cs.beginText();
                cs.setFont(font, 12);
                cs.setTextMatrix(new Matrix(0.5f, 0f, 0f, 1f, 120f, 580f));
                cs.showText("NARROW");
                cs.endText();
                // 旋转 30 度
                double rad = Math.toRadians(30);
                cs.beginText();
                cs.setFont(font, 12);
                cs.setTextMatrix(new Matrix((float) Math.cos(rad), (float) Math.sin(rad),
                        (float) -Math.sin(rad), (float) Math.cos(rad), 260f, 520f));
                cs.showText("ROTATED");
                cs.endText();
            }
            doc.save(pdf.toFile());
        }

        // 2. 转换
        try (PDFConverter converter = new PDFConverter(ofd)) {
            converter.convert(pdf);
        }

        // 3. 断言：仅普通横排文字保留为文字对象，且不带 CTM
        List<PageBlockType> blocks = readBlocks(ofd);
        List<TextObject> texts = new ArrayList<>();
        int paths = 0;
        for (PageBlockType b : blocks) {
            if (b instanceof TextObject) {
                texts.add((TextObject) b);
            } else if (b instanceof PathObject) {
                paths++;
            }
        }
        assertEquals(3, texts.size(), "普通横排与仅轴向的非等比缩放文字都应为文字对象，旋转文字回退");
        TextObject normal = texts.get(0);
        assertEquals("NORMAL", content(normal), "普通横排文字应保持文字对象");
        assertNull(normal.getCTM(), "普通横排文字不应带 CTM");
        // 非等比缩放由 CTM 的 x 列表达
        TextObject wide = texts.get(1);
        assertEquals("WIDE", content(wide), "拉宽文字应为带 CTM 的文字对象");
        assertNotNull(wide.getCTM(), "拉宽文字应带 CTM");
        assertEquals(1.29d, wide.getCTM().get(0), 1e-2, "CTM 的 a 分量应等于横向缩放比");
        TextObject narrow = texts.get(2);
        assertEquals("NARROW", content(narrow), "压扁文字应为带 CTM 的文字对象");
        assertEquals(0.5d, narrow.getCTM().get(0), 1e-2, "CTM 的 a 分量应等于横向缩放比");
        assertTrue(paths >= 1, "旋转文字应回退为轮廓路径，实际路径对象数：" + paths);

        // 4. 断言：文字对象落在 PDF 中的实际摆放位置，未贴到页面左上角
        /*
         * OFD 页面 y 轴向下：PDF 基线 680pt（页面高 792pt）对应页面坐标 (792 - 680) / PT_PER_MM ≈ 39.5 mm，
         * 拉宽文字的字形从该基线向上延伸；x 从 PDF 摆放位置 120pt ≈ 42.3 mm 开始。
         * 若位置丢失，字形会被写到页面原点附近，本断言即失败。
         */
        double[] wideOrigin = textOrigin(wide);
        assertEquals(42.33d, wideOrigin[0], 0.5d, "拉宽文字的首字形页面 x 应与 PDF 摆放位置一致");
        assertEquals(39.51d, wideOrigin[1], 0.5d, "拉宽文字的首字形页面 y 应与 PDF 摆放基线一致");

        // 5. 断言：旋转文字回退为轮廓路径，且落在 PDF 中的摆放位置
        PathObject rotated = pathIn(blocks, 85d, 120d, 85d, 105d);
        assertNotNull(rotated, "旋转文字应回退为轮廓路径，并落在 PDF 摆放的基线附近");
    }

    /**
     * 计算首个字形在页面上的坐标
     * <p>
     * 页面坐标 = 外接矩形左上角 + CTM · (TextCode.X, TextCode.Y)。
     *
     * @param text 文字对象
     * @return {x, y}，单位 mm（OFD 页面坐标）
     */
    private static double[] textOrigin(TextObject text) {
        org.ofdrw.core.basicType.ST_Box box = text.getBoundary();
        org.ofdrw.core.text.TextCode code = text.getTextCodes().get(0);
        double x = code.getX();
        double y = code.getY();
        if (text.getCTM() != null) {
            double a = text.getCTM().get(0);
            double b = text.getCTM().get(1);
            double c = text.getCTM().get(2);
            double d = text.getCTM().get(3);
            double tx = a * x + c * y;
            double ty = b * x + d * y;
            x = tx;
            y = ty;
        }
        return new double[]{box.getTopLeftX() + x, box.getTopLeftY() + y};
    }

    /**
     * 读取 OFD 第一页全部页面块（含嵌套页块）
     *
     * @param ofd OFD 文件
     * @return 页面块列表
     * @throws Exception 读取失败
     */
    private static List<PageBlockType> readBlocks(Path ofd) throws Exception {
        List<PageBlockType> out = new ArrayList<>();
        try (OFDReader reader = new OFDReader(ofd)) {
            Page page = reader.getPage(1);
            assertNotNull(page, "应存在第 1 页");
            for (Object layer : page.getContent().getLayers()) {
                collect(((CT_PageBlock) layer).getPageBlocks(), out);
            }
        }
        assertTrue(out.size() > 0, "转换结果应包含页面块");
        return out;
    }

    /**
     * 递归收集页面块
     *
     * @param blocks 页面块
     * @param out    输出列表
     */
    private static void collect(List<PageBlockType> blocks, List<PageBlockType> out) {
        if (blocks == null) {
            return;
        }
        for (PageBlockType b : blocks) {
            out.add(b);
            if (b instanceof CT_PageBlock) {
                collect(((CT_PageBlock) b).getPageBlocks(), out);
            }
        }
    }

    /**
     * 取起点落在指定区域内的首个路径对象
     * <p>
     * 文字按字形逐个回退为路径，字形路径的起点即该字形左下角；本用例每类文字独立摆放，
     * 因此可用“起点落在该文字摆放区域”的方式取出代表路径。
     *
     * @param blocks 页面块
     * @param xMin   横向区域下界，单位 mm（OFD 页面坐标）
     * @param xMax   横向区域上界，单位 mm（OFD 页面坐标）
     * @param yMin   纵向区域下界，单位 mm（OFD 页面坐标，y 向下）
     * @param yMax   纵向区域上界，单位 mm（OFD 页面坐标，y 向下）
     * @return 首个落在区域内的路径对象；没有时返回 null
     */
    private static PathObject pathIn(List<PageBlockType> blocks, double xMin, double xMax,
                                    double yMin, double yMax) {
        for (PageBlockType b : blocks) {
            if (!(b instanceof PathObject)) {
                continue;
            }
            double[] box = dataBox((PathObject) b);
            if (box == null) {
                continue;
            }
            if (box[0] >= xMin && box[0] <= xMax && box[1] >= yMin && box[1] <= yMax) {
                return (PathObject) b;
            }
        }
        return null;
    }

    /**
     * 取路径对象实际绘制坐标的包围盒
     *
     * @param path 路径对象
     * @return {minX, minY, maxX, maxY}，单位 mm（OFD 页面坐标）；无坐标时返回 null
     */
    private static double[] dataBox(PathObject path) {
        String data = path.getAbbreviatedData().toString();
        // AbbreviatedData 是“命令 + 坐标”的空白分隔序列，命令为单字母，其余均为数值对
        String[] tokens = data.trim().split("\\s+");
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        double px = 0d, py = 0d;
        int i = 0;
        while (i < tokens.length) {
            String cmd = tokens[i++];
            if (cmd.length() == 1 && Character.isLetter(cmd.charAt(0))) {
                if ("Z".equalsIgnoreCase(cmd) || "C".equalsIgnoreCase(cmd)) {
                    continue;
                }
                // M/L/Q 后跟 1 组坐标，M 之后的隐式 L 也按 1 组处理
                if (i + 1 < tokens.length) {
                    px = Double.parseDouble(tokens[i++]);
                    py = Double.parseDouble(tokens[i++]);
                    minX = Math.min(minX, px);
                    minY = Math.min(minY, py);
                    maxX = Math.max(maxX, px);
                    maxY = Math.max(maxY, py);
                }
            } else {
                i++;
            }
        }
        if (minX > maxX) {
            return null;
        }
        return new double[]{minX, minY, maxX, maxY};
    }

    /**
     * 读取文字对象的 TextCode 内容
     *
     * @param text 文字对象
     * @return 文本内容
     */
    private static String content(TextObject text) {
        StringBuilder sb = new StringBuilder();
        for (org.ofdrw.core.text.TextCode code : text.getTextCodes()) {
            sb.append(code.getContent());
        }
        return sb.toString();
    }
}