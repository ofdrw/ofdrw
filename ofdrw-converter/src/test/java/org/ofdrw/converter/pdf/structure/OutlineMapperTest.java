package org.ofdrw.converter.pdf.structure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ofdrw.core.action.Actions;
import org.ofdrw.core.action.CT_Action;
import org.ofdrw.core.action.EventType;
import org.ofdrw.core.action.actionType.actionGoto.CT_Dest;
import org.ofdrw.core.action.actionType.actionGoto.DestType;
import org.ofdrw.core.action.actionType.actionGoto.Goto;
import org.ofdrw.core.basicStructure.outlines.CT_OutlineElem;
import org.ofdrw.core.basicType.ST_RefID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 书签映射测试（决策 25：仅映射标题与目标，不做样式）
 *
 * @author 权观宇
 * @since 2.5.1
 */
class OutlineMapperTest {

    /**
     * 容限
     */
    private static final double EPS = 1e-6;

    /**
     * 页对象 ID → 页码（从 0 起）
     */
    private static Map<String, Integer> pages() {
        Map<String, Integer> map = new HashMap<>();
        map.put("10", 0);
        map.put("20", 1);
        map.put("30", 2);
        return map;
    }

    /**
     * 构造带 Goto 目标的大纲项
     *
     * @param title 标题
     * @param type  目标类型
     * @param pageId 目标页对象 ID
     * @param left  左侧，单位 mm
     * @param top   顶部，单位 mm
     * @return 大纲项
     */
    private static CT_OutlineElem elem(String title, DestType type, long pageId, Double left, Double top) {
        CT_Dest dest = new CT_Dest();
        dest.setType(type);
        dest.setPageID(new ST_RefID(pageId));
        if (left != null) {
            dest.setLeft(left);
        }
        if (top != null) {
            dest.setTop(top);
        }
        Goto gotoAction = new Goto(dest);
        CT_OutlineElem elem = new CT_OutlineElem(title);
        elem.setActions(new Actions().addAction(new CT_Action(EventType.CLICK, gotoAction)));
        return elem;
    }

    @Test
    @DisplayName("标题与目标：五种目标类型均映射，层级用子节点表达")
    void mapsTitleAndDestination() {
        CT_OutlineElem chapter = elem("第一章", DestType.XYZ, 10L, 10d, 20d);
        chapter.addOutlineElem(elem("第一节", DestType.Fit, 20L, null, null));
        chapter.addOutlineElem(elem("第二节", DestType.FitH, 30L, null, 5d));
        List<CT_OutlineElem> outlines = new ArrayList<>();
        outlines.add(chapter);
        outlines.add(elem("附录", DestType.FitR, 30L, 1d, 2d));

        OutlineMapper.Result<OutlineMapper.BookmarkNode> result = new OutlineMapper(pages()).map(outlines);

        assertEquals(2, result.getItems().size());
        List<String> flat = new ArrayList<>();
        for (OutlineMapper.BookmarkNode node : result.getItems()) {
            node.flatten(0, flat);
        }
        assertEquals(java.util.Arrays.asList("0:第一章->p0", "1:第一节->p1", "1:第二节->p2", "0:附录->p2"), flat);
        assertEquals(PdfDestination.Type.XYZ, result.getItems().get(0).getDestination().getType());
        assertEquals(PdfDestination.Type.FIT_R, result.getItems().get(1).getDestination().getType());
    }

    @Test
    @DisplayName("目标坐标：OFD 页面坐标（mm）→ PDF 坐标（pt，Y 向上）")
    void destinationConvertsToPdfCoordinates() {
        List<CT_OutlineElem> outlines = new ArrayList<>();
        outlines.add(elem("标题", DestType.XYZ, 10L, 10d, 20d));
        PdfDestination dest = new OutlineMapper(pages()).map(outlines).getItems().get(0).getDestination();

        // 页面高度 297mm：y_pdf = 297 - 20 = 277mm = 785.2pt
        PdfDestination pdf = dest.toPdfCoordinates(0, 297);
        assertEquals(org.ofdrw.converter.pdf.CoordSystem.mmToPt(10), pdf.getLeft(), EPS);
        assertEquals(org.ofdrw.converter.pdf.CoordSystem.mmToPt(277), pdf.getTop(), EPS);
        assertNull(pdf.getRight());
        assertNull(pdf.getBottom());
    }

    @Test
    @DisplayName("决策 25：不做样式，仅保留结构信息（Expanded 原样带出）")
    void keepsStructureOnly() {
        CT_OutlineElem elem = elem("含子项", DestType.Fit, 10L, null, null);
        elem.setExpanded(true);
        elem.addOutlineElem(elem("子项", DestType.Fit, 20L, null, null));
        List<CT_OutlineElem> outlines = new ArrayList<>();
        outlines.add(elem);

        OutlineMapper.BookmarkNode node = new OutlineMapper(pages()).map(outlines).getItems().get(0);
        assertEquals(Boolean.TRUE, node.getExpanded());
        assertEquals(1, node.getChildren().size());
    }

    @Test
    @DisplayName("容错：目标缺失/类型不支持时保留标题并告警")
    void missingDestinationKeepsTitle() {
        CT_OutlineElem noAction = new CT_OutlineElem("无目标");
        List<CT_OutlineElem> outlines = new ArrayList<>();
        outlines.add(noAction);

        OutlineMapper.Result<OutlineMapper.BookmarkNode> result = new OutlineMapper(pages()).map(outlines);
        assertEquals(1, result.getItems().size());
        assertEquals("无目标", result.getItems().get(0).getTitle());
        assertNull(result.getItems().get(0).getDestination());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("仅保留标题")), result.getWarnings().toString());
    }

    @Test
    @DisplayName("容错：空大纲返回空结果，无告警")
    void emptyOutline() {
        OutlineMapper.Result<OutlineMapper.BookmarkNode> result = new OutlineMapper(pages()).map(new ArrayList<>());
        assertTrue(result.isEmpty());
        assertTrue(result.getWarnings().isEmpty());
    }
}
