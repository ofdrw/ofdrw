package org.ofdrw.layout.element;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
       try( FileInputStream fIn = new FileInputStream(picture);){
           BufferedImage sourceImg = ImageIO.read(fIn);
           this.setWidth((double) sourceImg.getWidth() / 5);
           this.setHeight((double) sourceImg.getHeight() / 5);
       }
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
