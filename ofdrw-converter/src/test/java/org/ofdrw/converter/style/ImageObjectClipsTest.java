package org.ofdrw.converter.style;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ofdrw.converter.canvas.BufferedImageSurface;
import org.ofdrw.converter.canvas.CanvasImageMaker;
import org.ofdrw.reader.OFDReader;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
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
 * 图片对象（ImageObject）的图元级裁剪（{@code <Clips>}）测试。
 * <p>
 * 覆盖两个历史缺陷：
 * <ol>
 *     <li>裁剪矩阵在换算成设备空间时丢失缩放比例，裁剪框被放大到整页甚至失效；</li>
 *     <li>图片绘制尺寸按“像素数 / 目标尺寸”计算，图片只剩约 1/ppm 大小。</li>
 * </ol>
 * 断言方式：给图片上下两半涂不同颜色，按页面坐标校验图片位置与尺寸；
 * 同时覆盖带 {@code Clips} 的图片，确保裁剪换算不会破坏图片本身的绘制。
 *
 * @author 权观宇
 * @since 2026-10-25
 */
class ImageObjectClipsTest {

    /**
     * 临时目录，用于生成测试用 OFD 文档
     */
    @TempDir
    Path tempDir;

    /**
     * 渲染分辨率，单位：像素/毫米
     */
    private static final double PPM = 2.0;

    @Test
    @DisplayName("图片按 CTM 换算为物理尺寸并落在页面坐标上")
    void imageClipsMatchImageGeometry() throws Exception {
        // 图片 100x100 像素，CTM 目标尺寸 80x80（对象空间），即页面 (10,10)-(90,90) 毫米
        BufferedImage image = render(imageOfd("80 0 0 80 0 0", "0 0 80 40"));

        int x = (int) Math.round(50 * PPM);
        assertEquals(Color.RED, at(image, x, (int) Math.round(20 * PPM)), "图片上半部分应显示为红色");
        // 图片缩放到包围框（Boundary）尺寸：80x80 对象单位，故 red/blue 分界在包围框中点 40mm 处
        assertEquals(Color.RED, at(image, x, (int) Math.round(45 * PPM)), "裁剪框内仍显示图片上半部分");
        assertEquals(Color.WHITE, at(image, x, (int) Math.round(70 * PPM)), "裁剪框之外（对象空间 y>40）应被裁掉");
        assertEquals(Color.WHITE, at(image, (int) Math.round(5 * PPM), (int) Math.round(20 * PPM)),
                "图片左侧之外应为空白");
    }

