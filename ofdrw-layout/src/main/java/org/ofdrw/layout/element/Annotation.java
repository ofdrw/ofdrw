package org.ofdrw.layout.element;

import org.ofdrw.core.annotation.pageannot.Annot;
import org.ofdrw.core.annotation.pageannot.AnnotType;
import org.ofdrw.core.annotation.pageannot.Appearance;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.layout.element.canvas.Drawer;

import java.time.LocalDate;
import java.util.Map;

/**
 * 注释对象
 * <p>
 * 请设置绘制对象
 *
 * @author 权观宇
 * @since 2020-05-14 19:09:37
 */
public class Annotation extends Div implements DivContainer {

    /**
     * 绘制器
     */
    private Drawer drawer;

    /**
     * 底层的注解对象
     */
    private Annot annot;

    /**
     * 容器用于真正放置注解对象
     */
    private Appearance container;

    private Annotation() {
        annot = new Annot()
                .setAppearance(container)
                .setLastModDate(LocalDate.now())
                .setCreator("OFD R&W");
    }


    /**
     * 创建注解对象
     *
     * @param x      所处页面 y坐标
     * @param y      所处页面 y坐标
     * @param width  画布宽度
     * @param height 画布高度
     * @param type   注解类型
     * @param drawer 注解绘制器
     */
    public Annotation(Double x, Double y, Double width, Double height, AnnotType type, Drawer drawer) {
        super(width, height);
        if (type == null) {
            throw new IllegalArgumentException("注解对象类型(type)不能为空");
        }
        if (drawer == null) {
            throw new IllegalArgumentException("注解绘制器(drawer)不能为空");
        }
        this.setType(type)
                .setDrawer(drawer)
                .setPosition(Position.Absolute)
                .setX(x)
                .setY(y);
        container.setBoundary(new ST_Box(x, y, width, height));
    }

    /**
     * 获取注解外观容器对象
     *
     * @return 注释的静态呈现效果
     */
    public Appearance getContainer() {
        return container;
    }


    /**
     * 获取注释底层对象
     *
     * @return 注释对象
     */
    public Annot getAnnot() {
        return annot;
    }

    /**
     * 获取当前注释的绘制器
     *
     * @return 绘制器
     */
    public Drawer getDrawer() {
        return drawer;
    }

    /**
     * 设置注释绘制器
     *
     * @param drawer 注释绘制器
     * @return this
     */
    public Annotation setDrawer(Drawer drawer) {
        this.drawer = drawer;
        return this;
    }

    /**
     * 设置 注释类型
     * <p>
     * 具体取值见{@link AnnotType}
     *
     * @param type 注释类型
     * @return this
     */
    public Annotation setType(AnnotType type) {
        annot.setType(type);
        return this;
    }

    /**
     * 获取 注释类型
     * <p>
     * 具体取值见{@link AnnotType}
     *
     * @return 注释类型
     */
    public AnnotType getType() {
        return annot.getType();
    }


    /**
     * 设置 注释创建者
     *
     * @param creator 注释创建者
     * @return this
     */
    public Annotation setCreator(String creator) {
        annot.setCreator(creator);
        return this;
    }

    /**
     * 获取 注释创建者
     *
     * @return 注释创建者
     */
    public String getCreator() {
        return annot.getCreator();
    }

    /**
     * 设置 最近一次修改的时间
     *
     * @param lastModDate 最近一次修改的时间
     * @return this
     */
    public Annotation setLastModDate(LocalDate lastModDate) {
        annot.setLastModDate(lastModDate);
        return this;
    }

    /**
     * 获取 最近一次修改的时间
     *
     * @return 最近一次修改的时间
     */
    public LocalDate getLastModDate() {
        return annot.getLastModDate();
    }

    /**
     * 设置 注释子类型
     *
     * @param subtype 注释子类型
     * @return this
     */
    public Annotation setSubtype(String subtype) {
        annot.setSubtype(subtype);
        return this;
    }

