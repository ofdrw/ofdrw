package org.ofdrw.layout.element;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * 图片对象
 * <p>
 * 为了防止与Image对象命名冲突采用Img缩写
 *
 * @author 权观宇
 * @since 2020-02-03 03:34:31
 */
public class Img extends Div<Img> {
    /**
     * 图片文件路径
     */
    private Path src;

    private Img() {
        // 图片对象不可拆分
        this.setIntegrity(true);
    }

    /**
     * 构造图片对象
     * <p>
     * 该构造器不会输入图片进行解析，直接将图片作为资源使用。
     *
     * @param width  图片在页面内的宽度，单位毫米（mm）
     * @param height 图片在页面内的高度，单位毫米（mm）
     * @param src    图片路径（该构造器不会解析图片大小）
     * @throws IOException 读取异常
     */
    public Img(double width, double height, Path src) throws IOException {
        this();
        if (src == null || Files.notExists(src)) {
            throw new IllegalArgumentException("图片文件为空或不存在");
        }
        this.src = src;
        this.setWidth(width);
        this.setHeight(height);
    }

    /**
     * 创建图片Div
     * <p>
     * 图片宽度和高度以 每 5pix/mm （像素/毫米）的比例转换。
     * <p>
     * 不建议使用该方式构造图片，图片的宽度和高度应该在由调用者手动指定
     * 建议使用{@link Img#Img(double, double, java.nio.file.Path)}方法构造
     *
     * @param src 图片路径
     * @throws IOException 图片解析异常
     * @deprecated {@link Img#Img(double, double, java.nio.file.Path)}
     */
    @Deprecated
    public Img(Path src) throws IOException {
        this();
        if (src == null || Files.notExists(src)) {
            throw new IllegalArgumentException("图片文件为空或不存在");
        }
        this.src = src;
        parseImg();
    }

    /**
     * 解析路径上的图片
     *
     * @throws IOException 读取解析异常
     */
    private void parseImg() throws IOException {
        final File picture = src.toFile();
        BufferedImage imgObj = null;
        try (FileInputStream fIn = new FileInputStream(picture)) {
            imgObj = ImageIO.read(fIn);
        } catch (IIOException e) {
            /*
             * 图片类型的问题，如果图片格式而是CMYK格式，
             * 使用ImageIO.read方法有时报javax.imageio.IIOException: Unsupported Image Type
             * 此处尝试将CMYK装换为 RGB。
             */
            imgObj = readImage(picture);
        }
        if (imgObj != null) {
            this.setWidth((double) imgObj.getWidth() / 5);
            this.setHeight((double) imgObj.getHeight() / 5);
        }

    }


    /**
     * 尝试读取图片文件为 BufferedImage
     * <p>
     * 首先尝试采用RGB读取，如果失败那么将CMYK进行色彩空间转化为RGB。
     *
     * @param imageFile 图片文件
     * @return BufferedImage
     * @throws IOException 操作或转换异常
     * @author zwd
     */
    public static BufferedImage readImage(File imageFile) throws IOException {
        final ImageInputStream input = ImageIO.createImageInputStream(imageFile);
        Iterator<?> readers = ImageIO.getImageReaders(input);
        if (readers == null || !readers.hasNext()) {
            return null;
        }
        ImageReader reader = (ImageReader) readers.next();
        reader.setInput(input);
        BufferedImage image;
        try {
            // 尝试读取图片 (包括颜色的转换).RGB
            image = reader.read(0);
        } catch (IIOException e) {
//            System.err.println(">> 无法解析图片格式尝试CMYK格式转换");
            // 尝试将CMYK转换为RGB
            image = convertCMYK2RGB(reader);
        }
        return image;
    }

    /**
     * 装换CMYK图片为RGB
     *
     * @param reader 图片解析器
     * @return BufferedImage
     * @author zwd
     */
    private static BufferedImage convertCMYK2RGB(ImageReader reader) throws IOException {
        // 读取Raster (没有颜色的转换) CMYK
        Raster raster = reader.readRaster(0, null);
        int w = raster.getWidth();
        int h = raster.getHeight();
        byte[] rgb = new byte[w * h * 3];

        // 彩色空间转换
        float[] Y = raster.getSamples(0, 0, w, h, 0, (float[]) null);
        float[] Cb = raster.getSamples(0, 0, w, h, 1, (float[]) null);
        float[] Cr = raster.getSamples(0, 0, w, h, 2, (float[]) null);
        float[] K = raster.getSamples(0, 0, w, h, 3, (float[]) null);

        for (int i = 0, imax = Y.length, base = 0; i < imax; i++, base += 3) {
            float k = 220 - K[i], y = 255 - Y[i], cb = 255 - Cb[i],
                    cr = 255 - Cr[i];

            double val = y + 1.402 * (cr - 128) - k;
            val = (val - 128) * .65f + 128;
            rgb[base] = val < 0.0 ? (byte) 0 : val > 255.0 ? (byte) 0xff
                    : (byte) (val + 0.5);

            val = y - 0.34414 * (cb - 128) - 0.71414 * (cr - 128) - k;
            val = (val - 128) * .65f + 128;
            rgb[base + 1] = val < 0.0 ? (byte) 0 : val > 255.0 ? (byte) 0xff
                    : (byte) (val + 0.5);

            val = y + 1.772 * (cb - 128) - k;
            val = (val - 128) * .65f + 128;
            rgb[base + 2] = val < 0.0 ? (byte) 0 : val > 255.0 ? (byte) 0xff
                    : (byte) (val + 0.5);
        }
        raster = Raster.createInterleavedRaster(
                new DataBufferByte(rgb, rgb.length),
                w, h,
                w * 3,
                3,
                new int[]{0, 1, 2},
                null);
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        ColorModel cm = new ComponentColorModel(cs, false, true,
                Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        return new BufferedImage(cm, (WritableRaster) raster, true, null);
    }

    public Path getSrc() {
        return src;
    }

    public Img setSrc(Path src) {
        this.src = src;
        return this;
    }

    /**
     * 不允许切分
     */
    @Override
    public Div[] split(double sHeight) {
        throw new RuntimeException("图片对象不支持分割操作");
    }
}
