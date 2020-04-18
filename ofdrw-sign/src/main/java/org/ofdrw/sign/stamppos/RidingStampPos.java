package org.ofdrw.sign.stamppos;

import org.ofdrw.core.signatures.appearance.StampAnnot;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.sign.AtomicSignID;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.List;

/**
 * 骑缝章位置
 * <p>
 * 默认图章放在边的正中央
 *
 * @author 权观宇
 * @since 2020-04-18 10:07:08
 */
public class RidingStampPos implements StampAppearance {
    /**
     * 默认骑缝章以右侧边作为骑缝的位置
     */
    private Side side;

    /**
     * 图章整章宽度
     * <p>
     * 单位毫米mm
     */
    private double width;

    /**
     * 图章整章高度
     * <p>
     * 单位毫米mm
     */
    private double height;

    public RidingStampPos(double width, double height) {
        this.width = width;
        this.height = height;
        side = Side.Right;
    }

    public RidingStampPos(Side side, double width, double height) {
        this.side = side;
        this.width = width;
        this.height = height;
    }


    public Side getSide() {
        return side;
    }

    public RidingStampPos setSide(Side side) {
        this.side = side;
        return this;
    }

    public double getWidth() {
        return width;
    }

    public RidingStampPos setWidth(double width) {
        this.width = width;
        return this;
    }

    public double getHeight() {
        return height;
    }

    public RidingStampPos setHeight(double height) {
        this.height = height;
        return this;
    }

    @Override
    public List<StampAnnot> getAppearance(OFDDir ctx, AtomicSignID idProvider) {
        // TODO 2020-4-18 10:55:25 骑缝章对象转换
        throw new NotImplementedException();
    }
}