    /**
     * 获取 注释子类型
     *
     * @return 注释子类型
     */
    public String getSubtype() {
        return annot.getSubtype();
    }

    /**
     * 设置 表示该注释对象是否显示
     * <p>
     * 默认值为 true
     *
     * @param visible 表示该注释对象是否显示，默认值为 true
     * @return this
     */
    public Annotation setVisible(boolean visible) {
        annot.setVisible(visible);
        return this;
    }

    /**
     * 获取 表示该注释对象是否显示
     * <p>
     * 默认值为 true
     *
     * @return 表示该注释对象是否显示，默认值为 true
     */
    public boolean getVisible() {
        return annot.getVisible();
    }

    /**
     * 设置 对象的Remark 信息是否随页面一起打印
     * <p>
     * 默认值为 true
     *
     * @param print 对象的Remark 信息是否随页面一起打印
     * @return this
     */
    public Annotation setPrint(Boolean print) {
        annot.setPrint(print);
        return this;
    }

    /**
     * 设置 对象的Remark 信息是否随页面一起打印
     * <p>
     * 默认值为 true
     *
     * @return 对象的Remark 信息是否随页面一起打印
     */
    public Boolean getPrint() {
        return annot.getPrint();
    }

    /**
     * 设置 对象的 Remark 信息是否不随页面缩放而同步缩放
     * <p>
     * 默认值为 false
     *
     * @param noZoom 对象的 Remark 信息是否不随页面缩放而同步缩放
     * @return this
     */
    public Annotation setNoZoom(Boolean noZoom) {
        annot.setNoZoom(noZoom);
        return this;
    }

    /**
     * 获取 对象的 Remark 信息是否不随页面缩放而同步缩放
     * <p>
     * 默认值为 false
     *
     * @return 对象的 Remark 信息是否不随页面缩放而同步缩放
     */
    public Boolean getNoZoom() {
        return annot.getNoZoom();
    }

    /**
     * 设置 对象的 Remark 信息是否不随页面旋转而旋转
     * <p>
     * 默认值为 false
     *
     * @param noRotate 对象的 Remark 信息是否不随页面旋转而旋转
     * @return this
     */
    public Annotation setNoRotate(Boolean noRotate) {
        annot.setNoRotate(noRotate);
        return this;
    }

    /**
     * 获取 对象的 Remark 信息是否不随页面旋转而旋转
     * <p>
     * 默认值为 false
     *
     * @return 对象的 Remark 信息是否不随页面旋转而旋转
     */
    public Boolean getNoRotate() {
        return annot.getNoRotate();
    }

    /**
     * 设置 对象的 Remark 信息是否不能被用户更改
     * <p>
     * 默认值为 true
     *
     * @param readOnly 对象的 Remark 信息是否不能被用户更改
     * @return this
     */
    public Annotation setReadOnly(Boolean readOnly) {
        annot.setReadOnly(readOnly);
        return this;
    }

    /**
     * 获取 对象的 Remark 信息是否不能被用户更改
     * <p>
     * 默认值为 true
     *
     * @return 对象的 Remark 信息是否不能被用户更改
     */
    public Boolean getReadOnly() {
        return annot.getReadOnly();
    }


    /**
     * 设置 注释说明内容
     *
     * @param remark 注释说明内容
     * @return this
     */
    public Annotation setRemark(String remark) {
        annot.setRemark(remark);
        return this;
    }

    /**
     * 获取 注释说明内容
     *
     * @return 注释说明内容，如果注释说明内容不存在返还null
     */
    public String getRemark() {
        return annot.getRemark();
    }

    /**
     * 增加 注释参数
     *
     * @param name      键名
     * @param parameter 值
     * @return this
     */
    public Annotation addParameter(String name, String parameter) {
        annot.addParameter(name, parameter);
        return this;
    }

    /**
     * 获取 一组注释参数
     *
     * @return 注解参数映射表, 如果注释参数不存在那么返还空的集合
     */
    public Map<String, String> getParameters() {
        return annot.getParameters();
    }

}
