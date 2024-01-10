package org.ofdrw.layout.engine.render;

import org.ofdrw.core.basicStructure.pageObj.layer.block.CT_PageBlock;
import org.ofdrw.core.basicStructure.pageObj.layer.block.PathObject;
import org.ofdrw.core.basicType.STBase;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.graph.pathObj.AbbreviatedData;
import org.ofdrw.core.pageDescription.CT_GraphicUnit;
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
    public static void render(CT_PageBlock layer, Div<?> e, AtomicInteger maxUnitID) {
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

        Double borderTop = e.getBorderTop();
        Double borderRight = e.getBorderRight();
        Double borderBottom = e.getBorderBottom();
        Double borderLeft = e.getBorderLeft();

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
            double x = e.getX() + e.getMarginLeft() + borderLeft;
            double y = e.getY() + e.getMarginTop() + borderTop;
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

        // 虚线边框样式：[偏移量,虚线长,空白长, 虚线长,空白长, 虚线长,空白长, 虚线长,空白长, ...]
        Double[] borderDash = e.getBorderDash();

        // 4条边宽度都一致，那么直接定位并且绘制
        if (eq(borderTop, borderRight, borderBottom, borderLeft)) {
            double lineWidth = borderTop;
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
            if (borderDash != null) {
                setLineDash(border, borderDash);
            }
            layer.addPageBlock(border);
        }
        // 4条边宽度不一致，需要分别绘制各条边
        else {
            /*
                顶边
             */
            double topWidth = borderTop;
            double ZERO  = 0.00001;
            if (topWidth > ZERO) {
                ST_ID objId = new ST_ID(maxUnitID.incrementAndGet());
                PathObject topBorder = new PathObject(objId);
                double x = e.getX() + e.getMarginLeft();
                double y = e.getY() + e.getMarginTop();
                double w = borderLeft + e.getPaddingLeft() + e.getWidth() + e.getPaddingRight() + borderRight;
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
                if (borderDash != null) {
                    setLineDash(topBorder, borderDash);
                }
                layer.addPageBlock(topBorder);
            }
            /*
                底边
             */
            double bottomWidth = borderBottom;
            if (bottomWidth > ZERO) {
                ST_ID objId = new ST_ID(maxUnitID.incrementAndGet());
                PathObject bottomBorder = new PathObject(objId);
                double x = e.getX() + e.getMarginLeft();
                double y = e.getY()
                        + e.getMarginTop()
                        + borderTop
                        + e.getPaddingTop()
                        + e.getHeight()
                        + e.getPaddingBottom();
                double w = borderLeft + e.getPaddingLeft() + e.getWidth() + e.getPaddingRight() + borderRight;
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
                if (borderDash != null) {
                    setLineDash(bottomBorder, borderDash);
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
            double leftWidth = borderLeft;
            if (leftWidth > ZERO) {
                ST_ID objId = new ST_ID(maxUnitID.incrementAndGet());
                PathObject leftBorder = new PathObject(objId);
                double x = e.getX() + e.getMarginLeft();
                double y = e.getY() + e.getMarginTop() + topWidth;
                double h = borderTop + e.getPaddingTop()
                        + e.getHeight() + e.getPaddingBottom() + borderBottom
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
                if (borderDash != null) {
                    setLineDash(leftBorder, borderDash);
                }
                layer.addPageBlock(leftBorder);
            }

            /*
                右边
             */
            double rightWidth = borderRight;
            if (rightWidth > ZERO) {
                ST_ID objId = new ST_ID(maxUnitID.incrementAndGet());
                PathObject rightBorder = new PathObject(objId);
                double x = e.getX()
                        + e.getMarginLeft()
                        + borderLeft
                        + e.getPaddingLeft()
                        + e.getWidth()
                        + e.getPaddingRight();
                double y = e.getY() + e.getMarginTop() + topWidth;
                double h = borderTop + e.getPaddingTop()
                        + e.getHeight() + e.getPaddingBottom() + borderBottom
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
                if (borderDash != null) {
                    setLineDash(rightBorder, borderDash);
                }
                layer.addPageBlock(rightBorder);
            }
        }
    }

    /**
     * 设置线条虚线样式
     *
     * @param g    图元
     * @param dash 虚线样式，[偏移量,虚线长,空白长, 虚线长,空白长, 虚线长,空白长, 虚线长,空白长, ...]
     */
    private static void setLineDash(CT_GraphicUnit<?> g, Double[] dash) {
        if (dash == null || dash.length == 0 || (dash.length == 1 && dash[0] == null)) {
            return;
        }
        if (dash.length == 1) {
            // 单个参数，设置虚线长度与空白相同
            dash = new Double[]{0.0, dash[0], dash[0]};
        }
        // 设置 虚线偏移量，忽略double精度误差 0.000001
        if (dash[0] != null && dash[0] >= 0.000001) {
            g.setDashOffset(dash[0]);
        }
        // 截取 dash[0:] 创建 ST_Array
        ST_Array dashPattern = new ST_Array();
        for (int i = 1; i < dash.length; i++) {
            dashPattern.add(STBase.fmt(dash[i]));
        }
        g.setDashPattern(dashPattern);
    }

    /**
     * 比较两个double是否相等 忽略精度
     *
     * @param arr 数组
     * @return true - 相等, false - 不相等
     */
    public static boolean eq(double ...arr) {
        if (arr == null || arr.length == 0) {
            return true;
        }
        String last = null;
        for (double v : arr) {
            String fmt = STBase.fmt(v);
            if (last == null) {
                last = fmt;
                continue;
            }
            if (!last.equals(fmt)) {
                return false;
            }
        }
        return true;
    }
}
