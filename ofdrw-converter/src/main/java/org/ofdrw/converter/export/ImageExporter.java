package org.ofdrw.converter.export;

import org.ofdrw.converter.GeneralConvertException;
import org.ofdrw.converter.ImageMaker;
import org.ofdrw.reader.OFDReader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * OFD图片转换器
 *
 * @author 权观宇
 * @since 2023-3-8 19:50:52
 */
public class ImageExporter implements OFDExporter {

    /**
     * OFD解析器
     */
    final OFDReader ofdReader;
    /**
     * 图片转换器
     */
    final ImageMaker imageMaker;

    /**
     * 图片类型
     */
    final String imageType;

    /**
     * 图片文件输出路径
     */
    final Path outDirPath;

    /**
     * 转换生成的图片文件序列
     */
    List<Path> imgFileArr;

    /**
     * 是否已经关闭
     */
    private boolean closed = false;

    /**
     * 构造图片转换器
     *
     * @param ofdFilePath 待转换OFD文件
     * @param imgDirPath  生成图片存放目录
     * @throws IOException 文件解析异常
     */
    public ImageExporter(Path ofdFilePath, Path imgDirPath) throws IOException {
        this(ofdFilePath, imgDirPath, "PNG", 15);
    }

    /**
     * 构造图片转换器
     *
     * @param ofdInput   待转换OFD文件流，该流由调用者负责关闭
     * @param imgDirPath 生成图片存放目录
     * @throws IOException 文件解析异常
     */
    public ImageExporter(InputStream ofdInput, Path imgDirPath) throws IOException {
        this(ofdInput, imgDirPath, "PNG", 15);
    }

    /**
     * 构造图片转换器
     *
     * @param ofdFilePath 待转换OFD文件
     * @param imgDirPath  生成图片存放目录
     * @param imageType   生成图片的格式，如 JPG、PNG、GIF、BMP
     * @param ppm         转换图片质量，每毫米像素数量(Pixels per millimeter)
     * @throws IOException 文件解析异常
     */
    public ImageExporter(Path ofdFilePath, Path imgDirPath, String imageType, double ppm) throws IOException {
        ofdReader = new OFDReader(ofdFilePath);

        if (imgDirPath == null) {
            throw new IllegalArgumentException("导出图片文件路径为空");
        }
        imgDirPath = imgDirPath.toAbsolutePath();
        if (Files.exists(imgDirPath) && !Files.isDirectory(imgDirPath)) {
            throw new IllegalArgumentException("已经存在同名文件");
        }
        if (!Files.exists(imgDirPath)) {
            Files.createDirectories(imgDirPath);
        }
        imageMaker = new ImageMaker(ofdReader, ppm);
        imageMaker.config.setDrawBoundary(false);
        this.imageType = imageType;
        imgFileArr = new ArrayList<>();
        outDirPath = imgDirPath;
    }

    /**
     * 构造图片转换器
     *
     * @param ofdInput   待转换OFD文件流，该文件流由调用者负责关闭
     * @param imgDirPath 生成图片存放目录
     * @param imageType  生成图片的格式，如 JPG、PNG、GIF、BMP
     * @param ppm        转换图片质量，每毫米像素数量(Pixels per millimeter)
     * @throws IOException 文件解析异常
     */
    public ImageExporter(InputStream ofdInput, Path imgDirPath, String imageType, double ppm) throws IOException {
        ofdReader = new OFDReader(ofdInput);
        if (imgDirPath == null) {
            throw new IllegalArgumentException("导出图片文件路径为空");
        }
        imgDirPath = imgDirPath.toAbsolutePath();
        if (Files.exists(imgDirPath) && !Files.isDirectory(imgDirPath)) {
            throw new IllegalArgumentException("已经存在同名文件");
        }
        if (!Files.exists(imgDirPath)) {
            Files.createDirectories(imgDirPath);
        }
        imageMaker = new ImageMaker(ofdReader, ppm);
        imageMaker.config.setDrawBoundary(false);
        this.imageType = imageType;
        imgFileArr = new ArrayList<>();
        outDirPath = imgDirPath;
    }

    /**
     * 导出指定OFD页为图片
     *
     * @param indexes 页码序列，如果为空表示全部页码（注意：页码从0起）
     * @throws GeneralConvertException 转换异常
     */
    @Override
    public void export(int... indexes) throws GeneralConvertException {
        List<Integer> targetPages = new LinkedList<>();
        if (indexes == null || indexes.length == 0) {
            for (int i = 0; i < ofdReader.getNumberOfPages(); i++) {
                targetPages.add(i);
            }
        } else {
            int maxPageIndex = ofdReader.getNumberOfPages();
            // 获取指定页面信息
            for (int index : indexes) {
                if (index < 0 || index >= maxPageIndex) {
                    continue;
                }
                targetPages.add(index);
            }
        }
        try {
            for (Integer index : targetPages) {
                BufferedImage image = imageMaker.makePage(index);
                Path dst = this.outDirPath.resolve(imgFileArr.size() + "." + imageType.toLowerCase());
                ImageIO.write(image, imageType, dst.toFile());
                this.imgFileArr.add(dst);
            }
        } catch (IOException e) {
            throw new GeneralConvertException("图片转换异常", e);
        }
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        if (ofdReader != null) {
            ofdReader.close();
        }
    }

    /**
     * 获取已经转换完成的页面的图片路径
     *
     * @return 页面图片路径
     */
    public List<Path> getImgFilePaths() {
        return imgFileArr;
    }

    /**
     * 获取转换图片类型
     *
     * @return 图片类型
     */
    public String getImageType() {
        return imageType;
    }

    /**
     * 设置转换图片质量
     * <p>
     * 请在调用 {@link #export(int...)} 方法之前设置PPM！
     *
     * @param ppm 每毫米像素数量(Pixels per millimeter)
     */
    public void setPPM(double ppm) {
        if (imageMaker == null) {
            return;
        }
        imageMaker.setPPM(ppm);
    }
}
