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
public class Img extends Div{
    /**
     * 图片文件路径
     */
    private Path src;

    /**
     * 是否保持比例缩放
     * <p>
     * true - 保持比例缩放
     * false - 拉伸适应width和height
     */
    private boolean fit;

    private Img() {
        this.fit = true;
        // 图片对象不可拆分
        this.setIntegrity(true);
    }

    public Img(double width, double height, Path src) throws IOException {
        this(src);
        this.src = src;
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
        BufferedImage sourceImg = ImageIO.read(new FileInputStream(picture));
        this.setWidth((double) sourceImg.getWidth());
        this.setHeight((double) sourceImg.getHeight());
    }

    public Path getSrc() {
        return src;
    }

    public boolean isFit() {
        return fit;
    }

    public Img setFit(boolean fit) {
        this.fit = fit;
        return this;
    }

    public Img setSrc(Path src) {
        this.src = src;
        return this;
    }

    /**
     * 根据给定的高度切分元素
     * <p>
     * 截断元素前必须确定元素的宽度和高度，否则将会抛出异常
     * <p>
     * 元素的分割只作用于竖直方向上，水平方向不做分割每次只会截断1次。
     * <p>
     * 截断的元素在截断出均无margin、border、padding
     * <p>
     * 截断后的内容比截断高度高的多
     *
     * @param sHeight 切分高度
     * @return 根据给定空间分割之后的新元素
     */
    @Override
    public Div[] split(double sHeight) {
        throw new RuntimeException("图片对象不支持分割操作");
    }
}
