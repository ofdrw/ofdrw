package org.ofdrw.sign.stamppos;

import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.signatures.appearance.StampAnnot;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.sign.SignIDProvider;

import java.util.ArrayList;
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
    public List<StampAnnot> getAppearance(OFDReader ctx, SignIDProvider idProvider) {

        // 总页码数
        int numPage = ctx.getNumberOfPages();
        List<StampAnnot> res = new ArrayList<>(numPage);

        if (side == Side.Right || side == Side.Left) {
            // 按页码平分印章图片
            double itemWith = this.width / numPage;
            for (int i = 0; i < numPage; i++) {

                Page page = ctx.getPage(i + 1);
                ST_Box pageSize = ctx.getPageSize(page);
                double x;
                ST_Box clip = null;
                if (side == Side.Right) {
                    x = pageSize.getWidth() - itemWith * (i + 1);
                    clip = new ST_Box(i * itemWith, 0, itemWith, this.height);
                } else {
                    x = 0 - itemWith * (numPage - 1 - i);
                    clip = new ST_Box((numPage - 1 - i) * itemWith, 0, itemWith, this.height);
                }

                double y = pageSize.getHeight() / 2 - this.height / 2;

                ST_RefID ref = ctx.getPageObjectId(i + 1).ref();
                StampAnnot annot = new StampAnnot()
                        .setID(idProvider.incrementAndGet())
                        .setBoundary(new ST_Box(x, y, this.width, this.height))
                        .setPageRef(ref)
                        .setClip(clip);
                res.add(annot);
            }
        } else {
            double itemHeight = this.height / numPage;
            for (int i = 0; i < numPage; i++) {

                Page page = ctx.getPage(i + 1);
                ST_Box pageSize = ctx.getPageSize(page);

                double x = pageSize.getWidth() / 2 - this.width / 2;
                double y;
                ST_Box clip = null;
                if (side == Side.Bottom) {
                    y = pageSize.getHeight() - itemHeight * (i + 1);
                    clip = new ST_Box(0, itemHeight * i, this.width, itemHeight);
                } else {
                    y = 0 - itemHeight * (numPage - 1 - i);
                    clip = new ST_Box(0, (numPage - 1 - i) * itemHeight,  this.width, itemHeight);
                }

                ST_RefID ref = ctx.getPageObjectId(i + 1).ref();
                StampAnnot annot = new StampAnnot()
                        .setID(idProvider.incrementAndGet())
                        .setBoundary(new ST_Box(x, y, this.width, this.height))
                        .setPageRef(ref)
                        .setClip(clip);
                res.add(annot);
            }

        }
        return res;
    }
}