package org.ofdrw.layout.edit;

import org.ofdrw.core.annotation.pageannot.Annot;
import org.ofdrw.core.annotation.pageannot.AnnotType;
import org.ofdrw.core.annotation.pageannot.Appearance;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.layout.element.canvas.Drawer;

import java.time.LocalDate;
import java.util.*;

/**
 * 注释对象
 * <p>
 * 请设置绘制对象
 *
 * @author 权观宇
 * @since 2020-05-14 19:09:37
 */
public class Annotation {

    /**
     * 绘制器
     */
    private Drawer drawer;


    /**
     * 注释类型
     */
    private AnnotType type;

    /**
     * 注释创建者
     */
    private String creator;

    /**
     * 最近一次修改的时间
     */
    private LocalDate lastModDate;

    /**
     * 注释子类型
     */
    private String subtype = null;

    /**
     * 表示该注释对象是否显示
     */
    private Boolean visible = null;

    /**
     * 对象的 Remark 信息是否不随页面旋转而旋转
     */
    private Boolean noRotate = null;

    /**
     * 一组注释参数
     * <p>
     * 为了保证参数的顺序采用List存储
     */
    private LinkedHashMap<String, String> parameters = null;

    /**
     * 注释说明内容
     */
    private String remark = null;

    /**
     * 对象的 Remark 信息是否不能被用户更改
     */
    private Boolean readOnly;

    /**
     * 对象的Remark 信息是否随页面一起打印
     */
    private Boolean print = null;

    /**
     * 对象的 Remark 信息是否不随页面缩放而同步缩放
     */
    private Boolean noZoom;

    /**
     * 注释空间
     */
    private ST_Box boundary;


    private Annotation() {
    }


    /**
     * 创建注释对象
     *
     * @param x      所处页面 y坐标
     * @param y      所处页面 y坐标
     * @param width  画布宽度
     * @param height 画布高度
     * @param type   注释类型
     * @param drawer 注释绘制器
     */
    public Annotation(double x, double y,
                      double width, double height,
                      AnnotType type,
                      Drawer drawer) {
        this(new ST_Box(x, y, width, height), type, drawer);
    }

    /**
     * 创建注解对象
     *
     * @param boundary 注释对象绘制空间
     * @param type     注释类型
     * @param drawer   注释绘制器
     */
    public Annotation(ST_Box boundary, AnnotType type, Drawer drawer) {
        if (boundary == null) {
            throw new IllegalArgumentException("注释对象绘制空间(boundary)不能为空");
        }
        if (type == null) {
            throw new IllegalArgumentException("注解对象类型(type)不能为空");
        }
        if (drawer == null) {
            throw new IllegalArgumentException("注解绘制器(drawer)不能为空");
        }
        this.setDrawer(drawer);
        this.type = type;
        this.boundary = boundary;
    }


    /**
     * 获取注释底层对象
     *
     * @return 注释对象
     */
    public Annot build() {
        Annot annot = new Annot()
                .setType(type)
                .setCreator(creator == null ? "OFD R&W" : creator)
                .setLastModDate(lastModDate == null ? LocalDate.now() : lastModDate);

        if (visible != null) {
            annot.setVisible(visible);
        }
        if (subtype != null) {
            annot.setSubtype(subtype);
        }
        if (print != null) {
            annot.setPrint(print);
        }
        if (noZoom != null) {
            annot.setNoZoom(noZoom);
        }
        if (noRotate != null) {
            annot.setNoRotate(noRotate);
        }
        if (readOnly != null) {
            annot.setReadOnly(readOnly);
        }
        if (remark != null) {
            annot.setRemark(remark);
        }
        if (parameters != null) {
            parameters.forEach(annot::addParameter);
        }
        annot.setAppearance(new Appearance(boundary));
        return annot;
    }


    public ST_Box getBoundary() {
        return boundary;
    }

    public void setBoundary(ST_Box boundary) {
        this.boundary = boundary;
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
        this.type = type;
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
        return type;
    }

