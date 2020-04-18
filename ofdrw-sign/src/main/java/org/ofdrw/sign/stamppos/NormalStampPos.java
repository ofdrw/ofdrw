package org.ofdrw.sign.stamppos;

import org.ofdrw.core.signatures.appearance.StampAnnot;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.sign.AtomicSignID;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.List;

/**
 * 普通印章位置
 *
 * @author 权观宇
 * @since 2020-04-18 09:38:50
 */
public class NormalStampPos implements StampAppearance{

    /**
     * 图章所在页面页码
     * <p>
     * 从 1起
     */
    private int page;

    /**
     * 图章左上角X坐标
     * <p>
     * 单位毫米mm
     */
    private double tlx;
    /**
     * 图章左上角X坐标
     * <p>
     * 单位毫米mm
     */
    private double tly;

    /**
     * 图章宽度
     * <p>
     * 单位毫米mm
     */
    private double width;
    /**
     * 图章高度
     * <p>
     * 单位毫米mm
     */
    private double height;

    /**
     * 构造一个普通印章位置
     *
     * @param page   页码
     * @param tlx    左上角x坐标
     * @param tly    左上角y坐标
     * @param width  宽度
     * @param height 高度
     */
    public NormalStampPos(int page, double tlx, double tly, double width, double height) {
        this.page = page;
        this.tlx = tlx;
        this.tly = tly;
        this.width = width;
        this.height = height;
    }

    public int getPage() {
        return page;
    }

    public NormalStampPos setPage(int page) {
        this.page = page;
        return this;
    }

    public double getX() {
        return tlx;
    }

    public NormalStampPos setX(double tlx) {
        this.tlx = tlx;
        return this;
    }

    public double getY() {
        return tly;
    }

    public NormalStampPos setY(double tly) {
        this.tly = tly;
        return this;
    }

    public double getWidth() {
        return width;
    }

    public NormalStampPos setWidth(double width) {
        this.width = width;
        return this;
    }

    public double getHeight() {
        return height;
    }

    public NormalStampPos setHeight(double height) {
        this.height = height;
        return this;
    }

    @Override
    public List<StampAnnot> getAppearance(OFDDir ctx, AtomicSignID idProvider) {
        // TODO 2020-4-18 10:54:43 普通签章对象转换
        throw new NotImplementedException();
    }
}
