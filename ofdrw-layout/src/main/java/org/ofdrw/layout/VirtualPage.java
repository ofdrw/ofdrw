package org.ofdrw.layout;

import org.ofdrw.core.basicStructure.pageObj.Template;
import org.ofdrw.core.basicStructure.pageObj.layer.Type;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.layout.element.AFloat;
import org.ofdrw.layout.element.Div;
import org.ofdrw.layout.element.Position;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * 页面模板
     */
    private List<Template> templates = new ArrayList<>();

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
     * 不对元素进行分析直接加入到虚拟页面容器内
     * <p>
     * 请在调用该接口时，对待加入的元素进行分析，否则很有可能抛出异常。
     * <p>
     * 如果没有特殊需求请使用 {@link #add}，不要使用该API
     *
     * @param d 采用绝对定位的元素
     * @return this
     */
    public VirtualPage addUnsafe(Div d) {
        this.content.add(d);
        return this;
    }

    /**
     * 向虚拟页面中加入对象
     *
     * @param d 采用绝对定位的元素
     * @return this
     */
    public VirtualPage add(Div d) {
        if (d == null) {
            return this;
        }
        if (d.getFloat() != AFloat.left) {
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
        // 追加内容时进行预处理
        d.doPrepare(d.getWidth() + d.widthPlus());
        this.content.add(d);
        return this;
    }

    /**
     * 获取虚拟页面内的内容容器
     * <p>
     * 如果不需要分析元素，那么直接获取该容器来向容器内直接加入 元素
     *
     * @return 虚拟页面内的内容容器
     */
    public List<Div> getContent() {
        return content;
    }

    /**
     * 获取指定图层类型的Div
     *
     * @param layer 图层
     * @return 该图层相关的所有Div
     */
    public List<Div> getContent(Type layer) {
        List<Div> res = new LinkedList<>();
        for (Div div : content) {
            if (div.getLayer().equals(layer)) {
                res.add(div);
            }
        }
        return res;
    }

    /**
     * 返回图层相关的内容
     *
     * @return 图层列表依次是：前景层、中文层、背景层
     */
    public List<List<Div>> getLayerContent() {
        List<List<Div>> res = new ArrayList<>(3);
        if (content.isEmpty()) {
            return res;
        }
        List<Div> foreground = new ArrayList<>();
        List<Div> body = new ArrayList<>();
        List<Div> background = new ArrayList<>();
        res.add(foreground);
        res.add(body);
        res.add(background);
        for (Div div : content) {
            switch (div.getLayer()) {
                case Foreground:
                    foreground.add(div);
                    break;
                case Body:
                    body.add(div);
                    break;
                case Background:
                    background.add(div);
                    break;
            }
        }
        return res;
    }

    /**
     * 替换 虚拟页面内的内容容器
     *
     * @return this
     */
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
        if (pageNum <= 0) {
            throw new IllegalArgumentException("虚拟页面页码(pageNum)错误");
        }
        this.pageNum = pageNum;
        return this;
    }

    /**
     * 返回 页面引用的模板序列
     *
     * @return 页面模板序列
     */
    public List<Template> getTemplates() {
        return templates;
    }

    /**
     * 添加页面模板
     * <p>
     * 图层默认为 背景层 {@link Type#Background}
     *
     * @param id    模板页面对象ID
     * @param order 图层，null时为 背景层 {@link Type#Background}
     */
    public void addTemplate(String id, Type order) {
        final Template tpl = new Template();
        tpl.setTemplateID(ST_ID.getInstance(id).ref());
        if (order != null) {
            tpl.setZOrder(order);
        }
        templates.add(tpl);
    }
}
