package org.ofdrw.layout.engine.render;

import org.ofdrw.core.basicStructure.pageObj.layer.CT_Layer;
import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.text.TextCode;
import org.ofdrw.core.text.text.Weight;
import org.ofdrw.layout.element.Paragraph;
import org.ofdrw.layout.element.Span;
import org.ofdrw.layout.element.TxtLineBlock;
import org.ofdrw.layout.engine.ResManager;

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
    public static void render(CT_Layer layer, ResManager resManager, Paragraph e, AtomicInteger maxUnitID) {
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
        /*
        每一行左上角坐标
         */
        double lineTopX = e.getX() + e.getMarginLeft() + e.getBorderLeft() + e.getPaddingLeft();
        double offsetY = e.getY() + e.getMarginTop() + e.getBorderTop() + e.getPaddingTop();
        // 渲染每一行文字
        for (TxtLineBlock txtLine : lines) {
            // 行内的X偏移量为行开头
            double offsetX = lineTopX;
            // 一行内的所有Span的图元高度都为行高度（最高文字高度 + 行间距）
            double h = txtLine.getHeight();
            // 遍历行内的每个Span
            for (Span s : txtLine.getInlineSpans()) {
                // 将字体加入到资源中
                ST_ID id = resManager.addFont(s.getFont());
                // 新建字体对象
                TextObject txtObj = new TextObject(maxUnitID.incrementAndGet());
                // 文字图元宽度
                double w = s.blockSize().getWidth();
                txtObj.setBoundary(offsetX, offsetY, w, h)
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
                // 是否包含下划线
                if (s.isUnderline()) {
                    // TODO 2020-3-24 19:30:34 下划线暂时不实现
                }
                // 创建OFD文字定位对象
                TextCode tcSTTxt = new TextCode()
                        // 定位点位于文字的左下角，文字文字Y偏移量为该行最高文字的高度
                        .setCoordinate(0d, txtLine.getMaxSpanHeight())
                        .setContent(s.getText());
                Double[] deltaX = s.getDeltaX();
                if (deltaX.length > 0) {
                    // 如果多余一个字符那么需要设置字符偏移量
                    tcSTTxt.setDeltaX(deltaX);
                }
                // 加入到字符对象中
                txtObj.addTextCode(tcSTTxt);
                // 将文字对象加入到图层
                layer.addPageBlock(txtObj);
                // 计算行内下一个图元的X坐标
                offsetX += w;
            }
            offsetY += h;
        }
    }
}