    @Test
    @DisplayName("图片目标尺寸取自 CTM，不再被重复除以 ppm")
    void imageUsesCtmAsPhysicalSize() throws Exception {
        // 无 Clips：整张图片都应可见，且尺寸为 CTM 指定的 80x80 毫米
        BufferedImage image = render(imageOfd("80 0 0 80 0 0", null));

        assertEquals(Color.RED, at(image, (int) Math.round(50 * PPM), (int) Math.round(20 * PPM)),
                "图片上半部分应可见");
        assertEquals(Color.BLUE, at(image, (int) Math.round(50 * PPM), (int) Math.round(70 * PPM)),
                "图片下半部分应可见（说明图片按 CTM 尺寸铺满，而不是缩成几像素）");
        assertEquals(Color.WHITE, at(image, (int) Math.round(50 * PPM), (int) Math.round(95 * PPM)),
                "图片范围之外应为空白");
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
     * @return 位图（背景白色）
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
     * 构造只含一张图片的 OFD 文档。
     * <p>
     * 图片上半部分为红色、下半部分为蓝色；Clips 覆盖区域由 {@code clipArea} 指定。
     *
     * @param ctm      图片 CTM 六元组，null 表示不设置
     * @param clipArea 裁剪区域 "x y w h"，坐标单位：对象空间；null 表示不设置 Clips
     * @return OFD 文档路径
     * @throws IOException 写入失败
     */
    private Path imageOfd(String ctm, String clipArea) throws IOException {
        StringBuilder obj = new StringBuilder("<ofd:ImageObject ID=\"3\" ResourceID=\"9\"");
        if (ctm != null) {
            obj.append(" CTM=\"").append(ctm).append('"');
        }
        obj.append(" Boundary=\"10 10 80 80\">");
        if (clipArea != null) {
            String[] p = clipArea.split(" ");
            double w = Double.parseDouble(p[2]);
            double h = Double.parseDouble(p[3]);
            obj.append("<ofd:Clips TransFlag=\"false\"><ofd:Clip><ofd:Area><ofd:Path Stroke=\"false\" Fill=\"true\">")
                    .append("<ofd:AbbreviatedData>M 0 ").append(h)
                    .append(" L ").append(w).append(' ').append(h)
                    .append(" L ").append(w).append(" 0 L 0 0 C</ofd:AbbreviatedData>")
                    .append("</ofd:Path></ofd:Area></ofd:Clip></ofd:Clips>");
        }
        obj.append("</ofd:ImageObject>");

        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<ofd:Page xmlns:ofd=\"http://www.ofdspec.org/2016\">"
                + "<ofd:Area><ofd:PhysicalBox>0 0 100 100</ofd:PhysicalBox></ofd:Area>"
                + "<ofd:Content><ofd:Layer ID=\"2\">" + obj + "</ofd:Layer></ofd:Content></ofd:Page>";

        String documentRes = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<ofd:Res xmlns:ofd=\"http://www.ofdspec.org/2016\"><ofd:MultiMedias>"
                + "<ofd:MultiMedia ID=\"9\" Type=\"Image\"><ofd:MediaFile>Res/half.png</ofd:MediaFile></ofd:MultiMedia>"
                + "</ofd:MultiMedias></ofd:Res>";

        String document = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<ofd:Document xmlns:ofd=\"http://www.ofdspec.org/2016\">"
                + "<ofd:CommonData><ofd:PageArea><ofd:PhysicalBox>0 0 100 100</ofd:PhysicalBox></ofd:PageArea>"
                + "<ofd:DocumentRes>DocumentRes.xml</ofd:DocumentRes>"
                + "<ofd:MaxUnitID>9</ofd:MaxUnitID></ofd:CommonData>"
                + "<ofd:Pages><ofd:Page ID=\"1\" BaseLoc=\"Pages/Page_0/Content.xml\"/></ofd:Pages>"
                + "</ofd:Document>";

        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("OFD.xml", text("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<ofd:OFD xmlns:ofd=\"http://www.ofdspec.org/2016\" DocType=\"OFD\" Version=\"1.0\">"
                + "<ofd:DocBody><ofd:DocRoot>Doc_0/Document.xml</ofd:DocRoot></ofd:DocBody></ofd:OFD>"));
        entries.put("Doc_0/Document.xml", text(document));
        entries.put("Doc_0/DocumentRes.xml", text(documentRes));
        entries.put("Doc_0/Pages/Page_0/Content.xml", text(content));
        entries.put("Doc_0/Res/half.png", halfImage());

        Path ofdPath = tempDir.resolve("image-clips-" + System.nanoTime() + ".ofd");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(ofdPath))) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        }
        return ofdPath;
    }

    /**
     * 把 XML 字符串转为 UTF-8 字节。
     *
     * @param xml XML 内容
     * @return UTF-8 字节
     */
    private byte[] text(String xml) {
        return xml.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 生成上红下蓝的测试图片。
     *
     * @return PNG 字节
     * @throws IOException 编码失败
     */
    private byte[] halfImage() throws IOException {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.RED);
            graphics.fillRect(0, 0, 100, 50);
            graphics.setColor(Color.BLUE);
            graphics.fillRect(0, 50, 100, 50);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
