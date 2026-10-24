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
 * 组合对象与印章绘制的隔离性测试。
 * <p>
 * 覆盖两个历史缺陷：
 * <ol>
 *     <li>印章裁剪与后续图元的裁剪没有隔离（印章外层未使用 save/restore，导致后续图元被裁掉）；</li>
 *     <li>矢量图像（CompositeGraphicUnit）单位空间换算使用了毫米比例，导致内容被缩小到 1/25.4。
 *     本类用真实文档中“Content 外面再包一层 Layer”的结构验证缩放只应用一次、内容按物理尺寸铺满边界框。</li>
 * </ol>
 *
 * @author 权观宇
 * @since 2026-10-25
 */
class CompositeStackingTest {

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
    @DisplayName("矢量图像外层再包 Layer：内容按组合对象 CTM 缩放一次")
    void compositeContentInsideLayerFillsBoundary() throws Exception {
        // 单元 720x720（=254mm），内部路径 (0,0)-(720,720) 填满整个单元，边界框 100x100mm
        Path ofd = writeOfd("composite-layer.ofd", pageWithComposite(true));
        BufferedImage image = render(ofd);

        assertEquals(Color.BLACK, at(image, 50, 50), "组合对象内容应铺满边界框中部");
        assertEquals(Color.BLACK, at(image, 5, 5), "组合对象内容应覆盖边界框左上角");
        // 组合内容与页面共用坐标系，不受组合对象自身包围框限制
        assertEquals(Color.BLACK, at(image, 150, 50), "组合内容按自身坐标铺满其覆盖范围");
    }

    @Test
    @DisplayName("矢量图像内层 Layer 的包围框不限制内容（内容不被裁成一条）")
    void compositeContentNotClippedByInnerLayer() throws Exception {
        Path ofd = writeOfd("composite-inner-layer.ofd", pageWithComposite(true));
        BufferedImage image = render(ofd);
        // 内层 Layer 的 Boundary 只有 1x1，若被当作裁剪框，内容只剩 1mm 宽，这里检查中部与右下角都有内容
        assertEquals(Color.BLACK, at(image, 50, 50));
        assertEquals(Color.BLACK, at(image, 95, 95));
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
     * 构造含一个组合对象的页面内容。
     *
     * @param wrapInLayer 矢量图像内容外面是否再包一层 Layer
     * @return 页面内容 XML
     */
    private String pageWithComposite(boolean wrapInLayer) {
        String innerContent = "<ofd:PathObject ID=\"21\" Boundary=\"0 0 720 720\" Stroke=\"false\" Fill=\"true\">"
                + "<ofd:FillColor Value=\"0 0 0\"/>"
                + "<ofd:AbbreviatedData>M 0 0 L 720 0 L 720 720 L 0 720 C</ofd:AbbreviatedData>"
                + "</ofd:PathObject>";
        String unitContent = wrapInLayer
                ? "<ofd:Content ID=\"20\"><ofd:Layer ID=\"19\" Boundary=\"0 0 1 1\">" + innerContent
                + "</ofd:Layer></ofd:Content>"
                : "<ofd:Content ID=\"20\">" + innerContent + "</ofd:Content>";

        String documentRes = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<ofd:Res xmlns:ofd=\"http://www.ofdspec.org/2016\"><ofd:CompositeGraphicUnits>"
                + "<ofd:CompositeGraphicUnit ID=\"10\" Width=\"720\" Height=\"720\">" + unitContent
                + "</ofd:CompositeGraphicUnit></ofd:CompositeGraphicUnits></ofd:Res>";

        String page = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<ofd:Page xmlns:ofd=\"http://www.ofdspec.org/2016\">"
                + "<ofd:Area><ofd:PhysicalBox>0 0 200 150</ofd:PhysicalBox></ofd:Area>"
                + "<ofd:Content><ofd:Layer ID=\"2\">"
                + "<ofd:CompositeObject ID=\"3\" CTM=\"0.3528 0 0 0.3528 0 0\" Boundary=\"0 0 100 100\" ResourceID=\"10\"/>"
                + "</ofd:Layer></ofd:Content></ofd:Page>";
        return page + "|||" + documentRes;
    }

    /**
     * 写出包含页面与文档资源的 OFD 包。
     *
     * @param name    文件名
     * @param payload 页面 XML 与资源 XML，以 {@code |||} 分隔
     * @return 生成的 OFD 文件
     * @throws IOException 写入失败
     */
    private Path writeOfd(String name, String payload) throws IOException {
        String[] parts = payload.split("\\|\\|\\|");
        String page = parts[0];
        String documentRes = parts[1];
        String document = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<ofd:Document xmlns:ofd=\"http://www.ofdspec.org/2016\">"
                + "<ofd:CommonData><ofd:PageArea><ofd:PhysicalBox>0 0 200 150</ofd:PhysicalBox></ofd:PageArea>"
                + "<ofd:DocumentRes>DocumentRes.xml</ofd:DocumentRes>"
                + "<ofd:MaxUnitID>21</ofd:MaxUnitID></ofd:CommonData>"
                + "<ofd:Pages><ofd:Page ID=\"1\" BaseLoc=\"Pages/Page_0/Content.xml\"/></ofd:Pages>"
                + "</ofd:Document>";
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("OFD.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<ofd:OFD xmlns:ofd=\"http://www.ofdspec.org/2016\" DocType=\"OFD\" Version=\"1.0\">"
                + "<ofd:DocBody><ofd:DocRoot>Doc_0/Document.xml</ofd:DocRoot></ofd:DocBody></ofd:OFD>");
        entries.put("Doc_0/Document.xml", document);
        entries.put("Doc_0/DocumentRes.xml", documentRes);
        entries.put("Doc_0/Pages/Page_0/Content.xml", page);
        Path path = tempDir.resolve(name);
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(path))) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return path;
    }

}
