package org.ofdrw.converter.style;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ofdrw.converter.GeneralConvertException;
import org.ofdrw.converter.canvas.BufferedImageSurface;
import org.ofdrw.converter.canvas.CanvasImageMaker;
import org.ofdrw.converter.renderer.Graphics2DRenderer;
import org.ofdrw.converter.renderer.Renderer;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.pageDescription.color.color.CT_Color;
import org.ofdrw.core.pageDescription.drawParam.CT_DrawParam;
import org.ofdrw.reader.OFDReader;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 样式作用域链（{@link StyleScope}）与绘制参数结算测试。
 * <p>
 * 重点验证原实现中 {@code drawParams} 共享可变列表导致的参数污染已被消除：
 * 兄弟图层中先处理的图层追加的绘制参数不会影响后续兄弟图层。
 * 像素级用例直接驱动 {@code AWTMaker.writePageBlock}，绘制参数由进程内查找器提供，覆盖完整作用域链。
 *
 * @author 权观宇
 * @since 2026-10-25
 */
class StyleScopeTest {

    /**
     * 临时目录，用于生成测试用 OFD 文档
     */
    @TempDir
    Path tempDir;

    @Test
    @DisplayName("兄弟图层的绘制参数互不污染：第二图层不受第一图层红色绘制参数影响")
    void siblingLayerDrawParamDoesNotPollute() throws Exception {
        BufferedImage image = render(twoSiblingLayersContent(true, false));
        Color firstLayerFill = new Color(image.getRGB(10, 20));
        Color secondLayerFill = new Color(image.getRGB(110, 70));

        assertEquals(new Color(0xFF0000), firstLayerFill, "第一图层应使用红色绘制参数填充");
        assertNotEquals(new Color(0xFF0000), secondLayerFill, "第二图层被兄弟图层的绘制参数污染");
        assertEquals(Color.WHITE, secondLayerFill,
                "第二图层既无自身颜色也无绘制参数：路径填充分保持透明，画布保持白色");
    }

    @Test
    @DisplayName("第二图层自身的绘制参数只作用于自身")
    void secondLayerDrawParamOnlyAffectsItself() throws Exception {
        BufferedImage image = render(twoSiblingLayersContent(false, true));

        assertEquals(Color.WHITE, new Color(image.getRGB(10, 20)), "第一图层无绘制参数，路径填充分保持透明");
        assertEquals(new Color(0xFF0000), new Color(image.getRGB(110, 70)), "第二图层自身的绘制参数应生效");
    }

    @Test
    @DisplayName("绘制参数位于第一图层时不影响后续兄弟图层（修复前会被污染）")
    void firstLayerDrawParamDoesNotLeakToSibling() throws Exception {
        BufferedImage withParam = render(twoSiblingLayersContent(true, false));
        BufferedImage withoutParam = render(twoSiblingLayersContent(false, false));
        assertEquals(Color.WHITE, new Color(withParam.getRGB(120, 65)), "第二图层应保持透明填充（未使用第一图层的参数）");
        // 第二图层像素与“第一图层没有绘制参数”时完全一致，说明未被污染
        assertEquals(withoutParam.getRGB(110, 70), withParam.getRGB(110, 70),
                "第二图层像素应与无第一图层绘制参数时完全一致");
        assertEquals(new Color(0xFF0000), new Color(withParam.getRGB(10, 20)), "第一图层使用红色填充");
        assertEquals(Color.WHITE, new Color(withoutParam.getRGB(10, 20)), "无绘制参数时第一图层填充分保持透明");
    }

    @Test
    @DisplayName("作用域链不可变：child 返回新实例且不影响父作用域与兄弟作用域")
    void scopeIsImmutable() {
        StyleScope document = StyleScope.document();
        StyleScope layerA = document.child(StyleScopeKind.LAYER, new ST_RefID(1), null);
        StyleScope layerB = document.child(StyleScopeKind.LAYER, null, null);

        assertNotEquals(layerA, layerB);
        assertEquals(null, document.getDrawParamId(), "父作用域不应被修改");
        assertEquals(null, layerB.getDrawParamId(), "兄弟作用域不应看到其他节点的绘制参数");
        assertEquals("1", layerA.getDrawParamId().toString());
        assertEquals(document, layerA.getParent());
        assertEquals(document, layerB.getParent());
    }

