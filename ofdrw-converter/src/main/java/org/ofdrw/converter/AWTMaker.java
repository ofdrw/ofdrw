package org.ofdrw.converter;

import org.apache.commons.lang3.math.NumberUtils;
import org.ofdrw.converter.canvas.CanvasImageMaker;
import org.ofdrw.converter.font.FontWrapper;
import org.ofdrw.converter.font.GlyphData;
import org.ofdrw.converter.font.TrueTypeFont;
import org.ofdrw.converter.point.Tuple2;
import org.ofdrw.converter.renderer.BlendMode;
import org.ofdrw.converter.renderer.CanvasPaint;
import org.ofdrw.converter.renderer.DrawStyle;
import org.ofdrw.converter.renderer.FillRule;
import org.ofdrw.converter.renderer.Graphics2DRenderer;
import org.ofdrw.converter.renderer.ImageHandle;
import org.ofdrw.converter.renderer.Renderer;
import org.ofdrw.converter.renderer.ResolvedPaint;
import org.ofdrw.converter.style.DrawParamResolver;
import org.ofdrw.converter.style.GraphicUnitType;
import org.ofdrw.converter.style.LineCapStyle;
import org.ofdrw.converter.style.LineJoinStyle;
import org.ofdrw.converter.style.ResolvedStyle;
import org.ofdrw.converter.style.StyleResolver;
import org.ofdrw.converter.style.StyleScope;
import org.ofdrw.converter.style.StyleScopeKind;
import org.ofdrw.converter.utils.CommonUtil;
import org.ofdrw.converter.utils.MatrixUtils;
import org.ofdrw.converter.utils.StringUtils;
import org.ofdrw.core.annotation.pageannot.Annot;
import org.ofdrw.core.annotation.pageannot.Appearance;
import org.ofdrw.core.basicStructure.pageObj.Content;
import org.ofdrw.core.basicStructure.pageObj.layer.CT_Layer;
import org.ofdrw.core.basicStructure.pageObj.layer.PageBlockType;
import org.ofdrw.core.basicStructure.pageObj.layer.block.*;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.compositeObj.CT_VectorG;
import org.ofdrw.core.graph.pathObj.AbbreviatedData;
import org.ofdrw.core.graph.pathObj.OptVal;
import org.ofdrw.core.pageDescription.CT_GraphicUnit;
import org.ofdrw.core.pageDescription.color.color.CT_Color;
import org.ofdrw.core.pageDescription.color.colorSpace.CT_ColorSpace;
import org.ofdrw.core.pageDescription.color.colorSpace.OFDColorSpaceType;
import org.ofdrw.core.pageDescription.drawParam.CT_DrawParam;
import org.ofdrw.core.signatures.appearance.StampAnnot;
import org.ofdrw.core.text.CT_CGTransform;
import org.ofdrw.core.text.TextCode;
import org.ofdrw.core.text.font.CT_Font;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.PageInfo;
import org.ofdrw.reader.ResourceManage;
import org.ofdrw.reader.model.AnnotionEntity;
import org.ofdrw.reader.model.StampAnnotEntity;
import org.ofdrw.reader.tools.ImageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ujmp.core.Matrix;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.*;

/**
 * AWT设备转换类
 *
 * @author qaqtutu
 * @since 2021-05-06 23:00:01
 */
public abstract class AWTMaker {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    private OFDReader reader;

    /**
     * 每毫米像素数量(Pixels per millimeter)
     * <p>
     * 默认为： 7.874015748031496 ppm (约200 dpi）
     */
    protected double ppm = CommonUtil.dpiToPpm(200);

    public final Config config = new Config();

    /**
     * 是否是印章，用于渲染印章
     */
    protected boolean isStamp = false;


    protected List<PageInfo> pages;

    private final ResourceManage resourceManage;
    /**
     * 加载后的字体缓存
     * <p>
     * 防止重复加载文件读写和解析带来耗时
     * <p>
     * KEY: 字族名_字体名_字体路径
     */
    private final Map<String, FontWrapper<TrueTypeFont>> fontCache = new HashMap<>();

    /**
     * 样式结算器：把作用域链与图元内联属性结算为最终绘制样式
     */
    private final StyleResolver styleResolver;

    /**
     * 创建图片转换对象实例
     * <p>
     * OFD内部使用毫米作为基本单位
     * <p>
     * 如果需要更加精确的表示单位请使用 {@link #AWTMaker(org.ofdrw.reader.OFDReader, double)}
     *
     * @param reader OFD解析器
     * @param ppm    每毫米像素数量(Pixels per millimeter)
     */
    public AWTMaker(OFDReader reader, int ppm) {
        this.reader = reader;
        this.resourceManage = reader.getResMgt();
        this.pages = reader.getPageList();
        this.styleResolver = createStyleResolver();
        if (this.ppm > 0) {
            this.ppm = ppm;
        }
    }

    /**
     * 创建图片转换对象实例
     * <p>
     * OFD内部使用毫米作为基本单位
     *
     * @param reader OFD解析器
     * @param ppm    每毫米像素数量(Pixels per millimeter)，DPI与PPM转换可以使用{@link CommonUtil#dpiToPpm(int)}。
     * @author iandjava
     */
    public AWTMaker(OFDReader reader, double ppm) {
        this.reader = reader;
        this.resourceManage = reader.getResMgt();
        this.pages = reader.getPageList();
        this.styleResolver = createStyleResolver();
        if (this.ppm > 0) {
            this.ppm = ppm;
        }
    }

    /**
     * 创建样式结算器。
     * <p>
     * 绘制参数从当前文档的资源管理器查找，颜色解析复用 {@link #getColor(CT_Color)}。
     *
     * @return 样式结算器
     */
    private StyleResolver createStyleResolver() {
        DrawParamResolver.DrawParamLookup lookup = new DrawParamResolver.DrawParamLookup() {
            @Override
            public CT_DrawParam getDrawParam(String id) {
                return resourceManage.getDrawParam(id);
            }

            @Override
            public Color toColor(CT_Color color) {
                return getColor(color);
            }
        };
        return new StyleResolver(new DrawParamResolver(lookup), lookup);
    }

    public int pageSize() {
        return pages.size();
    }

