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

/**
 * 组合对象（CompositeObject / CompositeGraphicUnit）绘制测试。
 * <p>
 * 依据 ofdrw-viewer-engine 的 drawComposite 行为：组合对象的子图元与组合对象处于同一坐标系，
 * 由组合对象自身的 CTM 与 Boundary 定位；CompositeGraphicUnit 的 Width/Height 只描述矢量图像自身
 * 的坐标系范围（超出裁剪），<b>不</b>用于把子图元归一化到 Boundary。
 * 因此本类断言：子图元按“CTM 缩放 + Boundary 平移”落位，不做 Width/Height 缩放。
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
    @DisplayName("组合对象按 CTM 缩放定位，不做 Width/Height 归一化")
    void compositeUsesCtmAndBoundary() throws Exception {
        // CTM 为 0.5 倍（mm/单位）；内部方块 (10,10)-(60,60)
        BufferedImage image = render(compositeOfd(100, 100, "20 20 20 20", "0.5 0 0 0.5 0 0"));
        // 方块映射为 (5,5)-(30,30)mm，即 (5,5)-(30,30) 像素
        assertEquals(Color.BLACK, at(image, 15, 15), "方块应按 CTM 缩放后落在页面坐标系");
        assertEquals(Color.WHITE, at(image, 2, 2), "方块左上角之外应为空白");
        assertEquals(Color.WHITE, at(image, 40, 40), "超出方块范围应为空白");
    }

    @Test
    @DisplayName("单位空间尺寸不影响绘制结果（Width/Height 只描述矢量图像自身范围）")
    void compositeUnitSizeDoesNotScale() throws Exception {
        BufferedImage small = render(compositeOfd(100, 100, "20 20 20 20", "0.5 0 0 0.5 0 0"));
        BufferedImage large = render(compositeOfd(1000, 1000, "20 20 20 20", "0.5 0 0 0.5 0 0"));
        for (int y : new int[]{6, 15, 29}) {
            for (int x : new int[]{6, 15, 29}) {
                assertEquals(small.getRGB(x, y), large.getRGB(x, y),
                        "不同的 Width/Height 不应改变绘制结果：(" + x + "," + y + ")");
            }
        }
    }

    @Test
    @DisplayName("组合对象没有 CTM 时按单位坐标直接进入页面坐标系")
    void compositeWithoutCtmUsesBoundaryOnly() throws Exception {
        BufferedImage image = render(compositeOfd(100, 100, "20 20 20 20", null));
        // 无 CTM：组合内容与页面共用坐标系，内部方块 (10,10)-(60,60) 以毫米直接落位；
        // 组合对象自身的包围框 (20,20)-(40,40) 仅用于包围框裁剪，不影响子图元坐标
        assertEquals(Color.BLACK, at(image, 40, 40), "无 CTM 时子图元按原坐标绘制");
        assertEquals(Color.BLACK, at(image, 30, 30), "方块应连续覆盖到包围框范围之外");
        assertEquals(Color.WHITE, at(image, 90, 90), "超出方块范围应为空白");
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
     *
     * @param unitWidth  单位空间宽度，无量纲
     * @param unitHeight 单位空间高度，无量纲
     * @param boundary   边界框 "x y w h"，单位：毫米；null 表示不设置
     * @param ctm        CTM 六元组，null 表示不设置
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
                + "<ofd:Area><ofd:PhysicalBox>0 0 100 100</ofd:PhysicalBox></ofd:Area>"
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
                + "<ofd:CommonData><ofd:PageArea><ofd:PhysicalBox>0 0 100 100</ofd:PhysicalBox></ofd:PageArea>"
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
        return ofdPath;
    }
}
