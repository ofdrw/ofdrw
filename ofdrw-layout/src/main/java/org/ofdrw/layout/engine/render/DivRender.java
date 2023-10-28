package org.ofdrw.layout.engine.render;

import org.ofdrw.core.basicStructure.pageObj.layer.block.CT_PageBlock;
import org.ofdrw.core.basicStructure.pageObj.layer.block.PathObject;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.graph.pathObj.AbbreviatedData;
import org.ofdrw.core.pageDescription.color.color.CT_Color;
import org.ofdrw.layout.element.Div;
import org.ofdrw.layout.engine.GraphHelper;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 盒式模型渲染器
 * <p>
 * {@link org.ofdrw.layout.element.Div} 的渲染器
 *
 * @author 权观宇
 * @since 2020-03-21 14:18:40
 */
public class DivRender {
    /**
     * 渲染Div元素到指定图层
     *
     * @param layer     图层
     * @param e         元素
     * @param maxUnitID 自增的最大ID提供器
     */
    public static void render(CT_PageBlock layer, Div e, AtomicInteger maxUnitID) {
        final int[] bgColor = e.getBackgroundColor();
        if (bgColor == null && e.isNoBorder()) {
            // 没有背景颜色没有边框，那么这个Div就不需要绘制
            return;
        }

        // 图元透明度
        Integer alpha = null;
        if (e.getOpacity() != null) {
            alpha = (int) (e.getOpacity() * 255);
        }

        if (e.getHeight() == null) {
            throw new IllegalArgumentException("Div元素的高度必须指定");
        }
        /*
           基础的盒式模型绘制：
            1. 首先绘制背景颜色
            2. 绘制边框
         */
        // 背景颜色 (又背景颜色并且，内容存在高度)
        double eleContentHeight = e.getPaddingTop() + e.getHeight() + e.getPaddingBottom();
        if (bgColor != null && eleContentHeight > 0) {
            ST_ID objId = new ST_ID(maxUnitID.incrementAndGet());
            PathObject bg = new PathObject(objId);
            double x = e.getX() + e.getMarginLeft() + e.getBorderLeft();
            double y = e.getY() + e.getMarginTop() + e.getBorderTop();
            double w = e.getPaddingLeft() + e.getWidth() + e.getPaddingRight();
            bg.setBoundary(x, y, w, eleContentHeight)
                    // 设置填充颜色的矩形区域
                    .setAbbreviatedData(GraphHelper.rect(0, 0, w, eleContentHeight))
                    // 设置不描边、填充，并且设置填充颜色
                    .setStroke(false)
                    .setFill(true)
                    .setFillColor(CT_Color.rgb(bgColor));
            if (alpha != null) {
                bg.setAlpha(alpha);
            }
            // 加入图层
            layer.addPageBlock(bg);
        }
        /*
        边框的绘制有两种情况:
        1. 4条边宽度都一致。
        2. 4条边宽度不一致。
         */
        if (!e.isNoBorder()) {
            // 4条边宽度都一致，那么直接定位并且绘制
            if (e.getBorderTop().equals(e.getBorderRight())
                    && e.getBorderRight().equals(e.getBorderBottom())
                    && e.getBorderBottom().equals(e.getBorderLeft())) {
                double lineWidth = e.getBorderTop();
                ST_ID objId = new ST_ID(maxUnitID.incrementAndGet());
                PathObject border = new PathObject(objId);
                double x = e.getX() + e.getMarginLeft();
                double y = e.getY() + e.getMarginTop();
                double w = lineWidth + e.getPaddingLeft() + e.getWidth() + e.getPaddingRight() + lineWidth;
                double h = lineWidth + e.getPaddingTop() + e.getHeight() + e.getPaddingBottom() + lineWidth;
                border.setBoundary(x, y, w, h)
                        .setLineWidth(lineWidth)
                        .setAbbreviatedData(GraphHelper.rect(
                                lineWidth / 2, lineWidth / 2,
                                w - lineWidth,
                                h - lineWidth));
                // 如果存在边框颜色，那么设置颜色；默认颜色为 黑色
                int[] borderColor = e.getBorderColor();
                if (borderColor != null) {
                    border.setStrokeColor(CT_Color.rgb(borderColor));
                }
                if (alpha != null) {
                    border.setAlpha(alpha);
                }
                layer.addPageBlock(border);
            }
            // 4条边宽度不一致，需要分别绘制各条边
            else {
                /*
                顶边
                 */
                double topWidth = e.getBorderTop();
                if (topWidth != 0d) {
                    ST_ID objId = new ST_ID(maxUnitID.incrementAndGet());
                    PathObject topBorder = new PathObject(objId);
                    double x = e.getX() + e.getMarginLeft();
                    double y = e.getY() + e.getMarginTop();
                    double w = e.getBorderLeft() + e.getPaddingLeft() + e.getWidth() + e.getPaddingRight() + e.getBorderRight();
                    topBorder.setBoundary(x, y, w, topWidth)
                            .setLineWidth(topWidth)
                            .setAbbreviatedData(new AbbreviatedData().M(0, topWidth / 2).L(w, topWidth / 2));
                    int[] borderColor = e.getBorderColor();
                    if (borderColor != null) {
                        topBorder.setStrokeColor(CT_Color.rgb(borderColor));
                    }
                    if (alpha != null) {
                        topBorder.setAlpha(alpha);
                    }
                    layer.addPageBlock(topBorder);
                }
                /*
                底边
                 */
                double bottomWidth = e.getBorderBottom();
                if (bottomWidth != 0) {
                    ST_ID objId = new ST_ID(maxUnitID.incrementAndGet());
                    PathObject bottomBorder = new PathObject(objId);
                    double x = e.getX() + e.getMarginLeft();
                    double y = e.getY()
                            + e.getMarginTop()
                            + e.getBorderTop()
                            + e.getPaddingTop()
                            + e.getHeight()
                            + e.getPaddingBottom();
                    double w = e.getBorderLeft() + e.getPaddingLeft() + e.getWidth() + e.getPaddingRight() + e.getBorderRight();
                    bottomBorder.setBoundary(x, y, w, bottomWidth)
                            .setLineWidth(bottomWidth)
                            .setAbbreviatedData(new AbbreviatedData().M(0, bottomWidth / 2).L(w, bottomWidth / 2));
                    int[] borderColor = e.getBorderColor();
                    if (borderColor != null) {
                        bottomBorder.setStrokeColor(CT_Color.rgb(borderColor));
                    }
                    if (alpha != null) {
                        bottomBorder.setAlpha(alpha);
                    }
                    layer.addPageBlock(bottomBorder);
                }
                // 元素中没有任何内容和边框，那么认为是占位符，跳过绘制
                if ((topWidth + bottomWidth + eleContentHeight) == 0) {
                    return;
                }
                /*
                左边
                 */
                double leftWidth = e.getBorderLeft();
                if (leftWidth != 0) {
                    ST_ID objId = new ST_ID(maxUnitID.incrementAndGet());
                    PathObject leftBorder = new PathObject(objId);
                    double x = e.getX() + e.getMarginLeft();
                    double y = e.getY() + e.getMarginTop() + topWidth;
                    double h = e.getBorderTop() + e.getPaddingTop()
                            + e.getHeight() + e.getPaddingBottom() + e.getBorderBottom()
                            - topWidth - bottomWidth;
                    leftBorder.setBoundary(x, y, leftWidth, h)
                            .setLineWidth(leftWidth)
                            .setAbbreviatedData(new AbbreviatedData().M(leftWidth / 2, 0).L(leftWidth / 2, h));
                    int[] borderColor = e.getBorderColor();
                    if (borderColor != null) {
                        leftBorder.setStrokeColor(CT_Color.rgb(borderColor));
                    }
                    if (alpha != null) {
                        leftBorder.setAlpha(alpha);
                    }
                    layer.addPageBlock(leftBorder);
                }

                /*
                右边
                 */
                double rightWidth = e.getBorderRight();
                if (rightWidth != 0) {
                    ST_ID objId = new ST_ID(maxUnitID.incrementAndGet());
                    PathObject rightBorder = new PathObject(objId);
                    double x = e.getX()
                            + e.getMarginLeft()
                            + e.getBorderLeft()
                            + e.getPaddingLeft()
                            + e.getWidth()
                            + e.getPaddingRight();
                    double y = e.getY() + e.getMarginTop() + topWidth;
                    double h = e.getBorderTop() + e.getPaddingTop()
                            + e.getHeight() + e.getPaddingBottom() + e.getBorderBottom()
                            - topWidth - bottomWidth;
                    rightBorder.setBoundary(x, y, rightWidth, h)
                            .setLineWidth(rightWidth)
                            .setAbbreviatedData(new AbbreviatedData().M(rightWidth / 2, 0).L(rightWidth / 2, h));
                    int[] borderColor = e.getBorderColor();
                    if (borderColor != null) {
                        rightBorder.setStrokeColor(CT_Color.rgb(borderColor));
                    }
                    if (alpha != null) {
                        rightBorder.setAlpha(alpha);
                    }
                    layer.addPageBlock(rightBorder);
                }
            }
        }
    }
}