    @Test
    @DisplayName("结算优先级：绘制参数 > 类型默认值，图元自身颜色最高")
    void resolvePriority() {
        StyleScope document = StyleScope.document();
        StyleScope layer = document.child(StyleScopeKind.LAYER, new ST_RefID(20), null);
        StyleScope object = layer.child(StyleScopeKind.OBJECT, null, null);

        ResolvedStyle text = resolverWith(PARAM_FILL_BLUE).resolve(object, GraphicUnitType.TEXT, null,
                null, null, null, null);
        assertEquals(Integer.valueOf(0x0000FF), text.getFillColor(), "绘制参数中的填充色应覆盖类型默认值");
        assertTrue(text.isFill(), "Fill 开关未设置时取文字类型默认值");
        assertFalse(text.isStroke(), "Stroke 开关未设置时取文字类型默认值");

        ResolvedStyle path = resolverWith(PARAM_FILL_BLUE).resolve(object, GraphicUnitType.PATH, Boolean.FALSE,
                Boolean.TRUE, null, null, null);
        assertEquals(Integer.valueOf(0x0000FF), path.getFillColor());
        assertTrue(path.isStroke(), "Stroke 开关取图元属性");
        assertEquals(Integer.valueOf(0x000000), path.getStrokeColor(), "描边色取类型默认值黑色");

        ResolvedStyle own = resolverWith(PARAM_FILL_BLUE).resolve(object, GraphicUnitType.PATH, Boolean.TRUE,
                Boolean.TRUE, CT_Color.rgb(0, 128, 0), null, null);
        assertEquals(Integer.valueOf(0x008000), own.getFillColor(), "图元自身颜色优先级最高");
    }

    @Test
    @DisplayName("Fill/Stroke 开关与 Alpha 不从父作用域继承")
    void fillStrokeAlphaNotInherited() {
        // 父作用域内联样式为：填充、描边、透明度 0.5
        ResolvedStyle parentInline = new ResolvedStyle(0.353, LineCapStyle.BUTT, LineJoinStyle.MITER, 3.528,
                0, new double[0], 0x123456, 0x654321, true, true, 0.5);
        StyleScope layer = StyleScope.document().child(StyleScopeKind.LAYER, null, parentInline);
        StyleScope object = layer.child(StyleScopeKind.OBJECT, null, null);

        ResolvedStyle style = resolverWith(null).resolve(object, GraphicUnitType.IMAGE, null, null, null, null, null);
        assertEquals(Integer.valueOf(0x123456), style.getFillColor(), "颜色属于可继承内容");
        assertFalse(style.isFill(), "IMAGE 类型默认 Fill=false，说明开关没有从父作用域继承");
        assertFalse(style.isStroke(), "IMAGE 类型默认 Stroke=false，说明开关没有从父作用域继承");
        assertEquals(1.0, style.getAlpha(), 1e-9, "Alpha 不从父作用域继承，默认 1");
    }

    @Test
    @DisplayName("Relative 链按就近原则覆盖")
    void relativeChainOverrides() {
        CT_DrawParam base = new CT_DrawParam();
        base.setID(new ST_ID(11));
        base.setLineWidth(2.0);
        base.setFillColor(CT_Color.rgb(0, 0, 255));

        CT_DrawParam child = new CT_DrawParam();
        child.setID(new ST_ID(12));
        child.setRelative(new ST_RefID(11));
        child.setLineWidth(0.5);

        DrawParamResolver resolver = new DrawParamResolver(lookupOf(base, child));
        ResolvedStyle style = resolver.resolve(new ST_RefID(12));
        assertNotNull(style);
        assertEquals(0.5, style.getLineWidth(), 1e-9, "当前绘制参数覆盖基础绘制参数");
        assertEquals(Integer.valueOf(0x0000FF), style.getFillColor(), "基础绘制参数的填充色被继承");
    }

