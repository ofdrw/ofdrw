package org.ofdrw.converter.pdf.render;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ofdrw.converter.pdf.CoordSystem;
import org.ofdrw.converter.pdf.font.ImageEncoding;
import org.ofdrw.converter.pdf.model.Box;
import org.ofdrw.converter.pdf.model.PdfPageContent;
import org.ofdrw.converter.pdf.model.RectCommand;
import org.ofdrw.converter.pdf.model.PdfColor;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicStructure.pageObj.layer.CT_Layer;
import org.ofdrw.core.basicStructure.pageObj.layer.Type;
import org.ofdrw.core.basicStructure.pageObj.layer.block.PathObject;
import org.ofdrw.core.graph.pathObj.AbbreviatedData;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 页面渲染器测试：页框（决策 26）与图层 ZOrder
 * <p>
 * 图层内容使用路径图元承载，通过“页面内容指令顺序”验证 Background → Body → Foreground 的绘制顺序。
 *
 * @author 权观宇
 * @since 2.5.1
 */
class PageRendererImplTest {

    /**
     * 断言容限
     */
    private static final double EPS = 1e-6;

    /**
     * 构造带 OFD 命名空间的图层
     *
     * @param type   图层类型
     * @param x      路径外接矩形左上角横坐标，单位 mm
     * @return 图层
     */
    private static CT_Layer layer(Type type, double x) {
        org.dom4j.Element element = org.dom4j.DocumentHelper.createElement("Layer");
        element.addNamespace(org.ofdrw.core.Const.OFD_NAMESPACE.getPrefix(), org.ofdrw.core.Const.OFD_NAMESPACE.getURI());
        CT_Layer layer = new CT_Layer(element);
        layer.setType(type);
        layer.setObjID(new org.ofdrw.core.basicType.ST_ID((long) x));
        PathObject path = new PathObject(org.dom4j.DocumentHelper.createElement("PathObject"));
        path.setBoundary(x, 0, 10, 10);
        path.setAbbreviatedData(new AbbreviatedData().S(0, 0).L(10, 0).L(10, 10).C());
        path.setFill(true);
        path.setStroke(false);
        path.setFillColor(org.ofdrw.core.pageDescription.color.color.CT_Color.rgb(0, 0, 0));
        layer.addPageBlock(path);
        return layer;
    }

    /**
     * 构造页面
     *
     * @param layers 图层
     * @return 页面
     */
    private static Page page(CT_Layer... layers) {
        Page page = new Page();
        page.setArea(new org.ofdrw.core.basicStructure.doc.CT_PageArea(0, 0, 100, 100));
        org.ofdrw.core.basicStructure.pageObj.Content content = new org.ofdrw.core.basicStructure.pageObj.Content();
        for (CT_Layer layer : layers) {
            content.addLayer(layer);
        }
        page.setContent(content);
        return page;
    }

    /**
     * 渲染页面
     *
     * @param page 页面
     * @return 页面内容
     */
    private static PdfPageContent render(Page page) {
        PathRenderer pathRenderer = new PathRenderer(new GraphicsStateResolver(id -> null), null);
        return new PageRendererImpl(null, pathRenderer, null, new ImageRendererImpl()).renderPage(page, null);
    }

    @Test
    @DisplayName("装配工厂：create(reader) 返回完整管线，字体选择器与登记表共享同一实例")
    void createAssemblesPipeline() throws Exception {
        java.nio.file.Path sample = java.nio.file.Paths.get("src/test/resources/1.ofd");
        try (org.ofdrw.reader.OFDReader reader = new org.ofdrw.reader.OFDReader(sample)) {
            PageRendererImpl.RenderPipeline pipeline = PageRendererImpl.create(reader);
            assertTrue(pipeline != null);
            assertTrue(pipeline.pageRenderer() != null);
            assertTrue(pipeline.fontSelector() != null);
            assertTrue(pipeline.registry() != null);
            // 渲染侧与写入侧必须共享同一份字体登记表
            assertSame(pipeline.registry(), pipeline.fontSelector().getRegistry());
            assertSame(pipeline.registry(), PageRendererImpl.create(reader,
                    pipeline.registry()).registry());
        }
    }

