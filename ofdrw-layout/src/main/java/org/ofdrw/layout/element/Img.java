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
public class Img extends Div {
    /**
     * 图片文件路径
     */
    private Path src;

//    /**
//     * 是否保持比例缩放
//     * <p>
//     * true - 保持比例缩放
//     * false - 拉伸适应width和height
//     */
//    private boolean fit;

    private Img() {
//        this.fit = true;
        // 图片对象不可拆分
        this.setIntegrity(true);
    }

    public Img(double width, double height, Path src) throws IOException {
        this(src);
        this.setWidth(width)
                .setHeight(height);
    }

    public Img(Path src) throws IOException {
        this();
        if (src == null || Files.notExists(src)) {
            throw new IllegalArgumentException("图片文件为空或不存在");
        }
        this.src = src;
        parseImg();
    }

    private void parseImg() throws IOException {
        File picture = src.toFile();
//        try( FileInputStream fIn = new FileInputStream(picture);){
//            BufferedImage sourceImg = ImageIO.read(fIn);
//            this.setWidth((double) sourceImg.getWidth() / 5);
//            this.setHeight((double) sourceImg.getHeight() / 5);
//        }

        // 原先上面代码可能存在问题：
        //  java在对图片处理时，使用ImageIO.read方法有时报javax.imageio.IIOException: Unsupported Image Type错，
        //  上网查发现是图片类型的问题，图片不是RGB 格式而是CMYK格式，网上说图片在进过ps软件处理后，默认是CMYK格式的。
        //  使用下面的代码替换可以解决以上问题。
        BufferedImage sourceImg = readImage(picture);
        this.setWidth((double) sourceImg.getWidth() / 5);
        this.setHeight((double) sourceImg.getHeight() / 5);
    }

    /**
     * 传入文件的形式 zwd
     * @param file
     * @return
     * @throws IOException
     */
    public static BufferedImage readImage(File file) throws IOException {
        return readImage(ImageIO.createImageInputStream(file));
    }

    /**
     * 读文件 zwd
     * @param input
     * @return
     * @throws IOException
     */
    public static BufferedImage readImage(ImageInputStream input) throws IOException {
        Iterator<?> readers = ImageIO.getImageReaders(input);
        if(readers == null || !readers.hasNext()) {
            return null;
        }
        ImageReader reader = (ImageReader) readers.next();
        reader.setInput(input);
        BufferedImage image;
        try {
            // 尝试读取图片 (包括颜色的转换).RGB
            image = reader.read(0);
        } catch (IIOException e) {
            // 读取Raster (没有颜色的转换).CMYK
            Raster raster = reader.readRaster(0, null);
            image = createJPEG4(raster);
        }
        return image;
    }

    /**
     * 读取ps处理过的图片文件（CMYK） zwd
     * @param raster
     * @return
     */
    private static BufferedImage createJPEG4(Raster raster) {
        int w = raster.getWidth();
        int h = raster.getHeight();
        byte[] rgb = new byte[w * h * 3];

        //彩色空间转换
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
        raster = Raster.createInterleavedRaster(new DataBufferByte(rgb, rgb.length), w, h, w * 3, 3, new int[]{0, 1, 2}, null);
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        ColorModel cm = new ComponentColorModel(cs, false, true, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        return new BufferedImage(cm, (WritableRaster) raster, true, null);
    }

    public Path getSrc() {
        return src;
    }

//    public boolean isFit() {
//        return fit;
//    }
//
//    public Img setFit(boolean fit) {
//        this.fit = fit;
//        return this;
//    }

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
