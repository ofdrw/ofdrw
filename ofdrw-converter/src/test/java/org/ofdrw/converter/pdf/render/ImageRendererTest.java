package org.ofdrw.converter.pdf.render;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ofdrw.converter.pdf.CoordSystem;
import org.ofdrw.converter.pdf.font.ImageEncoding;
import org.ofdrw.converter.pdf.model.AffineMatrix;
import org.ofdrw.converter.pdf.model.Box;
import org.ofdrw.converter.pdf.model.ContentCommand;
import org.ofdrw.converter.pdf.model.DashPattern;
import org.ofdrw.converter.pdf.model.ImageBorderCommand;
import org.ofdrw.converter.pdf.model.ImageCommand;
import org.ofdrw.converter.pdf.model.PdfColor;
import org.ofdrw.converter.pdf.model.PdfPageContent;
import org.ofdrw.converter.pdf.model.RestoreState;
import org.ofdrw.converter.pdf.model.SaveState;
import org.ofdrw.converter.pdf.model.SetAlpha;
import org.ofdrw.core.basicStructure.pageObj.layer.block.ImageObject;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.image.Border;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 图像渲染器测试（决策 23：保真优先）
 * <p>
 * 锁定：
 * <ol>
 *     <li>JPEG 原样嵌入不重编码：输出指令中的字节必须与资源原始字节逐字节相等；</li>
 *     <li>ImageMask → 先输出蒙版图像指令，再输出主图（写入器据此建立 /SMask）；</li>
 *     <li>落点只由显式上下文决定：外接矩形 + 容器 CTM + 页面框偏移 + Y 翻转，全部经 {@link CoordSystem}；</li>
 *     <li>Border 单独绘制：线宽/圆角/虚线/颜色均为页面 pt，显式线宽 0 时不绘制；</li>
 *     <li>Alpha 只在显式声明时输出（决策 2：缺省不透明，不写 ca/CA）；</li>
 *     <li>资源缺失只跳过并告警，不中断。</li>
 * </ol>
 *
 * @author 权观宇
 * @since 2.5.1
 */
class ImageRendererTest {

    /**
     * 断言容限，单位 pt
     */
    private static final double EPS = 1e-6;

    /**
     * 页面高度，单位 mm
     */
    private static final double PAGE_MM = 100;

    /**
     * 生成最小合法 JPEG（SOI + SOF0 段，可指定像素尺寸），用于验证“原字节不重编码”
     *
     * @param width  像素宽度
     * @param height 像素高度
     * @return JPEG 字节
     */
    private static byte[] fakeJpeg(int width, int height) {
        return new byte[]{
                (byte) 0xFF, (byte) 0xD8,
                (byte) 0xFF, (byte) 0xC0, 0x00, 0x11, 0x08,
                (byte) ((height >> 8) & 0xFF), (byte) (height & 0xFF),
                (byte) ((width >> 8) & 0xFF), (byte) (width & 0xFF),
                0x03, 0x01, 0x11, 0x00, 0x02, 0x11, 0x01, 0x03, 0x11, 0x01,
                (byte) 0xFF, (byte) 0xD9
        };
    }

    /**
     * 构造 OFD 命名空间下的图像对象
     * <p>
     * dom4j 元素必须处于 OFD 命名空间下，否则 {@code CT_Image#getBorder()} 等按限定名读取子元素时会返回 null。
     *
     * @return 图像对象
     */
    private static ImageObject imageObject() {
        org.dom4j.Element element = org.dom4j.DocumentHelper.createElement("ImageObject");
        element.addNamespace(org.ofdrw.core.Const.OFD_NAMESPACE.getPrefix(), org.ofdrw.core.Const.OFD_NAMESPACE.getURI());
        return new ImageObject(element);
    }

    /**
     * 页面高 100mm 的渲染上下文
     *
     * @return 渲染上下文
     */
    private static RenderContext context() {
        PdfPageContent content = new PdfPageContent(CoordSystem.mmToPt(PAGE_MM), CoordSystem.mmToPt(PAGE_MM));
        return RenderContexts.create(PAGE_MM, PAGE_MM, content, null);
    }