    /**
     * 绘制印章注释。
     * <p>
     * 印章图片按正片叠底（{@link BlendMode#MULTIPLY}）合成：离屏后端（{@link Graphics2DRenderer} 绑定位图）下逐像素混合，
     * 外部 Graphics2D 后端（SVG/PDF）退化为普通覆盖并告警，与历史行为差异仅限混合算法本身。
     * 印章边界裁剪使用求交语义，与前序已生效的裁剪区域叠加。
     *
     * @param renderer      目标渲染器
     * @param stampAnnotVo  印章注释实体
     * @param stampAnnot    印章注释
     */
    private void writeStampAnnot(Renderer renderer, StampAnnotEntity stampAnnotVo, StampAnnot stampAnnot) {
        BufferedImage stampImage = null;
        renderer.save();
        try (ByteArrayInputStream inputStream = stampAnnotVo.getImageStream()) {
            if (stampAnnotVo.getImgType().equals("ofd")) {
                AWTMaker imageMaker = createStampMaker(new OFDReader(inputStream), ppm);
                imageMaker.isStamp = true;
                imageMaker.config.setDrawBoundary(config.drawBoundary);
                if (imageMaker.pageSize() > 0) {
                    logger.debug("渲染ofd格式印章");
                    stampImage = makeStampPage(imageMaker, 0);
                }
            } else {
                stampImage = ImageIO.read(inputStream);
            }
            if (stampImage != null) {
                if (config.clearStampBackground) {
                    stampImage = ImageUtils.clearWhiteBackground(stampImage, config.stampBackgroundGray);
                }

                ST_Box stBox = stampAnnot.getBoundary();
                ST_Box clip = stampAnnot.getClip();
                Matrix m = MatrixUtils.base();
                renderer.setTransform(MatrixUtils.createAffineTransform(m));

                // 调整比例
                final double fx = stBox.getWidth() / stampImage.getWidth();
                final double fy = stBox.getHeight() / stampImage.getHeight();
                m = MatrixUtils.scale(m, fx, fy);
                // 首先平移到指定位置
                m = MatrixUtils.move(m, stBox.getTopLeftX(), stBox.getTopLeftY());
                // 缩放适应
                m = MatrixUtils.scale(m, ppm, ppm);

                if (clip != null) {
                    Matrix m1 = MatrixUtils.base();
                    m1 = MatrixUtils.scale(m1, fx, fy);
                    m1 = MatrixUtils.move(m1, stBox.getTopLeftX() + clip.getTopLeftX(), stBox.getTopLeftY() + clip.getTopLeftY());
                    m1 = MatrixUtils.scale(m1, ppm, ppm);
                    // 印章裁剪区域同样以像素为单位，与既有裁剪区域求交
                    renderer.clip(new java.awt.geom.Rectangle2D.Double(
                            m1.getAsDouble(2, 0), m1.getAsDouble(2, 1),
                            stampImage.getWidth() * m1.getAsDouble(0, 0) * (clip.getWidth() / stBox.getWidth()),
                            stampImage.getHeight() * m1.getAsDouble(1, 1) * (clip.getHeight() / stBox.getHeight())),
                            FillRule.NON_ZERO);
                }
                renderer.setTransform(MatrixUtils.createAffineTransform(MatrixUtils.base()));
                ImageHandle handle = renderer.createImage(stampImage);
                renderer.drawImage(handle, MatrixUtils.createAffineTransform(m), 1f, true, null, BlendMode.MULTIPLY);
            }
        } catch (Exception e) {
            logger.error("印章绘制异常", e);
        } finally {
            renderer.restore();
        }
    }

    /**
     * 创建用于渲染嵌套 OFD 印章的绘制器。
     * <p>
     * 默认返回 {@link CanvasImageMaker}，印章统一由画布设备层渲染；子类可以覆盖该方法以替换印章渲染的设备层。
     *
     * @param nestedReader 印章 OFD 解析器
     * @param ppm          每毫米像素数量，单位：像素/毫米
     * @return 印章绘制器
     */
    protected AWTMaker createStampMaker(OFDReader nestedReader, double ppm) {
        return new CanvasImageMaker(nestedReader, ppm);
    }

    /**
     * 将印章绘制器的指定页面渲染为位图，供正片叠底合成使用。
     * <p>
     * 支持 {@link CanvasImageMaker}（默认印章绘制器）与 {@link ImageMaker} 两种绘制器；
     * 其它实现返回 null，子类可以覆盖该方法以支持自定义绘制器。
     *
     * @param maker     印章绘制器
     * @param pageIndex 页码，从 0 起
     * @return 位图，或 null 表示无法渲染
     */
    protected BufferedImage makeStampPage(AWTMaker maker, int pageIndex) {
        if (maker instanceof CanvasImageMaker) {
            return ((CanvasImageMaker) maker).makePage(pageIndex);
        }
        if (maker instanceof ImageMaker) {
            return ((ImageMaker) maker).makePage(pageIndex);
        }
        return null;
    }

//    /**
//     * 印章混合模式
//     *
//     * @return 正片叠底复合对象
//     */
//    protected Composite getStampComposite() {
//        // 正片叠底
//        return BlendComposite.getInstance(BlendMode.MULTIPLY, 1);
//    }

    /**
     * 创建绘制指定页面块的渲染器入口。
     * <p>
     * 供需要在文档资源之外提供样式结算器的场景复用同一套绘制逻辑（例如测试或外部样式注入）。
     * 调用方需先对渲染器执行 {@link Renderer#beginPage(int, int)}；本方法不负责页面生命周期。
     *
     * @param renderer      目标渲染器
     * @param pageBlock     页面块（页面内容、图层或注释外观）
     * @param styleResolver 样式结算器
     */
    protected void writePageBlock(Renderer renderer, CT_PageBlock pageBlock, StyleResolver styleResolver) {
        writeContent(renderer, pageBlock, null, null, styleResolver);
    }

    /**
     * 创建不绑定文档的样式结算器。
     * <p>
     * 用于在文档资源之外提供绘制参数与颜色解析（例如测试或外部样式注入），
     * 不依赖 {@link OFDReader}，也不影响当前转换器自身使用的结算器。
     *
     * @param lookup 绘制参数与颜色查找器
     * @return 样式结算器
     */
    public static StyleResolver newStyleResolver(DrawParamResolver.DrawParamLookup lookup) {
        return new StyleResolver(new DrawParamResolver(lookup), lookup);
    }

    /**
     * 使用指定样式结算器绘制页面内容（按 ZOrder 逐层）。
     * <p>
     * 与 {@link #writePage} 的图层遍历顺序一致，作用域起点均为文档根。
     *
     * @param renderer      目标渲染器
     * @param content       页面内容
     * @param styleResolver 样式结算器
     */
    protected void writeContent(Renderer renderer, Content content, StyleResolver styleResolver) {
        for (CT_Layer layer : content.getLayers()) {
            writeContent(renderer, layer, null, null, styleResolver);
        }
    }