    /**
     * 设置 注释创建者
     *
     * @param creator 注释创建者
     * @return this
     */
    public Annotation setCreator(String creator) {
        this.creator = creator;
        return this;
    }

    /**
     * 获取 注释创建者
     *
     * @return 注释创建者
     */
    public String getCreator() {
        return creator;
    }


    /**
     * 设置 最近一次修改的时间
     *
     * @param lastModDate 最近一次修改的时间
     * @return this
     */
    public Annotation setLastModDate(LocalDate lastModDate) {
        this.lastModDate = lastModDate;
        return this;
    }

    /**
     * 获取 最近一次修改的时间
     *
     * @return 最近一次修改的时间
     */
    public LocalDate getLastModDate() {
        return lastModDate;
    }


    /**
     * 设置 注释子类型
     *
     * @param subtype 注释子类型
     * @return this
     */
    public Annotation setSubtype(String subtype) {
        this.subtype = subtype;
        return this;
    }

    /**
     * 获取 注释子类型
     *
     * @return 注释子类型
     */
    public String getSubtype() {
        return subtype;
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
        this.visible = visible;
        return this;
    }

    /**
     * 获取 表示该注释对象是否显示
     * <p>
     * 默认值为 true
     *
     * @return 表示该注释对象是否显示，默认值为 true
     */
    public Boolean getVisible() {
        if (visible == null) {
            return true;
        }
        return visible;
    }


    /**
     * 设置 对象的Remark 信息是否随页面一起打印
     * <p>
     * 默认值为 true
     *
     * @param print 对象的Remark 信息是否随页面一起打印
     * @return this
     */
    public Annotation setPrint(boolean print) {
        this.print = print;
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
        if (print == null) {
            return true;
        }
        return print;
    }

    /**
     * 设置 对象的 Remark 信息是否不随页面缩放而同步缩放
     * <p>
     * 默认值为 false
     *
     * @param noZoom 对象的 Remark 信息是否不随页面缩放而同步缩放
     * @return this
     */
    public Annotation setNoZoom(boolean noZoom) {
        this.noZoom = noZoom;
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
        if (noZoom == null) {
            return false;
        }
        return noZoom;
    }


    /**
     * 设置 对象的 Remark 信息是否不随页面旋转而旋转
     * <p>
     * 默认值为 false
     *
     * @param noRotate 对象的 Remark 信息是否不随页面旋转而旋转
     * @return this
     */
    public Annotation setNoRotate(boolean noRotate) {
        this.noRotate = noRotate;
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
        if (noRotate == null) {
            return false;
        }
        return noRotate;
    }


    /**
     * 设置 对象的 Remark 信息是否不能被用户更改
     * <p>
     * 默认值为 true
     *
     * @param readOnly 对象的 Remark 信息是否不能被用户更改
     * @return this
     */
    public Annotation setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
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
        if (readOnly == null) {
            return true;
        }
        return readOnly;
    }


    /**
     * 设置 注释说明内容
     *
     * @param remark 注释说明内容
     * @return this
     */
    public Annotation setRemark(String remark) {
        this.remark = remark;
        return this;
    }

    /**
     * 获取 注释说明内容
     *
     * @return 注释说明内容，如果注释说明内容不存在返还null
     */
    public String getRemark() {
        return remark;
    }


    /**
     * 增加 注释参数
     *
     * @param name      键名
     * @param parameter 值
     * @return this
     */
    public Annotation addParameter(String name, String parameter) {
        if (parameters == null) {
            parameters = new LinkedHashMap<>();
        }
        if (name == null) {
            return this;
        }
        parameters.put(name, parameter);
        return this;
    }


    /**
     * 获取 一组注释参数
     *
     * @return 注解参数映射表, 如果注释参数不存在那么返还空的集合
     */
    public Map<String, String> getParameters() {
        if (parameters == null) {
            return Collections.emptyMap();
        }
        return parameters;
    }

}
