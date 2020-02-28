package org.ofdrw.layout;

import org.ofdrw.layout.element.ArrayParamTool;
import org.ofdrw.layout.element.Div;
import org.ofdrw.layout.element.Position;

import java.util.LinkedList;
import java.util.List;

/**
 * 虚拟页面
 *
 * 虚拟页面介于盒式模型和板式模型两种中间
 * 虚拟页面内包含多个Div对象，这些对象都为绝对定位。
 * 由于是绝对定位，因此不存在分页的情况。
 *
 * @author 权观宇
 * @since 2020-02-28 02:32:27
 */
public class VirtualPage {
    /**
     * 页面宽度
     */
    private Double width;
    /**
     * 页面高度
     */
    private Double height;

    /**
     * 外边距
     * <p>
     * 上 左 下 右
     * 默认值 36
     */
    private Double[] margin = {36d, 36d, 36d, 36d};

    private List<Div> content;

    private VirtualPage() {
    }

    public VirtualPage(Double width, Double height) {
        this.width = width;
        this.height = height;
        content = new LinkedList<>();
    }

    /**
     * 向虚拟页面中加入对象
     * @param d 对象
     * @return this
     */
    public VirtualPage add(Div d) {
        if (d == null) {
            return this;
        }
        if (d.getPosition() != Position.Absolute) {
            throw new IllegalArgumentException("加入虚拟页面的对象应该采用绝对定位（Position: Absolute）");
        }
        this.content.add(d);
        return this;

    }

    public Double getWidth() {
        return width;
    }

    public VirtualPage setWidth(Double width) {
        this.width = width;
        return this;
    }

    public Double getHeight() {
        return height;
    }

    public VirtualPage setHeight(Double height) {
        this.height = height;
        return this;
    }

    public Double[] getMargin() {
        return margin;
    }

    public VirtualPage setMargin(Double[] margin) {
        this.margin = ArrayParamTool.arr4p(margin);
        return this;
    }
}