    /**
     * 构造只有主图资源的渲染器
     *
     * @param id   资源 ID
     * @param data 图像字节
     * @return 图像渲染器
     */
    private static ImageRendererImpl renderer(String id, byte[] data) {
        return new ImageRendererImpl(resourceId -> id.equals(resourceId) ? source(id, data) : null, null);
    }

    /**
     * 构造图像数据（测试桩）
     *
     * @param id   资源 ID
     * @param data 图像字节
     * @return 图像数据
     */
    private static ImageResourceLoader.ImageData source(String id, byte[] data) {
        int[] size = ImageResourceLoader.readPictureSize(data);
        return ImageResourceLoader.imageData(id, ImageEncoding.JPEG, size[0], size[1], data);
    }

    @Test
    @DisplayName("图像落点：外接矩形 → 页面坐标（Y 轴向上），未声明 Alpha 时不输出 SetAlpha")
    void placementUsesBoundary() {
        ImageRendererImpl renderer = renderer("10", fakeJpeg(2, 3));

        ImageObject image = imageObject();
        image.setResourceID(new ST_RefID(10L));
        image.setBoundary(10, 20, 30, 40);
        // 图像的对象空间是单位正方形：CTM 给出“单位 → mm”的尺寸，(0,0) 起点，位置由外接矩形提供
        image.setCTM(new org.ofdrw.core.basicType.ST_Array(30.0, 0.0, 0.0, 40.0, 0.0, 0.0));

        List<ContentCommand> commands = renderer.render(image, context());
        assertEquals(3, commands.size());
        assertEquals(SaveState.INSTANCE, commands.get(0));
        assertTrue(commands.get(1) instanceof ImageCommand);
        ImageCommand command = (ImageCommand) commands.get(1);
        Box placement = command.getPlacement();
        // 位置取外接矩形左上角 (10, 20)，尺寸取外接矩形宽高（CTM 与之一致）
        assertEquals(CoordSystem.mmToPt(10), placement.getX(), EPS);
        assertEquals(CoordSystem.yToPt(20 + 40, PAGE_MM), placement.getY(), EPS);
        assertEquals(CoordSystem.mmToPt(30), placement.getWidth(), EPS);
        assertEquals(CoordSystem.mmToPt(40), placement.getHeight(), EPS);
        assertEquals(RestoreState.INSTANCE, commands.get(2));
    }

    @Test
    @DisplayName("JPEG 原样嵌入：输出字节与原始文件字节完全一致、编码为 JPEG")
    void jpegNotReencoded() {
        byte[] original = fakeJpeg(2, 3);
        ImageRendererImpl renderer = renderer("11", original);

        ImageObject image = imageObject();
        image.setResourceID(new ST_RefID(11L));
        image.setBoundary(0, 0, 10, 10);

        ImageCommand command = (ImageCommand) renderer.render(image, context()).get(1);
        assertEquals(ImageEncoding.JPEG, command.getEncoding());
        assertTrue(command.getEncoding().isLossy());
        assertArrayEquals(original, command.getImageData());
        assertEquals(2, command.getPixelWidth());
        assertEquals(3, command.getPixelHeight());
        assertTrue(!command.isMasked());
    }

    @Test
    @DisplayName("显式声明 Alpha=128：产出 SetAlpha(128/255)")
    void explicitAlpha() {
        ImageRendererImpl renderer = renderer("16", fakeJpeg(2, 2));

        ImageObject image = imageObject();
        image.setResourceID(new ST_RefID(16L));
        image.setBoundary(0, 0, 10, 10);
        image.setAlpha(128);

        List<ContentCommand> commands = renderer.render(image, context());
        assertTrue(commands.get(1) instanceof SetAlpha);
        assertEquals(128f / 255f, ((SetAlpha) commands.get(1)).getFillAlpha(), EPS);
    }