    @Test
    @DisplayName("Relative 环引用不死循环且截断")
    void relativeCycleIsTruncated() {
        CT_DrawParam a = new CT_DrawParam();
        a.setID(new ST_ID(31));
        a.setRelative(new ST_RefID(32));
        a.setLineWidth(1.5);

        CT_DrawParam b = new CT_DrawParam();
        b.setID(new ST_ID(32));
        b.setRelative(new ST_RefID(31));
        b.setFillColor(CT_Color.rgb(255, 0, 0));

        DrawParamResolver resolver = new DrawParamResolver(lookupOf(a, b));
        ResolvedStyle style = resolver.resolve(new ST_RefID(31));
        assertNotNull(style);
        assertEquals(1.5, style.getLineWidth(), 1e-9);
        assertEquals(Integer.valueOf(0xFF0000), style.getFillColor(), "环上另一节点的字段仍然生效，但展开被截断");
    }

    @Test
    @DisplayName("Relative 链超过最大深度时截断")
    void relativeTooDeepIsTruncated() {
        CT_DrawParam[] params = new CT_DrawParam[DrawParamResolver.DEFAULT_MAX_DEPTH + 5];
        for (int i = 0; i < params.length; i++) {
            params[i] = new CT_DrawParam();
            params[i].setID(new ST_ID(1000 + i));
            params[i].setLineWidth(1.0 + i);
            if (i + 1 < params.length) {
                params[i].setRelative(new ST_RefID(1000 + i + 1));
            }
        }
        DrawParamResolver resolver = new DrawParamResolver(lookupOf(params));
        ResolvedStyle style = resolver.resolve(new ST_RefID(1000));
        assertNotNull(style);
        // 起始参数自身字段生效，链尾超出深度的字段被截断
        assertEquals(1.0, style.getLineWidth(), 1e-9);
    }

    @Test
    @DisplayName("无 Font 属性的文字对象不抛异常，页面其余内容正常渲染")
    void textWithoutFontDoesNotThrow() throws Exception {
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<ofd:Page xmlns:ofd=\"http://www.ofdspec.org/2016\">"
                + "<ofd:Area><ofd:PhysicalBox>0 0 100 50</ofd:PhysicalBox></ofd:Area>"
                + "<ofd:Content><ofd:Layer ID=\"200\">"
                + "<ofd:TextObject ID=\"201\" Boundary=\"10 10 60 10\" Size=\"4\">"
                + "<ofd:TextCode X=\"0\" Y=\"4\">无字体属性</ofd:TextCode>"
                + "</ofd:TextObject>"
                + "<ofd:PathObject ID=\"202\" Boundary=\"10 30 20 10\" Fill=\"true\" Stroke=\"false\">"
                + "<ofd:AbbreviatedData>M 0 0 L 20 0 L 20 10 L 0 10 C</ofd:AbbreviatedData>"
                + "</ofd:PathObject>"
                + "</ofd:Layer></ofd:Content></ofd:Page>";
        BufferedImage image = render(content);
        // 文字缺少字体时被跳过，页面不中断：整个画布保持白色且无异常
        assertEquals(Color.WHITE, new Color(image.getRGB(10, 35)));
        assertEquals(Color.WHITE, new Color(image.getRGB(50, 20)), "无字体属性的文字不产生任何绘制");
    }

    @Test
    @DisplayName("空画布与非法页码仍抛出转换异常")
    void invalidArgumentsThrow() throws Exception {
        Path ofd = stubOfd("helloworld-like", "Document.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<ofd:Document xmlns:ofd=\"http://www.ofdspec.org/2016\">"
                + "<ofd:CommonData><ofd:PageArea><ofd:PhysicalBox>0 0 100 50</ofd:PhysicalBox></ofd:PageArea>"
                + "<ofd:MaxUnitID>1</ofd:MaxUnitID></ofd:CommonData>"
                + "<ofd:Pages><ofd:Page ID=\"1\" BaseLoc=\"Pages/Page_0/Content.xml\"/></ofd:Pages>"
                + "</ofd:Document>");
        try (OFDReader reader = new OFDReader(ofd)) {
            CanvasImageMaker maker = new CanvasImageMaker(reader, 1.0);
            assertThrows(GeneralConvertException.class, () -> maker.drawPage(null, 0));
            assertThrows(GeneralConvertException.class,
                    () -> maker.drawPage(new BufferedImageSurface(10, 10, Color.WHITE), 9));
        }
    }

    /**
     * 暴露受保护页面块绘制入口的测试渲染器。
     */
    private static final class TestMaker extends CanvasImageMaker {

