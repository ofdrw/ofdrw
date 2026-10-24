package org.ofdrw.converter.export;

import org.ofdrw.converter.GeneralConvertException;
import org.ofdrw.converter.canvas.BufferedImageSurface;
import org.ofdrw.converter.canvas.CanvasImageMaker;
import org.ofdrw.converter.canvas.CanvasSurface;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.reader.OFDReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于像素画布抽象（{@link CanvasSurface}）的OFD图片导出器。
 * <p>
 * 相比 {@link ImageExporter}，本类提供：
 * <ul>
 *     <li>像素上限校验，超限快速失败，避免内存溢出；</li>
 *     <li>页面像素尺寸向上取整（{@code Math.ceil}），避免右侧、底部被裁掉；</li>
 *     <li>默认按页码命名（{@code page-0.png}），也可切换为按导出序号命名以兼容旧行为；</li>
 *     <li>JPG 质量参数、物理分辨率（DPI）元数据；</li>
 *     <li>背景色、透明背景与顺时针旋转 0/90/180/270 度；</li>
 *     <li>画布像素在使用后及时释放。</li>
 * </ul>
 * <p>
 * 使用方式：
 * <pre>
 *     try (CanvasImageExporter exporter = new CanvasImageExporter(ofdPath, outDir)) {
 *         exporter.setImageFormat("JPG");
 *         exporter.setJpegQuality(0.92f);
 *         exporter.setRotation(90);
 *         exporter.export();
 *     }
 * </pre>
 * <p>
 * 参数设置方法均应在调用 {@link #export(int...)} 之前调用。
 *
 * @author ofdrw
 * @since 2.5.0
 */
public class CanvasImageExporter implements OFDExporter {

    /**
     * 日志
     */
    private static final Logger logger = LoggerFactory.getLogger(CanvasImageExporter.class);

    /**
     * 默认每毫米像素数量（Pixels per millimeter）
     */
    public static final double DEFAULT_PPM = 15;

    /**
     * 默认 JPG 质量
     */
    public static final float DEFAULT_JPEG_QUALITY = 0.92f;

    /**
     * 默认单页像素上限，单位：像素
     */
    public static final long DEFAULT_MAX_PIXELS = 64000000L;

    /**
     * 默认文件名模式，占位符为页码
     */
    public static final String DEFAULT_FILENAME_PATTERN = "page-%d";

    /**
     * 未知输出格式时使用的文件扩展名
     */
    private static final String UNKNOWN_EXTENSION = "img";

    /**
     * PNG 元数据根节点名称
     */
    private static final String PNG_METADATA_NATIVE = "javax_imageio_png_1.0";

    /**
     * PNG aux 标准元数据根节点名称
     */
    private static final String PNG_METADATA_AUX_NATIVE = "javax_imageio_1.0";

    /**
     * 文件名模式中的整型占位符（支持 d/x/o/X 转换与宽度、标志修饰，如 %d、%03d）
     */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%(?:\\d+\\$)?[-#+ 0,(]*\\d*(?:\\.\\d+)?[dxoX]");

    /**
     * JPEG 元数据根节点名称
     */
    private static final String JPEG_METADATA_NATIVE = "javax_imageio_jpeg_image_1.0";

    /**
     * OFD解析器
     */
    private final OFDReader ofdReader;

    /**
     * 是否由本类创建并持有 OFDReader 的所有权
     * <p>
     * 外部传入的 OFDReader 不由本类关闭。
     */
    private final boolean ownsReader;

    /**
     * 图片输出目录
     */
    private final Path outDirPath;

    /**
     * 画布图片绘制器
     */
    private final CanvasImageMaker imageMaker;

    /**
     * 输出图片格式（已统一为大写），如 PNG、JPG、BMP
     */
    private volatile String imageFormat;

    /**
     * JPG 质量，取值 0.0–1.0
     */
    private volatile float jpegQuality = DEFAULT_JPEG_QUALITY;

    /**
     * 背景色，默认白色；为 null 表示透明（仅 PNG 有效）
     */
    private volatile Color background = Color.WHITE;

    /**
     * 顺时针旋转角度，取值 0/90/180/270，单位：度
     */
    private volatile int rotation = 0;

    /**
     * 单页像素上限，单位：像素；不大于 0 表示不限制
     */
    private volatile long maxPixels = DEFAULT_MAX_PIXELS;

    /**
     * 文件名模式，使用 String.format 语义，必须恰好一个整型占位符
     */
    private volatile String filenamePattern = DEFAULT_FILENAME_PATTERN;

    /**
     * 文件名占位符取值基准
     */
    private volatile IndexBase filenameIndexBase = IndexBase.PAGE;

    /**
     * 是否已经关闭
     */
    private volatile boolean closed = false;

    /**
     * 已导出的图片文件序列，顺序与导出顺序一致
     */
    private final List<Path> imgFileArr = new ArrayList<>();

    /**
     * 最近一次导出的图片信息
     */
    private final List<ImageInfo> imageInfos = new ArrayList<>();

    /**
     * 构造图片导出器，使用默认格式 PNG 与默认 {@link #DEFAULT_PPM 分辨率}。
     *
     * @param ofdFile 待转换OFD文件
     * @param outDir  生成图片存放目录，不存在时自动创建
     * @throws IOException 文件解析失败
     */
    public CanvasImageExporter(Path ofdFile, Path outDir) throws IOException {
        this(ofdFile, outDir, "PNG", DEFAULT_PPM);
    }

    /**
     * 构造图片导出器，使用默认格式 PNG 与默认 {@link #DEFAULT_PPM 分辨率}。
     *
     * @param ofdInput 待转换OFD文件流，该流由调用者负责关闭
     * @param outDir   生成图片存放目录，不存在时自动创建
     * @throws IOException 文件解析失败
     */
    public CanvasImageExporter(InputStream ofdInput, Path outDir) throws IOException {
        this(ofdInput, outDir, "PNG", DEFAULT_PPM);
    }

    /**
     * 构造图片导出器。
     *
     * @param ofdFile    待转换OFD文件
     * @param outDir     生成图片存放目录，不存在时自动创建
     * @param imageFormat 生成图片格式，支持 PNG / JPG / BMP（大小写不敏感）
     * @param ppm        每毫米像素数量（Pixels per millimeter），不大于 0 时使用默认值
     * @throws IOException 文件解析失败
     */
    public CanvasImageExporter(Path ofdFile, Path outDir, String imageFormat, double ppm) throws IOException {
        if (ofdFile == null) {
            throw new IllegalArgumentException("待转换OFD文件路径为空");
        }
        this.ofdReader = new OFDReader(ofdFile);
        this.ownsReader = true;
        this.outDirPath = prepareOutDir(outDir);
        this.imageMaker = createMaker(ppm);
        setImageFormat(imageFormat);
        if (ppm <= 0) {
            logger.warn("PPM {} 非法，使用默认值 {}", ppm, DEFAULT_PPM);
        }
    }

    /**
     * 构造图片导出器。
     *
     * @param ofdInput    待转换OFD文件流，该流由调用者负责关闭
     * @param outDir      生成图片存放目录，不存在时自动创建
     * @param imageFormat 生成图片格式，支持 PNG / JPG / BMP（大小写不敏感）
     * @param ppm         每毫米像素数量（Pixels per millimeter），不大于 0 时使用默认值
     * @throws IOException 文件解析失败
     */
    public CanvasImageExporter(InputStream ofdInput, Path outDir, String imageFormat, double ppm) throws IOException {
        if (ofdInput == null) {
            throw new IllegalArgumentException("待转换OFD文件流为空");
        }
        this.ofdReader = new OFDReader(ofdInput);
        this.ownsReader = true;
        this.outDirPath = prepareOutDir(outDir);
        this.imageMaker = createMaker(ppm);
        setImageFormat(imageFormat);
        if (ppm <= 0) {
            logger.warn("PPM {} 非法，使用默认值 {}", ppm, DEFAULT_PPM);
        }
    }

    /**
     * 构造图片导出器，复用外部传入的 OFD 解析器。
     * <p>
     * <b>归属约定</b>：外部传入的 OFD 解析器由调用方负责关闭，{@link #close()} 不会关闭该解析器。
     *
     * @param reader      外部 OFD 解析器，不能为空，由调用方负责关闭
     * @param outDir      生成图片存放目录，不存在时自动创建
     * @param imageFormat 生成图片格式，支持 PNG / JPG / BMP（大小写不敏感）
     * @param ppm         每毫米像素数量（Pixels per millimeter），不大于 0 时使用默认值
     * @throws IOException 目录创建失败
     */
    public CanvasImageExporter(OFDReader reader, Path outDir, String imageFormat, double ppm) throws IOException {
        if (reader == null) {
            throw new IllegalArgumentException("OFD解析器为空");
        }
        this.ofdReader = reader;
        this.ownsReader = false;
        this.outDirPath = prepareOutDir(outDir);
        this.imageMaker = createMaker(ppm);
        setImageFormat(imageFormat);
        if (ppm <= 0) {
            logger.warn("PPM {} 非法，使用默认值 {}", ppm, DEFAULT_PPM);
        }
    }

    /**
     * 校验并创建输出目录。
     *
     * @param outDir 输出目录
     * @return 输出目录的绝对路径
     * @throws IOException 目录创建失败
     */
    private static Path prepareOutDir(Path outDir) throws IOException {
        if (outDir == null) {
            throw new IllegalArgumentException("导出图片文件路径为空");
        }
        Path dir = outDir.toAbsolutePath();
        if (Files.exists(dir) && !Files.isDirectory(dir)) {
            throw new IllegalArgumentException(String.format("图片输出路径已存在同名文件：%s", dir));
        }
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        return dir;
    }

    /**
     * 创建画布图片绘制器。
     *
     * @param ppm 每毫米像素数量，单位：像素/毫米，不大于 0 时使用默认值
     * @return 画布图片绘制器
     */
    private CanvasImageMaker createMaker(double ppm) {
        double actualPpm = ppm <= 0 ? DEFAULT_PPM : ppm;
        CanvasImageMaker maker = new CanvasImageMaker(ofdReader, actualPpm);
        // 导出图片不绘制页面边界
        maker.config.setDrawBoundary(false);
        return maker;
    }

    /**
     * 导出指定OFD页为图片。
     * <p>
     * 页码越界时跳过；页码序列可以乱序或重复。
     *
     * @param indexes 页码序列，如果为空表示全部页码（注意：页码从0起）
     * @throws GeneralConvertException 导出失败，如导出器已关闭、像素超过上限、编码失败
     */
    @Override
    public void export(int... indexes) throws GeneralConvertException {
        if (closed) {
            throw new GeneralConvertException("导出器已关闭");
        }
        List<Integer> targetPages = normalizePages(indexes);
        // 本次导出开始前清空结果，避免重复调用时累积
        imgFileArr.clear();
        imageInfos.clear();
        String format = imageFormat;
        for (int sequence = 0; sequence < targetPages.size(); sequence++) {
            int pageIndex = targetPages.get(sequence);
            exportPage(pageIndex, sequence, format);
        }
    }

    /**
     * 规范化待导出页码序列。
     *
     * @param indexes 调用方传入的页码序列，可为空
     * @return 待导出页码序列，页码从 0 起，按入参顺序保留
     */
    private List<Integer> normalizePages(int[] indexes) {
        int pageCount = ofdReader.getNumberOfPages();
        List<Integer> targetPages = new LinkedList<>();
        if (indexes == null || indexes.length == 0) {
            for (int i = 0; i < pageCount; i++) {
                targetPages.add(i);
            }
            return targetPages;
        }
        for (int index : indexes) {
            // 越界页码跳过，沿用 OFDExporter 既有约定
            if (index < 0 || index >= pageCount) {
                logger.warn("页码 {} 超出范围 [0, {})，已跳过", index, pageCount);
                continue;
            }
            targetPages.add(index);
        }
        return targetPages;
    }

    /**
     * 导出一页图片。
     *
     * @param pageIndex 页码，从 0 起
     * @param sequence  本次导出中的序号，从 0 起
     * @param format    输出格式，已统一为大写
     * @throws GeneralConvertException 像素超限、编码失败或写入失败
     */
    private void exportPage(int pageIndex, int sequence, String format) {
        double ppm = imageMaker.getPPM();
        int degree = rotation;
        // 页面尺寸，单位：毫米
        ST_Box pageBox = imageMaker.pageSize(pageIndex);
        // 毫米 转 像素，向上取整避免右侧/底部被裁掉
        int baseW = Math.max(1, (int) Math.ceil(ppm * pageBox.getWidth()));
        int baseH = Math.max(1, (int) Math.ceil(ppm * pageBox.getHeight()));
        boolean swap = degree == 90 || degree == 270;
        int outW = swap ? baseH : baseW;
        int outH = swap ? baseW : baseH;
        // 像素上限按旋转后的输出尺寸判断
        checkMaxPixels(pageIndex, outW, outH);

        // 透明仅在 PNG 下有效，JPG/BMP 不支持透明通道
        Color effectiveBackground = background;
        if (effectiveBackground == null && !supportsAlpha(format)) {
            effectiveBackground = Color.WHITE;
        }

        Path dst = resolveOutputPath(pageIndex, sequence, format);
        // 未旋转方向渲染，尺寸由绘制器内部按 ppm 缩放，整图旋转在编码前完成
        CanvasSurface surface = CanvasSurface.create(baseW, baseH, effectiveBackground);
        BufferedImage image = null;
        try {
            imageMaker.drawPage(surface, pageIndex);
            CanvasSurface output = surface;
            if (degree != 0) {
                output = rotateSurface(surface, degree, effectiveBackground);
            }
            try {
                image = toEncodableImage(output, format, effectiveBackground);
            } finally {
                if (output != surface) {
                    output.flush();
                }
            }
            try {
                encode(image, dst, format, ppm);
            } finally {
                image.flush();
            }
        } catch (IOException e) {
            throw new GeneralConvertException(String.format("图片导出失败，页码：%d，文件：%s", pageIndex, dst), e);
        } finally {
            surface.flush();
        }
        imgFileArr.add(dst);
        imageInfos.add(new ImageInfo(pageIndex, outW, outH, degree, ppm, dst));
    }

    /**
     * 对已完成绘制的画布做整图像素级顺时针旋转，生成新画布。
     * <p>
     * 布局层在绘制元素时会重置画布变换，因此旋转只能在整页绘制完成后对像素做，避免文字与矢量图形不跟随旋转。
     * 新画布先按背景色铺满，避免旋转后边角出现未初始化的黑色。
     *
     * @param source     原始画布（未旋转方向）
     * @param degree     顺时针旋转角度，取值 90/180/270，单位：度
     * @param background 背景色，为 null 表示保持透明
     * @return 旋转后的新画布，90/270 度时宽高互换
     */
    private static CanvasSurface rotateSurface(CanvasSurface source, int degree, Color background) {
        int sourceW = source.getWidth();
        int sourceH = source.getHeight();
        boolean swap = degree == 90 || degree == 270;
        int outW = swap ? sourceH : sourceW;
        int outH = swap ? sourceW : sourceH;
        CanvasSurface target = CanvasSurface.create(outW, outH, background);
        Graphics2D g = target.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            switch (degree) {
                case 90:
                    // 顺时针 90 度：原图左上角落到输出画布右上角
                    g.translate(outW, 0);
                    g.rotate(Math.PI / 2);
                    break;
                case 180:
                    g.translate(outW, outH);
                    g.rotate(Math.PI);
                    break;
                case 270:
                    // 顺时针 270 度：原图左上角落到输出画布左下角
                    g.translate(0, outH);
                    g.rotate(3 * Math.PI / 2);
                    break;
                default:
                    break;
            }
            g.drawImage(surfaceImage(source), 0, 0, null);
        } finally {
            g.dispose();
        }
        return target;
    }

    /**
     * 获取画布的像素图像。
     *
     * @param surface 画布
     * @return 画布底层位图
     * @throws GeneralConvertException 画布不支持像素读取
     */
    private static BufferedImage surfaceImage(CanvasSurface surface) {
        if (!(surface instanceof BufferedImageSurface)) {
            throw new GeneralConvertException(String.format("画布实现 %s 不支持像素读取", surface.getClass().getName()));
        }
        return ((BufferedImageSurface) surface).getImage();
    }

    /**
     * 校验单页像素数量是否超过上限。
     *
     * @param pageIndex 页码，从 0 起
     * @param width     输出宽度，单位：像素
     * @param height    输出高度，单位：像素
     * @throws GeneralConvertException 像素数量超过上限
     */
    private void checkMaxPixels(int pageIndex, int width, int height) {
        long limit = maxPixels;
        if (limit <= 0) {
            return;
        }
        long pixels = (long) width * (long) height;
        if (pixels > limit) {
            throw new GeneralConvertException(String.format(
                    "页码 %d 的图片尺寸 %d x %d（%d 像素）超过像素上限 %d 像素，"
                            + "可以通过 setPPM 降低分辨率或 setMaxPixels 调整上限",
                    pageIndex, width, height, pixels, limit));
        }
    }

    /**
     * 将画布像素转换为可编码的位图。
     * <p>
     * JPG/BMP 不支持透明通道，非 PNG 输出统一合成到 RGB 位图上，避免透明像素变成黑块。
     *
     * @param surface      已完成绘制的画布
     * @param format       输出格式，已统一为大写
     * @param background   背景色，为 null 表示透明
     * @return 编码用位图，与画布同尺寸
     * @throws GeneralConvertException 画布不支持像素读取
     */
    private static BufferedImage toEncodableImage(CanvasSurface surface, String format, Color background) {
        BufferedImage source = surfaceImage(surface);
        if (supportsAlpha(format)) {
            return source;
        }
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            // 先铺底色，再把画布内容覆盖上去
            g.setComposite(AlphaComposite.Src);
            g.setColor(background == null ? Color.WHITE : background);
            g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            g.setComposite(AlphaComposite.SrcOver);
            g.drawImage(source, 0, 0, null);
        } finally {
            g.dispose();
        }
        return rgb;
    }

    /**
     * 编码并写入图片文件。
     * <p>
     * 使用 {@link ImageWriter} 以便设置 JPG 质量与物理分辨率元数据；
     * 元数据写入失败时降级为无元数据输出并告警，不中断导出。
     *
     * @param image    待编码位图
     * @param dst      目标文件
     * @param format   输出格式，已统一为大写
     * @param ppm      每毫米像素数量，单位：像素/毫米
     * @throws IOException 文件写入失败
     */
    private void encode(BufferedImage image, Path dst, String format, double ppm) throws IOException {
        String writerFormat = "JPG".equals(format) ? "JPEG" : format;
        ImageWriter writer = ImageIO.getImageWritersByFormatName(writerFormat).next();
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            // 仅 JPG 设置压缩质量；PNG 的 Deflate 与 BMP 的 RLE 不适用质量参数
            if (param.canWriteCompressed() && "JPEG".equals(writerFormat)) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                String[] compressionTypes = param.getCompressionTypes();
                if (compressionTypes != null && compressionTypes.length > 0) {
                    param.setCompressionType(compressionTypes[0]);
                }
                param.setCompressionQuality(jpegQuality);
            }
            IIOMetadata metadata = buildMetadata(writer, param, image, writerFormat, ppm);
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(dst.toFile())) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(image, null, metadata), param);
            }
        } finally {
            writer.dispose();
        }
    }

    /**
     * 构造带物理分辨率元数据的图片元数据。
     * <p>
     * 元数据构造失败时返回 null（使用写入器默认元数据）并告警，不中断导出。
     *
     * @param writer        图片写入器
     * @param param         写入参数
     * @param image         待编码位图
     * @param writerFormat  写入器格式名，如 PNG、JPEG、BMP
     * @param ppm           每毫米像素数量，单位：像素/毫米
     * @return 图片元数据，或 null 表示使用默认元数据
     */
    private IIOMetadata buildMetadata(ImageWriter writer, ImageWriteParam param, BufferedImage image,
                                      String writerFormat, double ppm) {
        try {
            ImageTypeSpecifier type = ImageTypeSpecifier.createFromRenderedImage(image);
            IIOMetadata metadata = writer.getDefaultImageMetadata(type, param);
            String nativeFormat = metadata.getNativeMetadataFormatName();
            if (nativeFormat == null) {
                return metadata;
            }
            if (PNG_METADATA_NATIVE.equals(nativeFormat)) {
                if (!applyPngPixelSize(metadata, nativeFormat, ppm)) {
                    logger.warn("PNG 分辨率元数据写入失败，降级为无元数据输出");
                    return null;
                }
            } else if (JPEG_METADATA_NATIVE.equals(nativeFormat)) {
                if (!applyJpegDpi(metadata, nativeFormat, ppm)) {
                    logger.warn("JPEG 分辨率元数据写入失败，降级为无元数据输出");
                    return null;
                }
            }
            return metadata;
        } catch (Exception e) {
            logger.warn("图片分辨率元数据构造失败，降级为无元数据输出：{}", writerFormat, e);
            return null;
        }
    }

    /**
     * 写入 PNG 的物理像素尺寸元数据。
     * <p>
     * pHYs 节点中一个像素的宽度、高度以米为单位，取整后写入；先尝试标准格式树，
     * 失败时按规范重写 aux 标准元数据节点。分辨率非法或无需求时不做处理，视为成功。
     *
     * @param metadata    PNG 图片元数据
     * @param nativeFormat PNG 原生元数据格式名
     * @param ppm         每毫米像素数量，单位：像素/毫米
     * @return true 表示写入成功或无需写入
     */
    private static boolean applyPngPixelSize(IIOMetadata metadata, String nativeFormat, double ppm) {
        if (ppm <= 0) {
            return true;
        }
        // 一个像素的宽度，单位：米/像素
        double pixelSizeM = 1 / (ppm * 1000);
        // 每米像素数量，PHYs 以整数存储
        int pixelsPerUnit = (int) Math.round(1 / pixelSizeM);
        if (pixelsPerUnit <= 0) {
            logger.warn("PPM {} 过低，无法写入 PNG 分辨率元数据", ppm);
            return true;
        }
        try {
            IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(nativeFormat);
            root.appendChild(buildPngPhysNode(pixelsPerUnit));
            metadata.setFromTree(nativeFormat, root);
            return true;
        } catch (IIOInvalidTreeException | RuntimeException e) {
            logger.debug("PNG 标准格式树写入 pHYs 失败，改用 aux 标准节点", e);
        }
        try {
            metadata.mergeTree(nativeFormat, buildPngAuxMetadataRoot(pixelsPerUnit));
            return true;
        } catch (IIOInvalidTreeException | RuntimeException e) {
            logger.debug("PNG aux 标准节点写入失败", e);
            return false;
        }
    }

    /**
     * 构造 PNG 标准格式树中的 pHYs 节点。
     *
     * @param pixelsPerUnit 每米像素数量，单位：像素/米
     * @return pHYs 元数据节点
     */
    private static IIOMetadataNode buildPngPhysNode(int pixelsPerUnit) {
        IIOMetadataNode pHYs = new IIOMetadataNode("pHYs");
        pHYs.setAttribute("pixelsPerUnitXAxis", Integer.toString(pixelsPerUnit));
        pHYs.setAttribute("pixelsPerUnitYAxis", Integer.toString(pixelsPerUnit));
        pHYs.setAttribute("unitSpecifier", "meter");
        return pHYs;
    }

    /**
     * 构造包含 pHYs 的 PNG aux 标准元数据根节点。
     *
     * @param pixelsPerUnit 每米像素数量，单位：像素/米
     * @return aux 标准元数据根节点
     */
    private static IIOMetadataNode buildPngAuxMetadataRoot(int pixelsPerUnit) {
        IIOMetadataNode phys = new IIOMetadataNode("tEXtEntry");
        phys.setAttribute("keyword", "pHYs");
        phys.setAttribute("value", pixelsPerUnit + "," + pixelsPerUnit + ",meter");
        IIOMetadataNode text = new IIOMetadataNode("tEXt");
        text.appendChild(phys);
        IIOMetadataNode aux = new IIOMetadataNode(PNG_METADATA_AUX_NATIVE);
        aux.appendChild(text);
        return aux;
    }

    /**
     * 写入 JPEG 的 JFIF 像素密度元数据。
     * <p>
     * JFIF 以“每英寸像素数”表示密度，由 ppm 换算得到 DPI。
     *
     * @param metadata    JPEG 图片元数据
     * @param nativeFormat JPEG 原生元数据格式名
     * @param ppm          每毫米像素数量，单位：像素/毫米
     * @return true 表示写入成功或无需写入
     */
    private static boolean applyJpegDpi(IIOMetadata metadata, String nativeFormat, double ppm) {
        if (ppm <= 0) {
            return true;
        }
        int dpi = (int) Math.round(ppm * 25.4);
        if (dpi <= 0) {
            logger.warn("PPM {} 过低，无法写入 JPEG 分辨率元数据", ppm);
            return true;
        }
        try {
            IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(nativeFormat);
            IIOMetadataNode jfif = findChild(root, "JPEGvariety");
            if (jfif == null) {
                jfif = new IIOMetadataNode("JPEGvariety");
                root.appendChild(jfif);
            }
            IIOMetadataNode app0 = findChild(jfif, "app0JFIF");
            if (app0 == null) {
                app0 = new IIOMetadataNode("app0JFIF");
                app0.setAttribute("majorVersion", "1");
                app0.setAttribute("minorVersion", "2");
                app0.setAttribute("thumbWidth", "0");
                app0.setAttribute("thumbHeight", "0");
                jfif.appendChild(app0);
            }
            app0.setAttribute("resUnits", "1");
            app0.setAttribute("Xdensity", Integer.toString(dpi));
            app0.setAttribute("Ydensity", Integer.toString(dpi));
            metadata.setFromTree(nativeFormat, root);
            return true;
        } catch (IIOInvalidTreeException | RuntimeException e) {
            logger.debug("JPEG 标准格式树写入 JFIF 密度失败", e);
            return false;
        }
    }

    /**
     * 按节点名查找子节点。
     *
     * @param parent   父节点
     * @param nodeName 子节点名称
     * @return 匹配的子节点，不存在时返回 null
     */
    private static IIOMetadataNode findChild(IIOMetadataNode parent, String nodeName) {
        for (int i = 0; i < parent.getLength(); i++) {
            if (nodeName.equalsIgnoreCase(parent.item(i).getNodeName())) {
                return (IIOMetadataNode) parent.item(i);
            }
        }
        return null;
    }

    /**
     * 解析单页输出文件路径。
     *
     * @param pageIndex 页码，从 0 起
     * @param sequence  本次导出中的序号，从 0 起
     * @param format    输出格式，已统一为大写
     * @return 输出文件路径
     */
    private Path resolveOutputPath(int pageIndex, int sequence, String format) {
        int index = filenameIndexBase == IndexBase.SEQUENCE ? sequence : pageIndex;
        String baseName = String.format(filenamePattern, index);
        Path dst = outDirPath.resolve(baseName + "." + extensionOf(format));
        if (!dst.normalize().getParent().equals(outDirPath)) {
            throw new GeneralConvertException(String.format("文件名模式 %s 生成的路径 %s 不在输出目录内", filenamePattern, dst));
        }
        return dst;
    }

    /**
     * 判断格式是否支持透明通道。
     *
     * @param format 输出格式，已统一为大写
     * @return true 表示支持透明（仅 PNG）
     */
    private static boolean supportsAlpha(String format) {
        return "PNG".equals(format);
    }

    /**
     * 获取格式对应的文件扩展名（小写）。
     *
     * @param format 输出格式，已统一为大写
     * @return 文件扩展名
     */
    private static String extensionOf(String format) {
        if ("JPG".equals(format)) {
            return "jpg";
        }
        if ("PNG".equals(format)) {
            return "png";
        }
        if ("BMP".equals(format)) {
            return "bmp";
        }
        return UNKNOWN_EXTENSION;
    }

    /**
     * 规范化输出格式名称，做别名归一（jpeg → jpg），并校验是否受支持。
     *
     * @param format 调用方传入的格式名称
     * @return 规范化后的格式名称（大写）
     * @throws IllegalArgumentException 格式为空或不支持
     */
    private static String normalizeFormat(String format) {
        if (format == null || format.trim().isEmpty()) {
            throw new IllegalArgumentException("图片格式为空");
        }
        String upper = format.trim().toUpperCase(Locale.ROOT);
        if ("JPEG".equals(upper)) {
            upper = "JPG";
        }
        if (!"PNG".equals(upper) && !"JPG".equals(upper) && !"BMP".equals(upper)) {
            throw new IllegalArgumentException(String.format("不支持的图片格式：%s，仅支持 PNG、JPG、BMP", format));
        }
        return upper;
    }

    /**
     * 设置每毫米像素数量。
     * <p>
     * 请在调用 {@link #export(int...)} 之前设置。
     *
     * @param ppm 每毫米像素数量（Pixels per millimeter），不大于 0 时忽略
     */
    public void setPPM(double ppm) {
        if (ppm <= 0) {
            logger.warn("PPM {} 非法，设置被忽略", ppm);
            return;
        }
        imageMaker.setPPM(ppm);
    }

    /**
     * 设置输出图片格式，大小写不敏感。
     * <p>
     * 支持 PNG、JPG（别名 JPEG）、BMP。请在调用 {@link #export(int...)} 之前设置。
     *
     * @param imageFormat 图片格式
     * @throws IllegalArgumentException 格式为空或不支持
     */
    public void setImageFormat(String imageFormat) {
        this.imageFormat = normalizeFormat(imageFormat);
    }

    /**
     * 设置 JPG 质量，取值 0.0–1.0，默认 0.92。
     * <p>
     * 质量参数仅对 JPG 生效；PNG、BMP 会忽略该参数。请在调用 {@link #export(int...)} 之前设置。
     *
     * @param quality 质量，单位：无量纲，取值 0.0–1.0，越界时取边界值
     */
    public void setJpegQuality(float quality) {
        if (Float.isNaN(quality)) {
            logger.warn("JPG 质量为 NaN，设置被忽略");
            return;
        }
        float actual = Math.max(0f, Math.min(1f, quality));
        if (actual != quality) {
            logger.warn("JPG 质量 {} 超出范围 [0, 1]，已修正为 {}", quality, actual);
        }
        this.jpegQuality = actual;
    }

    /**
     * 设置背景色。
     * <p>
     * 为 null 表示透明背景，仅 PNG 有效；JPG、BMP 不支持透明通道，null 时按白色背景处理。
     * 请在调用 {@link #export(int...)} 之前设置。
     *
     * @param background 背景色，null 表示透明
     */
    public void setBackground(Color background) {
        this.background = background;
    }

    /**
     * 设置顺时针旋转角度。
     * <p>
     * 仅接受 0、90、180、270（负值与 360 的倍数会归一化，如 -90 等价于 270）；其余值忽略并告警。
     *
     * @param degree 顺时针旋转角度，单位：度
     */
    public void setRotation(int degree) {
        int normalized = ((degree % 360) + 360) % 360;
        if (normalized != 0 && normalized != 90 && normalized != 180 && normalized != 270) {
            logger.warn("旋转角度 {} 非法，仅支持 0/90/180/270，设置被忽略", degree);
            return;
        }
        this.rotation = normalized;
    }

    /**
     * 设置单页像素上限，默认 {@link #DEFAULT_MAX_PIXELS}。
     * <p>
     * 单页输出像素数（宽 × 高）超过上限时，导出抛出 {@link GeneralConvertException}。
     * 不大于 0 表示不限制。请在调用 {@link #export(int...)} 之前设置。
     *
     * @param maxPixels 单页像素上限，单位：像素
     */
    public void setMaxPixels(long maxPixels) {
        this.maxPixels = maxPixels;
    }

    /**
     * 设置文件名模式，使用 {@link String#format(String, Object...)} 语义。
     * <p>
     * 必须恰好包含一个整型占位符（如 {@code %d}、{@code %02d}），扩展名由图片格式决定。
     * 默认 {@code page-%d}。请在调用 {@link #export(int...)} 之前设置。
     *
     * @param pattern 文件名模式，如 {@code page-%d}
     * @throws IllegalArgumentException 模式为空、占位符数量不为 1 或格式非法
     */
    public void setFilenamePattern(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            throw new IllegalArgumentException("文件名模式为空");
        }
        Matcher placeholder = PLACEHOLDER_PATTERN.matcher(pattern);
        if (!placeholder.find()) {
            throw new IllegalArgumentException(String.format("文件名模式 %s 必须包含一个整型占位符", pattern));
        }
        if (placeholder.find()) {
            throw new IllegalArgumentException(String.format("文件名模式 %s 必须恰好包含一个整型占位符", pattern));
        }
        String sample = String.format(pattern, 0);
        if (sample.contains("/") || sample.contains("\\")) {
            throw new IllegalArgumentException(String.format("文件名模式 %s 不能包含路径分隔符", pattern));
        }
        this.filenamePattern = pattern;
    }

    /**
     * 设置文件名占位符取值基准。
     * <p>
     * {@link IndexBase#PAGE}（默认）代入页码，{@link IndexBase#SEQUENCE} 代入本次导出的序号。
     * 请在调用 {@link #export(int...)} 之前设置。
     *
     * @param base 取值基准，null 时使用 {@link IndexBase#PAGE}
     */
    public void setFilenameIndexBase(IndexBase base) {
        this.filenameIndexBase = base == null ? IndexBase.PAGE : base;
    }

    /**
     * 获取已导出图片路径，顺序与导出顺序一致。
     *
     * @return 图片路径列表
     */
    public List<Path> getImgFilePaths() {
        return Collections.unmodifiableList(imgFileArr);
    }

    /**
     * 获取最近一次导出的图片尺寸与导出参数快照。
     *
     * @return 图片信息列表
     */
    public List<ImageInfo> getImageInfos() {
        return Collections.unmodifiableList(imageInfos);
    }

    /**
     * 关闭导出器。
     * <p>
     * 仅当 OFD 解析器由本类创建时才会被关闭；外部通过构造器传入的解析器由调用方负责关闭。
     *
     * @throws IOException 关闭失败
     */
    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        if (ownsReader && ofdReader != null) {
            ofdReader.close();
        }
    }
}
