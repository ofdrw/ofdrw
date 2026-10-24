package org.ofdrw.converter.style;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ofdrw.converter.canvas.BufferedImageSurface;
import org.ofdrw.converter.canvas.CanvasImageMaker;
import org.ofdrw.reader.OFDReader;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 组合对象（CompositeObject / CompositeGraphicUnit）坐标系换算测试。
 * <p>
 * 依据 GB/T 33190-2016 对矢量图像（CompositeGraphicUnit）的定义：其 Width/Height 界定内部图元的单位空间，
 * 内部坐标需按 Boundary 宽高比例映射后落入页面。本类构造单位空间（100x100）与目标边界框（20x20mm）不一致的文档，
 * 断言渲染出的图形尺寸与位置符合预期。
 *
 * @author 权观宇
 * @since 2026-10-25
 */
class CompositeObjectScaleTest {

    /**
     * 临时目录，用于生成测试用 OFD 文档
     */
    @TempDir
    Path tempDir;

    /**
     * 渲染分辨率，单位：像素/毫米
     */
    private static final double PPM = 1.0;

    @Test
    @DisplayName("组合对象按 Width/Height 换算：100x100 单位空间映射到 20x20mm 边界框")
    void compositeUnitMapsToBoundarySize() throws Exception {
        // 单位空间内绘制 (10,10)-(60,60) 的黑色方块，边界框位于 (20,20) 尺寸 20x20mm
        BufferedImage image = render(compositeOfd(100, 100, "20 20 20 20", null));

        // 预期：方块映射为 (22,22)-(32,32)mm，即 1 像素/毫米下 (22,22)-(32,32) 像素
        assertEquals(Color.BLACK, at(image, 27, 27), "单位空间方块应缩放后落在边界框中部");
        assertEquals(Color.WHITE, at(image, 17, 17), "边界框左上外侧应为空白");
        assertEquals(Color.WHITE, at(image, 37, 37), "边界框右下外侧应为空白（未按 1:1 铺开）");
        assertEquals(Color.WHITE, at(image, 21, 21), "单位空间原点 (0,0) 对应边界框左上角，应仍为空白");
    }

    @Test
    @DisplayName("单位空间换算与 CTM 叠加：CTM 只做整体平移时图形整体右移")
    void compositeUnitMapsWithCtm() throws Exception {
        // 无 CTM：方块落在 (22,22)-(32,32)mm
        BufferedImage plain = render(compositeOfd(100, 100, "20 20 20 20", null));
        // CTM 平移 40mm：方块整体右移到 (62,22)-(72,32)mm
        BufferedImage shifted = render(compositeOfd(100, 100, "20 20 20 20", "1 0 0 1 40 0"));
        assertEquals(Color.BLACK, at(plain, 27, 27), "无 CTM 时方块位于边界框中部");
        assertEquals(Color.WHITE, at(shifted, 27, 27), "带 CTM 平移后原位置应变为空白");
        assertEquals(Color.BLACK, at(shifted, 67, 27), "CTM 平移后方块应整体右移");
    }

    @Test
    @DisplayName("边界框缺失时以 Width/Height 作为目标尺寸（不缩放）")
    void compositeWithoutBoundaryFallsBackToUnitSize() throws Exception {
        BufferedImage image = render(compositeOfd(60, 60, null, null));
        // 无边界框：单位空间按 1:1 作为毫米使用，方块 (10,10)-(60,60) 直接落在页面
        assertEquals(Color.BLACK, at(image, 30, 30), "无边界框时应按单位尺寸直接绘制");
    }

    /**
     * 读取像素颜色。
     *
     * @param image 位图
     * @param x     X 坐标，单位：像素
     * @param y     Y 坐标，单位：像素
     * @return 颜色
     */
    private Color at(BufferedImage image, int x, int y) {
        return new Color(image.getRGB(x, y));
    }