        /**
         * 创建测试渲染器。
         *
         * @param reader OFD 解析器
         * @param ppm    每毫米像素数量，单位：像素/毫米
         */
        TestMaker(OFDReader reader, double ppm) {
            super(reader, ppm);
        }

        /**
         * 使用指定样式结算器绘制页面内容。
         *
         * @param renderer 目标渲染器
         * @param content  页面内容
         * @param resolver 样式结算器
         */
        void renderContent(Renderer renderer, org.ofdrw.core.basicStructure.pageObj.Content content,
                           StyleResolver resolver) {
            writeContent(renderer, content, resolver);
        }
    }

    /**
     * 生成包含两个兄弟图层的页面内容。
     * <p>
     * 两个图层内各有一个矩形路径对象（Fill=true、Stroke=false 且自身不设置颜色）：绘制参数 70 只改变填充色为红色，
     * 因此矩形填充色可以区分“类型默认值黑色”与“绘制参数颜色红色”。
     *
     * @param firstLayerRefParam  第一个图层是否引用绘制参数 70
     * @param secondLayerRefParam 第二个图层是否引用绘制参数 70
     * @return 页面内容 XML
     */
    private String twoSiblingLayersContent(boolean firstLayerRefParam, boolean secondLayerRefParam) {
        String firstDrawParam = firstLayerRefParam ? " DrawParam=\"70\"" : "";
        String secondDrawParam = secondLayerRefParam ? " DrawParam=\"70\"" : "";
        // 两个图层：第一图层矩形位于页面左上，第二图层矩形位于页面右下；矩形自身不设置任何颜色
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<ofd:Page xmlns:ofd=\"http://www.ofdspec.org/2016\">"
                + "<ofd:Area><ofd:PhysicalBox>0 0 200 100</ofd:PhysicalBox></ofd:Area>"
                + "<ofd:Content>"
                + "<ofd:Layer ID=\"100\"" + firstDrawParam + ">"
                + "<ofd:PathObject ID=\"103\" Boundary=\"10 10 80 15\" Fill=\"true\" Stroke=\"false\">"
                + "<ofd:AbbreviatedData>M 0 0 L 80 0 L 80 15 L 0 15 C</ofd:AbbreviatedData>"
                + "</ofd:PathObject>"
                + "</ofd:Layer>"
                + "<ofd:Layer ID=\"101\"" + secondDrawParam + ">"
                + "<ofd:PathObject ID=\"104\" Boundary=\"110 60 80 15\" Fill=\"true\" Stroke=\"false\">"
                + "<ofd:AbbreviatedData>M 0 0 L 80 0 L 80 15 L 0 15 C</ofd:AbbreviatedData>"
                + "</ofd:PathObject>"
                + "</ofd:Layer>"
                + "</ofd:Content></ofd:Page>";
    }

    /**
     * 渲染页面内容为位图。
     * <p>
     * 复用 AWTMaker 的页面块绘制入口，绘制参数 70 由进程内查找器提供，覆盖一次绘制中的完整作用域链。
     *
     * @param content 页面内容 XML
     * @return 位图；背景白色，1 像素代表 1 毫米
     * @throws Exception 内容解析或文档构造失败
     */
    private BufferedImage render(String content) throws Exception {
        final CT_DrawParam redParam = new CT_DrawParam();
        redParam.setID(new ST_ID(70));
        redParam.setFillColor(CT_Color.rgb(255, 0, 0));
        DrawParamResolver.DrawParamLookup lookup = new DrawParamResolver.DrawParamLookup() {
            @Override
            public CT_DrawParam getDrawParam(String id) {
                return redParam.getID().toString().equals(id) ? redParam : null;
            }

            @Override
            public Color toColor(CT_Color color) {
                if (color == null || color.getValue() == null) {
                    return null;
                }
                List<String> values = color.getValue().getArray();
                return new Color(Integer.parseInt(values.get(0)), Integer.parseInt(values.get(1)),
                        Integer.parseInt(values.get(2)));
            }
        };
        StyleResolver resolver = new StyleResolver(new DrawParamResolver(lookup), lookup);
        Page page = new Page(org.dom4j.DocumentHelper.parseText(content).getRootElement());
        BufferedImageSurface surface = new BufferedImageSurface(200, 100, Color.WHITE);
        // 页面块绘制只依赖转换器的像素比例，通过临时的 1 像素/毫米 转换器驱动
        try (OFDReader reader = new OFDReader(stubOfd("style-render", "Document.xml", emptyDocument()))) {
            TestMaker maker = new TestMaker(reader, 1.0);
            Graphics2DRenderer renderer = new Graphics2DRenderer(surface.createGraphics());
            try {
                renderer.beginPage(200, 100);
                maker.renderContent(renderer, page.getContent(), resolver);
                renderer.endPage();
            } finally {
                renderer.close();
            }
        }
        return surface.getImage();
    }