    @Test
    @DisplayName("装配工厂：文字内容真的产出（整类内容缺失回归）")
    void createRendersTextContent() throws Exception {
        java.nio.file.Path sample = java.nio.file.Paths.get("src/test/resources/helloworld.ofd");
        try (org.ofdrw.reader.OFDReader reader = new org.ofdrw.reader.OFDReader(sample)) {
            PageRendererImpl.RenderPipeline pipeline = PageRendererImpl.create(reader);
            PdfPageContent content = pipeline.pageRenderer().renderPage(reader.getPage(1), pipeline.fontSelector());
            int textCommands = 0;
            int glyphs = 0;
            for (org.ofdrw.converter.pdf.model.ContentCommand command : content.getCommands()) {
                if (command instanceof org.ofdrw.converter.pdf.model.TextCommand) {
                    textCommands++;
                    glyphs += ((org.ofdrw.converter.pdf.model.TextCommand) command).getGlyphs().size();
                }
            }
            // 只断言“指令数 > 0”会被纯图形文档骗过：字体解析器缺失时整页文字会被静默跳过
            assertTrue(textCommands > 0, "文字指令数：" + textCommands + "，总指令：" + content.size());
            assertTrue(glyphs > 0, "字形数：" + glyphs);
        }
    }

    @Test
    @DisplayName("无图片文档：渲染不产生图像资源相关的 WARN")
    void noImageDocumentHasNoResourceWarning() throws Exception {
        java.nio.file.Path sample = java.nio.file.Paths.get("src/test/resources/helloworld.ofd");
        LogCapture capture = LogCapture.start(ImageResourceLoader.class.getName());
        try (org.ofdrw.reader.OFDReader reader = new org.ofdrw.reader.OFDReader(sample)) {
            PageRendererImpl.RenderPipeline pipeline = PageRendererImpl.create(reader);
            PdfPageContent content = pipeline.pageRenderer().renderPage(reader.getPage(1), pipeline.fontSelector());
            assertTrue(content.isBalanced());
        } finally {
            capture.close();
        }
        // 资源索引/资源描述文件读取失败只是内部细节，不应以 WARN 出现（更不应出现 PublicRes.xml 之类）；
        // 这里再校验捕获器本身有效：主动打一条 WARN 必须被捕获到
        capture.close();
        LogCapture probe = LogCapture.start(ImageResourceLoader.class.getName());
        org.apache.logging.log4j.LogManager.getLogger(ImageResourceLoader.class).warn("provider-check");
        probe.close();
        assertTrue(probe.warnings().contains("provider-check"), "日志捕获器未生效");
        assertTrue(capture.warnings().isEmpty(), "不应产生告警：" + capture.warnings());
    }

    @Test
    @DisplayName("有图片文档：图片资源正常加载（原始字节非空、尺寸正确）")
    void imageDocumentLoadsResources() throws Exception {
        java.nio.file.Path sample = java.nio.file.Paths.get("src/test/resources/containsJPEG.ofd");
        try (org.ofdrw.reader.OFDReader reader = new org.ofdrw.reader.OFDReader(sample)) {
            ImageResourceLoader loader = new ImageResourceLoader(reader);
            ImageResourceLoader.ImageData data = loader.load("4");
            assertTrue(data != null, "资源 4 应能加载");
            assertEquals(ImageEncoding.JPEG, data.getEncoding());
            assertTrue(data.getData().length > 0);
            assertTrue(data.getWidth() > 0 && data.getHeight() > 0);
            assertTrue(data.isRawJpeg());
            // 不存在的资源返回 null，且不抛异常
            assertTrue(loader.load("404") == null);
        }
    }

    @Test
    @DisplayName("页面框口径：页面无 Area 时沿用文档级 PageArea（1.ofd = 210×140，不是 A4）")
    void pageBoxFallsBackToDocumentPageArea() throws Exception {
        java.nio.file.Path sample = java.nio.file.Paths.get("src/test/resources/pdf-match/1.ofd");
        try (org.ofdrw.reader.OFDReader reader = new org.ofdrw.reader.OFDReader(sample)) {
            org.ofdrw.core.basicStructure.pageObj.Page page = reader.getPage(1);
            // 前提：该页自身没有 Area，尺寸来自文档级 CommonData/PageArea
            assertTrue(page.getArea() == null, "样本前提不成立：页面自带 Area");

            PageRendererImpl.PageBox withReader = PageRendererImpl.pageBox(page, reader);
            assertEquals(210, withReader.getWidth(), 1e-6);
            assertEquals(140, withReader.getHeight(), 1e-6);

            // 单参重载无法取得文档级区域，会回退 A4 —— 这正是“表格线画到页外”的成因，必须保留为反例
            PageRendererImpl.PageBox withoutReader = PageRendererImpl.pageBox(page);
            assertEquals(297, withoutReader.getHeight(), 1e-6);

            // 渲染必须使用带 reader 的口径
            PageRendererImpl.RenderPipeline pipeline = PageRendererImpl.create(reader);
            PdfPageContent content = pipeline.pageRenderer().renderPage(page, pipeline.fontSelector());
            assertEquals(210 * CoordSystem.MM_TO_PT, content.getWidthPt(), 1e-3);
            assertEquals(140 * CoordSystem.MM_TO_PT, content.getHeightPt(), 1e-3);
            assertTrue(content.isBalanced());
        }
    }

