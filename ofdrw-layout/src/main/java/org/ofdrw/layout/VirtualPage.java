package org.ofdrw.layout;

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
    private PageCfg style;

    /**
     * 虚拟页面的内容
     */
    private List<Div> content = new LinkedList<>();

    private VirtualPage() {
    }

    public VirtualPage(PageCfg style) {
        this.style = style;
    }

    public VirtualPage(Double width, Double height) {
        this.style = new PageCfg(width, height);
    }

    public PageCfg getStyle() {
        return style;
    }

    public void setStyle(PageCfg style) {
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
        if (d.getPosition() != Position.Absolute) {
            throw new IllegalArgumentException("加入虚拟页面的对象应该采用绝对定位（Position: Absolute）");
        }
        this.content.add(d);
        return this;

    }
}
