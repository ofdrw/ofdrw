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

    /**
     * 图章在边上距离原地最近的边的偏移坐标
     * <p>
     * 单位毫米mm
     * <p>
     * 默认居中，为null
     */
    private Double offset = null;

    /**
     * 图章在边上的margin
     * <p>
     * 单位毫米mm
     * <p>
     * 默认为0
     */
    private double margin = 0;

    /**
     * 图章指定切割等份数量
     * <p>
     * 如果页面数量大于切割数量，印章将会重复。
     * <p>
     * 单位份
     * <p>
     * 默认以页数，为0
     */
    private int clipNumber = 0;

    /**
     * 右侧边居中骑缝章
     *
     * @param width  章宽度，单位毫米mm
     * @param height 章高度，单位毫米mm
     */
    public RidingStampPos(double width, double height) {
        this.width = width;
        this.height = height;
        side = Side.Right;
    }

    /**
     * 居中骑缝章
     *
     * @param side   指定图章所处的边
     * @param width  章宽度，单位毫米mm
     * @param height 章高度，单位毫米mm
     */
    public RidingStampPos(Side side, double width, double height) {
        this.side = side;
        this.width = width;
        this.height = height;
    }

    /**
     * 指定图章在边上的相对位置
     *
     * @param side   指定图章所处的边
     * @param offset 相对于原点最近的边的顶点位置，null则默认居中
     * @param width  章宽度，单位毫米mm
     * @param height 章高度，单位毫米mm
     */
    public RidingStampPos(Side side, Double offset, double width, double height) {
        this.side = side;
        this.width = width;
        this.height = height;
        this.offset = offset;
    }

    /**
     * 指定图章在边上的相对位置
     *
     * @param side   指定图章所处的边
     * @param offset 相对于原点最近的边的顶点位置，null则默认居中
     * @param width  章宽度，单位毫米mm
     * @param height 章高度，单位毫米mm
     * @param margin 页边距，单位毫米mm
     */
    public RidingStampPos(Side side, Double offset, double width, double height, double margin) {
        this.side = side;
        this.width = width;
        this.height = height;
        this.offset = offset;
        this.margin = margin;
    }

    /**
     * 指定图章在边上的相对位置
     *
     * @param side       指定图章所处的边
     * @param offset     相对于原点最近的边的顶点位置，null则默认居中
     * @param clipNumber 指定切割份数
     * @param width      章宽度，单位毫米mm
     * @param height     章高度，单位毫米mm
     * @param margin     页边距，单位毫米mm
     */
    public RidingStampPos(Side side, Double offset, Integer clipNumber, double width, double height, double margin) {
        this.side = side;
        this.width = width;
        this.height = height;
        this.offset = offset;
        this.margin = margin;
        this.clipNumber = clipNumber;
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

    public Double getOffset() {
        return offset;
    }

    public RidingStampPos setOffset(Double offset) {
        this.offset = offset;
        return this;
    }

    public double getMargin() {
        return margin;
    }

    public RidingStampPos setMargin(double margin) {
        this.margin = margin;
        return this;
    }

    public int getClipNumber() {
        return clipNumber;
    }

    public RidingStampPos setClipNumber(int clipNumber) {
        this.clipNumber = clipNumber;
        return this;
    }

    @Override
    public List<StampAnnot> getAppearance(OFDReader ctx, SignIDProvider idProvider) {

        // 总页码数
        int numPage = ctx.getNumberOfPages();
        List<StampAnnot> res = new ArrayList<>(numPage);
        boolean isClipNumber = this.clipNumber > 0 && this.clipNumber < numPage;
        int leftClipNumber = 0;
        if (side == Side.Right || side == Side.Left) {
            // 按页码平分印章图片
            double itemWith = this.width / numPage;
            if (isClipNumber) {
                itemWith = this.width / clipNumber;
                leftClipNumber = numPage % this.clipNumber;
                if (leftClipNumber == 1) {
                    leftClipNumber = this.clipNumber + 1;
                }
            }
            for (int i = 0; i < numPage; i++) {
                int index = leftClipNumber == 0 ? i : (leftClipNumber + i - numPage);
                if (numPage - i <= leftClipNumber) {
                    clipNumber = leftClipNumber;
                    itemWith = this.width / clipNumber;
                }
                Page page = ctx.getPage(i + 1);
                ST_Box pageSize = ctx.getPageSize(page);
                double x;
                ST_Box clip = null;
                if (side == Side.Right) {
                    x = pageSize.getWidth() - itemWith * (i + 1) - margin;
                    clip = new ST_Box(i * itemWith, 0, itemWith, this.height);
                    if (isClipNumber) {
                        x = pageSize.getWidth() - (Math.floorMod(index, clipNumber) + 1) * itemWith - margin;
                        clip = new ST_Box(Math.floorMod(index, clipNumber) * itemWith, 0, itemWith, this.height);
                    }
                } else {
                    x = 0 - itemWith * (numPage - 1 - i) + margin;
                    clip = new ST_Box((numPage - 1 - i) * itemWith, 0, itemWith, this.height);
                    if (isClipNumber) {
                        x = 0 - itemWith * (Math.floorMod(index, clipNumber)) + margin;
                        clip = new ST_Box(Math.floorMod(index, clipNumber) * itemWith, 0, itemWith, this.height);
                    }
                }

                double y;
                if (this.offset == null) {
                    // 居中
                    y = pageSize.getHeight() / 2 - this.height / 2;
                } else {
                    y = this.offset;
                }

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
            if (isClipNumber) {
                itemHeight = this.height / clipNumber;
                leftClipNumber = numPage % this.clipNumber;
                if (leftClipNumber == 1) {
                    leftClipNumber = this.clipNumber + 1;
                }
            }
            for (int i = 0; i < numPage; i++) {
                int index = leftClipNumber == 0 ? i : (leftClipNumber + i - numPage);
                if (numPage - i <= leftClipNumber) {
                    clipNumber = leftClipNumber;
                    itemHeight = this.width / clipNumber;
                }
                Page page = ctx.getPage(i + 1);
                ST_Box pageSize = ctx.getPageSize(page);

                double x;
                if (this.offset == null) {
                    // 居中
                    x = pageSize.getWidth() / 2 - this.width / 2;
                } else {
                    x = this.offset;
                }

                double y;
                ST_Box clip = null;
                if (side == Side.Bottom) {
                    y = pageSize.getHeight() - itemHeight * (i + 1) - margin;
                    clip = new ST_Box(0, itemHeight * i, this.width, itemHeight);
                    if (isClipNumber) {
                        y = pageSize.getHeight() - (Math.floorMod(index, clipNumber) + 1) * itemHeight - margin;
                        clip = new ST_Box(0, itemHeight * Math.floorMod(index, clipNumber), this.width, itemHeight);
                    }

                } else {
                    y = 0 - itemHeight * (numPage - 1 - i) + margin;
                    clip = new ST_Box(0, (numPage - 1 - i) * itemHeight, this.width, itemHeight);
                    if (isClipNumber) {
                        y = 0 - itemHeight * Math.floorMod(index, clipNumber) + margin;
                        clip = new ST_Box(0, Math.floorMod(index, clipNumber) * itemHeight, this.width, itemHeight);
                    }
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