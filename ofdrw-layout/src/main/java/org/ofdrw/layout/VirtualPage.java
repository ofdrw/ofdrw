package org.ofdrw.layout;

import org.ofdrw.layout.element.AFloat;
import org.ofdrw.layout.element.Div;
import org.ofdrw.layout.element.Position;

import java.util.LinkedList;
import java.util.List;

/**
 * 虚拟页面
 * <p>
 * 虚拟页面介于盒式模型和板式模型两种中间
 * 虚拟页面内包含多个Div对象，这些对象都为绝对定位。
 * 由于是绝对定位，因此不存在分页的情况。
 *
 * @author 权观宇
 * @since 2020-02-28 02:32:27
 */
public class VirtualPage {

    /**
     * 虚拟页面的样式
     */
    private PageLayout style;

    /**
     * 虚拟页面的内容
     */
    private List<Div> content = new LinkedList<>();

    /**
     * 插入的虚拟页面页码（插入位置）
     * <p>
     * 仅在不为空时候表示需要将插入到指定页码位置。
     */
    private Integer pageNum = null;

    protected VirtualPage() {
    }

    public VirtualPage(PageLayout style) {
        this.style = style;
    }

    public VirtualPage(Double width, Double height) {
        this.style = new PageLayout(width, height);
    }

    public PageLayout getStyle() {
        return style;
    }

    public void setStyle(PageLayout style) {
        this.style = style;
    }

    /**
     * 向虚拟页面中加入对象
     *
     * @param d 对象
     * @return this
     */
    public VirtualPage add(Div d) {
        if (d == null) {
            return this;
        }
        if (d.getFloat() != AFloat.left && Position.Absolute == d.getPosition()) {
            System.err.println("虚拟页面下不支持浮动属性，仅支持绝对定位");
        }
        if (d.getPosition() != Position.Absolute) {
            throw new IllegalArgumentException("加入虚拟页面的对象应该采用绝对定位（Position: Absolute）");
        }
        if (d.getX() == null || d.getY() == null) {
            throw new IllegalArgumentException("处于绝对定位的模式下的元素应该设置 X 和 Y 坐标");
        }
        if (d.getWidth() == null) {
            throw new IllegalArgumentException("绝对定位元素至少需要指定元素宽度（Width）");
        }
        this.content.add(d);
        return this;
    }

    public List<Div> getContent() {
        return content;
    }

    VirtualPage setContent(List<Div> content) {
        this.content = content;
        return this;
    }

    /**
     * 获取虚拟页面页码
     *
     * @return 页码（从1起）
     */
    public Integer getPageNum() {
        return pageNum;
    }

    /**
     * 设置虚拟页面页码
     *
     * @param pageNum 页码（从1起）
     * @return this
     */
    public VirtualPage setPageNum(int pageNum) {
        if(pageNum <= 0){
            throw new IllegalArgumentException("虚拟页面页码(pageNum)错误");
        }
        this.pageNum = pageNum;
        return this;
    }
}