    /**
     * 使用历史签名绘制页面：内部包装为 {@link Graphics2DRenderer}，行为与 {@link #writePage(Renderer, PageInfo, Matrix)} 一致。
     * <p>
     * 保留该重载是为了兼容既有子类（如 {@code SVGMaker}、{@code CanvasImageMaker}）与外部扩展，
     * 新代码建议直接传入 {@link Renderer}。
     *
     * @param graphics 图形操作上下文
     * @param pageInfo 页面信息
     * @param matrix   变换矩阵，可为 null
     */
    protected void writePage(Graphics2D graphics, PageInfo pageInfo, Matrix matrix) {
        if (graphics == null) {
            throw new IllegalArgumentException("绘制上下文不能为空");
        }
        Graphics2DRenderer renderer = new Graphics2DRenderer(graphics);
        writePage(renderer, pageInfo, matrix);
    }

    /**
     * 绘制页面
     * <p>
     * 渲染器生命周期由调用方管理（{@link Renderer#beginPage(int, int)} 需在调用前完成）；
     * 页面坐标单位为像素，内部完成毫米到像素的换算。
     *
     * @param renderer 目标渲染器
     * @param pageInfo 页面信息
     * @param matrix   变换矩阵，可为 null
     */
    protected void writePage(Renderer renderer, PageInfo pageInfo, Matrix matrix) {
        // 获取页面内容出现的所有图层，包含模板页（所有页面均按照定义ZOrder排列）
        final List<CT_Layer> layerList = pageInfo.getAllLayer();
        for (CT_Layer layer : layerList) {
            writeContent(renderer, layer, null, matrix, styleResolver);
        }

        final String pageId = pageInfo.getId().toString();
        // 绘制电子印章图片
        for (StampAnnotEntity stampAnnotEntity : reader.getStampAnnots()) {
            List<StampAnnot> stampAnnots = stampAnnotEntity.getStampAnnots();
            for (StampAnnot stampAnnot : stampAnnots) {
                if (stampAnnot.getPageRef().toString().equals(pageId)) {
                    writeStampAnnot(renderer, stampAnnotEntity, stampAnnot);
                }
            }
        }

        // 绘制注解对象
        for (AnnotionEntity annotionEntity : reader.getAnnotationEntities()) {
            if (pageId.equals(annotionEntity.getPageId()) && null != annotionEntity.getAnnots()) {
                for (Annot annot : annotionEntity.getAnnots()) {
                    Appearance appearance = annot.getAppearance();
                    // 注释外观是独立绘制空间，以注释作用域为继承起点
                    writeContent(renderer, appearance,
                            StyleScope.document().child(StyleScopeKind.ANNOTATION, null, null), null, styleResolver);
                }
            }
        }
    }

    /**
     * 绘制页面块内容。
     *
     * @param renderer     目标渲染器
     * @param pageBlock    页面块
     * @param scope        父样式作用域，null 表示文档根
     * @param parentMatrix 父级变换矩阵，null 表示单位矩阵
     * @param resolver     样式结算器
     */
    private void writeContent(Renderer renderer, CT_PageBlock pageBlock, StyleScope scope, Matrix parentMatrix,
                              StyleResolver resolver) {
        if (pageBlock == null) {
            return;
        }
        renderer.save();
        try {
            // 作用域为文档根，每个块内部再按自身类型追加作用域
            if (scope == null) scope = StyleScope.document();
            if (parentMatrix == null) parentMatrix = MatrixUtils.base();

            // 块自身的绘制参数作为本块作用域，块内图元与子块共用该作用域，不会修改父级
            if (pageBlock instanceof CT_Layer) {
                ST_RefID drawParamRef = ((CT_Layer) pageBlock).getDrawParam();
                scope = scope.child(StyleScopeKind.LAYER, drawParamRef, null);
            }

            if (pageBlock.attribute("Boundary") != null) {
                String data = (String) pageBlock.attribute("Boundary").getData();
                ST_Box stBox = ST_Box.getInstance(data);
                parentMatrix = MatrixUtils.move(parentMatrix, stBox.getTopLeftX(), stBox.getTopLeftY());
            }

            for (PageBlockType object : pageBlock.getPageBlocks()) {
                try {
                    if (object instanceof TextObject) {
                        TextObject textObject = (TextObject) object;
                        writeText(renderer, textObject, objectScope(scope, textObject, resolver), parentMatrix, resolver);
                    } else if (object instanceof ImageObject) {
                        ImageObject imageObject = (ImageObject) object;
                        writeImage(renderer, imageObject, objectScope(scope, imageObject, resolver), parentMatrix, resolver);
                    } else if (object instanceof PathObject) {
                        PathObject pathObject = (PathObject) object;
                        writePath(renderer, pathObject, objectScope(scope, pathObject, resolver), parentMatrix, resolver);
                    } else if (object instanceof CompositeObject) {
                        CompositeObject compositeObject = (CompositeObject) object;
                        writeComposite(renderer, compositeObject, objectScope(scope, compositeObject, resolver), parentMatrix, resolver);
                    } else if (object instanceof CT_PageBlock) {
                        // 子块（非图层）：以页块作用域递归，不再追加绘制参数
                        CT_PageBlock block = (CT_PageBlock) object;
                        writeContent(renderer, block, scope.child(StyleScopeKind.PAGEBLOCK, null, null), parentMatrix, resolver);
                    } else if (object instanceof CT_Layer) {
                        CT_Layer layer = (CT_Layer) object;
                        writeContent(renderer, layer, scope, parentMatrix, resolver);
                    }
                } catch (Exception e) {
                    logger.warn("PageBlock无法渲染:", e);
                }
            }
        } finally {
            renderer.restore();
        }
    }

    /**
     * 对象作用域内联线型的中性起点：不含颜色、开关与透明度，只有线型才会被内联覆盖。
     */
    private static final ResolvedStyle INLINE_STYLE_BASE = new ResolvedStyle(
            StyleResolver.DEFAULT_LINE_WIDTH, LineCapStyle.BUTT, LineJoinStyle.MITER,
            StyleResolver.DEFAULT_MITER_LIMIT, 0, new double[0], null, null, false, false, 1);

