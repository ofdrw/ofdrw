package org.ofdrw.converter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ofdrw.converter.canvas.CanvasImageMaker;
import org.ofdrw.reader.OFDReader;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PathObject 的 CTM 与路径坐标兼容性测试。
 * <p>
 * 福昕等工具生成的 OFD 中，PathObject 的 CTM 平移分量等于 -Boundary 原点，同时 AbbreviatedData
 * 已使用页面绝对坐标。若把 CTM 平移再叠加一次，填充块会被平移到页面外而整块丢失（例如深层文档的
 * 深蓝色表头底色）。本测试构造一个 60x60mm 的文档：
 * <ul>
 *     <li>Path A：常规写法（CTM 为单位矩阵，路径坐标相对 Boundary 原点）；</li>
 *     <li>Path B：绝对坐标写法（CTM 平移 = -Boundary 原点，路径坐标 = 页面绝对坐标）。</li>
 * </ul>
 * 两者都应落在页面内并可见。
 *
 * @author ofdrw
 */
class PathCtmAbsoluteCoordinateTest {

    /**
     * 临时目录
     */
    @TempDir
    Path tempDir;

    /**
     * 常规写法与绝对坐标写法都应被正确绘制。
     *
     * @throws Exception 文档生成或渲染失败
     */
    @Test
    void absoluteCoordinateCtmIsNotAppliedTwice() throws Exception {
        Path ofd = tempDir.resolve("path-ctm.ofd");
        createDocument(ofd);

        BufferedImage image;
        try (OFDReader reader = new OFDReader(ofd)) {
            CanvasImageMaker maker = new CanvasImageMaker(reader, 4);
            image = maker.makePage(0);
        }

        // Path A：Boundary(5,5,20,20)，CTM 缺省，路径坐标相对 Boundary 原点
        assertEquals(Color.RED.getRGB(), image.getRGB((int) ((5 + 10) * 4), (int) ((5 + 10) * 4)) | 0xFF000000,
                "常规写法（相对坐标）应被绘制在 Boundary 区域");
        // Path B：Boundary(30,5,20,20)，CTM 平移 = -Boundary 原点，路径坐标为页面绝对坐标
        int bx = (int) ((30 + 10) * 4);
        int by = (int) ((5 + 10) * 4);
        assertEquals(Color.BLUE.getRGB(), image.getRGB(bx, by) | 0xFF000000,
                "绝对坐标写法不应被 CTM 平移二次偏移（否则会落到页面外而丢失）");
    }

    /**
     * 生成测试文档。
     *
     * @param output 输出路径
     * @throws IOException 写文件失败
     */
    private void createDocument(Path output) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(output))) {
            put(zip, "OFD.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<ofd:OFD xmlns:ofd=\"http://www.ofdspec.org/2016\" Version=\"1.0\" DocType=\"OFD\">"
                    + "<ofd:DocBody><ofd:DocInfo><ofd:DocID>path-ctm</ofd:DocID></ofd:DocInfo>"
                    + "<ofd:DocRoot>Doc_0/Document.xml</ofd:DocRoot></ofd:DocBody></ofd:OFD>");
            put(zip, "Doc_0/Document.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<ofd:Document xmlns:ofd=\"http://www.ofdspec.org/2016\"><ofd:CommonData>"
                    + "<ofd:MaxUnitID>20</ofd:MaxUnitID>"
                    + "<ofd:PageArea><ofd:PhysicalBox>0 0 60 60</ofd:PhysicalBox></ofd:PageArea>"
                    + "</ofd:CommonData><ofd:Pages>"
                    + "<ofd:Page ID=\"1\" BaseLoc=\"Pages/Page_0/Content.xml\"/>"
                    + "</ofd:Pages></ofd:Document>");
            put(zip, "Doc_0/Pages/Page_0/Content.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<ofd:Page xmlns:ofd=\"http://www.ofdspec.org/2016\"><ofd:Content><ofd:Layer ID=\"1\">"
                    // Path A：相对坐标，无 CTM
                    + "<ofd:PathObject ID=\"10\" Boundary=\"5 5 20 20\" Fill=\"true\" Stroke=\"false\">"
                    + "<ofd:FillColor Value=\"255 0 0\"/>"
                    + "<ofd:AbbreviatedData>M 0 0 L 20 0 L 20 20 L 0 20 C</ofd:AbbreviatedData></ofd:PathObject>"
                    // Path B：绝对坐标，CTM 平移 = -Boundary 原点
                    + "<ofd:PathObject ID=\"11\" CTM=\"1 0 0 1 -30 -5\" Boundary=\"30 5 20 20\" Fill=\"true\" Stroke=\"false\">"
                    + "<ofd:FillColor Value=\"0 0 255\"/>"
                    + "<ofd:AbbreviatedData>M 30 5 L 50 5 L 50 25 L 30 25 C</ofd:AbbreviatedData></ofd:PathObject>"
                    + "</ofd:Layer></ofd:Content></ofd:Page>");
            zip.closeEntry();
        }
    }

    /**
     * 写入一个压缩包条目。
     *
     * @param zip     压缩包
     * @param name    条目名称
     * @param content 条目内容
     * @throws IOException 写入失败
     */
    private static void put(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes("UTF-8"));
        zip.closeEntry();
    }

    /**
     * 防止误用：确保测试资源目录存在（失败时给出明确提示）。
     */
    @Test
    void testResourcesAreAvailable() {
        assertTrue(Files.isDirectory(Paths.get("src/test/resources")), "测试资源目录应存在");
    }
}