    @Test
    @DisplayName("页面无 Area 的样本：所有路径指令坐标都落在页面范围内（表格线不得画出页外）")
    void pathsStayInsidePageWhenPageAreaIsDocumentLevel() throws Exception {
        java.nio.file.Path sample = java.nio.file.Paths.get("src/test/resources/pdf-match/1.ofd");
        try (org.ofdrw.reader.OFDReader reader = new org.ofdrw.reader.OFDReader(sample)) {
            PageRendererImpl.RenderPipeline pipeline = PageRendererImpl.create(reader);
            PdfPageContent content = pipeline.pageRenderer().renderPage(reader.getPage(1), pipeline.fontSelector());
            double pageH = content.getHeightPt();
            double pageW = content.getWidthPt();
            int paths = 0;
            for (org.ofdrw.converter.pdf.model.ContentCommand command : content.getCommands()) {
                if (!(command instanceof org.ofdrw.converter.pdf.model.PathCommand)) {
                    continue;
                }
                paths++;
                for (org.ofdrw.converter.pdf.model.PathSegment segment : ((org.ofdrw.converter.pdf.model.PathCommand) command).getSegments()) {
                    double[] point = endPoint(segment);
                    if (point == null) {
                        continue;
                    }
                    assertTrue(point[0] >= -1 && point[0] <= pageW + 1,
                            "路径 x 越界：" + point[0] + "（页宽 " + pageW + "）");
                    assertTrue(point[1] >= -1 && point[1] <= pageH + 1,
                            "路径 y 越界：" + point[1] + "（页高 " + pageH + "，若按 A4 297mm 翻转会得到 ~790pt）");
                }
            }
            // 1.ofd 的表格线在模板页里，必须真的有路径产出
            assertTrue(paths > 0, "该样本应产出路径指令（表格线）");
        }
    }

    @Test
    @DisplayName("注释外观渲染使用带 reader 的页面框（不会因 A4 回退而偏移）")
    void renderAnnotationsUsesDocumentPageArea() throws Exception {
        java.nio.file.Path sample = java.nio.file.Paths.get("src/test/resources/pdf-match/1.ofd");
        try (org.ofdrw.reader.OFDReader reader = new org.ofdrw.reader.OFDReader(sample)) {
            PageRendererImpl.RenderPipeline pipeline = PageRendererImpl.create(reader);
            org.ofdrw.core.basicStructure.pageObj.Page page = reader.getPage(1);
            PdfPageContent content = pipeline.pageRenderer().renderAnnotations(page,
                    java.util.Collections.emptyList(), pipeline.fontSelector());
            // 注释层与正文层同尺寸（同一页面框口径）
            assertEquals(210 * CoordSystem.MM_TO_PT, content.getWidthPt(), 1e-3);
            assertEquals(140 * CoordSystem.MM_TO_PT, content.getHeightPt(), 1e-3);
        }
    }

    @Test
    @DisplayName("页面自带 Area 的样本：正文与模板的路径/文字坐标都在页内（testImageNotFound）")
    void pageContentStaysInsidePageForExplicitArea() throws Exception {
        java.nio.file.Path sample = java.nio.file.Paths.get("src/test/resources/pdf-match/testImageNotFound.ofd");
        try (org.ofdrw.reader.OFDReader reader = new org.ofdrw.reader.OFDReader(sample)) {
            PageRendererImpl.RenderPipeline pipeline = PageRendererImpl.create(reader);
            PageRendererImpl.PageBox box = PageRendererImpl.pageBox(reader.getPage(1), reader);
            PdfPageContent content = pipeline.pageRenderer().renderPage(reader.getPage(1), pipeline.fontSelector());
            assertEquals(box.getWidth() * CoordSystem.MM_TO_PT, content.getWidthPt(), 1e-3);
            assertEquals(box.getHeight() * CoordSystem.MM_TO_PT, content.getHeightPt(), 1e-3);
            double pageH = content.getHeightPt();
            double pageW = content.getWidthPt();
            int paths = 0;
            for (org.ofdrw.converter.pdf.model.ContentCommand command : content.getCommands()) {
                if (command instanceof org.ofdrw.converter.pdf.model.PathCommand) {
                    paths++;
                    for (org.ofdrw.converter.pdf.model.PathSegment segment
                            : ((org.ofdrw.converter.pdf.model.PathCommand) command).getSegments()) {
                        double[] point = endPoint(segment);
                        if (point == null) {
                            continue;
                        }
                        assertTrue(point[0] >= -1 && point[0] <= pageW + 1, "路径 x 越界：" + point[0]);
                        assertTrue(point[1] >= -1 && point[1] <= pageH + 1, "路径 y 越界：" + point[1]);
                    }
                }
            }
            assertTrue(paths > 0, "该样本应产出路径指令");
        }
    }