    /**
     * 为图元追加对象作用域。
     * <p>
     * 作用域同时携带图元自身的绘制参数引用与内联线型（线宽、线帽、线接、尖角截断、虚线），
     * 内联属性在结算时优先级高于绘制参数与继承值。
     *
     * @param scope 父作用域
     * @param unit  图元
     * @return 图元对象作用域（新实例）
     */
    private StyleScope objectScope(StyleScope scope, CT_GraphicUnit<?> unit, StyleResolver resolver) {
        ResolvedStyle inline = null;
        if (hasInlineStroke(unit)) {
            inline = INLINE_STYLE_BASE.derive(
                    unit.attributeValue("LineWidth") == null ? null : unit.getLineWidth(),
                    capOf(unit.attributeValue("Cap") == null ? null : unit.getCap()),
                    joinOf(unit.attributeValue("Join") == null ? null : unit.getJoin()),
                    unit.attributeValue("MiterLimit") == null ? null : unit.getMiterLimit(),
                    unit.attributeValue("DashOffset") == null ? null : unit.getDashOffset(),
                    parseDashPattern(unit.getDashPattern()),
                    null, null, null, null, null);
        }
        // 图元自身的绘制参数作为对象作用域的引用，参与就近结算
        return scope.child(StyleScopeKind.OBJECT, unit.getDrawParam(), inline);
    }

    /**
     * 判断图元是否设置了内联线型属性。
     *
     * @param unit 图元
     * @return true 表示至少设置了一项线型属性
     */
    private boolean hasInlineStroke(CT_GraphicUnit<?> unit) {
        return unit.attributeValue("LineWidth") != null
                || unit.attributeValue("Cap") != null
                || unit.attributeValue("Join") != null
                || unit.attributeValue("MiterLimit") != null
                || unit.attributeValue("DashOffset") != null
                || unit.getDashPattern() != null;
    }

    /**
     * 解析 OFD 虚线数组。
     *
     * @param array 虚线数组属性
     * @return 虚线数组，单位：毫米（mm）；null 表示未设置或无法解析
     */
    private double[] parseDashPattern(ST_Array array) {
        if (array == null) {
            return null;
        }
        try {
            List<String> items = array.getArray();
            double[] pattern = new double[items.size()];
            for (int i = 0; i < items.size(); i++) {
                pattern[i] = Double.parseDouble(items.get(i).trim());
            }
            return pattern;
        } catch (Exception e) {
            logger.warn("虚线样式非法：{}", array);
            return null;
        }
    }

    /**
     * 把 OFD 线端点样式换算为内部枚举。
     *
     * @param cap OFD 线端点样式
     * @return 内部线端点枚举，null 表示未设置
     */
    private LineCapStyle capOf(org.ofdrw.core.pageDescription.drawParam.LineCapType cap) {
        if (cap == null) {
            return null;
        }
        switch (cap) {
            case Round:
                return LineCapStyle.ROUND;
            case Square:
                return LineCapStyle.SQUARE;
            case Butt:
            default:
                return LineCapStyle.BUTT;
        }
    }

    /**
     * 把 OFD 线条连接样式换算为内部枚举。
     *
     * @param join OFD 线条连接样式
     * @return 内部线条连接枚举，null 表示未设置
     */
    private LineJoinStyle joinOf(org.ofdrw.core.pageDescription.drawParam.LineJoinType join) {
        if (join == null) {
            return null;
        }
        switch (join) {
            case Round:
                return LineJoinStyle.ROUND;
            case Bevel:
                return LineJoinStyle.BEVEL;
            case Miter:
            default:
                return LineJoinStyle.MITER;
        }
    }

    private void writeComposite(Renderer renderer, CompositeObject compositeObject, StyleScope scope, Matrix parentMatrix,
                                StyleResolver resolver) {
        ST_RefID refID = compositeObject.getResourceID();
        if (refID == null) return;

        CT_VectorG vectorG = resourceManage.getCompositeGraphicUnit(refID.getRefId().getId().toString());
        if (vectorG == null) return;
        ST_Box boundary = compositeObject.getBoundary();

        Matrix m = MatrixUtils.base();

        if (compositeObject.getCTM() != null) {
            m = m.mtimes(MatrixUtils.ctm(compositeObject.getCTM().toDouble()));
        }
        if (boundary != null) {
            m = MatrixUtils.move(m, boundary.getTopLeftX(), boundary.getTopLeftY());
        }
        m = m.mtimes(parentMatrix);

        writeContent(renderer, vectorG.getContent(), scope.child(StyleScopeKind.COMPOSITE, null, null), m, resolver);
    }


    private void writePath(Renderer renderer, PathObject pathObject, StyleScope scope, Matrix parentMatrix,
                           StyleResolver resolver) {
        ST_Box boundary = pathObject.getBoundary();
        Matrix baseMatrix = renderBoundaryAndSetClip(renderer, boundary, parentMatrix);
        Matrix matrix = MatrixUtils.base();
        if (pathObject.getCTM() != null) {
            matrix = matrix.mtimes(MatrixUtils.ctm(pathObject.getCTM().toDouble()));
        }

        if (boundary != null) {
            matrix = MatrixUtils.move(matrix, boundary.getTopLeftX(), boundary.getTopLeftY());
        }
        matrix = matrix.mtimes(baseMatrix);
        renderer.transform(MatrixUtils.createAffineTransform(matrix));

        // 路径点位于图元自身坐标系，变换由渲染器的当前变换在绘制时应用
        Path2D path2D = buildPath(pathObject.getAbbreviatedData());

        ResolvedStyle style = resolver.resolve(scope, GraphicUnitType.PATH, pathObject.getFill(), pathObject.getStroke(),
                pathObject.getFillColor(), pathObject.getStrokeColor(), null);

        if (style.isStroke()) {
            Color strokeColor = toAwtColor(style.getStrokeColor());
            if (strokeColor != null) {
                renderer.stroke(path2D, createStroke(style), resolvePaint(strokeColor, 1f));
            }
        }
        if (style.isFill()) {
            Color fillColor = toAwtColor(style.getFillColor());
            if (fillColor != null) {
                renderer.fill(path2D, createStroke(style), resolvePaint(fillColor, 1f), resolveFillRule(pathObject));
            }
        }
    }