    /**
     * 构造空文档 XML，仅用于提供 AWTMaker 所需的解析器上下文。
     *
     * @return 文档 XML
     */
    private String emptyDocument() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<ofd:Document xmlns:ofd=\"http://www.ofdspec.org/2016\">"
                + "<ofd:CommonData><ofd:PageArea><ofd:PhysicalBox>0 0 100 50</ofd:PhysicalBox></ofd:PageArea>"
                + "<ofd:MaxUnitID>1</ofd:MaxUnitID></ofd:CommonData>"
                + "<ofd:Pages><ofd:Page ID=\"1\" BaseLoc=\"Pages/Page_0/Content.xml\"/></ofd:Pages>"
                + "</ofd:Document>";
    }

    /**
     * 生成最小可解析的 OFD 包。
     *
     * @param name     文件名前缀
     * @param docXml   文档 XML
     * @param fileName 文档文件名（位于 Doc_0 下）
     * @return OFD 文件路径
     * @throws Exception 写入失败
     */
    private Path stubOfd(String name, String fileName, String docXml) throws Exception {
        if (!fileName.endsWith(".xml")) {
            throw new IllegalArgumentException("文档文件名必须以 .xml 结尾");
        }
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("OFD.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<ofd:OFD xmlns:ofd=\"http://www.ofdspec.org/2016\" DocType=\"OFD\" Version=\"1.0\">"
                + "<ofd:DocBody><ofd:DocRoot>Doc_0/" + fileName + "</ofd:DocRoot></ofd:DocBody></ofd:OFD>");
        entries.put("Doc_0/" + fileName, docXml);
        entries.put("Doc_0/Pages/Page_0/Content.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<ofd:Page xmlns:ofd=\"http://www.ofdspec.org/2016\">"
                + "<ofd:Area><ofd:PhysicalBox>0 0 100 50</ofd:PhysicalBox></ofd:Area>"
                + "<ofd:Content><ofd:Layer ID=\"9\"/></ofd:Content></ofd:Page>");
        Path ofdPath = tempDir.resolve(name + ".ofd");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(ofdPath))) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return ofdPath;
    }

    /**
     * 构造使用给定绘制参数集合的样式结算器。
     *
     * @param params 绘制参数，可为 null
     * @return 样式结算器
     */
    private StyleResolver resolverWith(CT_DrawParam... params) {
        DrawParamResolver.DrawParamLookup lookup = lookupOf(params);
        return new StyleResolver(new DrawParamResolver(lookup), lookup);
    }

    /**
     * 构造内存绘制参数查找器。
     *
     * @param params 绘制参数集合，可为 null
     * @return 查找器
     */
    private DrawParamResolver.DrawParamLookup lookupOf(final CT_DrawParam... params) {
        return new DrawParamResolver.DrawParamLookup() {
            @Override
            public CT_DrawParam getDrawParam(String id) {
                if (params == null) {
                    return null;
                }
                for (CT_DrawParam param : params) {
                    if (param.getID() != null && param.getID().toString().equals(id)) {
                        return param;
                    }
                }
                return null;
            }

            @Override
            public Color toColor(CT_Color color) {
                if (color == null || color.getValue() == null) {
                    return null;
                }
                List<String> values = color.getValue().getArray();
                return new Color(Integer.parseInt(values.get(0)), Integer.parseInt(values.get(1)),
                        Integer.parseInt(values.get(2)));
            }
        };
    }

    /**
     * 蓝色绘制参数，用于优先级测试。
     */
    private static final CT_DrawParam PARAM_FILL_BLUE;

    static {
        PARAM_FILL_BLUE = new CT_DrawParam();
        PARAM_FILL_BLUE.setID(new ST_ID(20));
        PARAM_FILL_BLUE.setFillColor(CT_Color.rgb(0, 0, 255));
    }
}
