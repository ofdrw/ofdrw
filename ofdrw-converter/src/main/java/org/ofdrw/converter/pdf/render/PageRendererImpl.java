package org.ofdrw.converter.pdf.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.ofdrw.converter.pdf.CoordSystem;
import org.ofdrw.converter.pdf.font.FontSelector;
import org.ofdrw.converter.pdf.font.FontSelectorImpl;
import org.ofdrw.converter.pdf.font.TrueTypePdfFont;
import org.ofdrw.converter.pdf.model.AffineMatrix;
import org.ofdrw.converter.pdf.model.ContentCommand;
import org.ofdrw.converter.pdf.model.PdfPageContent;
import org.ofdrw.core.annotation.pageannot.Annot;
import org.ofdrw.core.annotation.pageannot.Appearance;
import org.ofdrw.core.basicStructure.pageObj.CT_TemplatePage;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicStructure.pageObj.Template;
import org.ofdrw.core.basicStructure.pageObj.layer.CT_Layer;
import org.ofdrw.core.basicStructure.pageObj.layer.block.CT_PageBlock;
import org.ofdrw.core.basicStructure.pageObj.layer.PageBlockType;
import org.ofdrw.core.basicStructure.pageObj.layer.Type;
import org.ofdrw.core.basicStructure.pageObj.layer.block.CompositeObject;
import org.ofdrw.core.basicStructure.pageObj.layer.block.ImageObject;
import org.ofdrw.core.basicStructure.pageObj.layer.block.PathObject;
import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.compositeObj.CT_VectorG;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.ResourceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 页面级渲染器实现：页框、模板页与图层 ZOrder、PageBlock 嵌套、CompositeObject 调度
 * <p>
 * 设计 4.1/4.2 节与 3.2 节要求：
 * <ul>
 *     <li>页面框（决策 26）：{@code MediaBox = CropBox = PhysicalBox}，物理页面区域由 {@link #pageBox} 给出；</li>
 *     <li>渲染顺序：Background 模板 → Background 层 → Body 模板 → Body 层 → Foreground 模板 → Foreground 层，
 *         同一 {@link Type} 内先模板后图层；</li>
 *     <li>缩放<b>只</b>由显式容器上下文决定：进入模板页、CompositeObject 时通过
 *         {@link RenderContext#withCtm} 叠加容器 CTM，绝不嗅探父节点类型；</li>
 *     <li>PageBlock 嵌套：{@code CT_PageBlock} 递归渲染，循环引用深度上限 {@link #MAX_BLOCK_DEPTH}；</li>
 *     <li>容错：单图元/单容器异常只跳过并 warning，不中断整页（设计约束 3）。</li>
 * </ul>
 * <p>
 * CompositeObject 的容器语义（依据 GB/T 33190 第 7.12 节「矢量图像」与现有实现实测）：
 * 组合内容的坐标与组合对象处于<b>同一坐标系</b>，组合对象的外接矩形只描述自身位置，不再对内容叠加平移；
 * 组合自身 CTM 作用于内容，叠加在父容器 CTM 之前。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class PageRendererImpl implements PageRenderer {

    /**
     * 日志
     */
    private static final Logger logger = LoggerFactory.getLogger(PageRendererImpl.class);

    /**
     * PageBlock 嵌套与容器递归的最大深度
     */
    public static final int MAX_BLOCK_DEPTH = 32;

    /**
     * 物理页面区域（决策 26）
     *
     * @author 权观宇
     * @since 2.5.1
     */
    public static final class PageBox {

        /**
         * 页面框左上角横坐标，单位 mm
         */
        private final double x0;

        /**
         * 页面框左上角纵坐标，单位 mm
         */
        private final double y0;

        /**
         * 页面宽度，单位 mm
         */
        private final double width;

        /**
         * 页面高度，单位 mm
         */
        private final double height;

        /**
         * 构造物理页面区域
         *
         * @param x0     页面框左上角横坐标，单位 mm
         * @param y0     页面框左上角纵坐标，单位 mm
         * @param width  页面宽度，单位 mm
         * @param height 页面高度，单位 mm
         */
        public PageBox(double x0, double y0, double width, double height) {
            this.x0 = x0;
            this.y0 = y0;
            this.width = width;
            this.height = height;
        }

        /**
         * @return 页面框左上角横坐标，单位 mm
         */
        public double getX0() {
            return x0;
        }

        /**
         * @return 页面框左上角纵坐标，单位 mm
         */
        public double getY0() {
            return y0;
        }

        /**
         * @return 页面宽度，单位 mm
         */
        public double getWidth() {
            return width;
        }

        /**
         * @return 页面高度，单位 mm
         */
        public double getHeight() {
            return height;
        }

        /**
         * @return 页面矩形（PDF 页面坐标，单位 pt，左下角原点），即 MediaBox = CropBox
         */
        public org.ofdrw.converter.pdf.model.Box toPdfBox() {
            return CoordSystem.pageBoxToPt(x0, y0, width, height);
        }

        @Override
        public String toString() {
            return "PageBox[" + x0 + ", " + y0 + ", " + width + " x " + height + "mm]";
        }
    }

    /**
     * OFD 读取器（提供资源定位与文档结构），可为空（此时模板页与复合对象只告警不渲染）
     */
    private final OFDReader reader;

    /**
     * 路径渲染器
     */
    private final PathRenderer pathRenderer;

    /**
     * 文字渲染器，可为空（为空时文字图元跳过并告警）
     */
    private final TextRenderer textRenderer;

    /**
     * 图像渲染器
     */
    private final ImageRenderer imageRenderer;

    /**
     * 已告警原因
     */
    private final Set<String> reported = ConcurrentHashMap.newKeySet();

    /**
     * 构造页面渲染器
     *
     * @param reader        OFD 读取器，可为空
     * @param pathRenderer  路径渲染器，不能为空
     * @param textRenderer  文字渲染器，可为空
     * @param imageRenderer 图像渲染器，不能为空
     */
    public PageRendererImpl(OFDReader reader, PathRenderer pathRenderer, TextRenderer textRenderer,
                            ImageRenderer imageRenderer) {
        if (pathRenderer == null || imageRenderer == null) {
            throw new IllegalArgumentException("路径渲染器与图像渲染器不能为空");
        }
        this.reader = reader;
        this.pathRenderer = pathRenderer;
        this.textRenderer = textRenderer;
        this.imageRenderer = imageRenderer;
    }

    /**
     * 渲染管线装配结果：页面渲染器 + 字体选择器 + 字体登记表
     * <p>
     * 字体选择器与写入侧必须共享同一个 {@link TrueTypePdfFont.Registry} 实例：
     * 渲染时登记字体并分配资源名（F1、F2…），写入时按资源名取回字体，
     * 两边各建一个实例会导致资源名错配。
     *
     * @author 权观宇
     * @since 2.5.1
     */
    public static final class RenderPipeline {

        /**
         * 页面渲染器
         */
        private final PageRendererImpl pageRenderer;

        /**
         * 字体选择器（渲染侧使用）
         */
        private final FontSelectorImpl fontSelector;

        /**
         * 字体登记表（渲染侧与写入侧共享）
         */
        private final TrueTypePdfFont.Registry registry;

        /**
         * 构造渲染管线
         *
         * @param pageRenderer 页面渲染器，不能为空
         * @param fontSelector 字体选择器，不能为空
         * @param registry     字体登记表，不能为空
         */
        RenderPipeline(PageRendererImpl pageRenderer, FontSelectorImpl fontSelector,
                       TrueTypePdfFont.Registry registry) {
            this.pageRenderer = pageRenderer;
            this.fontSelector = fontSelector;
            this.registry = registry;
        }

        /**
         * @return 页面渲染器
         */
        public PageRendererImpl pageRenderer() {
            return pageRenderer;
        }

        /**
         * @return 字体选择器（作为 {@code renderPage(page, fontSelector)} 的入参）
         */
        public FontSelectorImpl fontSelector() {
            return fontSelector;
        }

        /**
         * @return 字体登记表（写入侧据此按资源名取回字体）
         */
        public TrueTypePdfFont.Registry registry() {
            return registry;
        }
    }

    /**
     * 按 OFD 文档装配完整渲染管线
     * <p>
     * 内部构造路径、文字、图像渲染器及其资源查找器；资源域（文档级 → 公共资源）复用
     * {@link OFDReader#getResourceLocator()} 与 {@link org.ofdrw.reader.ResourceManage} 的既有查找口径，
     * 模板页与复合对象则以显式容器上下文进入，调用方无需重复实现。
     *
     * @param reader OFD 文档读取器，可为空（为空时渲染空白页，不抛异常）
     * @return 已装配好的渲染管线
     */
    public static RenderPipeline create(OFDReader reader) {
        return create(reader, new TrueTypePdfFont.Registry());
    }

    /**
     * 按 OFD 文档装配完整渲染管线，复用调用方已创建的字体登记表
     * <p>
     * 装配关系：
     * <ol>
     *     <li>{@link GraphicsStateResolver}：绘制参数（含 {@code Relative} 继承链）查找；</li>
     *     <li>{@link ColorResolver.ColorSpaceLookup}：颜色空间（含调色板/ICC 引用）查找；</li>
     *     <li>{@link ImageSource}：图像原始字节读取（JPEG 原样嵌入不重编码）；</li>
     *     <li>{@link TextRendererImpl}：文字布局与字形映射；</li>
     *     <li>{@link ImageRendererImpl}：图像与边框，第二个参数复用同一个 {@link PathRenderer} 作为颜色解析器；</li>
     *     <li>{@link FontSelectorImpl}：按资源定位器与共享登记表选择字体。</li>
     * </ol>
     *
     * @param reader   OFD 文档读取器，可为空（为空时渲染空白页，不抛异常）
     * @param registry 字体登记表，不能为空；用于与写入侧共享
     * @return 已装配好的渲染管线
     */
    public static RenderPipeline create(OFDReader reader, TrueTypePdfFont.Registry registry) {
        if (registry == null) {
            registry = new TrueTypePdfFont.Registry();
        }
        GraphicsStateResolver.DrawParamLookup drawParamLookup =
                id -> reader == null || reader.getResMgt() == null ? null : reader.getResMgt().getDrawParam(id);
        ColorResolver.ColorSpaceLookup colorSpaceLookup =
                id -> reader == null || reader.getResMgt() == null ? null : reader.getResMgt().getColorSpace(id);
        ImageSource imageSource = reader == null ? resourceId -> null : new ImageResourceLoader(reader);
        GraphicsStateResolver stateResolver = new GraphicsStateResolver(drawParamLookup);
        PathRenderer pathRenderer = new PathRenderer(stateResolver, colorSpaceLookup);
        ColorResolver colorResolver = new ColorResolver(colorSpaceLookup::getColorSpace);
        TextRendererImpl textRenderer = new TextRendererImpl(colorResolver, stateResolver, colorSpaceLookup);
        ImageRendererImpl imageRenderer = new ImageRendererImpl(imageSource, pathRenderer);
        // 字体选择器必须注入“资源 ID → CT_Font”解析器：文字渲染器传入的是资源 ID，
        // 缺此解析器会把整页文字判为“无可用字体”而全部跳过
        FontSelectorImpl fontSelector = reader == null
                ? new FontSelectorImpl(null, registry)
                : new FontSelectorImpl(reader.getResourceLocator(), registry)
                .fontResolver(id -> reader.getResMgt() == null ? null : reader.getResMgt().getFont(id));
        PageRendererImpl pageRenderer = new PageRendererImpl(reader, pathRenderer, textRenderer, imageRenderer);
        return new RenderPipeline(pageRenderer, fontSelector, registry);
    }

    @Override
    public String getName() {
        return "PageRenderer";
    }

    /**
     * 把注释的静态外观（{@code Annot/Appearance}）追加绘制到页面内容
     * <p>
     * 设计 3.8 节要求注释“视觉用 Appearance 重绘，交互用对应注释类型”：PDF 注释对象只承载交互
     * （链接、高亮、图章语义），<b>视觉一律由页面内容流绘制</b>，这样与正文共用同一套渲染器，
     * 结果与阅读器是否渲染注释外观（/AP）无关，像素回归才稳定。
     * <p>
     * 坐标：外观内容位于页面坐标系，与正文图元一致（注释外观的 {@code Boundary} 不构成额外平移，
     * 见 {@code Appearance} 的说明：内容自身坐标即页面坐标）。因此这里使用与正文相同的
     * {@link RenderContexts#create(double, double, double, double, PdfPageContent, FontSelector)} 上下文，
     * 不叠加任何容器 CTM。
     * <p>
     * Z 序：注释在水印/图章语义上位于正文之上，因此由调用方在正文渲染完成后调用本方法，
     * 指令直接追加在页面内容末尾。
     *
     * @param page         页面，可为空（为空不绘制）
     * @param annots       该页注释列表，可为空（为空不绘制）
     * @param fontSelector 字体选择器，可为空
     * @return 页面内容；包含正文指令与原注释外观指令（原内容对象不被修改时返回新对象）
     */
    public PdfPageContent renderAnnotations(Page page, List<Annot> annots, FontSelector fontSelector) {
        // 页面未声明 Area 时必须回退文档级 PageArea，否则页高被当成 A4 的 297mm，
        // 外观内的图元会按错误的页高做 Y 翻转而落到页面之外
        PageBox pageBox = pageBox(page, this.reader);
        org.ofdrw.converter.pdf.model.Box media = pageBox.toPdfBox();
        PdfPageContent content = new PdfPageContent(media.getWidth(), media.getHeight());
        if (annots == null || annots.isEmpty()) {
            return content;
        }
        RenderContext context = RenderContexts.create(pageBox.getX0(), pageBox.getY0(),
                pageBox.getWidth(), pageBox.getHeight(), content, fontSelector);
        for (Annot annot : annots) {
            renderAppearance(annot, context);
        }
        if (!content.isBalanced()) {
            warnOnce("annot-unbalanced", "注释外观渲染后 q/Q 不成对，已强制恢复：深度=" + content.stateDepth());
            while (content.stateDepth() > 0) {
                content.restoreState();
            }
        }
        return content;
    }

    /**
     * 绘制单个注释的外观
     * <p>
     * 不可见注释（{@code Visible="false"}）不绘制；外观缺失或内容为空时跳过。
     * 单个注释异常只跳过并告警，不影响其它注释与正文（设计约束 3）。
     *
     * @param annot   注释，可为空
     * @param context 渲染上下文
     */
    private void renderAppearance(Annot annot, RenderContext context) {
        if (annot == null) {
            return;
        }
        if (Boolean.FALSE.equals(annot.getVisible())) {
            return;
        }
        String id = annot.getID() == null ? "?" : annot.getID().toString();
        try {
            Appearance appearance = annot.getAppearance();
            if (appearance == null) {
                return;
            }
            List<PageBlockType> blocks = appearance.getPageBlocks();
            if (blocks == null || blocks.isEmpty()) {
                warnOnce("annot-empty-" + id, "注释外观内容为空，已跳过绘制：ID=" + id);
                return;
            }
            // 注释外观的 Boundary 是“外观在页面上的位置与尺寸”（GB/T 33190 表 33 区域）：
            // 外观内的图元坐标相对外观自身，因此必须先按 Boundary 平移，再交给正文渲染器。
            // 缺省 Boundary 时按 (0,0) 处理，即外观内容直接使用页面坐标。
            org.ofdrw.core.basicType.ST_Box boundary = appearance.getBoundary();
            double x = boundary == null || boundary.getTopLeftX() == null ? 0 : boundary.getTopLeftX();
            double y = boundary == null || boundary.getTopLeftY() == null ? 0 : boundary.getTopLeftY();
            RenderContext appearanceContext = context;
            if (x != 0 || y != 0) {
                // 容器 CTM 使用“mm 输入、页面坐标（Y 向下）”的约定：先平移后由各图元渲染器统一做 Y 翻转
                appearanceContext = context.withCtm(org.ofdrw.converter.pdf.model.AffineMatrix.translate(x, y));
            }
            for (PageBlockType block : blocks) {
                context.getPageContent().addAll(renderUnit(block, appearanceContext, 0));
            }
        } catch (RuntimeException e) {
            warnOnce("annot-error-" + id, "注释外观绘制失败，已跳过：ID=" + id + "，原因：" + e.getMessage());
        }
    }

    /**
     * 计算页面物理区域（决策 26：{@code MediaBox = CropBox = PhysicalBox}）
     * <p>
     * 优先取页面自身的 {@code PhysicalBox}，缺失时取 {@code ApplicationBox}；两者都没有时<b>回退 A4</b>。
     * <p>
     * <b>注意</b>：页面未声明 {@code Area} 时按 GB/T 33190 表 27 应沿用文档级 {@code PageArea}，
     * 单参重载无法取得文档级区域，会把 `0 0 210 140` 这类横向页误判成 A4 竖向页，
     * 导致 Y 翻转量错误、图层内容被画到页面之外。渲染与写入路径必须使用
     * {@link #pageBox(Page, OFDReader)}。本重载仅保留给“已知页面自带区域”的调用方与测试。
     *
     * @param page OFD 页面对象，不能为空
     * @return 页面物理区域，单位 mm
     */
    public static PageBox pageBox(Page page) {
        return pageBox(page, null);
    }

    /**
     * 计算页面物理区域，页面自身未声明时回退到文档级 {@code CommonData/PageArea}
     * <p>
     * GB/T 33190 表 27：{@code Page} 的 {@code Area} 是<b>可选</b>属性，缺省时沿用文档级的 {@code PageArea}。
     * 若只读页面自身区域就直接回退 A4，会让 `0 0 210 140` 这类横向页面被错误放大成 A4 竖向页，
     * 进而使 {@code y_pdf = (pageH - y) × k} 的整体翻转量错误、文字落到页面之外。
     *
     * @param page   OFD 页面对象，可为空
     * @param reader 文档读取器，用于取文档级页面区域，可为空
     * @return 页面物理区域，单位 mm
     */
    public static PageBox pageBox(Page page, OFDReader reader) {
        org.ofdrw.core.basicStructure.doc.CT_PageArea area =
                page == null ? null : page.getArea();
        org.ofdrw.core.basicType.ST_Box box = physicalBoxOf(area);
        if (box == null && reader != null) {
            // 页面自身未声明区域：回退到文档级 PageArea（表 27 的可选属性缺省语义）
            try {
                org.ofdrw.core.basicStructure.doc.Document document = reader.cdDefaultDoc();
                org.ofdrw.core.basicStructure.doc.CT_CommonData common =
                        document == null ? null : document.getCommonData();
                box = physicalBoxOf(common == null ? null : common.getPageArea());
            } catch (java.io.IOException | org.dom4j.DocumentException | RuntimeException e) {
                logger.warn("[{}] 读取文档级页面区域失败，按 A4 处理：{}", RenderContext.DIAGNOSTIC_TAG, e.getMessage());
            }
        }
        if (box == null) {
            return new PageBox(0, 0, 210, 297);
        }
        double x0 = box.getTopLeftX() == null ? 0 : box.getTopLeftX();
        double y0 = box.getTopLeftY() == null ? 0 : box.getTopLeftY();
        return new PageBox(x0, y0, box.getWidth(), box.getHeight());
    }

    /**
     * 取页面区域中的物理区域框，物理区域缺失时退用应用区域框
     *
     * @param area 页面区域，可为空
     * @return 尺寸合法的区域框，单位 mm；无可用区域时返回 {@code null}
     */
    private static org.ofdrw.core.basicType.ST_Box physicalBoxOf(
            org.ofdrw.core.basicStructure.doc.CT_PageArea area) {
        if (area == null) {
            return null;
        }
        org.ofdrw.core.basicType.ST_Box box = area.getPhysicalBox();
        if (box == null) {
            box = area.getApplicationBox();
        }
        if (box == null || box.getWidth() == null || box.getHeight() == null
                || box.getWidth() <= 0 || box.getHeight() <= 0) {
            return null;
        }
        return box;
    }

    /**
     * 渲染一页为中间模型指令序列
     * <p>
     * 页面异常（解析失败等）输出空白页并记录 warning（设计约束 3：页面级异常不抛给调用方）。
     *
     * @param page          OFD 页面对象，不能为空
     * @param fontSelector  字体选择器，可为空
     * @return 页面内容；保证 {@code q/Q} 成对
     */
    public PdfPageContent renderPage(Page page, FontSelector fontSelector) {
        // 页面未声明区域时需回退文档级 PageArea，因此这里传入 reader
        PageBox pageBox = pageBox(page, this.reader);
        org.ofdrw.converter.pdf.model.Box media = pageBox.toPdfBox();
        PdfPageContent content = new PdfPageContent(media.getWidth(), media.getHeight());
        if (page == null) {
            warnOnce("page-null", "页面对象为空，输出空白页");
            return content;
        }
        try {
            RenderContext base = RenderContexts.create(pageBox.getX0(), pageBox.getY0(),
                    pageBox.getWidth(), pageBox.getHeight(), content, fontSelector);
            renderPageOrder(page, base, 0);
        } catch (RuntimeException e) {
            logger.warn("[{}] 页面渲染失败，已输出空白页：{}", RenderContext.DIAGNOSTIC_TAG, e.getMessage(), e);
            return new PdfPageContent(media.getWidth(), media.getHeight());
        }
        if (!content.isBalanced()) {
            warnOnce("page-unbalanced", "页面渲染后 q/Q 不成对，已强制恢复：深度=" + content.stateDepth());
            while (content.stateDepth() > 0) {
                content.restoreState();
            }
        }
        return content;
    }

    /**
     * 按 ZOrder 渲染模板页与页面图层
     *
     * @param page    页面，不能为空
     * @param context 渲染上下文
     * @param depth   当前递归深度
     */
    private void renderPageOrder(Page page, RenderContext context, int depth) {
        List<Template> templates = page.getTemplates() == null ? Collections.emptyList() : page.getTemplates();
        List<CT_Layer> layers = page.getContent() == null || page.getContent().getLayers() == null
                ? Collections.emptyList() : page.getContent().getLayers();
        for (Type order : new Type[]{Type.Background, Type.Body, Type.Foreground}) {
            for (Template template : templates) {
                Type zOrder = template.getZOrder() == null ? Type.Body : template.getZOrder();
                if (zOrder == order) {
                    renderTemplate(template, context, depth);
                }
            }
            for (CT_Layer layer : layers) {
                Type type = layer.getType() == null ? Type.Body : layer.getType();
                if (type == order) {
                    renderLayer(layer, context, depth);
                }
            }
        }
    }

    /**
     * 渲染模板页
     * <p>
     * 模板页是独立页面描述：其内容坐标处于模板自身的页坐标系，图元外接矩形用页面的物理页面区域换算，
     * 因此只叠加显式容器上下文（模板的容器 CTM，默认单位矩阵），不嗅探父节点类型（设计 3.2 节）。
     *
     * @param template 模板引用，不能为空
     * @param context  渲染上下文
     * @param depth    当前递归深度
     */
    private void renderTemplate(Template template, RenderContext context, int depth) {
        if (reader == null) {
            warnOnce("template-no-reader", "未配置 OFD 读取器，模板页无法加载，已跳过");
            return;
        }
        String id = GraphicsStateResolver.idOf(template.getTemplateID());
        if (id == null) {
            warnOnce("template-no-id", "模板引用缺少 TemplateID，已跳过");
            return;
        }
        try {
            org.ofdrw.reader.model.TemplatePageEntity entity = reader.getTemplate(id);
            if (entity == null || entity.getPage() == null) {
                warnOnce("template-missing-" + id, "模板页不存在，已跳过：ID=" + id);
                return;
            }
            // 模板页是独立页面描述：其内容坐标以模板页自身的物理页面区域（mm）为基准，
            // 因此进入模板时用模板页的页面框替换容器页面框（否则模板内容会按父页面高度做 Y 翻转而错位）
            PageBox templateBox = pageBox(entity.getPage(), this.reader);
            RenderContext templateContext = RenderContexts.create(templateBox.getX0(), templateBox.getY0(),
                    templateBox.getWidth(), templateBox.getHeight(), context.getPageContent(),
                    context.getFontSelector()).withCtm(context.getCurrentCtm());
            renderPageOrder(entity.getPage(), templateContext, depth + 1);
        } catch (RuntimeException e) {
            warnOnce("template-error-" + id, "模板页渲染失败，已跳过：ID=" + id + "，原因：" + e.getMessage());
        }
    }

    /**
     * 渲染图层
     *
     * @param layer   图层，不能为空
     * @param context 渲染上下文
     * @param depth   当前递归深度
     */
    private void renderLayer(CT_Layer layer, RenderContext context, int depth) {
        List<ContentCommand> block = renderBlock(layer, context, depth);
        if (block.isEmpty()) {
            return;
        }
        // 图层是独立的图形状态单元：整层隔离在 q/Q 之间，保证裁剪/绘制参数不会扩散到其它图层
        PdfPageContent content = context.getPageContent();
        content.saveState();
        content.addAll(block);
        content.restoreState();
    }

    /**
     * 渲染页块（可嵌套）
     *
     * @param block   页块，不能为空
     * @param context 渲染上下文
     * @param depth   当前递归深度
     * @return 指令序列；页块为空时返回空列表
     */
    private List<ContentCommand> renderBlock(PageBlockType block, RenderContext context, int depth) {
        if (depth > MAX_BLOCK_DEPTH) {
            warnOnce("block-depth", "PageBlock 嵌套深度超过 " + MAX_BLOCK_DEPTH + "，已截断");
            return Collections.emptyList();
        }
        List<PageBlockType> children = block instanceof CT_PageBlock
                ? ((CT_PageBlock) block).getPageBlocks()
                : null;
        if (children == null || children.isEmpty()) {
            return Collections.emptyList();
        }
        List<ContentCommand> commands = new ArrayList<>(children.size() * 2);
        for (PageBlockType child : children) {
            commands.addAll(renderUnit(child, context, depth));
        }
        return commands;
    }

    /**
     * 渲染单个图元（分派到专用渲染器）
     *
     * @param unit    图元，不能为空
     * @param context 渲染上下文
     * @param depth   当前递归深度
     * @return 指令序列
     */
    private List<ContentCommand> renderUnit(PageBlockType unit, RenderContext context, int depth) {
        try {
            if (unit instanceof TextObject) {
                if (textRenderer == null) {
                    warnOnce("text-no-renderer", "未配置文字渲染器，文字图元已跳过");
                    return Collections.emptyList();
                }
                return textRenderer.render(unit, context);
            }
            if (unit instanceof PathObject) {
                return pathRenderer.render(unit, context);
            }
            if (unit instanceof ImageObject) {
                return imageRenderer.render(unit, context);
            }
            if (unit instanceof CompositeObject) {
                return renderComposite((CompositeObject) unit, context, depth);
            }
            if (unit instanceof CT_PageBlock) {
                return renderBlock(unit, context, depth + 1);
            }
            warnOnce("unit-unsupported-" + unit.getClass().getSimpleName(),
                    "暂不支持的图元类型，已跳过：" + unit.getClass().getSimpleName());
            return Collections.emptyList();
        } catch (RuntimeException e) {
            warnOnce("unit-error-" + unit.getClass().getSimpleName(),
                    "图元渲染失败，已跳过：" + unit.getClass().getSimpleName() + "，原因：" + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 渲染组合对象（CompositeObject → CT_VectorG）
     * <p>
     * 组合内容与组合对象同坐标系：容器 CTM = 组合自身 CTM × 父容器 CTM，不叠加外接矩形平移（见类注释）。
     *
     * @param composite 组合对象，不能为空
     * @param context   渲染上下文
     * @param depth     当前递归深度
     * @return 指令序列
     */
    private List<ContentCommand> renderComposite(CompositeObject composite, RenderContext context, int depth) {
        if (reader == null) {
            warnOnce("composite-no-reader", "未配置 OFD 读取器，组合对象无法解析，已跳过");
            return Collections.emptyList();
        }
        ST_RefID ref = composite.getResourceID();
        if (ref == null) {
            warnOnce("composite-no-resource", "组合对象缺少资源引用，已跳过");
            return Collections.emptyList();
        }
        String id = GraphicsStateResolver.idOf(ref);
        CT_VectorG vectorG = loadVectorG(id);
        if (vectorG == null) {
            warnOnce("composite-missing-" + id, "组合对象引用的矢量图像不存在，已跳过：ID=" + id);
            return Collections.emptyList();
        }
        org.ofdrw.core.compositeObj.Content content = vectorG.getContent();
        if (content == null || content.getPageBlocks() == null || content.getPageBlocks().isEmpty()) {
            warnOnce("composite-empty-" + id, "组合对象内容为空，已跳过：ID=" + id);
            return Collections.emptyList();
        }
        AffineMatrix ctm = ImageGeometry.elementCtm(composite.getCTM());
        RenderContext inner = context.withCtm(ctm.multiply(context.getCurrentCtm()));
        List<ContentCommand> commands = new ArrayList<>();
        commands.addAll(renderBlock(content, inner, depth + 1));
        return commands;
    }

    /**
     * 加载组合图形单元
     *
     * @param id 资源 ID
     * @return 矢量图像；不存在返回 {@code null}
     */
    private CT_VectorG loadVectorG(String id) {
        if (id == null) {
            return null;
        }
        try {
            if (reader.getResMgt() != null) {
                CT_VectorG unit = reader.getResMgt().getCompositeGraphicUnit(id);
                if (unit != null) {
                    return unit;
                }
            }
        } catch (RuntimeException e) {
            logger.debug("[{}] 资源管理器组合图形查询失败：{}", RenderContext.DIAGNOSTIC_TAG, e.getMessage());
        }
        return loadVectorGFromResources(id);
    }

    /**
     * 直接读取文档级/公共资源中的组合图形单元（资源管理器未索引时的兜底）
     *
     * @param id 资源 ID
     * @return 矢量图像；不存在返回 {@code null}
     */
    private CT_VectorG loadVectorGFromResources(String id) {
        final ResourceLocator rl = reader.getResourceLocator();
        rl.save();
        try {
            org.ofdrw.core.basicStructure.doc.Document document = reader.cdDefaultDoc();
            if (document == null || document.getCommonData() == null) {
                return null;
            }
            ST_Loc docRoot = reader.getOFDDir().getOfd().getDocBody(0).getDocRoot();
            List<ST_Loc> resLocs = new ArrayList<>();
            resLocs.addAll(document.getCommonData().getPublicResList());
            resLocs.addAll(document.getCommonData().getDocumentResList());
            for (ST_Loc loc : resLocs) {
                if (loc == null) {
                    continue;
                }
                org.ofdrw.core.basicStructure.res.Res res =
                        rl.get(docRoot.cat(loc).getLoc(), org.ofdrw.core.basicStructure.res.Res::new);
                if (res == null || res.getResources() == null) {
                    continue;
                }
                for (org.ofdrw.core.basicStructure.res.OFDResource resource : res.getResources()) {
                    if (!(resource instanceof org.ofdrw.core.basicStructure.res.resources.CompositeGraphicUnits)) {
                        continue;
                    }
                    for (CT_VectorG unit : ((org.ofdrw.core.basicStructure.res.resources.CompositeGraphicUnits) resource)
                            .getCompositeGraphicUnits()) {
                        if (unit != null && unit.getID() != null && id.equals(unit.getID().toString())) {
                            return unit;
                        }
                    }
                }
            }
            return null;
        } catch (java.io.IOException | org.dom4j.DocumentException | RuntimeException e) {
            logger.debug("[{}] 组合图形资源读取失败：{}", RenderContext.DIAGNOSTIC_TAG, e.getMessage());
            return null;
        } finally {
            rl.restore();
        }
    }

    /**
     * 解析图元 CTM 数组（供外部复用）
     *
     * @param ctm 图元 CTM，可为空
     * @return 仿射矩阵；为空或操作数不为 6 时返回单位矩阵
     */
    public static AffineMatrix elementCtm(ST_Array ctm) {
        return ImageGeometry.elementCtm(ctm);
    }

    /**
     * 输出一次告警（相同原因只告警一次）
     *
     * @param key     告警分类键
     * @param message 告警内容
     */
    private void warnOnce(String key, String message) {
        if (reported.add(key)) {
            logger.warn("[{}] {}", RenderContext.DIAGNOSTIC_TAG, message);
        }
    }
}