    private void writeImage(Renderer renderer, ImageObject imageObject, StyleScope scope, Matrix parentMatrix,
                            StyleResolver resolver) {
        ST_Box boundary = imageObject.getBoundary();
        Matrix baseMatrix = renderBoundaryAndSetClip(renderer, boundary, parentMatrix);

        BufferedImage image;
        try {
            // 解析图片对象获取图片
            image = resourceManage.getImage(imageObject);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (image == null) {
            return;
        }
        Matrix m = MatrixUtils.base();
        // 把图片还原成1*1
        m = MatrixUtils.scale(m, Double.valueOf(1.0 / image.getWidth()).floatValue(), Double.valueOf(1.0 / image.getHeight()).floatValue());

        if (imageObject.getCTM() != null) {
            m = m.mtimes(MatrixUtils.ctm(imageObject.getCTM().toDouble()));
        }
        if (boundary != null) {
            m = MatrixUtils.move(m, boundary.getTopLeftX(), boundary.getTopLeftY());
        }
        m = m.mtimes(baseMatrix);

        float alpha = 1;
        if (imageObject.attributeValue("Alpha") != null) {
            alpha = imageObject.getAlpha() / 255.0f;
        }
        // 图像透明度同样参与样式结算（绘制参数 Alpha × 图元 Alpha），保证与其它图元一致
        ResolvedStyle style = resolver.resolve(scope, GraphicUnitType.IMAGE, null, null, null, null,
                imageObject.attributeValue("Alpha") == null ? null : Double.valueOf(alpha));
        alpha = (float) style.getAlpha();

        renderer.setTransform(MatrixUtils.createAffineTransform(MatrixUtils.base()));
        ImageHandle handle = renderer.createImage(image);
        renderer.drawImage(handle, MatrixUtils.createAffineTransform(m), alpha, true, null, BlendMode.NORMAL);
    }

    /**
     * 获取字号 ，若无法获取则设置为默认值 0.353。
     *
     * @param textObject 文字对象
     * @return 字号。
     */
    private double getTextObjectSize(TextObject textObject) {
        double fontSize = 0.353;
        if (textObject == null) {
            return fontSize;
        }
        try {
            fontSize = textObject.getSize();
        } catch (Exception e) {
            fontSize = 0.353;
        }
        return fontSize;
    }

    /**
     * 获取文字透明度 默认透明度为1.0
     *
     * @param textObject 文字对象
     * @return 文字透明度。
     */
    private float getTextObjectAlpha(TextObject textObject) {
        float alpha = 1.0f;
        if (textObject == null || textObject.getAlpha() == 0) {
            return alpha;
        }
        try {
            alpha = NumberUtils.createBigDecimal(textObject.getAlpha().toString()).divide(new BigDecimal(255), 2, RoundingMode.HALF_UP).floatValue();
        } catch (Exception e) {
            alpha = 1.0f;
        }
        return alpha;
    };

    private void writeText(Renderer renderer, TextObject textObject, StyleScope scope, Matrix parentMatrix,
                           StyleResolver resolver) {
        logger.debug("┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━TextObject━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓");
        Double fontSize = getTextObjectSize(textObject);
        ResolvedStyle style = resolver.resolve(scope, GraphicUnitType.TEXT, textObject.getFill(), textObject.getStroke(),
                textObject.getFillColor(), textObject.getStrokeColor(), Double.valueOf(getTextObjectAlpha(textObject)));
        Color strokeColor = style.isStroke() ? toAwtColor(style.getStrokeColor()) : null;
        Color fillColor = style.isFill() ? toAwtColor(style.getFillColor()) : null;
        if (fillColor == null && style.isFill()) fillColor = Color.black;

        ST_Box boundary = textObject.getBoundary();
        Matrix baseMatrix = renderBoundaryAndSetClip(renderer, boundary, parentMatrix);


        // 读取字体
        FontWrapper<TrueTypeFont> fontWrapper = getFont(textObject);
        if (fontWrapper == null) {
            // 图元未声明 Font 或资源中找不到字体：跳过该文字对象并留下可定位告警，避免 NPE 被静默吞掉
            String fontRef = textObject.getFont() == null ? "无" : textObject.getFont().toString();
            logger.warn("文字对象缺少可用字体，已跳过渲染：TextObjectID={}，Font引用={}",
                    textObject.getID(), fontRef);
            return;
        }
        TrueTypeFont typeFont = fontWrapper.getFont();
        List<Number> fontMatrix = null;

        if (typeFont == null) {
            logger.info("无法加载字体ID：" + textObject.getFont());
            typeFont = FontLoader.getInstance().loadDefaultFont();
        }
        if (typeFont == null) {
            logger.warn("无法加载字体ID：" + textObject.getFont() + "无法渲染 textObject:" + textObject);
            return;
        } else {
            try {
                fontMatrix = typeFont.getFontMatrix();
            } catch (Exception e) {
                logger.warn("解析加载异常", e);
            }
        }

        // 创建字形变换映射
        CGTransformMap tsfMap = new CGTransformMap(textObject);
        /*
        字符偏移量：一个TextObject 可以含有多个TextCode，
        多个TextCode按照顺序出现，TextCode内含有多个字符，
        该变量用于计数，当前字符在整个TextObject中字符的偏移量。
         */
        int globalOffset = 0;
        Double previousX = null;
        Double previousY = null;
        for (TextCode textCode : textObject.getTextCodes()) {
            // 移除内容中包含的换行符
            String content = StringUtils.removeNewline(textCode.getContent());
            // 该TextCode 字符编码数量
            int len = content.length();
            // 当前正在处理的字符编码 在 该字符编码中的偏移量
            int offset = 0;
            // 待绘制的字形及其字宽（字宽用于 DeltaX 缺失时估算字符步进）
            List<GeneralPath> tbDrawChars = new ArrayList<>(5);
            List<Integer> tbDrawGids = new ArrayList<>(5);
            while (offset < len) {
                CT_CGTransform tsfInfo = tsfMap.get(globalOffset);
                // 不存在字形变换，使用字体cmap查找字形
                if (tsfInfo == null) {
                    char c = content.charAt(offset);
                    try {
                        // 通过字符编码获取字形
                        GlyphData glyphData = typeFont.getUnicodeGlyph(c);
                        if (glyphData != null) {
                            tbDrawChars.add(glyphData.getPath());
                        } else {
                            // 找不到字形
                            tbDrawChars.add(null);
                        }
                        tbDrawGids.add(typeFont.getUnicodeCmapLookup().getGlyphId(c));
                    } catch (Exception e) {
                        tbDrawChars.add(null);
                        tbDrawGids.add(-1);
                        logger.debug(String.format("找不到字形 unicode: %c", c));
                    }
                    globalOffset++;
                    offset++;
                } else {
                    // 变换关系中字符的数量
                    int codeCount = tsfInfo.getCodeCount();
                    // 变换关系汇总字形索引的个数
                    int glyphCount = tsfInfo.getGlyphCount();
                    // 获取字形索引序列，解决长度不足或过长的问题
                    int[] glyphIndexArr = tsfInfo.getGlyphs().expectIntArr(glyphCount);
                    /*
                     * 本质上绘制字形时，只关心字形序列，字形序列与DeltaX或DeltaY对应，
                     * 与字符序列无关。
                     *
                     * "一对一"、“多对一”、“多对多” 指的是字符和字形的映射关系，不用于绘制。
                     * 用于处理字符偏移量。
                     */
                    // 字体被相似字体或默认字体替换后，字体内的字形索引与原文档字体不再对应，跳过该段字形
                    if (fontWrapper != null && fontWrapper.isEnableSimilarFontReplace()) {
                        logger.warn("字体已替换，字形索引失效，跳过字形绘制, 字体ID: {}, gid: {}",
                                textObject.getFont(), Arrays.toString(glyphIndexArr));
                        tbDrawChars.add(null);
                        tbDrawGids.add(-1);
                    } else {
                        for (int gid : glyphIndexArr) {
                            GeneralPath drawPath;
                            try {
                                // 通过字形索引到字体中找到字形数据
                                drawPath = typeFont.getPath(gid);
                            } catch (Exception e) {
                                // 单个字形解析失败时跳过该字形，不影响其余文字渲染
                                drawPath = null;
                                logger.debug(String.format("找不到字形 gid: %s", gid), e);
                            }
                            if (drawPath == null) {
                                logger.debug(String.format("字形索引越界或字体中不存在该字形 gid: %s", gid));
                            }
                            tbDrawChars.add(drawPath);
                            tbDrawGids.add(gid);
                        }
                    }
                    // 根据变换信息处理全局偏移量和局部偏移量
                    globalOffset += codeCount;
                    offset += codeCount;
                }
            }
            /*
             * 计算偏移量和字形相关属性绘制字形
             *
             * 特别的：待绘制字形序列 与 字形偏移量 一一对应
             */
            List<Double> deltaX = parseDelta(textCode.getDeltaX());
            List<Double> deltaY = parseDelta(textCode.getDeltaY());
            Double x = textCode.getX();
            // 如果X或Y不出现，则采用上一个TextCode的X或Y值
            if (x == null && previousX != null) {
                x = previousX;
            } else if (x == null) {
                x = 0.0;
            }
            Double y = textCode.getY();
            if (y == null && previousY != null) {
                y = previousY;
            } else if (y == null) {
                y = 0.0;
            }

            for (int drawOffset = 0; drawOffset < tbDrawChars.size(); drawOffset++) {
                // 第一个字符的X和Y就是原始的X和Y

                // 字形在字符偏移量中的位置
                int deltaOffset = drawOffset - 1;
                if (deltaOffset >= 0) {
                    // 非第一个字符需要添加字符偏移量
                    if (deltaX.size() > 0) {
                        // 计算X偏移量
                        if (deltaOffset < deltaX.size()) {
                            x += deltaX.get(deltaOffset);
                        } else {
                            // 如果deltaX 数组长度不及字符长度，使用deltaX数组的最后一个数值作为剩余字符的偏移量，防止其错位打印。
                            x += deltaX.get(deltaX.size() - 1);
                        }
                    } else {
                        // DeltaX 缺失，按上一个字形在当前字体中的字宽估算字符步进
                        x += estimateAdvanceWidth(typeFont, tbDrawGids.get(drawOffset - 1), fontSize, fontMatrix);
                    }
                    if (deltaY.size() > 0) {
                        // 计算Y偏移量
                        if (deltaOffset < deltaY.size()) {
                            y += deltaY.get(deltaOffset);
                        } else {
                            y += deltaY.get(deltaY.size() - 1);
                        }
                    }
                }
                GeneralPath shape = tbDrawChars.get(drawOffset);
                if (shape == null) {
                    // 没有字形，那么忽略绘制
                    continue;
                }
                // 结合变换矩阵绘制字形
                Matrix matrix = chatMatrix(textObject, x, y, fontSize, fontMatrix, baseMatrix);
                renderChar(renderer, shape, matrix, strokeColor, fillColor, (float) style.getAlpha());
            }
            // 更新上一个TextCode的X和Y，用于缺失 X或Y时准备
            if (textCode.getX() != null) {
                previousX = textCode.getX();
            }
            if (textCode.getY() != null) {
                previousY = textCode.getY();
            }
        }
        logger.debug("┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛");
    }

    /**
     * 估算字形的字符步进（水平排版时下一个字形的 X 位置增量）
     * <p>
     * 字宽取自字体的 hmtx 表，按字号与字体矩阵换算为页面上毫米单位的步进量；
     * 字体没有字宽（如裸 CFF 无 hmtx 表）或字形索引无效时，按 1 个 em 估算。
     *
     * @param typeFont   当前绘制使用的字体
     * @param gid        用于估算步进的字形索引，无效时为负数
     * @param fontSize   字号，单位为毫米（mm）
     * @param fontMatrix 字体矩阵，用于把字体单位换算为 em
     * @return 字符步进，单位为毫米（mm）
     */
    static double estimateAdvanceWidth(TrueTypeFont typeFont, int gid, double fontSize, List<Number> fontMatrix) {
        double advanceWidth = typeFont.getAdvanceWidth(gid);
        if (advanceWidth > 0 && fontMatrix != null && fontMatrix.size() > 0) {
            // 渲染前字形的横向缩放为 |fontMatrix[0]|，用它把字体单位换算为 em，再乘以字号得到页面步进
            double scaleX = Math.abs(fontMatrix.get(0).doubleValue());
            return advanceWidth * scaleX * fontSize;
        }
        // 字体没有字宽信息（如裸 CFF 无 hmtx），按 1 个 em 估算，保证字符位置单调递增且间距合理
        return fontSize;
    }

    private Matrix chatMatrix(TextObject ctText, Double deltaX, Double deltaY, Double fontSize, List<Number> fontMatrix, Matrix baseMatrix) {
        Matrix m = MatrixUtils.base();
        m = MatrixUtils.imageMatrix(m, 0, 1, 0);
        if (ctText.getHScale() != null) {
            m = MatrixUtils.scale(m, ctText.getHScale(), 1);
        }
        m = m.mtimes(MatrixUtils.create(fontMatrix.get(0).doubleValue(), fontMatrix.get(1).doubleValue(),
                fontMatrix.get(2).doubleValue(), fontMatrix.get(3).doubleValue(),
                fontMatrix.get(4).doubleValue(), fontMatrix.get(5).doubleValue()));
        m = MatrixUtils.scale(m, fontSize, fontSize);
        m = MatrixUtils.move(m, deltaX, deltaY);
        if (ctText.getCTM() != null) {
            m = m.mtimes(MatrixUtils.ctm(ctText.getCTM().toDouble()));
        }
        if (ctText.getBoundary() != null) {
            m = MatrixUtils.move(m, ctText.getBoundary().getTopLeftX(), ctText.getBoundary().getTopLeftY());
        }
        m = m.mtimes(baseMatrix);
        return m;
    }

    /**
     * 绘制单个字形。
     * <p>
     * 字形轮廓位于字体自身坐标系，渲染器按传入矩阵变换到页面像素坐标；
     * 透明度直接设定（不累积），与历史 {@code AlphaComposite(SRC_OVER, alpha)} 语义一致。
     *
     * @param renderer 目标渲染器
     * @param shape    字形轮廓，null 表示无字形
     * @param m        字形到页面像素的变换矩阵
     * @param stroke   描边颜色，null 表示不描边
     * @param fill     填充颜色，null 表示不填充
     * @param alpha    不透明度，取值 0..1
     */
    private void renderChar(Renderer renderer, Shape shape, Matrix m, Color stroke, Color fill, float alpha) {
        if (shape == null) return;
        renderer.setTransform(MatrixUtils.createAffineTransform(m));
        renderer.setAlpha(alpha);
        DrawStyle style = DrawStyle.defaultStyle();
        if (stroke != null) {
            renderer.stroke(shape, style, resolvePaint(stroke, 1f));
        }
        if (fill != null) {
            renderer.fill(shape, style, resolvePaint(fill, 1f), FillRule.NON_ZERO);
        }
    }

    /**
     * 解析字体对象获取字体
     *
     * @param textObject 字体对象
     * @return 字体
     */
    private FontWrapper<TrueTypeFont> getFont(TextObject textObject) {
        ST_RefID stRefID = textObject.getFont();
        if (stRefID == null) return null;

        CT_Font ctFont = resourceManage.getFont(stRefID.toString());
        if (ctFont == null) {
            return null;
        }

        String key = String.format("%s_%s_%s", ctFont.getFamilyName(), ctFont.getFontName(), ctFont.getFontFile());
        if (fontCache.containsKey(key)) {
            // 命中缓存，直接返还已经缓存的字体对象
            return fontCache.get(key);
        }
        // 加载字体
        FontWrapper<TrueTypeFont> trueTypeFont = FontLoader.getInstance().loadFontSimilar(this.reader.getResourceLocator(), ctFont);
        // 更新缓存 即便 trueTypeFont 也设置，不存在字体时(null)重复加载问题。
        fontCache.put(key, trueTypeFont);
        return trueTypeFont;
    }

    /**
     * 计算图元的世界变换，并按包围框设置裁剪。
     * <p>
     * 裁剪采用求交语义：新包围框与已有裁剪区域取交集，父级裁剪继续生效（修正历史实现中“覆盖父级裁剪”的缺陷）。
     * 调试开关 {@code config.drawBoundary} 为真时，用细红线描出包围框。
     *
     * @param renderer     目标渲染器
     * @param boundary     包围框，单位：毫米；null 表示无包围框
     * @param parentMatrix 父级变换矩阵，null 表示单位矩阵
     * @return 图元的世界变换（已换算为像素）
     */
    private Matrix renderBoundaryAndSetClip(Renderer renderer, ST_Box boundary, Matrix parentMatrix) {
        Matrix m = MatrixUtils.base().mtimes(parentMatrix);
        if (boundary != null) {
            /*
             * 计算包围框在世界坐标系的位置
             * */
            Polygon shape = new Polygon();
            Tuple2<Double, Double> p00 = MatrixUtils.pointTransform(m, boundary.getTopLeftX(), boundary.getTopLeftY() - 1);
            Tuple2<Double, Double> p01 = MatrixUtils.pointTransform(m, boundary.getTopLeftX() + boundary.getWidth() + 2, boundary.getTopLeftY() - 1);
            Tuple2<Double, Double> p10 = MatrixUtils.pointTransform(m, boundary.getTopLeftX(), boundary.getTopLeftY() + boundary.getHeight() + 2);
            Tuple2<Double, Double> p11 = MatrixUtils.pointTransform(m, boundary.getTopLeftX() + boundary.getWidth() + 2, boundary.getTopLeftY() + boundary.getHeight() + 2);

            shape.addPoint(Double.valueOf(p00.getFirst() * ppm).intValue(), Double.valueOf(p00.getSecond() * ppm).intValue());
            shape.addPoint(Double.valueOf(p01.getFirst() * ppm).intValue(), Double.valueOf(p01.getSecond() * ppm).intValue());
            shape.addPoint(Double.valueOf(p11.getFirst() * ppm).intValue(), Double.valueOf(p11.getSecond() * ppm).intValue());
            shape.addPoint(Double.valueOf(p10.getFirst() * ppm).intValue(), Double.valueOf(p10.getSecond() * ppm).intValue());

            if (config.drawBoundary) {
                // 包围框以像素为单位直接落在画布上，绘制时不受当前变换影响
                renderer.save();
                renderer.setTransform(new AffineTransform());
                renderer.stroke(shape, new DrawStyle(0.1f * (float) ppm, null, 0f,
                        java.awt.BasicStroke.CAP_BUTT, java.awt.BasicStroke.JOIN_MITER, 10f),
                        resolvePaint(Color.RED, 1f));
                renderer.restore();
            }
            if (config.clip) {
                renderer.clip(shape, FillRule.NON_ZERO);
            }
        }

        m = MatrixUtils.scale(m, ppm, ppm);
        return m;
    }

    private Path2D buildPath(String abbreviatedData) {
        // Path 压缩格式解析
        LinkedList<OptVal> optValArr = AbbreviatedData.parse(abbreviatedData);
        Path2D path = new Path2D.Double();
        path.moveTo(0, 0);
        for (OptVal optVal : optValArr) {
            double[] arr = optVal.expectValues();
            switch (optVal.opt) {
                case "S":
                case "M":
                    path.moveTo(arr[0], arr[1]);
                    break;
                case "L":
                    path.lineTo(arr[0], arr[1]);
                    break;
                case "Q":
                    path.quadTo(arr[0], arr[1], arr[2], arr[3]);
                    break;
                case "B":
                    path.curveTo(arr[0], arr[1], arr[2], arr[3], arr[4], arr[5]);
                    break;
                case "A":
                    // path.append(new
                    // Arc2D.Double(Double.valueOf(s[i+1]),Double.valueOf(s[i+2]),Double.valueOf(s[i+3]),Double.valueOf(s[i+4]),Double.valueOf(s[i+5]),Double.valueOf(s[i+3]),-1),true);
                    break;
                case "C":
                    path.closePath();
                    break;
            }
        }
        return path;
    }

    /**
     * 按结算样式创建绘制样式（线宽、端点、连接、虚线）。
     *
     * @param style 结算后的样式；线宽与虚线长度单位：毫米（mm）
     * @return 渲染器绘制样式；线宽与虚线长度已换算为像素
     */
    private DrawStyle createStroke(ResolvedStyle style) {
        float width = Math.max(0.01f, (float) (style.getLineWidth() * ppm));
        double[] pattern = style.getDashPattern();
        float[] dash = null;
        if (pattern.length > 0) {
            dash = new float[pattern.length];
            for (int i = 0; i < pattern.length; i++) {
                dash[i] = (float) (pattern[i] * ppm);
            }
        }
        return new DrawStyle(width, dash, (float) (style.getDashOffset() * ppm),
                style.getLineCap().toAwtValue(), style.getLineJoin().toAwtValue(), (float) style.getMiterLimit());
    }

    /**
     * 把单色颜色包装为渲染器着色源。
     *
     * @param color   颜色，不能为空
     * @param opacity 不透明度，取值 0..1，无量纲
     * @return 已解析着色源
     */
    private ResolvedPaint resolvePaint(Color color, float opacity) {
        return new ResolvedPaint(CanvasPaint.of(color), opacity);
    }

    /**
     * 读取路径的填充规则。
     *
     * @param pathObject 路径对象
     * @return 填充规则，未设置时按非零环绕数规则
     */
    private FillRule resolveFillRule(PathObject pathObject) {
        try {
            org.ofdrw.core.graph.pathObj.Rule rule = pathObject.getRule();
            if (rule == org.ofdrw.core.graph.pathObj.Rule.Even_Odd) {
                return FillRule.EVEN_ODD;
            }
        } catch (Exception ignore) {
            // 规则非法时按默认非零环绕数处理
        }
        return FillRule.NON_ZERO;
    }

    /**
     * 把 sRGB 整数换算为 AWT 颜色。
     *
     * @param rgb sRGB 0xRRGGBB，null 表示无颜色
     * @return AWT 颜色，null 表示无颜色
     */
    private Color toAwtColor(Integer rgb) {
        return rgb == null ? null : new Color(rgb.intValue());
    }

    public Color getColor(CT_Color ctColor) {
        if (ctColor == null) return null;
        ST_Array array = ctColor.getValue();


        OFDColorSpaceType type = OFDColorSpaceType.RGB;
        ST_RefID refID = ctColor.getColorSpace();
        CT_ColorSpace ctColorSpace = null;
        if (refID != null) {
            ctColorSpace = resourceManage.getColorSpace(refID.toString());
        }
        if (ctColorSpace != null) {
            try{
                OFDColorSpaceType otype = ctColorSpace.getType();
                if (otype != null) {
                    type = otype;
                }
            }catch (Exception e){
                logger.warn("获取颜色空间类型失败", e);
            }

            if (array == null && ctColor.getIndex() != null) {
                array = ctColorSpace.getPalette().getColorByIndex(ctColor.getIndex());
            }

        }
        if (array == null) return null;
        int[] color = new int[array.size()];
        for (int i = 0; i < array.size(); i++) {
            String s = array.getArray().get(i);
            if (s.startsWith("#")) {
                color[i] = Integer.parseInt(s.replaceAll("#", ""), 16);
            } else if (s.indexOf('.') != -1) {
                color[i] = (int) Float.parseFloat(s);
            } else {
                color[i] = Integer.parseInt(s);
            }
        }
        switch (type) {
            case GRAY:
                return new Color(color[0], color[0], color[0]);
            case CMYK:
                int r = 255 * (100 - color[0]) * (100 - color[3]) / 10000;
                int g = 255 * (100 - color[1]) * (100 - color[3]) / 10000;
                int b = 255 * (100 - color[2]) * (100 - color[3]) / 10000;
                return new Color(r, g, b);
            case RGB:
            default:
                return new Color(color[0], color[1], color[2]);
        }
    }

    /**
     * 设置转换质量
     *
     * @param ppm 每毫米像素数量(Pixels per millimeter)
     */
    public void setPPM(double ppm) {
        if (ppm <= 0) {
            return;
        }
        this.ppm = ppm;
    }

    public static List<Double> parseDelta(ST_Array array) {
        if (array == null) return new ArrayList<>(0);
        List<Double> arr = new ArrayList<>();

        int i = 0;
        while (i < array.size()) {
            String current = array.getArray().get(i);
            if ("g".equals(current)) {
                int num = Integer.parseInt(array.getArray().get(i + 1));
                Double delta = Double.valueOf(array.getArray().get(i + 2));
                for (int j = 1; j <= num; j++) {
                    arr.add(delta);
                }
                i += 3;
            } else {
                Double delta = Double.valueOf(current);
                arr.add(delta);
                i++;
            }
        }
        return arr;
    }

    public static class Config {
        /*
         * 印章透明度
         * */
        private float stampOpacity = 0.75f;
        /*
         * 是否清除印章背景色
         * */
        private boolean clearStampBackground = true;
        /*
         * 印章背景灰度阈值，高于此值被设置为透明
         * */
        private int stampBackgroundGray = 255;

        /*
         * 是否绘制元素边框 （调试使用）
         * */
        private boolean drawBoundary;

        /*
         * 是否进行剪裁
         * */
        private boolean clip = true;

        public float getStampOpacity() {
            return stampOpacity;
        }

        public void setStampOpacity(float stampOpacity) {
            if (stampOpacity >= 0 && stampOpacity <= 1) {
                this.stampOpacity = stampOpacity;
            }
        }

        public boolean isDrawBoundary() {
            return drawBoundary;
        }

        public void setDrawBoundary(boolean drawBoundary) {
            this.drawBoundary = drawBoundary;
        }

        public boolean isClearStampBackground() {
            return clearStampBackground;
        }

        public void setClearStampBackground(boolean clearStampBackground) {
            this.clearStampBackground = clearStampBackground;
        }

        public int getStampBackgroundGray() {
            return stampBackgroundGray;
        }

        /**
         * 印章背景灰度阈值，高于此值被设置为透明
         *
         * @param stampBackgroundGray 灰度阈值
         */
        public void setStampBackgroundGray(int stampBackgroundGray) {
            if (stampBackgroundGray < 0 || stampBackgroundGray > 255) {
                stampBackgroundGray = 255;
            }
            this.stampBackgroundGray = stampBackgroundGray;
        }

        public boolean isClip() {
            return clip;
        }

        public void setClip(boolean clip) {
            this.clip = clip;
        }
    }
}
