package org.ofdrw.converter.pdf.write;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;
import org.ofdrw.converter.pdf.font.ImageEncoding;
import org.ofdrw.converter.pdf.model.AffineMatrix;
import org.ofdrw.converter.pdf.model.Box;
import org.ofdrw.converter.pdf.model.ContentCommand;
import org.ofdrw.converter.pdf.model.DashPattern;
import org.ofdrw.converter.pdf.model.ImageBorderCommand;
import org.ofdrw.converter.pdf.model.ImageCommand;
import org.ofdrw.converter.pdf.model.LineCap;
import org.ofdrw.converter.pdf.model.LineJoin;
import org.ofdrw.converter.pdf.model.ClipCommand;
import org.ofdrw.converter.pdf.model.ConcatMatrix;
import org.ofdrw.converter.pdf.model.FillRule;
import org.ofdrw.converter.pdf.model.MarkerCommand;
import org.ofdrw.converter.pdf.model.PathCommand;
import org.ofdrw.converter.pdf.model.PathSegment;
import org.ofdrw.converter.pdf.model.PatternCommand;
import org.ofdrw.converter.pdf.model.PdfColor;
import org.ofdrw.converter.pdf.model.RectCommand;
import org.ofdrw.converter.pdf.model.RestoreState;
import org.ofdrw.converter.pdf.model.SaveState;
import org.ofdrw.converter.pdf.model.SetAlpha;
import org.ofdrw.converter.pdf.model.ShadingCommand;
import org.ofdrw.converter.pdf.model.StrokeStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 中间模型指令回放器（P3）：把 {@link ContentCommand} 序列写入 PDF 内容流
 * <p>
 * 用途：底纹单元的 {@code CellContent} 内部可包含路径、矩形、渐变、底纹、透明度等指令，
 * 需要在本模块内递归写出；图案自身的平铺由 {@link PdfPatternWriter} 负责。
 * <p>
 * 约定（设计约束 1、3.4 节）：
 * <ul>
 *     <li>每条指令按“显式写入图形状态”处理：颜色一律按原颜色空间写（CMYK 用 {@code k}，绝不转 RGB）；</li>
 *     <li>{@code SaveState}/{@code RestoreState} 原样映射为 {@code q}/{@code Q}；</li>
 *     <li>单条指令失败只跳过该指令并输出 warning，不中断整个回放。</li>
 * </ul>
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class PdfCommandReplayer {

    private static final Logger logger = LoggerFactory.getLogger(PdfCommandReplayer.class);

    /**
     * 路径工厂
     */
    private final PdfPathFactory pathFactory;

    /**
     * 已生成的图像对象，KEY 为图像资源标识：同一图像只嵌入一次，跨页复用
     */
    private final Map<String, PDImageXObject> imageCache = new HashMap<>();

    /**
     * 待配对的主图之前出现过的蒙版指令（渲染器输出顺序：蒙版 → 主图）
     */
    private ImageCommand pendingMask;

    /**
     * 已解码的蒙版图像对象，供紧随的主图合成 {@code /SMask}
     */
    private PDImageXObject pendingMaskXObject;

    /**
     * 底纹（平铺图案）写入器
     */
    private final PdfPatternWriter patternWriter;

    /**
     * 文字写入器，可为空：底纹单元内的文字必须写进图案内容流，由本回放器转交
     */
    private PdfTextWriter textWriter;

    /**
     * 字体登记表，可为空：底纹单元内的文字需要按资源名查出字体
     */
    private org.ofdrw.converter.pdf.font.TrueTypePdfFont.Registry fontRegistry;

    /**
     * 资源名序号（旋转图像以 cm + Do 绘制时使用）
     */
    private int imageIndex = 1;

    /**
     * 构造回放器
     *
     * @param pathFactory 路径工厂，不能为空
     */
    /**
     * 注入底纹内文字所需的写入器与字体登记表
     *
     * @param textWriter   文字写入器，可为空
     * @param fontRegistry 字体登记表，可为空
     */
    public void usePatternText(PdfTextWriter textWriter,
                               org.ofdrw.converter.pdf.font.TrueTypePdfFont.Registry fontRegistry) {
        this.textWriter = textWriter;
        this.fontRegistry = fontRegistry;
    }

    /**
     * 构造回放器
     *
     * @param pathFactory 路径工厂，不能为空
     */
    public PdfCommandReplayer(PdfPathFactory pathFactory) {
        this.patternWriter = new PdfPatternWriter(pathFactory);
        if (pathFactory == null) {
            throw new IllegalArgumentException("路径工厂不能为空");
        }
        this.pathFactory = pathFactory;
    }

    /**
     * 单条命令的委托钩子：返回 true 表示该命令已由钩子处理，回放跳过它
     * <p>
     * 用途：文字指令需交给 {@code PdfTextWriter} 写入，同时保持与路径、图像指令的相对顺序（Z 序）。
     * 若改为“先回放全部再单独写文字”，文字会被整体挪到页面末尾，遮挡关系出错；
     * 若改为“逐条调用 replay”，{@code q}/{@code Q} 的作用域会被拆散并产生孤立 {@code Q}。
     * 因此统一采用单次遍历 + 钩子委托。
     *
     * @since 2.5.1
     */
    @FunctionalInterface
    public interface CommandHook {

        /**
         * 处理一条指令
         *
         * @param command 指令
         * @return true 表示已处理，回放应跳过该指令；false 表示由回放继续处理
         * @throws IOException 写入失败
         */
        boolean handle(ContentCommand command) throws IOException;
    }

    /**
     * 回放指令序列
     *
     * @param document  PDF 文档
     * @param stream    内容流
     * @param resources 资源字典
     * @param commands  指令序列
     * @throws IOException 写入失败
     */
    public void replay(PDDocument document, PDPageContentStream stream, PDResources resources,
                       List<ContentCommand> commands) throws IOException {
        replay(document, stream, resources, commands, null);
    }

    /**
     * 回放指令序列，可选的命令钩子优先处理部分指令
     * <p>
     * 单次遍历：{@code SaveState}/{@code RestoreState} 的深度在同一调用内累计，
     * 钩子委托不会打断 {@code q ... Q} 的作用域。
     *
     * @param document  PDF 文档
     * @param stream    内容流
     * @param resources 资源字典
     * @param commands  指令序列
     * @param hook      命令钩子，可为空（为空时与 4 参重载行为一致）
     * @throws IOException 写入失败
     */
    public void replay(PDDocument document, PDPageContentStream stream, PDResources resources,
                       List<ContentCommand> commands, CommandHook hook) throws IOException {
        if (commands == null) {
            return;
        }
        int depth = 0;
        for (ContentCommand command : commands) {
            try {
                if (hook != null && hook.handle(command)) {
                    continue;
                }
                if (command instanceof SaveState) {
                    stream.saveGraphicsState();
                    depth++;
                } else if (command instanceof RestoreState) {
                    if (depth > 0) {
                        stream.restoreGraphicsState();
                        depth--;
                    }
                } else if (command instanceof ConcatMatrix) {
                    org.ofdrw.converter.pdf.model.AffineMatrix m =
                            ((ConcatMatrix) command).getMatrix();
                    stream.transform(new org.apache.pdfbox.util.Matrix((float) m.getA(), (float) m.getB(),
                            (float) m.getC(), (float) m.getD(), (float) m.getE(), (float) m.getF()));
                } else if (command instanceof SetAlpha) {
                    SetAlpha alpha = (SetAlpha) command;
                    org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState state =
                            new org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState();
                    state.setNonStrokingAlphaConstant(alpha.getFillAlpha());
                    state.setStrokingAlphaConstant(alpha.getStrokeAlpha());
                    stream.setGraphicsStateParameters(state);
                } else if (command instanceof StrokeStyle) {
                    StrokeStyle style = (StrokeStyle) command;
                    applyStrokeStyle(stream, style);
                } else if (command instanceof RectCommand) {
                    RectCommand rect = (RectCommand) command;
                    setFillColor(stream, rect.getFillColor());
                    stream.addRect((float) rect.getRect().getX(), (float) rect.getRect().getY(),
                            (float) rect.getRect().getWidth(), (float) rect.getRect().getHeight());
                    stream.fill();
                } else if (command instanceof ClipCommand) {
                    // OFD 的 Clips 必须在绘制前落成 PDF 裁剪路径，否则超出的填充/图像会溢出图元（如徽标方框）
                    ClipCommand clip = (ClipCommand) command;
                    pathFactory.clip(stream, clip.getSegments(), clip.getFillRule());
                } else if (command instanceof PathCommand) {
                    PathCommand path = (PathCommand) command;
                    // 颜色自带的 Alpha 只作用于本次绘制：必须在绘制前落到图形状态，
                    // 并在绘制后恢复，否则会污染同一 q/Q 作用域内的后续图元
                    float fillAlpha = path.getFillColor() == null ? 1f : path.getFillColor().getOpacity();
                    float strokeAlpha = path.getStrokeColor() == null ? 1f : path.getStrokeColor().getOpacity();
                    logger.debug("[{}] PathCommand 颜色透明度 fill={}, stroke={}", "OFD2PDF", fillAlpha, strokeAlpha);
                    boolean alphaOverride = fillAlpha < 1f || strokeAlpha < 1f;
                    if (alphaOverride) {
                        stream.saveGraphicsState();
                        /*
                         * 透明度必须走 ExtGState（{@code /GSn gs}）：{@code ca}/{@code CA} 是图形状态字典的键，
                         * 不是内容流算子，裸写会让阅读器报 "Unknown operator" 并直接忽略透明度。
                         */
                        setAlphaState(stream, fillAlpha, strokeAlpha);
                    }
                    try {
                        if (path.getFillColor() != null) {
                            setFillColor(stream, path.getFillColor());
                        }
                        if (path.getStrokeColor() != null) {
                            setStrokeColor(stream, path.getStrokeColor());
                        }
                        pathFactory.append(stream, path.getSegments());
                        fillOrStroke(stream, path);
                    } finally {
                        if (alphaOverride) {
                            stream.restoreGraphicsState();
                        }
                    }
                } else if (command instanceof ShadingCommand) {
                    ShadingCommand shading = (ShadingCommand) command;
                    PDShading pdShading = new PdfShadingWriter(pathFactory).createShading(document, shading);
                    pathFactory.clip(stream, shading.getArea(), shading.getFillRule());
                    stream.shadingFill(pdShading);
                } else if (command instanceof PatternCommand) {
                    // PatternCommand 自带填充区域，必须写成 PDF Tiling Pattern 才能平铺；
                    // 直接回放单元内容会把图案只画一次（且落在错误位置）
                    patternWriter.write(document, stream, resources, (PatternCommand) command,
                            ((PatternCommand) command).getArea(), 1f, null, textWriter, fontRegistry);
                } else if (command instanceof ImageCommand) {
                    drawImage(document, stream, resources, (ImageCommand) command);
                } else if (command instanceof ImageBorderCommand) {
                    drawImageBorder(stream, (ImageBorderCommand) command);
                } else if (command instanceof MarkerCommand) {
                    stream.appendRawCommands("% " + ((MarkerCommand) command).getLabel() + "\n");
                } else {
                    logger.debug("[{}] 回放时忽略指令：{}", "OFD2PDF", command.getClass().getSimpleName());
                }
            } catch (IOException | RuntimeException e) {
                logger.warn("[{}] 指令回放失败，已跳过：{}（{}）", "OFD2PDF",
                        command.getClass().getSimpleName(), e.getMessage());
            }
        }
        // 兜底：保证 q/Q 成对
        while (depth-- > 0) {
            stream.restoreGraphicsState();
        }
    }

    /**
     * 把描边样式指令完整写入 PDF 图形状态
     * <p>
     * 写全五项（线宽、端点、连接、斜接限制、虚线）。其中虚线**即使是实线也要显式写入空数组**：
     * PDF 的虚线属于图形状态，若不显式复位，同一 {@code q/Q} 作用域内前一个图元设置的虚线会残留到后续图元，
     * 违反“不依赖隐式状态”的设计约束。
     *
     * @param stream 内容流
     * @param style  描边样式指令，线宽与虚线长度已换算为 pt
     * @throws IOException 写入失败
     */
    private static void applyStrokeStyle(PDPageContentStream stream, StrokeStyle style) throws IOException {
        stream.setLineWidth((float) style.getLineWidthPt());
        stream.setLineCapStyle(pdfCap(style.getLineCap()));
        stream.setLineJoinStyle(pdfJoin(style.getLineJoin()));
        stream.setMiterLimit((float) style.getMiterLimit());
        org.ofdrw.converter.pdf.model.DashPattern dash = style.getDashPattern();
        double[] array = dash == null ? null : dash.getDashArray();
        if (array == null || array.length == 0) {
            // 实线：显式复位，避免继承上一图元的虚线
            stream.setLineDashPattern(new float[0], 0f);
        } else {
            float[] dashes = new float[array.length];
            for (int i = 0; i < array.length; i++) {
                dashes[i] = (float) array[i];
            }
            stream.setLineDashPattern(dashes, (float) dash.getDashPhase());
        }
    }

    /**
     * 写入透明度图形状态（ExtGState）
     * <p>
     * {@code ca}/{@code CA} 是 ExtGState 字典中的键，必须通过 {@code /GSn gs} 引用；
     * 直接把 {@code 0.4 ca} 写进内容流属于非法算子，阅读器会忽略并保持不透明。
     *
     * @param stream    内容流
     * @param fillAlpha 非描边（填充）不透明度，0~1
     * @param strokeAlpha 描边不透明度，0~1
     * @throws IOException 写入失败
     */
    static void setAlphaState(PDPageContentStream stream, float fillAlpha, float strokeAlpha)
            throws IOException {
        org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState state =
                new org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState();
        state.setNonStrokingAlphaConstant(fillAlpha);
        state.setStrokingAlphaConstant(strokeAlpha);
        stream.setGraphicsStateParameters(state);
    }

    /**
     * 线端点样式 → PDF 的 {@code J} 操作符取值
     *
     * @param cap 线端点样式
     * @return 0 平头、1 圆头、2 方头
     */
    private static int pdfCap(org.ofdrw.converter.pdf.model.LineCap cap) {
        if (cap == org.ofdrw.converter.pdf.model.LineCap.ROUND) {
            return 1;
        }
        return cap == org.ofdrw.converter.pdf.model.LineCap.SQUARE ? 2 : 0;
    }

    /**
     * 线连接样式 → PDF 的 {@code j} 操作符取值
     *
     * @param join 线连接样式
     * @return 0 尖角、1 圆角、2 斜角
     */
    private static int pdfJoin(org.ofdrw.converter.pdf.model.LineJoin join) {
        if (join == org.ofdrw.converter.pdf.model.LineJoin.ROUND) {
            return 1;
        }
        return join == org.ofdrw.converter.pdf.model.LineJoin.BEVEL ? 2 : 0;
    }

    /**
     * 绘制图像指令
     * <p>
     * 保真优先（决策 23）：JPEG 原始字节以 {@code DCTDecode} 直接嵌入，不重新编码；
     * 其余编码由 PDFBox 按原格式生成 {@link PDImageXObject}。
     * 蒙版指令（{@link ImageCommand#isMasked()}）与同落点的主图配对，作为 {@code /SMask}。
     *
     * @param document  PDF 文档
     * @param stream    内容流
     * @param resources 资源字典（图像 XObject 登记于此）
     * @param command   图像指令
     * @throws IOException 写入失败
     */
    private void drawImage(PDDocument document, PDPageContentStream stream, PDResources resources,
                           ImageCommand command) throws IOException {
        if (command.isMasked()) {
            /*
             * 蒙版图像：渲染器为 ImageMask 单独产出一条同落点的指令，顺序为「蒙版 → 主图」。
             * 这里先缓存蒙版，等紧随的主图到达时合成 /SMask，蒙版本身不单独绘制
             * （否则会在页面上画出黑白的蒙版图）。
             */
            pendingMask = command;
            pendingMaskXObject = toImageXObject(document, command);
            return;
        }
        PDImageXObject image = imageCache.get(command.getResourceKey());
        if (image == null) {
            image = toImageXObject(document, command);
            if (pendingMaskXObject != null) {
                // 用蒙版建立 /SMask：Stencil 蒙版的 0 值透明、1 值不透明
                org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject mask = pendingMaskXObject;
                int width = mask.getWidth();
                int height = mask.getHeight();
                org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace gray =
                        org.apache.pdfbox.pdmodel.graphics.color.PDDeviceGray.INSTANCE;
                COSStream smaskStream = document.getDocument().createCOSStream();
                smaskStream.setItem(COSName.TYPE, COSName.XOBJECT);
                smaskStream.setItem(COSName.SUBTYPE, COSName.IMAGE);
                smaskStream.setInt(COSName.WIDTH, width);
                smaskStream.setInt(COSName.HEIGHT, height);
                smaskStream.setInt(COSName.BITS_PER_COMPONENT, 8);
                smaskStream.setItem(COSName.COLORSPACE, gray);
                smaskStream.setItem(COSName.FILTER, COSName.FLATE_DECODE);
                // 蒙版像素：非 0 → 不透明（255），0 → 透明（0）
                byte[] alphas = new byte[width * height];
                java.io.InputStream raw = mask.getStream().createInputStream();
                try {
                    int read = 0;
                    int value;
                    while (read < alphas.length && (value = raw.read()) >= 0) {
                        alphas[read++] = (byte) (value == 0 ? 0 : 255);
                    }
                } finally {
                    raw.close();
                }
                java.io.OutputStream out = smaskStream.createOutputStream(COSName.FLATE_DECODE);
                try {
                    out.write(alphas);
                } finally {
                    out.close();
                }
                image.getCOSObject().setItem(COSName.SMASK, smaskStream);
            }
            imageCache.put(command.getResourceKey(), image);
        }
        pendingMask = null;
        pendingMaskXObject = null;
        Box placement = command.getPlacement();
        /*
         * 放置矩阵描述的是「单位正方形 → 页面坐标」的完整变换，其中包含渲染阶段已完成的 Y 翻转与
         * mm→pt 缩放；PDF 的 Do 以图像单位正方形为用户空间、且自身坐标轴也由同一矩阵决定。
         * 因此这里只用 drawImage(x, y, w, h) 绘制外接矩形，旋转/镜像由渲染阶段写出的图像像素决定，
         * 不能再次施加放置矩阵（否则会二次翻转，图像上下颠倒）。
         */
        // 把图像登记到页面资源字典：PDFBox 的 drawImage 只写内联图像，资源字典需显式登记，
        // 否则 /SMask 等字典项与后续复用都无从引用
        registerImage(document, resources, command, image);
        AffineMatrix placementMatrix = command.getPlacementMatrix();
        if (placementMatrix != null && !isAxisAligned(placementMatrix, placement)) {
            // 旋转/镜像图像：cm 施加完整放置矩阵，再在单位正方形上绘制（drawImage 的 x/y/w/h 会丢失旋转）
            stream.saveGraphicsState();
            stream.transform(new org.apache.pdfbox.util.Matrix(
                    (float) placementMatrix.getA(), (float) placementMatrix.getB(),
                    (float) placementMatrix.getC(), (float) placementMatrix.getD(),
                    (float) placementMatrix.getE(), (float) placementMatrix.getF()));
            stream.drawImage(image, 0f, 0f, 1f, 1f);
            stream.restoreGraphicsState();
            return;
        }
        stream.drawImage(image, (float) placement.getX(), (float) placement.getY(),
                (float) placement.getWidth(), (float) placement.getHeight());
    }

    /**
     * 把已解码的图像登记到页面资源字典（同名资源已存在且对象相同时跳过）
     *
     * @param document  PDF 文档
     * @param resources 页面资源字典
     * @param command   图像指令
     * @param image     图像对象
     * @return 资源名
     */
    private COSName registerImage(PDDocument document, PDResources resources, ImageCommand command,
                                  PDImageXObject image) throws IOException {
        COSName name = COSName.getPDFName("Im" + Math.abs(command.getResourceKey().hashCode()));
        if (resources.getXObject(name) != image) {
            resources.put(name, image);
        }
        return name;
    }

    /**
     * 把图像登记到页面资源字典，返回资源名
     *
     * @param document  PDF 文档
     * @param resources 页面资源字典
     * @param command   图像指令
     * @return 资源名
     */
    private COSName registerImage(PDDocument document, PDResources resources, ImageCommand command) throws IOException {
        PDImageXObject image = imageCache.get(command.getResourceKey());
        if (image == null) {
            image = toImageXObject(document, command);
            imageCache.put(command.getResourceKey(), image);
        }
        for (COSName name : resources.getXObjectNames()) {
            if (resources.getXObject(name) == image) {
                return name;
            }
        }
        COSName name = COSName.getPDFName("Img" + (imageIndex++));
        resources.put(name, image);
        return name;
    }

    /**
     * 判断放置矩阵是否为轴对齐矩形（无旋转/镜像）
     *
     * @param matrix    放置矩阵，可为空
     * @param placement 外接矩形
     * @return true 表示可直接用 drawImage 绘制
     */
    private static boolean isAxisAligned(org.ofdrw.converter.pdf.model.AffineMatrix matrix, Box placement) {
        if (matrix == null) {
            return true;
        }
        double eps = 1e-6;
        boolean noSkew = Math.abs(matrix.getB()) < eps && Math.abs(matrix.getC()) < eps;
        // 放置矩阵由渲染阶段生成，已包含“OFD 的 Y 向下 → PDF 的 Y 向上”翻转，因此 d 恒为负；
        // 轴对齐判定不能要求 d > 0，否则所有普通图像都会走完整矩阵分支而被上下颠倒。
        boolean positiveX = matrix.getA() > 0;
        boolean matches = Math.abs(matrix.getA() - placement.getWidth()) < 1e-3
                && Math.abs(Math.abs(matrix.getD()) - placement.getHeight()) < 1e-3;
        return noSkew && positiveX && matches;
    }

    /**
     * 把图像指令转换为 PDF 图像对象
     *
     * @param document PDF 文档
     * @param command  图像指令
     * @return 图像对象
     * @throws IOException 图像解析失败
     */
    private static PDImageXObject toImageXObject(PDDocument document, ImageCommand command) throws IOException {
        byte[] data = command.getImageData();
        if (command.getEncoding() == ImageEncoding.JPEG) {
            return JPEGFactory.createFromStream(document, new ByteArrayInputStream(data));
        }
        if (command.getEncoding() == ImageEncoding.JPEG2000) {
            return rawImage(document, data, COSName.JPX_DECODE, command,
                    org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB.INSTANCE);
        }
        if (command.getEncoding() == ImageEncoding.JBIG2) {
            return jbig2Image(document, data, command);
        }
        if (command.getEncoding() == ImageEncoding.CCITT) {
            return rawImage(document, data, COSName.CCITTFAX_DECODE, command,
                    org.apache.pdfbox.pdmodel.graphics.color.PDDeviceGray.INSTANCE);
        }
        if (isGif(data)) {
            // GIF 是索引色格式，PDFBox 无法原样嵌入：按参考实现转为 RGB JPEG，并附加不透明 /SMask
            return gifImage(document, data);
        }
        // PNG 等其余格式交给 PDFBox 解码后无损重建（决策 23 的“其余无损”分支）
        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(data));
        if (bufferedImage == null) {
            throw new IOException("无法解码图像数据：" + command.getResourceKey());
        }
        /*
         * 带 alpha 通道但整体完全不透明的位图：参考实现会压平为 RGB 后按 JPEG（质量 0.75）嵌入，
         * 并附加全不透明 /SMask（与 GIF 分支同一策略）。真实含透明像素的位图仍走无损嵌入，
         * 否则透明区域与边缘抗锯齿会丢失。
         */
        if (isOpaqueAlpha(bufferedImage)) {
            return lossyOpaqueImage(document, bufferedImage);
        }
        return LosslessFactory.createFromImage(document, bufferedImage);
    }

    /**
     * 判断位图的 alpha 通道是否整体不透明
     *
     * @param image 已解码位图，不能为空
     * @return true 表示带 alpha 通道且所有像素 alpha 均为 255
     */
    private static boolean isOpaqueAlpha(BufferedImage image) {
        if (!image.getColorModel().hasAlpha()) {
            return false;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if ((image.getRGB(x, y) >>> 24) != 0xFF) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 压平为 RGB 后以 JPEG 嵌入，并附加全不透明 /SMask
     *
     * @param document PDF 文档
     * @param decoded  带 alpha 但不透明的位图
     * @return 图像对象
     * @throws IOException 写入失败
     */
    private static PDImageXObject lossyOpaqueImage(PDDocument document, BufferedImage decoded) throws IOException {
        int width = decoded.getWidth();
        int height = decoded.getHeight();
        BufferedImage rgb = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D graphics = rgb.createGraphics();
        try {
            graphics.setColor(java.awt.Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.drawImage(decoded, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        PDImageXObject image = JPEGFactory.createFromImage(document, rgb, 0.75f);
        image.getCOSObject().setItem(COSName.SMASK, opaqueSmask(document, width, height));
        return image;
    }

    /**
     * 判断是否为 GIF 数据
     *
     * @param data 图像原始字节
     * @return true 表示 GIF89a/GIF87a
     */
    private static boolean isGif(byte[] data) {
        return data != null && data.length > 6
                && data[0] == 'G' && data[1] == 'I' && data[2] == 'F'
                && data[3] == '8' && (data[4] == '7' || data[4] == '9') && data[5] == 'a';
    }

    /**
     * 将 GIF 图像转为 PDF 图像对象
     * <p>
     * 无透明通道时以 RGB JPEG 嵌入并附加全不透明 /SMask（与参考 PDF 的图像结构一致）；
     * 带透明通道时按无损方式嵌入，由 PDFBox 生成真实 /SMask。
     *
     * @param document PDF 文档
     * @param data     GIF 原始字节
     * @return 图像对象
     * @throws IOException 解码或写入失败
     */
    private static PDImageXObject gifImage(PDDocument document, byte[] data) throws IOException {
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(data));
        if (decoded == null) {
            throw new IOException("无法解码 GIF 图像");
        }
        if (decoded.getColorModel().hasAlpha()) {
            return LosslessFactory.createFromImage(document, decoded);
        }
        int width = decoded.getWidth();
        int height = decoded.getHeight();
        BufferedImage rgb = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D graphics = rgb.createGraphics();
        try {
            graphics.setColor(java.awt.Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.drawImage(decoded, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        PDImageXObject image = JPEGFactory.createFromImage(document, rgb, 0.75f);
        image.getCOSObject().setItem(COSName.SMASK, opaqueSmask(document, width, height));
        return image;
    }

    /**
     * 创建全不透明 /SMask
     *
     * @param document PDF 文档
     * @param width    宽度，单位像素
     * @param height   高度，单位像素
     * @return 灰度图像流，所有像素值为 255
     * @throws IOException 写入失败
     */
    private static COSStream opaqueSmask(PDDocument document, int width, int height) throws IOException {
        COSStream stream = document.getDocument().createCOSStream();
        stream.setItem(COSName.TYPE, COSName.XOBJECT);
        stream.setItem(COSName.SUBTYPE, COSName.IMAGE);
        stream.setInt(COSName.WIDTH, width);
        stream.setInt(COSName.HEIGHT, height);
        stream.setInt(COSName.BITS_PER_COMPONENT, 8);
        stream.setItem(COSName.COLORSPACE, org.apache.pdfbox.pdmodel.graphics.color.PDDeviceGray.INSTANCE);
        stream.setItem(COSName.FILTER, COSName.FLATE_DECODE);
        byte[] alphas = new byte[width * height];
        java.util.Arrays.fill(alphas, (byte) 0xFF);
        java.io.OutputStream out = stream.createOutputStream(COSName.FLATE_DECODE);
        try {
            out.write(alphas);
        } finally {
            out.close();
        }
        return stream;
    }

    /**
     * 嵌入 JBIG2 二值图像
     * <p>
     * PDF 7.4.7 规定 {@code JBIG2Decode} 内嵌流<b>不得包含 JBIG2 文件头</b>（8 字节魔数
     * {@code 97 4A 42 32 0D 0A 1A 0A} + 1 字节文件组织标志）。OFD 的 {@code .jb2} 资源是完整文件，
     * 直接原样嵌入会让解码器把魔数当成段类型（jbig2dec 报 “page has no image”），二维码因此不显示。
     *
     * @param document PDF 文档
     * @param data     JBIG2 资源字节
     * @param command  图像指令（提供像素尺寸）
     * @return 图像对象
     * @throws IOException 写入失败
     */
    private static PDImageXObject jbig2Image(PDDocument document, byte[] data, ImageCommand command)
            throws IOException {
        byte[] payload = stripJbig2FileHeader(data);
        PDImageXObject image = new PDImageXObject(document, new ByteArrayInputStream(payload),
                COSName.JBIG2_DECODE, command.getPixelWidth(), command.getPixelHeight(), 1,
                org.apache.pdfbox.pdmodel.graphics.color.PDDeviceGray.INSTANCE);
        return image;
    }

    /**
     * 去掉 JBIG2 文件头（魔数 + 文件组织标志）
     *
     * @param data JBIG2 资源字节
     * @return 去掉文件头后的段数据；非 JBIG2 文件或已剥离时原样返回
     */
    static byte[] stripJbig2FileHeader(byte[] data) {
        byte[] magic = {(byte) 0x97, 0x4A, 0x42, 0x32, 0x0D, 0x0A, 0x1A, 0x0A};
        if (data == null || data.length <= 9) {
            return data;
        }
        for (int i = 0; i < magic.length; i++) {
            if (data[i] != magic[i]) {
                return data;
            }
        }
        /*
         * 文件头 = 8 字节魔数 + 1 字节文件组织标志；顺序组织（标志最低位为 1）时，
         * 每个段头前还多 4 字节段号，PDF 内嵌流同样要求去掉它，否则解码器把段号当成段头解析
         * （jbig2dec 报 “Too few symbols in JBIG2 symbol dictionary”），二维码无法还原。
         */
        boolean sequential = (data[8] & 0x01) != 0;
        int header = 9 + (sequential ? 4 : 0);
        if (data.length <= header) {
            return data;
        }
        byte[] payload = new byte[data.length - header];
        System.arraycopy(data, header, payload, 0, payload.length);
        return payload;
    }

    /**
     * 以指定过滤器原样嵌入图像数据（不重新编码）
     *
     * @param document PDF 文档
     * @param data     原始编码字节
     * @param filter   PDF 过滤器名
     * @param command  图像指令（提供像素尺寸）
     * @param colorSpace 颜色空间
     * @return 图像对象
     * @throws IOException 写入失败
     */
    private static PDImageXObject rawImage(PDDocument document, byte[] data, COSName filter,
                                           ImageCommand command,
                                           org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace colorSpace)
            throws IOException {
        return new PDImageXObject(document, new ByteArrayInputStream(data), filter,
                command.getPixelWidth(), command.getPixelHeight(), 8, colorSpace);
    }

    /**
     * 绘制图像边框（圆角矩形路径 + 虚线 + 颜色，尺寸单位 pt）
     *
     * @param stream 内容流
     * @param border 边框指令
     * @throws IOException 写入失败
     */
    private static void drawImageBorder(PDPageContentStream stream, ImageBorderCommand border) throws IOException {
        if (border.getLineWidthPt() <= 0) {
            return;
        }
        stream.saveGraphicsState();
        applyStrokeStyle(stream, new StrokeStyle(border.getLineWidthPt(), LineCap.BUTT, LineJoin.MITER,
                10, border.getDashPattern()));
        if (border.getColor() != null) {
            setStrokeColor(stream, border.getColor());
        }
        Box rect = border.getRect();
        double radius = border.getEffectiveCornerRadiusPt();
        if (radius <= 0) {
            stream.addRect((float) rect.getX(), (float) rect.getY(),
                    (float) rect.getWidth(), (float) rect.getHeight());
        } else {
            appendRoundRect(stream, rect, radius);
        }
        stream.stroke();
        stream.restoreGraphicsState();
    }

    /**
     * 追加圆角矩形路径（四段 90° 圆弧，用三次贝塞尔近似）
     *
     * @param stream 内容流
     * @param rect   矩形，单位 pt
     * @param radius 圆角半径，单位 pt
     * @throws IOException 写入失败
     */
    private static void appendRoundRect(PDPageContentStream stream, Box rect, double radius) throws IOException {
        double x = rect.getX();
        double y = rect.getY();
        double w = rect.getWidth();
        double h = rect.getHeight();
        double r = Math.min(radius, Math.min(w, h) / 2);
        // 圆弧控制点系数：4/3 · tan(θ/4)，θ = 90°
        double k = r * 0.5522847498d;
        stream.moveTo((float) (x + r), (float) y);
        stream.lineTo((float) (x + w - r), (float) y);
        stream.curveTo((float) (x + w - r + k), (float) y, (float) (x + w), (float) (y + r - k), (float) (x + w), (float) (y + r));
        stream.lineTo((float) (x + w), (float) (y + h - r));
        stream.curveTo((float) (x + w), (float) (y + h - r + k), (float) (x + w - r + k), (float) (y + h), (float) (x + w - r), (float) (y + h));
        stream.lineTo((float) (x + r), (float) (y + h));
        stream.curveTo((float) (x + r - k), (float) (y + h), (float) x, (float) (y + h - r + k), (float) x, (float) (y + h - r));
        stream.lineTo((float) x, (float) (y + r));
        stream.curveTo((float) x, (float) (y + r - k), (float) (x + r - k), (float) y, (float) (x + r), (float) y);
        stream.closePath();
    }

    /**
     * 按填充/描边组合执行绘制
     *
     * @param stream 内容流
     * @param path   路径指令
     * @throws IOException 写入失败
     */
    private static void fillOrStroke(PDPageContentStream stream, PathCommand path) throws IOException {
        boolean evenOdd = path.getFillRule() == FillRule.EVEN_ODD;
        if (path.isFill() && path.isStroke()) {
            if (evenOdd) {
                stream.fillAndStrokeEvenOdd();
            } else {
                stream.fillAndStroke();
            }
        } else if (path.isFill()) {
            if (evenOdd) {
                stream.fillEvenOdd();
            } else {
                stream.fill();
            }
        } else {
            stream.stroke();
        }
    }

    /**
     * 设置填充颜色（保持原颜色空间）
     *
     * @param stream 内容流
     * @param color  颜色，可为空
     * @throws IOException 写入失败
     */
    static void setFillColor(PDPageContentStream stream, PdfColor color) throws IOException {
        if (color == null) {
            return;
        }
        switch (color.getColorSpace()) {
            case GRAY:
                stream.setNonStrokingColor(color.getChannel(0));
                break;
            case CMYK:
                stream.setNonStrokingColor(color.getChannel(0), color.getChannel(1),
                        color.getChannel(2), color.getChannel(3));
                break;
            case RGB:
            default:
                stream.setNonStrokingColor(color.getChannel(0), color.getChannel(1), color.getChannel(2));
                break;
        }
    }

    /**
     * 设置描边颜色（保持原颜色空间）
     *
     * @param stream 内容流
     * @param color  颜色，可为空
     * @throws IOException 写入失败
     */
    static void setStrokeColor(PDPageContentStream stream, PdfColor color) throws IOException {
        if (color == null) {
            return;
        }
        switch (color.getColorSpace()) {
            case GRAY:
                stream.setStrokingColor(color.getChannel(0));
                break;
            case CMYK:
                stream.setStrokingColor(color.getChannel(0), color.getChannel(1),
                        color.getChannel(2), color.getChannel(3));
                break;
            case RGB:
            default:
                stream.setStrokingColor(color.getChannel(0), color.getChannel(1), color.getChannel(2));
                break;
        }
    }
}