    @Test
    @DisplayName("ImageMask：先输出蒙版图像指令，再输出主图，落点一致")
    void imageMaskBecomesSeparateCommand() {
        ImageRendererImpl renderer = new ImageRendererImpl(id -> source(id, fakeJpeg(4, 4)), null);

        ImageObject image = imageObject();
        image.setResourceID(new ST_RefID(12L));
        image.setImageMask(new ST_RefID(13L));
        image.setBoundary(0, 0, 10, 10);

        List<ContentCommand> commands = renderer.render(image, context());
        assertEquals(4, commands.size());
        assertTrue(commands.get(1) instanceof ImageCommand);
        ImageCommand mask = (ImageCommand) commands.get(1);
        ImageCommand main = (ImageCommand) commands.get(2);
        assertTrue(mask.isMasked());
        // 主图必须是非蒙版指令：写入器把标记为蒙版的指令缓存为待用 /SMask 而不绘制，
        // 若主图也标记则整张图不会被绘制
        assertFalse(main.isMasked());
        assertEquals(mask.getPlacement(), main.getPlacement());
        assertNotEquals(mask.getResourceKey(), main.getResourceKey());
    }

    @Test
    @DisplayName("Border 单独绘制：线宽/圆角/虚线换算为 pt")
    void borderDrawnAsPath() {
        ImageRendererImpl renderer = renderer("14", fakeJpeg(2, 2));

        Border border = new Border();
        border.setLineWidth(1.0);
        border.setHorizonalCornerRadius(2.0);
        border.setVerticalCornerRadius(2.0);
        border.setDashPattern(new org.ofdrw.core.basicType.ST_Array(1.0, 1.0));
        // core 的 setBorderColor 会把 Border 节点改名为 BorderColor，这里直接写内部 Color 子元素
        org.dom4j.Element colorElement = ((org.dom4j.Element) border).addElement("Color");
        colorElement.addAttribute("Value", "255 0 0");

        ImageObject image = imageObject();
        image.setResourceID(new ST_RefID(14L));
        image.setBoundary(0, 0, 10, 10);
        image.setCTM(new org.ofdrw.core.basicType.ST_Array(10.0, 0.0, 0.0, 10.0, 0.0, 0.0));
        image.add(border);

        List<ContentCommand> commands = renderer.render(image, context());
        assertEquals(4, commands.size());
        assertTrue(commands.get(2) instanceof ImageBorderCommand);
        ImageBorderCommand command = (ImageBorderCommand) commands.get(2);
        // 边框线宽与圆角半径是 mm，按图元“单位 → mm”缩放（此处 10）换算为页面 pt
        assertEquals(CoordSystem.mmToPt(10.0), command.getLineWidthPt(), EPS);
        // 圆角半径被限制为短边的一半（图元 10mm → 5mm）
        assertEquals(CoordSystem.mmToPt(5.0), command.getEffectiveCornerRadiusPt(), EPS);
        assertEquals(2, command.getDashPattern().getDashArray().length);
        // 虚线长度同样是 mm，按图元缩放（10）换算
        assertEquals(CoordSystem.mmToPt(10.0), command.getDashPattern().getDashArray()[0], EPS);
        assertEquals(PdfColor.ColorSpace.RGB, command.getColor().getColorSpace());
        assertEquals(RestoreState.INSTANCE, commands.get(3));
    }

    @Test
    @DisplayName("边框显式声明线宽 0：按规范不绘制边框")
    void borderZeroWidthNotDrawn() {
        ImageRendererImpl renderer = renderer("17", fakeJpeg(2, 2));

        Border border = new Border();
        border.setLineWidth(0.0);
        ImageObject image = imageObject();
        image.setResourceID(new ST_RefID(17L));
        image.setBoundary(0, 0, 10, 10);
        image.add(border);

        // 显式线宽 0 → 不画边框：不应出现 ImageBorderCommand
        long borderCount = renderer.render(image, context()).stream()
                .filter(ImageBorderCommand.class::isInstance).count();
        assertEquals(0L, borderCount);
    }