    /**
     * 渲染文档首页为位图。
     *
     * @param ofd OFD 文档
     * @return 位图（背景白色，1 像素代表 1 毫米）
     * @throws IOException 读取失败
     */
    private BufferedImage render(Path ofd) throws IOException {
        try (OFDReader reader = new OFDReader(ofd)) {
            int w = (int) Math.ceil(PPM * reader.getPageList().get(0).getSize().getWidth());
            int h = (int) Math.ceil(PPM * reader.getPageList().get(0).getSize().getHeight());
            BufferedImageSurface surface = new BufferedImageSurface(w, h, Color.WHITE);
            new CanvasImageMaker(reader, PPM).drawPage(surface, 0);
            return surface.getImage();
        }
    }

    /**
     * 构造只含一个组合对象的 OFD 文档。
     * <p>
     * 组合图形单元的单位空间为 {@code unitWidth x unitHeight}，内部黑色方块占 (10,10)-(60,60)；
     * 组合对象可选设置边界框与 CTM。
     *
     * @param unitWidth   单位空间宽度，无量纲
     * @param unitHeight  单位空间高度，无量纲
     * @param boundary    边界框 "x y w h"，单位：毫米；null 表示不设置
     * @param ctm         CTM 六元组，null 表示不设置
     * @return OFD 文档路径
     * @throws IOException 写入失败
     */
    private Path compositeOfd(double unitWidth, double unitHeight, String boundary, String ctm) throws IOException {
        StringBuilder composite = new StringBuilder("<ofd:CompositeObject ID=\"3\" ResourceID=\"10\"");
        if (boundary != null) {
            composite.append(" Boundary=\"").append(boundary).append('"');
        }
        if (ctm != null) {
            composite.append(" CTM=\"").append(ctm).append('"');
        }
        composite.append("/>");

        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<ofd:Page xmlns:ofd=\"http://www.ofdspec.org/2016\">"
                + "<ofd:Area><ofd:PhysicalBox>0 0 100 80</ofd:PhysicalBox></ofd:Area>"
                + "<ofd:Content><ofd:Layer ID=\"2\">" + composite + "</ofd:Layer></ofd:Content></ofd:Page>";

        String documentRes = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<ofd:Res xmlns:ofd=\"http://www.ofdspec.org/2016\"><ofd:CompositeGraphicUnits>"
                + "<ofd:CompositeGraphicUnit ID=\"10\" Width=\"" + unitWidth + "\" Height=\"" + unitHeight + "\">"
                + "<ofd:Content ID=\"11\">"
                + "<ofd:PathObject ID=\"12\" Boundary=\"10 10 50 50\" Stroke=\"false\" Fill=\"true\">"
                + "<ofd:FillColor Value=\"0 0 0\"/>"
                + "<ofd:AbbreviatedData>M 10 10 L 60 10 L 60 60 L 10 60 C</ofd:AbbreviatedData>"
                + "</ofd:PathObject>"
                + "</ofd:Content></ofd:CompositeGraphicUnit>"
                + "</ofd:CompositeGraphicUnits></ofd:Res>";

        String document = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<ofd:Document xmlns:ofd=\"http://www.ofdspec.org/2016\">"
                + "<ofd:CommonData><ofd:PageArea><ofd:PhysicalBox>0 0 100 80</ofd:PhysicalBox></ofd:PageArea>"
                + "<ofd:DocumentRes>DocumentRes.xml</ofd:DocumentRes>"
                + "<ofd:MaxUnitID>12</ofd:MaxUnitID></ofd:CommonData>"
                + "<ofd:Pages><ofd:Page ID=\"1\" BaseLoc=\"Pages/Page_0/Content.xml\"/></ofd:Pages>"
                + "</ofd:Document>";

        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("OFD.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<ofd:OFD xmlns:ofd=\"http://www.ofdspec.org/2016\" DocType=\"OFD\" Version=\"1.0\">"
                + "<ofd:DocBody><ofd:DocRoot>Doc_0/Document.xml</ofd:DocRoot></ofd:DocBody></ofd:OFD>");
        entries.put("Doc_0/Document.xml", document);
        entries.put("Doc_0/DocumentRes.xml", documentRes);
        entries.put("Doc_0/Pages/Page_0/Content.xml", content);

        Path ofdPath = tempDir.resolve("composite-scale-" + System.nanoTime() + ".ofd");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(ofdPath))) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        assertTrue(Files.size(ofdPath) > 0);
        return ofdPath;
    }
}
