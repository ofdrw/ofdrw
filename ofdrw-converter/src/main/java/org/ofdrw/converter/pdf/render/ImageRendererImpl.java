package org.ofdrw.converter.pdf.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.ofdrw.converter.pdf.CoordSystem;
import org.ofdrw.converter.pdf.font.ImageEncoding;
import org.ofdrw.converter.pdf.model.AffineMatrix;
import org.ofdrw.converter.pdf.model.Box;
import org.ofdrw.converter.pdf.model.ContentCommand;
import org.ofdrw.converter.pdf.model.DashPattern;
import org.ofdrw.converter.pdf.model.ImageBorderCommand;
import org.ofdrw.converter.pdf.model.ImageCommand;
import org.ofdrw.converter.pdf.model.PdfColor;
import org.ofdrw.converter.pdf.model.SaveState;
import org.ofdrw.converter.pdf.model.SetAlpha;
import org.ofdrw.converter.pdf.model.StrokeStyle;
import org.ofdrw.converter.pdf.model.RestoreState;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.image.Border;
import org.ofdrw.core.basicStructure.pageObj.layer.block.ImageObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 图像渲染器实现：OFD ImageObject → 中间模型图像指令（决策 23：保真优先）
 * <p>
 * 处理流程：
 * <ol>
 *     <li>按资源 ID 取原始字节；JPEG 保持原字节（{@link ImageEncoding#JPEG}），其余格式按无损编码标记；</li>
 *     <li>按显式上下文计算落点：图元 CTM × 容器 CTM × 外接矩形 × 页面框偏移 → mm→pt + Y 翻转（{@link ImageGeometry}）；</li>
 *     <li>{@code ImageMask} 单独输出一条蒙版图像指令（同一落点），写入器据此生成 {@code /SMask}；
 *         两个图像条目的 {@code masked} 标记表明它们共用同一落点，写入器必须先建立蒙版再绘制主图；</li>
 *     <li>{@code Border} 输出 {@link ImageBorderCommand}（圆角矩形路径 + 虚线），不依赖图像 XObject。</li>
 * </ol>
 * 容错（设计约束 3、决策 8）：单图元异常/资源缺失只跳过该图元并输出 warning，不中断整页。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class ImageRendererImpl implements ImageRenderer {

    /**
     * 日志
     */
    private static final Logger logger = LoggerFactory.getLogger(ImageRendererImpl.class);

    /**
     * 图像边框默认颜色：黑色
     */
    private static final PdfColor DEFAULT_BORDER_COLOR = PdfColor.BLACK;

    /**
     * 图像资源加载器，可为空（为空时所有图像都按资源缺失处理并告警）
     */
    private final ImageSource loader;

    /**
     * 颜色解析器（复用路径渲染器的 OFD 颜色解析逻辑），可为空表示按通道数量推断
     */
    private final PathRenderer colorResolver;

    /**
     * 已告警原因
     */
    private final Set<String> reported = ConcurrentHashMap.newKeySet();

    /**
     * 构造图像渲染器
     *
     * @param loader        图像数据来源，可为空
     * @param colorResolver 颜色解析器（用于读取 Border 颜色），可为空
     */
    public ImageRendererImpl(ImageSource loader, PathRenderer colorResolver) {
        this.loader = loader;
        this.colorResolver = colorResolver;
    }

    /**
     * 构造图像渲染器（不依赖资源加载器，仅用于无资源的单测）
     */
    public ImageRendererImpl() {
        this(null, null);
    }

    @Override
    public String getName() {
        return "ImageRenderer";
    }

    /**
     * 把 OFD 图像对象渲染为中间模型指令
     *
     * @param input   OFD 图像对象（{@link ImageObject}）
     * @param context 渲染上下文
     * @return 指令序列：{@code q + SetAlpha + ImageCommand(+ 蒙版指令) + 边框指令 + Q}；
     * 资源缺失、不可见或输入不支持时返回空列表
     */
    @Override
    public List<ContentCommand> render(Object input, RenderContext context) {
        if (!(input instanceof ImageObject) || context == null) {
            return Collections.emptyList();
        }
        ImageObject imageObject = (ImageObject) input;
        try {
            if (Boolean.FALSE.equals(imageObject.getVisible())) {
                return Collections.emptyList();
            }
            // 图像的对象空间是单位正方形 (0,0)-(1,1)：CTM 已把单位正方形拉伸到外接矩形的宽高并平移到其左上角，
            // 因此落点矩形由单位正方形的四个角经变换得到，不能再把 Boundary 的 mm 宽高参与变换（会与 CTM 缩放叠加）
            AffineMatrix toPage = ImageGeometry.unitSquareToPage(imageObject.getCTM(), imageObject.getBoundary(), context);
            Box placement = ImageGeometry.unitSquareBox(toPage);
            if (placement == null) {
                warnOnce("image-boundary", "图像图元缺少有效外接矩形，已跳过："
                        + (imageObject.getResourceID() == null ? "无资源" : imageObject.getResourceID()));
                return Collections.emptyList();
            }
            ST_RefID resourceId = imageObject.getResourceID();
            if (resourceId == null) {
                warnOnce("image-no-resource", "图像图元缺少资源引用，已跳过：" + imageObject.getBoundary());
                return Collections.emptyList();
            }
            String id = GraphicsStateResolver.idOf(resourceId);
            List<ContentCommand> commands = new ArrayList<>(6);
            commands.add(SaveState.INSTANCE);
            // 决策 2：缺省不透明；只有显式声明 Alpha 时才写 ca/CA，避免把“未设置”当成显式 255
            // （CT_GraphicUnit#getAlpha() 在属性缺失时返回 255，无法区分“未声明”）
            Integer alpha = imageObject.getAlphaExplicit();
            if (alpha != null) {
                commands.add(SetAlpha.fromOfd(alpha));
            }

            ImageResourceLoader.ImageData main = load(id);
            if (main == null) {
                // 资源缺失：整条指令序列作废（不产生半截的 q/Q），由调用方跳过该图元，告警已在 load 中输出
                return Collections.emptyList();
            }
            if (imageObject.getSubstitution() != null) {
                warnOnce("image-substitution", "图像图元声明了替换图（Substitution），按决策 23 仅保留主图：" + id);
            }
            ImageResourceLoader.ImageData mask = loadMask(imageObject.getImageMask());
            if (mask != null) {
                commands.add(toCommand(mask, placement, true, toPage));
            }
            commands.add(toCommand(main, placement, mask != null, toPage));
            ContentCommand border = borderCommand(imageObject, context, toPage, placement);
            if (border != null) {
                commands.add(border);
            }
            commands.add(RestoreState.INSTANCE);
            return commands;
        } catch (RuntimeException e) {
            warnOnce("image-error", "图像图元渲染失败，已跳过："
                    + (imageObject.getResourceID() == null ? "无资源" : imageObject.getResourceID()) + "，原因：" + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 生成图像指令（保留原始字节，不做任何重编码，决策 23）
     *
     * @param data      图像原始数据
     * @param placement 页面落点矩形，单位 pt
     * @param masked    是否为蒙版图像
     * @return 图像指令
     */
    static ImageCommand toCommand(ImageResourceLoader.ImageData data, Box placement, boolean masked) {
        return toCommand(data, placement, masked, null);
    }

    /**
     * 生成图像指令（带完整放置矩阵，用于旋转/镜像图像）
     *
     * @param data            图像原始数据
     * @param placement       页面落点外接矩形，单位 pt
     * @param masked          是否为蒙版图像
     * @param placementMatrix 单位正方形 → 页面坐标的放置矩阵，可为空
     * @return 图像指令
     */
    static ImageCommand toCommand(ImageResourceLoader.ImageData data, Box placement, boolean masked,
                                  AffineMatrix placementMatrix) {
        return new ImageCommand("Img" + data.getResourceId(), data.getEncoding(), data.getWidth(), data.getHeight(),
                data.getData(), placement, masked, placementMatrix);
    }

    /**
     * 加载图像资源
     *
     * @param resourceId 资源 ID
     * @return 图像数据；加载器为空或资源缺失返回 {@code null}
     */
    private ImageResourceLoader.ImageData load(String resourceId) {
        if (loader == null) {
            warnOnce("image-no-loader", "未配置图像资源加载器，图像已跳过");
            return null;
        }
        return loader.load(resourceId);
    }

    /**
     * 加载蒙版图像（ImageMask）
     *
     * @param maskRef 蒙版资源引用，可为空
     * @return 蒙版数据；未声明蒙版或加载失败返回 {@code null}
     */
    private ImageResourceLoader.ImageData loadMask(ST_RefID maskRef) {
        if (maskRef == null) {
            return null;
        }
        String maskId = GraphicsStateResolver.idOf(maskRef);
        ImageResourceLoader.ImageData mask = load(maskId);
        if (mask == null) {
            warnOnce("image-mask-missing", "图像蒙版资源缺失，按无蒙版绘制：ID=" + maskId);
        }
        return mask;
    }

    /**
     * 生成图像边框指令
     * <p>
     * Border 的线宽与圆角半径单位为 mm（OFD 对象空间），需经 {@link CoordSystem} 换算为页面 pt 后再按当前 CTM 缩放。
     *
     * @param imageObject 图像对象
     * @param context     渲染上下文（仅用于告警定位）
     * @param toPage      对象空间 → 页面坐标矩阵
     * @param placement   图像页面落点矩形，单位 pt
     * @return 边框指令；未声明边框或线宽无效时返回 {@code null}
     */
    private ContentCommand borderCommand(ImageObject imageObject, RenderContext context,
                                          AffineMatrix toPage, Box placement) {
        Border border = imageObject.getBorder();
        if (border == null) {
            return null;
        }
        Box rect = placement;
        // Border#getLineWidth() 在属性缺失时返回默认值 0.353，无法区分“未声明”与“显式 0”；
        // 规范语义：显式写 0 表示不绘制边框，因此必须使用仅取显式属性的访问器
        Double explicitWidth = border.getLineWidthExplicit();
        if (explicitWidth != null && !(Math.abs(explicitWidth) > 0)) {
            warnOnce("image-border-zero", "图像边框显式声明线宽 0，按规范不绘制边框");
            return null;
        }
        double lineWidthMm = explicitWidth == null ? 0.353 : explicitWidth;
        if (!(lineWidthMm > 0)) {
            warnOnce("image-border-width", "图像边框线宽无效，已跳过边框绘制：" + lineWidthMm);
            return null;
        }
        double scale = ImageGeometry.unitScale(toPage) / CoordSystem.MM_TO_PT;
        double lineWidthPt = CoordSystem.mmToPt(lineWidthMm) * scale;
        // 圆角半径同样只在显式声明时生效（避免属性缺失时读到非零默认值把直角画成圆角）
        Double explicitH = explicitAttribute(border, "HorizonalCornerRadius");
        Double explicitV = explicitAttribute(border, "VerticalCornerRadius");
        double hRadius = explicitH == null ? 0 : explicitH;
        double vRadius = explicitV == null ? 0 : explicitV;
        if (hRadius != vRadius && (hRadius > 0 || vRadius > 0)) {
            warnOnce("image-border-radius", "图像边框横/纵圆角半径不一致，按较小值绘制椭圆角近似：" + hRadius + "/" + vRadius);
        }
        double radiusPt = CoordSystem.mmToPt(Math.max(0, Math.min(hRadius <= 0 ? vRadius : hRadius,
                vRadius <= 0 ? hRadius : vRadius))) * scale;
        double[] dashes = ImageGeometry.dashArray(border.getDashPattern());
        double[] dashesPt = new double[dashes.length];
        for (int i = 0; i < dashes.length; i++) {
            dashesPt[i] = CoordSystem.mmToPt(dashes[i]) * scale;
        }
        Double explicitPhase = explicitAttribute(border, "DashOffset");
        double phasePt = explicitPhase == null ? 0 : CoordSystem.mmToPt(explicitPhase) * scale;
        DashPattern dash = new DashPattern(dashesPt, phasePt);
        PdfColor color = borderColor(border);
        return new ImageBorderCommand(rect, lineWidthPt, radiusPt, color, dash, ImageEncoding.RAW);
    }

    /**
     * 读取元素的显式数值属性（属性缺失返回 {@code null}）
     * <p>
     * core 的部分 getter 在属性缺失时返回规范默认值，无法区分“未声明”，因此这里直接读取属性文本。
     *
     * @param element  元素，不能为空
     * @param attrName 属性名
     * @return 数值；属性缺失或无法解析时返回 {@code null}
     */
    private static Double explicitAttribute(org.dom4j.Element element, String attrName) {
        String text = element.attributeValue(attrName);
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            double value = Double.parseDouble(text.trim());
            return Double.isFinite(value) ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 解析边框颜色
     *
     * @param border 边框，不能为空
     * @return 颜色；未声明颜色时使用黑色
     */
    private PdfColor borderColor(Border border) {
        if (border.getBorderColor() == null) {
            return DEFAULT_BORDER_COLOR;
        }
        if (colorResolver == null) {
            return DEFAULT_BORDER_COLOR;
        }
        PdfColor color = colorResolver.resolveColor(border.getBorderColor(), "边框");
        return color == null ? DEFAULT_BORDER_COLOR : color;
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
