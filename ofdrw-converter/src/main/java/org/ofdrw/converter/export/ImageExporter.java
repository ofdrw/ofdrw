package org.ofdrw.converter.export;

import org.ofdrw.converter.GeneralConvertException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

/**
 * OFD图片转换器。
 * <p>
 * <b>已废弃</b>：请改用 {@link CanvasImageExporter}。本类仅为兼容 2.4.x 调用方保留，
 * 内部完全委托给 {@link CanvasImageExporter}，并固定使用“导出序号”命名（{@code 0.png}、{@code 1.png}），
 * 与旧版本产物命名一致。
 * <p>
 * 与旧实现的差异（来自 {@link CanvasImageExporter}）：
 * <ul>
 *     <li>页面像素尺寸改为向上取整，高分辨率下右侧、底部不再被裁掉 1 像素；</li>
 *     <li>JPG 使用默认 0.92 质量编码，并写入物理分辨率（DPI）元数据；</li>
 *     <li>导出图片像素在使用后及时释放。</li>
 * </ul>
 * 为兼容既有调用方，委托时关闭像素上限校验（{@code setMaxPixels(0)}），保持旧实现“不限制尺寸”的行为。
 *
 * @author 权观宇
 * @since 2023-3-8 19:50:52
 * @deprecated 请使用 {@link CanvasImageExporter}，该类支持页码命名、背景色、旋转、JPG 质量与像素上限等能力
 */
@Deprecated
public class ImageExporter implements OFDExporter {

    /**
     * 兼容旧行为的文件名模式：占位符仅输出导出序号
     */
    private static final String LEGACY_FILENAME_PATTERN = "%d";

    /**
     * 实际执行导出的图片导出器
     */
    private final CanvasImageExporter delegate;

    /**
     * 图片类型
     */
    private final String imageType;

    /**
     * 构造图片转换器
     *
     * @param ofdFilePath 待转换OFD文件
     * @param imgDirPath  生成图片存放目录
     * @throws IOException 文件解析异常
     * @deprecated 请使用 {@link CanvasImageExporter#CanvasImageExporter(Path, Path)}
     */
    @Deprecated
    public ImageExporter(Path ofdFilePath, Path imgDirPath) throws IOException {
        this(ofdFilePath, imgDirPath, "PNG", 15);
    }

    /**
     * 构造图片转换器
     *
     * @param ofdInput   待转换OFD文件流，该流由调用者负责关闭
     * @param imgDirPath 生成图片存放目录
     * @throws IOException 文件解析异常
     * @deprecated 请使用 {@link CanvasImageExporter#CanvasImageExporter(InputStream, Path)}
     */
    @Deprecated
    public ImageExporter(InputStream ofdInput, Path imgDirPath) throws IOException {
        this(ofdInput, imgDirPath, "PNG", 15);
    }

    /**
     * 构造图片转换器
     *
     * @param ofdFilePath 待转换OFD文件
     * @param imgDirPath  生成图片存放目录
     * @param imageType   生成图片的格式，支持 PNG、JPG、BMP
     * @param ppm         每毫米像素数量(Pixels per millimeter)
     * @throws IOException 文件解析异常
     * @deprecated 请使用 {@link CanvasImageExporter#CanvasImageExporter(Path, Path, String, double)}
     */
    @Deprecated
    public ImageExporter(Path ofdFilePath, Path imgDirPath, String imageType, double ppm) throws IOException {
        this.delegate = configureLegacyNaming(new CanvasImageExporter(ofdFilePath, imgDirPath, imageType, ppm));
        this.imageType = imageType;
    }

    /**
     * 构造图片转换器
     *
     * @param ofdInput   待转换OFD文件流，该文件流由调用者负责关闭
     * @param imgDirPath 生成图片存放目录
     * @param imageType  生成图片的格式，支持 PNG、JPG、BMP
     * @param ppm        每毫米像素数量(Pixels per millimeter)
     * @throws IOException 文件解析异常
     * @deprecated 请使用 {@link CanvasImageExporter#CanvasImageExporter(InputStream, Path, String, double)}
     */
    @Deprecated
    public ImageExporter(InputStream ofdInput, Path imgDirPath, String imageType, double ppm) throws IOException {
        this.delegate = configureLegacyNaming(new CanvasImageExporter(ofdInput, imgDirPath, imageType, ppm));
        this.imageType = imageType;
    }

    /**
     * 把画布导出器配置为旧版行为：按导出序号命名、不限制单页像素。
     *
     * @param exporter 新建的画布导出器
     * @return 完成配置的导出器
     */
    private static CanvasImageExporter configureLegacyNaming(CanvasImageExporter exporter) {
        exporter.setFilenamePattern(LEGACY_FILENAME_PATTERN);
        exporter.setFilenameIndexBase(IndexBase.SEQUENCE);
        // 旧实现未限制页面像素数量，保持不设上限，避免既有调用方因尺寸受限而失败
        exporter.setMaxPixels(0);
        return exporter;
    }

    /**
     * 导出指定OFD页为图片
     *
     * @param indexes 页码序列，如果为空表示全部页码（注意：页码从0起）
     * @throws GeneralConvertException 转换异常
     * @deprecated 请使用 {@link CanvasImageExporter#export(int...)}
     */
    @Override
    @Deprecated
    public void export(int... indexes) throws GeneralConvertException {
        delegate.export(indexes);
    }

    /**
     * 关闭图片转换器。
     *
     * @throws IOException 关闭失败
     * @deprecated 请关闭 {@link CanvasImageExporter}
     */
    @Override
    @Deprecated
    public void close() throws IOException {
        delegate.close();
    }

    /**
     * 获取已经转换完成的页面的图片路径
     *
     * @return 页面图片路径
     * @deprecated 请使用 {@link CanvasImageExporter#getImgFilePaths()}
     */
    @Deprecated
    public List<Path> getImgFilePaths() {
        return delegate.getImgFilePaths();
    }

    /**
     * 获取转换图片类型
     *
     * @return 图片类型
     * @deprecated 请使用 {@link CanvasImageExporter#setImageFormat(String)}
     */
    @Deprecated
    public String getImageType() {
        return imageType;
    }

    /**
     * 设置转换图片质量
     * <p>
     * 请在调用 {@link #export(int...)} 方法之前设置PPM！
     *
     * @param ppm 每毫米像素数量(Pixels per millimeter)
     * @deprecated 请使用 {@link CanvasImageExporter#setPPM(double)}
     */
    @Deprecated
    public void setPPM(double ppm) {
        delegate.setPPM(ppm);
    }
}
