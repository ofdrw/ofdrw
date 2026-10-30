package org.ofdrw.converter.pdf.render;

import org.ofdrw.converter.pdf.CoordSystem;
import org.ofdrw.converter.pdf.font.FontSelectorImpl;
import org.ofdrw.converter.pdf.font.PdfFontRef;
import org.ofdrw.converter.pdf.font.TrueTypePdfFont;
import org.ofdrw.converter.pdf.model.AffineMatrix;
import org.ofdrw.converter.pdf.model.ContentCommand;
import org.ofdrw.converter.pdf.model.PdfColor;
import org.ofdrw.converter.pdf.model.TextCommand;
import org.ofdrw.converter.pdf.render.text.GlyphMapper;
import org.ofdrw.converter.pdf.render.text.OfdBox;
import org.ofdrw.converter.pdf.render.text.OfdText;
import org.ofdrw.converter.pdf.render.text.OfdTextAdapter;
import org.ofdrw.converter.pdf.render.text.TextLayout;
import org.ofdrw.converter.pdf.render.text.TextLayoutEngine;
import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 文字渲染器实现（P2）：OFD TextObject → 中间模型 {@link TextCommand}
 * <p>
 * 处理内容（设计 3.6 节、决策 3）：
 * <ul>
 *     <li>X/Y 定位与 DeltaX/DeltaY 双队列（含末位尾部增量）——见 {@link TextLayoutEngine}；</li>
 *     <li>CharDirection × ReadDirection 组合（表 47）——阅读方向决定 Delta 的推进方向，字符方向决定字形朝向；</li>
 *     <li>CGTransform 一对一/多对一/一对多/多对多——字形索引优先采用显式 GID；</li>
 *     <li>字形优先 + ToUnicode：每个写入的字形登记 {@code 字形 → Unicode}，保证文本可提取；</li>
 *     <li>整字体嵌入与替换兜底：字体来自 {@link FontSelectorImpl}，替换时输出 warning（决策 16、7）。</li>
 * </ul>
 * 容错（设计约束 3）：单图元异常一律跳过并输出 warning，不中断整页。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class TextRendererImpl implements TextRenderer {

    private static final Logger logger = LoggerFactory.getLogger(TextRendererImpl.class);

    /**
     * 颜色解析器（P3）：把文字填充色按颜色空间解析，CMYK 不转 RGB
     */
    private final ColorResolver colorResolver;

    /**
     * 绘制参数解析器（P3）：提供 DrawParam 链上的 Alpha（就近原则）
     */
    private final GraphicsStateResolver stateResolver;

    /**
     * 已输出的字形替换告警（同一原因只报一次）
     */
    private final java.util.Set<String> reportedGlyphWarnings = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * 文字布局计算
     */
    private final TextLayoutEngine layoutEngine = new TextLayoutEngine();

    /**
     * 字形映射
     */
    private final GlyphMapper glyphMapper = new GlyphMapper();

    /**
     * 构造文字渲染器（不解析 DrawParam 链，仅使用图元自身的颜色与 Alpha）
     */
    public TextRendererImpl() {
        this(null, null, null);
    }

    /**
     * 构造文字渲染器
     *
     * @param colorResolver     颜色解析器，可为空（空时按通道数量推断颜色空间，无法解析调色板）
     * @param stateResolver     绘制参数解析器，可为空（空时只看图元自身的 Alpha）
     * @param colorSpaceLookup  颜色空间查找函数，可为空
     */
    public TextRendererImpl(ColorResolver colorResolver, GraphicsStateResolver stateResolver,
                            ColorResolver.ColorSpaceLookup colorSpaceLookup) {
        this.colorResolver = colorResolver != null ? colorResolver : new ColorResolver(
                colorSpaceLookup == null ? null : colorSpaceLookup::getColorSpace);
        this.stateResolver = stateResolver;
    }

    @Override
    public String getName() {
        return "TextRenderer";
    }

    @Override
    public List<ContentCommand> render(Object input, RenderContext context) {
        if (!(input instanceof TextObject)) {
            logger.warn("{} 收到非文字对象，已跳过：{}", getName(), input == null ? "null" : input.getClass().getName());
            return Collections.emptyList();
        }
        if (context == null) {
            logger.warn("{} 缺少渲染上下文，已跳过", getName());
            return Collections.emptyList();
        }
        TextObject textObject = (TextObject) input;
        try {
            return renderText(textObject, context);
        } catch (RuntimeException e) {
            logger.warn("文字图元渲染失败，已跳过：" + textObject.getObjID(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 渲染单个文字对象
     *
     * @param textObject 文字对象
     * @param context    渲染上下文
     * @return 指令序列
     */
    private List<ContentCommand> renderText(TextObject textObject, RenderContext context) {
        OfdText text = OfdTextAdapter.adapt(textObject);
        if (text.getTextCodes().isEmpty()) {
            return Collections.emptyList();
        }
        // 1) 字体选择（内嵌优先，替换兜底）
        TrueTypePdfFont font = selectFont(textObject, context);
        if (font == null) {
            logger.warn("文字图元 {} 没有可用字体，已跳过", textObject.getObjID());
            return Collections.emptyList();
        }
        if (font.isSubstituted()) {
            logger.warn("文字图元 {} 使用的字体 {} 已替换为 {}", textObject.getObjID(),
                    text.getBoundary(), font.getFamilyName());
        }

        // 2) 布局（Delta 双队列、CGTransform、方向）
        /*
         * 字号必须乘图元 CTM 的线性缩放：CTM 只作用于字形偏移（TextCode 的 X/Y 与 Delta），
         * 不作用于外接矩形锚点（实测 testImageNotFound：位置 x 与 Boundary.x 完全相等），
         * 因此字形的紧凑程度由“字号 × 缩放”决定。Size=1mm、CTM 缩放 9.1722 时 ≈ 26pt，与基准一致。
         */
        double fontSizePt = CoordSystem.mmToPt(text.getSizeMm()) * elementScale(text);
        double nominalAdvancePt = font.nominalAdvancePt(fontSizePt) * text.getHScale();
        TextLayout layout = layoutEngine.layout(text, CoordSystem.ptToMm(nominalAdvancePt));
        // 图元 CTM 只作用于字形偏移（TextCode 的 X/Y 与 Delta），锚点保持外接矩形角点不变
        TextLayout scaled = elementPositions(text, layout);
        for (String warning : layout.getWarnings()) {
            logger.debug("文字图元 {}：{}", textObject.getObjID(), warning);
        }
        if (scaled.isEmpty()) {
            return Collections.emptyList();
        }

        /*
         * 3) 容器 CTM → 页面坐标（决策 1 逐点换算，Y 翻转让 CoordSystem 完成）。
         * 图元 CTM 的乘算点见第 2 步的 elementPositions：它只作用在字形偏移上（锚点不缩放），
         * 因此这里只叠加上层容器 CTM 与页面框偏移，避免同一份变换被乘两次。
         * 该矩阵输出的是页面 pt 坐标，字形位置直接取它的变换结果（见 GlyphMapper）。
         * 页面框偏移 pageX0/pageY0 与路径、图像、注释保持同一口径（决策 26）。
         */
        AffineMatrix base = CoordSystem.absoluteToPage(context.getCurrentCtm(),
                context.getPageX0Mm(), context.getPageY0Mm(), context.getPageHeightMm());

        // 4) 颜色（决策 2/3.4）：颜色空间保持原样（CMYK 不转 RGB），
        //    颜色自身的 Alpha × 图元 Alpha（图元显式优先，其次 DrawParam 链，缺省不透明）
        Integer unitAlpha = effectiveAlpha(textObject);
        PdfColor color = textColor(textObject, unitAlpha);
        // 颜色自身的 Alpha 已在 PdfColor.opacity 中，与图元 Alpha 相乘后的最终不透明度
        float alpha = color.getOpacity();

        // 5) 字形映射
        List<String> warnings = new ArrayList<>();
        TextCommand command = glyphMapper.map(text, scaled, font, fontSizePt,
                base, color, warnings);
        if (command == null) {
            return Collections.emptyList();
        }
        for (String warning : warnings) {
            // 字形替换必须告警（决策 7/8）：同页同字体同原因只报一次，避免整页刷屏
            if (reportedGlyphWarnings.add(warning)) {
                logger.warn("[{}] 文字图元 {}：{}", RenderContext.DIAGNOSTIC_TAG, textObject.getObjID(), warning);
            }
        }
        /*
         * 5) 不透明度：只有颜色或图元显式声明了 Alpha 才输出 SetAlpha（决策 2：缺省不透明，
         *    但“未声明”与“显式 255”在语义上不同，未声明时不发指令，避免掩盖真正的缺失）。
         *    包装固定为 q + [SetAlpha] + TextCommand + Q，保证图形状态成对（设计约束 1）。
         */
        boolean alphaExplicit = textObject.getAlphaExplicit() != null
                || (textObject.getFillColor() != null && textObject.getFillColor().getAlphaExplicit() != null);
        List<ContentCommand> commands = new ArrayList<>(4);
        commands.add(org.ofdrw.converter.pdf.model.SaveState.INSTANCE);
        if (alphaExplicit) {
            commands.add(org.ofdrw.converter.pdf.model.SetAlpha.of(alpha));
        }
        commands.add(command);
        commands.add(org.ofdrw.converter.pdf.model.RestoreState.INSTANCE);
        return commands;
    }

    /**
     * 按图元 CTM 缩放字形偏移（锚点保持外接矩形角点不变）
     * <p>
     * GB/T 33190 的图元 CTM 作用于对象**偏移量**（TextCode 的 X/Y、DeltaX/DeltaY），
     * 文字流的起点仍是外接矩形角点。实测 testImageNotFound 可证：基准中文字 x 与 Boundary.x
     * 完全相等（206.9258pt vs 73mm），而字形间距与字号按 CTM 缩放（Size=1mm、缩放 9.1722 → 26pt）。
     * 因此这里只把“相对角点的偏移”经 CTM 线性部分缩放，再把角点加回去。
     *
     * @param text   文字图元视图（提供外接矩形与图元 CTM）
     * @param layout 对象空间布局结果
     * @return 缩放后的布局（无 CTM 时原样返回）
     */
    private static TextLayout elementPositions(OfdText text, TextLayout layout) {
        if (text.getCtm() == null || layout.isEmpty()) {
            return layout;
        }
        AffineMatrix ctm = CoordSystem.fromCtm(text.getCtm());
        OfdBox box = text.getBoundary();
        double bx = box == null ? 0 : box.getX();
        double by = box == null ? 0 : box.getY();
        List<TextLayout.PlacedGlyph> glyphs = new ArrayList<>(layout.getGlyphs().size());
        for (TextLayout.PlacedGlyph g : layout.getGlyphs()) {
            double[] moved = ctm.transform(g.getX() - bx, g.getY() - by);
            glyphs.add(new TextLayout.PlacedGlyph(g.getText(), g.getCodePoints(), g.getExplicitGlyphId(),
                    bx + moved[0], by + moved[1], g.getAdvanceMm(), g.isShared()));
        }
        return new TextLayout(glyphs, layout.getWarnings());
    }

    /**
     * 计算图元 CTM 的等效线性缩放
     * <p>
     * 取 CTM 线性部分的两个轴向量长度均值：纯缩放时即缩放倍数，旋转（正交阵）时为 1，
     * 斜切等非均匀情况取平均作为近似，与实际字形位置的缩放保持一致。
     *
     * @param text 文字图元视图
     * @return 缩放倍数（无量纲）；无 CTM 时为 1
     */
    private static double elementScale(OfdText text) {
        if (text.getCtm() == null) {
            return 1d;
        }
        AffineMatrix ctm = CoordSystem.fromCtm(text.getCtm());
        double sx = Math.hypot(ctm.getA(), ctm.getB());
        double sy = Math.hypot(ctm.getC(), ctm.getD());
        double scale = (sx + sy) / 2d;
        return scale <= 0 ? 1d : scale;
    }

    /**
     * 选择字体
     *
     * @param textObject 文字对象
     * @param context    渲染上下文
     * @return 字体引用；无法确定时返回 null
     */
    private TrueTypePdfFont selectFont(TextObject textObject, RenderContext context) {
        if (textObject.getFont() == null) {
            logger.warn("文字图元 {} 缺少 Font 属性，已跳过", textObject.getObjID());
            return null;
        }
        if (!(context.getFontSelector() instanceof FontSelectorImpl)) {
            logger.warn("渲染上下文没有可用的字体选择器，文字图元 {} 已跳过", textObject.getObjID());
            return null;
        }
        FontSelectorImpl selector = (FontSelectorImpl) context.getFontSelector();
        // Font 属性为资源 ID：先解析为字体资源（资源管理器由上层注入），无法解析时交由选择器降级为替代字体
        String resourceId = textObject.getFont().toString();
        Object resource = selector.resolveFont(resourceId);
        PdfFontRef ref = selector.select(Collections.singletonList(resource == null ? resourceId : resource), context);
        return ref instanceof TrueTypePdfFont ? (TrueTypePdfFont) ref : null;
    }

    /**
     * 计算文字的有效 Alpha
     * <p>
     * 就近原则：图元显式 {@code Alpha}（{@link TextObject#getAlphaExplicit()}）优先，
     * 其次 DrawParam 与 {@code Relative} 链上的 Alpha，最后是规范默认值 255（决策 2：缺省不透明）。
     *
     * @param textObject 文字对象
     * @return Alpha，取值 0~255；无法确定时返回 255
     */
    private Integer effectiveAlpha(TextObject textObject) {
        Integer explicit = textObject.getAlphaExplicit();
        if (stateResolver == null) {
            return explicit == null ? 255 : explicit;
        }
        EffectiveDrawParam params = stateResolver.applyUnitAttributes(
                stateResolver.resolve(textObject.getDrawParam()),
                textObject.getLineWidth(), textObject.getCapExplicit(), textObject.getJoinExplicit(),
                textObject.getMiterLimit(), textObject.getDashPattern(), explicit);
        return params.getAlpha();
    }

    /**
     * 计算文字填充颜色
     * <p>
     * 文字只有填充色（表 45，默认黑色）；颜色空间按 {@code ColorSpace} 引用解析，
     * CMYK 保持原颜色空间（设计 3.4）。颜色自身的 Alpha 写入 {@link PdfColor#getOpacity()}，
     * 与图元 Alpha 的乘积由 {@code SetAlpha} 统一表达。
     * <p>
     * 就近原则（GB/T 33190 8.2）：图元未直接给出 {@code FillColor} 时沿用其 {@code DrawParam} 的填充色。
     * 水印/注释外观常以 {@code DrawParam} 提供浅灰颜色，漏掉这一层会让文字退化为不透明纯黑。
     *
     * @param textObject 文字对象
     * @param unitAlpha  图元 Alpha，可为空
     * @return 文字颜色；颜色缺失时返回黑色
     */
    private PdfColor textColor(TextObject textObject, Integer unitAlpha) {
        org.ofdrw.core.pageDescription.color.color.CT_Color fillColor = textObject.getFillColor();
        if (fillColor == null && stateResolver != null) {
            fillColor = drawParamFillColor(textObject.getDrawParam());
        }
        // 该重载内部完成「颜色自身 Alpha × 图元 Alpha」（决策 2）
        PdfColor color = colorResolver.resolve(fillColor, unitAlpha, "文字");
        if (color != null) {
            return color;
        }
        // 颜色缺失按黑色处理，但图元 Alpha 仍须生效（水印常以 Alpha 表达半透明，不能因缺色而变成不透明）
        return PdfColor.BLACK.withOpacity(ColorResolver.opacity(unitAlpha));
    }

    /**
     * 沿 {@code Relative} 链取绘制参数的填充色
     * <p>
     * 就近原则：更近一层的参数中<b>已定义</b>的填充色优先，未定义才向上继承。
     *
     * @param ref 图元引用的绘制参数，可为空
     * @return 填充色；链上没有定义时返回 {@code null}
     */
    private org.ofdrw.core.pageDescription.color.color.CT_Color drawParamFillColor(
            org.ofdrw.core.basicType.ST_RefID ref) {
        org.ofdrw.core.basicType.ST_RefID current = ref;
        for (int depth = 0; depth < 8 && current != null; depth++) {
            org.ofdrw.core.pageDescription.drawParam.CT_DrawParam param =
                    stateResolver.getDrawParam(current.toString());
            if (param == null) {
                return null;
            }
            if (param.getFillColor() != null) {
                return param.getFillColor();
            }
            current = param.getRelative();
        }
        return null;
    }

    /**
     * 取图元 CTM 的等效缩放系数
     * <p>
     * 等比例缩放时取 {@code hypot(a, b)}（即 x 轴的模长）；未声明 CTM 时返回 1。
     * 非等比缩放取 x 轴模长作为近似，与 PDF 文本矩阵只能承载单一字号的事实一致。
     *
     * @param ctm 图元 CTM，可为空
     * @return 等效缩放系数，未声明 CTM 时为 1
     */
    private static double ctmScale(double[] ctm) {
        if (ctm == null || ctm.length < 4) {
            return 1d;
        }
        double scale = Math.hypot(ctm[0], ctm[1]);
        return scale > 0 && !Double.isNaN(scale) && !Double.isInfinite(scale) ? scale : 1d;
    }
}
