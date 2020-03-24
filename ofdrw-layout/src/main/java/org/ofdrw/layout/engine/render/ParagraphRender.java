package org.ofdrw.layout.engine.render;

import org.ofdrw.core.basicStructure.pageObj.layer.CT_Layer;
import org.ofdrw.layout.element.Paragraph;
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
            return;
        }

        /*
        每一行左上角坐标
         */
        double lineTopX = e.getX() + e.getMarginLeft() + e.getBorderLeft() + e.getPaddingLeft();
        double lineTopY = e.getY() + e.getMarginTop() + e.getBorderTop() + e.getPaddingTop();
        for (TxtLineBlock txtLine : lines) {
            double offsetX = lineTopX;


        }
    }
}