    /**
     * 取路径段的终点坐标
     *
     * @param segment 路径段
     * @return 终点 {@code [x, y]}；闭合段返回 {@code null}
     */
    private static double[] endPoint(org.ofdrw.converter.pdf.model.PathSegment segment) {
        if (segment instanceof org.ofdrw.converter.pdf.model.ClosePath) {
            return null;
        }
        return new double[]{segment.getEndX(), segment.getEndY()};
    }

    @Test
    @DisplayName("结构源页面框与渲染同口径：无 Area 页面取文档级 PageArea（书签/注释目标不偏移）")
    void structureSourcePageBoxMatchesRenderer() throws Exception {
        java.nio.file.Path sample = java.nio.file.Paths.get("src/test/resources/pdf-match/1.ofd");
        try (org.ofdrw.reader.OFDReader reader = new org.ofdrw.reader.OFDReader(sample)) {
            org.ofdrw.converter.pdf.write.StructureSource source =
                    org.ofdrw.converter.pdf.write.StructureSource.load(reader, null);
            assertTrue(!source.getPages().isEmpty(), "样本应能读到页面");
            PageRendererImpl.PageBox fromSource = source.pageBox(0);
            PageRendererImpl.PageBox fromRenderer = PageRendererImpl.pageBox(reader.getPage(1), reader);
            assertEquals(fromRenderer.getWidth(), fromSource.getWidth(), 1e-6);
            assertEquals(fromRenderer.getHeight(), fromSource.getHeight(), 1e-6);
            // 必须使用文档级 PageArea（140mm），不能回退 A4
            assertEquals(140, fromSource.getHeight(), 1e-6);
        }
    }

    @Test
    @DisplayName("页面框：MediaBox = CropBox = PhysicalBox（决策 26）")
    void pageBoxFollowsPhysicalBox() {
        PageRendererImpl.PageBox box = PageRendererImpl.pageBox(page());
        assertEquals(100, box.getWidth(), EPS);
        assertEquals(100, box.getHeight(), EPS);
        Box pdf = box.toPdfBox();
        assertEquals(100 * CoordSystem.MM_TO_PT, pdf.getWidth(), EPS);
        assertEquals(100 * CoordSystem.MM_TO_PT, pdf.getHeight(), EPS);
    }

    @Test
    @DisplayName("图层 ZOrder：Background → Body → Foreground，且每层 q/Q 隔离")
    void layersOrderedByZOrder() {
        PdfPageContent content = render(page(
                layer(Type.Foreground, 30),
                layer(Type.Background, 10),
                layer(Type.Body, 20)));

        assertTrue(content.isBalanced());
        List<org.ofdrw.converter.pdf.model.ContentCommand> commands = content.getCommands();
        // 每层：q + 图元自身 q/Q(含样式与路径) + Q，具体条数与写入器无关，这里只校验路径出现顺序
        assertTrue(commands.size() >= 15, "实际指令：" + commands);
        double[] xs = new double[3];
        int index = 0;
        for (org.ofdrw.converter.pdf.model.ContentCommand command : commands) {
            if (command instanceof org.ofdrw.converter.pdf.model.PathCommand) {
                xs[index++] = ((org.ofdrw.converter.pdf.model.PathCommand) command).getSegments().get(0).getEndX();
            }
        }
        // 层顺序：背景（x=10mm）→ 正文（20mm）→ 前景（30mm）
        assertEquals(CoordSystem.mmToPt(10), xs[0], EPS);
        assertEquals(CoordSystem.mmToPt(20), xs[1], EPS);
        assertEquals(CoordSystem.mmToPt(30), xs[2], EPS);
    }
}
