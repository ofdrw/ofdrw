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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 图元级裁剪（{@code <Clips>}）作用域测试。
 * <p>
 * 用例在同页放置两张位置错开、各自带 {@code Clips} 的图片，
 * 断言两张图片都能完整绘制，即第一个图元的裁剪不会波及后续图元。
 * <p>
 * 页面块循环中裁剪的应用顺序为「先 save()，再 applyUnitClips()」：
 * 若颠倒成「先 applyUnitClips()，再 save()」，压栈保存的就是图元自身的裁剪，
 * {@code restore()} 撤销不掉，这份裁剪会泄漏给后续图元。
 *
 * @author 权观宇
 * @since 2026-10-25
 */
class ClipsScopeLeakTest {

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
    @DisplayName("同一页内每个图元的裁剪只作用于自身，不泄漏到后续图元")
    void clipsDoNotLeakToFollowingUnits() throws Exception {
        BufferedImage page = render(twoImageOfd());

        // 第一张图片：页面 (10,10)-(90,90) 毫米，应被正常绘制
        assertNotWhite(at(page, 50, 30), "第一张图片应可见");
        // 第二张图片：页面 (110,10)-(190,90) 毫米，必须不被第一张图片的裁剪框影响
        assertNotWhite(at(page, 150, 100), "第二张图片应可见，不能被上一图元的裁剪裁掉");
    }

    /**
     * 断言像素不是页面背景白色。
     *
     * @param color   实际颜色
     * @param message 断言失败时的说明
     */
    private void assertNotWhite(Color color, String message) {
        int rgb = color.getRGB() & 0xFFFFFF;
        assertTrue(rgb != 0xFFFFFF, message + "，实际颜色：" + color);
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
     * 构造含两张并排图片的 OFD 文档。
     * <p>
     * 两张图片都声明覆盖自身范围的 {@code Clips}，图片内容为上红下蓝。
     *
     * @return OFD 文档路径
     * @throws IOException 写入失败
     */
    private Path twoImageOfd() throws IOException {
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<ofd:Page xmlns:ofd=\"http://www.ofdspec.org/2016\">"
                + "<ofd:Area><ofd:PhysicalBox>0 0 200 120</ofd:PhysicalBox></ofd:Area>"
                + "<ofd:Content><ofd:Layer ID=\"2\">"
                + imageObject("3", 10, 10, 80, 80)
                + imageObject("5", 110, 60, 80, 80)
                + "</ofd:Layer></ofd:Content></ofd:Page>";

        String documentRes = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<ofd:Res xmlns:ofd=\"http://www.ofdspec.org/2016\"><ofd:MultiMedias>"
                + "<ofd:MultiMedia ID=\"9\" Type=\"Image\"><ofd:MediaFile>Res/half.png</ofd:MediaFile></ofd:MultiMedia>"
                + "</ofd:MultiMedias></ofd:Res>";

        String document = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<ofd:Document xmlns:ofd=\"http://www.ofdspec.org/2016\">"
                + "<ofd:CommonData><ofd:PageArea><ofd:PhysicalBox>0 0 200 120</ofd:PhysicalBox></ofd:PageArea>"
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

        Path ofdPath = tempDir.resolve("clips-scope-" + System.nanoTime() + ".ofd");
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
     * 构造一个带覆盖自身范围裁剪的图片对象 XML 片段。
     *
     * @param id 图元 ID
     * @param x  包围框左上角 X 坐标，单位：毫米
     * @param y  包围框左上角 Y 坐标，单位：毫米
     * @param w  裁剪区宽度，单位：毫米
     * @param h  裁剪区高度，单位：毫米
     * @return 图片对象 XML 片段
     */
    private String imageObject(String id, double x, double y, double w, double h) {
        return "<ofd:ImageObject ID=\"" + id + "\" ResourceID=\"9\" CTM=\"80 0 0 80 0 0\" Boundary=\""
                + x + " " + y + " 80 80\">"
                + "<ofd:Clips TransFlag=\"false\"><ofd:Clip><ofd:Area><ofd:Path Boundary=\"0 0 " + w + " " + h
                + "\" Stroke=\"false\" Fill=\"true\">"
                + "<ofd:AbbreviatedData>M 0 " + h + " L " + w + " " + h + " L " + w + " 0 L 0 0 C</ofd:AbbreviatedData>"
                + "</ofd:Path></ofd:Area></ofd:Clip></ofd:Clips></ofd:ImageObject>";
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
