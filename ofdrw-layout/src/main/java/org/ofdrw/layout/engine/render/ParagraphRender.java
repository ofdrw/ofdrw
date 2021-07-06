package org.ofdrw.layout.engine.render;

import org.ofdrw.core.basicStructure.pageObj.layer.CT_Layer;
import org.ofdrw.core.basicStructure.pageObj.layer.block.CT_PageBlock;
import org.ofdrw.core.basicStructure.pageObj.layer.block.PathObject;
import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.graph.pathObj.AbbreviatedData;
import org.ofdrw.core.pageDescription.color.color.CT_Color;
import org.ofdrw.core.text.TextCode;
import org.ofdrw.core.text.text.Weight;
import org.ofdrw.layout.element.Paragraph;
import org.ofdrw.layout.element.PlaceholderSpan;
import org.ofdrw.layout.element.Span;
import org.ofdrw.layout.element.TxtLineBlock;
import org.ofdrw.layout.engine.ResManager;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 段落渲染器
 *
 * @author 权观宇
 * @since 2020-03-24 04:31:37
 */
public class ParagraphRender {

    /**
     * 将段落渲染到图层上
     *
     * @param layer      图层
     * @param resManager 资源管理器
     * @param e          段落对象
     * @param maxUnitID  对象ID提供者
     */
    public static void render(CT_PageBlock layer, ResManager resManager, Paragraph e, AtomicInteger maxUnitID) {
        if (e == null) {
            return;
        }
        LinkedList<TxtLineBlock> lines = e.getLines();
        // 段落中没有任何内容，那么跳过不渲染
        if (lines == null || lines.isEmpty()) {
            // 直接加入到虚拟页面的段落对象是经过预处理的，所以此处尝试预处理。
            e.doPrepare(e.getWidth() + e.widthPlus());
            lines = e.getLines();
            if (lines == null || lines.isEmpty()) {
                // 如果还是没有行，那么是个空段落，跳过
                return;
            }
        }

        // 可容纳元素的总高度
        Double containerHeight = e.getHeight();
        /*
        每一行左上角坐标
         */
        double lineTopX = e.getX() + e.getMarginLeft() + e.getBorderLeft() + e.getPaddingLeft();
        double offsetY = e.getY() + e.getMarginTop() + e.getBorderTop() + e.getPaddingTop();
        // 已经写入的元素高度统计
        double hCount = 0;
        // 渲染每一行文字
        for (TxtLineBlock txtLine : lines) {
            // 行内的X偏移量为行开头
            double offsetX;
            // 根据浮动方式确定起始字符坐标
            switch (txtLine.getTextAlign()) {
                case right:
                    offsetX = lineTopX + (txtLine.getLineMaxAvailableWidth() - txtLine.getWidth());
                    break;
                case center:
                    offsetX = lineTopX + (txtLine.getLineMaxAvailableWidth() - txtLine.getWidth()) / 2;
                    break;
                case left:
                default:
                    offsetX = lineTopX;
                    break;
            }

            // 一行内的所有Span的图元高度都为行高度（最高文字高度 + 行间距）
            double h = txtLine.getHeight();
            if (hCount + h > containerHeight) {
                // 如果文字内容高度大于容纳文字的容器高度，那么将该部分内容舍弃，不渲染。
                break;
            }

            // 遍历行内的每个Span
            for (Span s : txtLine.getInlineSpans()) {
                // 文字图元宽度
                double w = s.blockSize().getWidth();
                if (w == 0) {
                    // 空行
                    continue;
                }
                if (s instanceof PlaceholderSpan) {
                    // 忽略占位符的渲染只进行坐标偏移
                    offsetX += w;
                    continue;
                }

                // 将字体加入到资源中
                ST_ID id = null;
                try {
                    id = resManager.addFont(s.getFont());
                } catch (IOException ex) {
                    throw new RenderException("渲染异常，字体复制失败：" + ex.getMessage(), ex);
                }
                // 新建字体对象
                TextObject txtObj = new TextObject(maxUnitID.incrementAndGet());
                ST_Box boundary = new ST_Box(offsetX, offsetY, w, h);
                txtObj.setBoundary(boundary)
                        // 设置字体ID
                        .setFont(id.ref())
                        // 设置字体大小
                        .setSize(s.getFontSize());
                // 判断字体是否加粗
                if (s.isBold()) {
                    txtObj.setWeight(Weight.W_800);
                }
                // 是否是斜体
                if (s.isItalic()) {
                    txtObj.setItalic(true);
                }
                // 是否填充，默认为true表示填充
                if (!s.isFill()) {
                    txtObj.setFill(false);
                }
                // 设置字体颜色，默认颜色为黑色
                int[] color = s.getColor();
                if (color != null && color.length >= 3) {
                    txtObj.setFillColor(CT_Color.rgb(color));
                }
                // 创建OFD文字定位对象
                Double offset = txtLine.getMaxSpanHeight();
                TextCode tcSTTxt = new TextCode()
                        // 定位点位于文字的左下角，文字文字Y偏移量为该行最高文字的高度
                        .setCoordinate(0d, offset)
                        .setContent(s.getText());
                Double[] deltaX = s.getDeltaX();
                if (deltaX.length > 0) {
                    // 如果多余一个字符那么需要设置字符偏移量
                    tcSTTxt.setDeltaX(deltaX);
                }
                // 加入到字符对象中
                txtObj.addTextCode(tcSTTxt);
                if (e.getOpacity() != null) {
                    // 图元透明度
                    txtObj.setAlpha((int) (e.getOpacity() * 255));
                }
                // 将文字对象加入到图层
                layer.addPageBlock(txtObj);
                // 是否包含下划线
                if (s.isUnderline()) {
                    ST_ID underlineId = new ST_ID(maxUnitID.incrementAndGet());
                    // 构造下划线
                    PathObject underline = drawUnderline(underlineId, boundary, offset);
                    if (e.getOpacity() != null) {
                        // 图元透明度
                        underline.setAlpha((int) (e.getOpacity() * 255));
                    }
                    // 加入到文字对象的上方
                    layer.addPageBlock(underline);
                }
                // 计算行内下一个图元的X坐标
                offsetX += w;
            }
            offsetY += h;
            hCount += h;
        }
    }


    /**
     * 绘制文字的下划线
     *
     * @param id       下划线ID
     * @param boundary 绘制下划线的区域
     * @param offset   在绘制区域内Y的偏移量
     * @return 路径对象
     */
    private static PathObject drawUnderline(ST_ID id, ST_Box boundary, double offset) {
        PathObject res = new PathObject(id);
        offset += 1.2d;
        res.setBoundary(boundary)
                .setAbbreviatedData(new AbbreviatedData().M(0, offset).lineTo(boundary.getWidth(), offset))
                .setLineWidth(0.353);
        return res;
    }
}