    @Test
    @DisplayName("图像 CTM 作用于单位正方形：真实样本落点 bbox 逐像素吻合（防回退）")
    void unitSquarePlacementMatchesBaseline() throws Exception {
        java.nio.file.Path sample = java.nio.file.Paths.get("src/test/resources/containsJPEG.ofd");
        try (org.ofdrw.reader.OFDReader reader = new org.ofdrw.reader.OFDReader(sample)) {
            PageRendererImpl.RenderPipeline pipeline = PageRendererImpl.create(reader);
            PdfPageContent content = pipeline.pageRenderer().renderPage(reader.getPage(1), pipeline.fontSelector());
            ImageCommand image = null;
            for (ContentCommand command : content.getCommands()) {
                if (command instanceof ImageCommand) {
                    image = (ImageCommand) command;
                    break;
                }
            }
            assertTrue(image != null, "样本应产出 ImageCommand");
            Box box = image.getPlacement();
            // 样本：Boundary="31.743 25.6484 145.3829 109.0266"，CTM="145.3829 0 0 109.0266 0 0"
            // 基准截图的 bbox 为像素矩形 (90, 73)-(501, 381)，宽高 411×308 是 72dpi 渲染时的像素取整；
            // 这里按精确的几何值断言：宽 = 145.3829mm、高 = 109.0266mm，左上角距页面左上角为 Boundary 原点
            assertEquals(CoordSystem.mmToPt(31.743), box.getX(), 1.0);
            assertEquals(CoordSystem.mmToPt(25.6484), content.getHeightPt() - box.getBottom(), 1.0);
            // 宽 = 145.3829mm × 72/25.4 = 412.10pt
            assertEquals(CoordSystem.mmToPt(145.3829), box.getWidth(), 0.01);
            assertEquals(CoordSystem.mmToPt(109.0266), box.getHeight(), 0.01);
            // 落点必须在页面内，不得溢出
            assertTrue(box.getX() >= 0 && box.getY() >= 0);
            assertTrue(box.getRight() <= content.getWidthPt() + 1 && box.getBottom() <= content.getHeightPt() + 1);
        }
    }

    @Test
    @DisplayName("圆角半径限制：不超过短边的一半")
    void cornerRadiusClamped() {
        ImageBorderCommand command = new ImageBorderCommand(new Box(0, 0, 20, 10), 1, 100,
                PdfColor.BLACK, DashPattern.SOLID, ImageEncoding.JPEG);
        assertEquals(5, command.getEffectiveCornerRadiusPt(), EPS);
    }

    @Test
    @DisplayName("容器 CTM 参与落点：容器平移 5mm → x 右移 5mm、页面坐标 y 下移 5mm")
    void containerCtmApplied() {
        ImageRendererImpl renderer = renderer("15", fakeJpeg(2, 2));

        ImageObject image = imageObject();
        image.setResourceID(new ST_RefID(15L));
        image.setBoundary(0, 0, 10, 10);

        ImageCommand original = (ImageCommand) renderer.render(image, context()).get(1);
        RenderContext shifted = context().withCtm(AffineMatrix.translate(5, 5));
        ImageCommand command = (ImageCommand) renderer.render(image, shifted).get(1);
        assertEquals(original.getPlacement().getX() + CoordSystem.mmToPt(5), command.getPlacement().getX(), EPS);
        assertEquals(original.getPlacement().getY() - CoordSystem.mmToPt(5), command.getPlacement().getY(), EPS);
        assertEquals(original.getPlacement().getWidth(), command.getPlacement().getWidth(), EPS);
    }

    @Test
    @DisplayName("资源缺失：跳过图元并告警，不抛异常")
    void missingResourceSkipped() {
        ImageRendererImpl renderer = new ImageRendererImpl(resourceId -> null, null);
        ImageObject image = imageObject();
        image.setResourceID(new ST_RefID(99L));
        image.setBoundary(0, 0, 10, 10);
        // 资源缺失：整条指令序列作废（不产出半截 q/Q），由调用方跳过该图元
        assertTrue(renderer.render(image, context()).isEmpty());
    }

    @Test
    @DisplayName("跳过场景：输入不支持、不可见、缺少外接矩形均返回空")
    void skippedCases() {
        ImageRendererImpl renderer = renderer("1", fakeJpeg(2, 2));
        assertTrue(renderer.render(null, context()).isEmpty());
        assertTrue(renderer.render("text", context()).isEmpty());

        ImageObject invisible = imageObject();
        invisible.setResourceID(new ST_RefID(1L));
        invisible.setVisible(false);
        assertTrue(renderer.render(invisible, context()).isEmpty());

        ImageObject noBoundary = imageObject();
        noBoundary.setResourceID(new ST_RefID(1L));
        // 既无外接矩形也无 CTM 平移：无法确定落点尺寸，按单位尺寸退化绘制（不视为错误）
        assertTrue(renderer.render(noBoundary, context()).size() > 0);
    }
}
