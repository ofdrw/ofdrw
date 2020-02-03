package org.ofdrw.layout;

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

    /**
     * 是否保持比例缩放
     * <p>
     * true - 保持比例缩放
     * false - 拉伸适应width和height
     */
    private boolean fit;

    private Img() {
        this.fit = true;
    }

    public Img(double width, double height,Path src) {
        this(src);
        this.src = src;
        this.setWidth(width)
                .setHeight(height);
    }

    public Img(Path src) {
        this();
        if (src == null || Files.notExists(src)) {
            throw new IllegalArgumentException("图片文件为空或不存在");
        }
        this.src = src;
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
}
