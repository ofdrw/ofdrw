package org.ofdrw.converter.export;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextStrokeExportTest {

    @TempDir
    Path tempDir;

    @Test
    void svgDoesNotStrokeTextWhenStrokeIsFalseOrOmitted() throws Exception {
        List<String> pages = exportTextStrokePages();

        assertFalse(hasRedStroke(pages.get(0)),
                "Stroke=false must override an explicit StrokeColor");
        assertFalse(hasRedStroke(pages.get(1)),
                "The default Stroke=false must override an inherited StrokeColor");
        assertTrue(pages.get(0).contains("<path"), "Filled text must still be rendered");
        assertTrue(pages.get(1).contains("<path"), "Filled text must still be rendered");
    }

    @Test
    void svgStrokesTextWhenStrokeIsTrue() throws Exception {
        List<String> pages = exportTextStrokePages();

        assertTrue(hasRedStroke(pages.get(2)),
                "Stroke=true must use an explicit StrokeColor");
        assertTrue(hasRedStroke(pages.get(3)),
                "Stroke=true must use an inherited StrokeColor");
    }

    @Test
    void imageExportUsesTheSameTextStrokeSwitch() throws Exception {
        List<BufferedImage> pages = exportTextStrokeImages();

        assertFalse(hasRedPixels(pages.get(0)),
                "Stroke=false must not paint the explicit stroke in an image");
        assertFalse(hasRedPixels(pages.get(1)),
                "The default Stroke=false must not paint the inherited stroke in an image");
        assertTrue(hasRedPixels(pages.get(2)),
                "Stroke=true must paint the explicit stroke in an image");
        assertTrue(hasRedPixels(pages.get(3)),
                "Stroke=true must paint the inherited stroke in an image");
    }

    private List<String> exportTextStrokePages() throws Exception {
        Path ofd = tempDir.resolve("text-stroke.ofd");
        Path svgDir = tempDir.resolve("svg");
        createTextStrokeOFD(ofd);

        List<Path> paths;
        try (SVGExporter exporter = new SVGExporter(ofd, svgDir)) {
            exporter.export();
            paths = exporter.getSvgFilePaths();
        }

        return java.util.Arrays.asList(
                read(paths.get(0)),
                read(paths.get(1)),
                read(paths.get(2)),
                read(paths.get(3)));
    }

    private List<BufferedImage> exportTextStrokeImages() throws Exception {
        Path ofd = tempDir.resolve("text-stroke-image.ofd");
        Path imageDir = tempDir.resolve("images");
        createTextStrokeOFD(ofd);

        List<Path> paths;
        try (ImageExporter exporter = new ImageExporter(ofd, imageDir)) {
            exporter.export();
            paths = exporter.getImgFilePaths();
        }

        return java.util.Arrays.asList(
                ImageIO.read(paths.get(0).toFile()),
                ImageIO.read(paths.get(1).toFile()),
                ImageIO.read(paths.get(2).toFile()),
                ImageIO.read(paths.get(3).toFile()));
    }

    private static boolean hasRedStroke(String svg) {
        return svg.contains("stroke=\"red\"") || svg.contains("stroke=\"rgb(255,0,0)\"");
    }

    private static boolean hasRedPixels(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color color = new Color(image.getRGB(x, y));
                if (color.getRed() > color.getGreen() + 50
                        && color.getRed() > color.getBlue() + 50) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static void createTextStrokeOFD(Path output) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(output))) {
            put(zip, "OFD.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<ofd:OFD xmlns:ofd=\"http://www.ofdspec.org/2016\" Version=\"1.0\" DocType=\"OFD\">"
                    + "<ofd:DocBody><ofd:DocInfo><ofd:DocID>text-stroke-test</ofd:DocID></ofd:DocInfo>"
                    + "<ofd:DocRoot>Doc_0/Document.xml</ofd:DocRoot></ofd:DocBody></ofd:OFD>");
            put(zip, "Doc_0/Document.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<ofd:Document xmlns:ofd=\"http://www.ofdspec.org/2016\"><ofd:CommonData>"
                    + "<ofd:MaxUnitID>20</ofd:MaxUnitID>"
                    + "<ofd:PageArea><ofd:PhysicalBox>0 0 60 60</ofd:PhysicalBox></ofd:PageArea>"
                    + "<ofd:PublicRes>PublicRes.xml</ofd:PublicRes></ofd:CommonData><ofd:Pages>"
                    + pageRef(0, 1) + pageRef(1, 2) + pageRef(2, 3) + pageRef(3, 4)
                    + "</ofd:Pages></ofd:Document>");
            put(zip, "Doc_0/PublicRes.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<ofd:Res xmlns:ofd=\"http://www.ofdspec.org/2016\" BaseLoc=\"Res\">"
                    + "<ofd:Fonts><ofd:Font ID=\"10\" FontName=\"sysfST\">"
                    + "<ofd:FontFile>font.ttf</ofd:FontFile></ofd:Font></ofd:Fonts>"
                    + "<ofd:DrawParams><ofd:DrawParam ID=\"11\">"
                    + "<ofd:StrokeColor Value=\"255 0 0\"/></ofd:DrawParam></ofd:DrawParams></ofd:Res>");
            put(zip, "Doc_0/Pages/Page_0/Content.xml", textPage(5, "Stroke=\"false\"", true));
            put(zip, "Doc_0/Pages/Page_1/Content.xml", textPage(6, "DrawParam=\"11\"", false));
            put(zip, "Doc_0/Pages/Page_2/Content.xml", textPage(7, "Stroke=\"true\"", true));
            put(zip, "Doc_0/Pages/Page_3/Content.xml", textPage(8, "Stroke=\"true\" DrawParam=\"11\"", false));
            put(zip, "Doc_0/Res/font.ttf", resource("/font_13132_0_edit.ttf"));
        }
    }

    private static String pageRef(int index, int id) {
        return "<ofd:Page ID=\"" + id + "\" BaseLoc=\"Pages/Page_" + index + "/Content.xml\"/>";
    }

    private static String textPage(int id, String attributes, boolean explicitStrokeColor) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<ofd:Page xmlns:ofd=\"http://www.ofdspec.org/2016\"><ofd:Content><ofd:Layer ID=\"" + (id + 10) + "\">"
                + "<ofd:TextObject ID=\"" + id + "\" Boundary=\"10 10 30 30\" Font=\"10\" Size=\"20\" "
                + "LineWidth=\"1\" " + attributes + ">"
                + "<ofd:FillColor Value=\"0 0 0\"/>"
                + (explicitStrokeColor ? "<ofd:StrokeColor Value=\"255 0 0\"/>" : "")
                + "<ofd:TextCode X=\"0\" Y=\"20\">/</ofd:TextCode>"
                + "</ofd:TextObject></ofd:Layer></ofd:Content></ofd:Page>";
    }

    private static byte[] resource(String name) throws IOException {
        try (InputStream input = TextStrokeExportTest.class.getResourceAsStream(name)) {
            if (input == null) {
                throw new IOException("Missing test resource: " + name);
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int length;
            while ((length = input.read(buffer)) != -1) {
                output.write(buffer, 0, length);
            }
            return output.toByteArray();
        }
    }

    private static void put(ZipOutputStream zip, String name, String content) throws IOException {
        put(zip, name, content.getBytes(StandardCharsets.UTF_8));
    }

    private static void put(ZipOutputStream zip, String name, byte[] content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content);
        zip.closeEntry();
    }
}
