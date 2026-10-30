package org.ofdrw.converter.pdf.structure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ofdrw.core.action.Actions;
import org.ofdrw.core.action.CT_Action;
import org.ofdrw.core.action.EventType;
import org.ofdrw.core.action.actionType.URI;
import org.ofdrw.core.action.actionType.actionGoto.CT_Dest;
import org.ofdrw.core.action.actionType.actionGoto.DestType;
import org.ofdrw.core.action.actionType.actionGoto.Goto;
import org.ofdrw.core.action.actionType.actionMovie.Movie;
import org.ofdrw.core.annotation.pageannot.Annot;
import org.ofdrw.core.annotation.pageannot.AnnotType;
import org.ofdrw.core.annotation.pageannot.Appearance;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_RefID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 注释映射测试（设计 3.8 节、决策 18）
 *
 * @author 权观宇
 * @since 2.5.1
 */
class AnnotationMapperTest {

    /**
     * 容限
     */
    private static final double EPS = 1e-6;

    /**
     * 构造注释
     *
     * @param id      注释 ID
     * @param type    注释类型
     * @param actions 动作列表，可为空
     * @return 注释
     */
    private static Annot annot(long id, AnnotType type, Actions actions) {
        Annot annot = new Annot();
        annot.setID(new ST_ID(id));
        annot.setType(type);
        annot.setAppearance(new Appearance(new ST_Box(10, 20, 30, 40)));
        if (actions != null) {
            annot.add(actions);
        }
        return annot;
    }

    /**
     * 构造 URI 动作
     *
     * @param uri 链接
     * @return 动作列表
     */
    private static Actions uri(String uri) {
        return new Actions().addAction(new CT_Action(EventType.CLICK, new URI(uri)));
    }

    /**
     * 页对象 ID → 页码
     *
     * @return 映射
     */
    private static Map<String, Integer> pages() {
        Map<String, Integer> map = new HashMap<>();
        map.put("10", 0);
        map.put("20", 1);
        return map;
    }

    @Test
    @DisplayName("类型映射：Link→Link，Highlight→Highlight，Path/Stamp/Watermark→Stamp")
    void mapsAnnotationTypes() {
        List<Annot> annots = new ArrayList<>();
        annots.add(annot(1, AnnotType.Link, uri("https://ofdrw.github.io")));
        annots.add(annot(2, AnnotType.Highlight, null));
        annots.add(annot(3, AnnotType.Path, null));
        annots.add(annot(4, AnnotType.Stamp, null));
        annots.add(annot(5, AnnotType.Watermark, null));

        AnnotationMapper.Result<AnnotationMapper.AnnotationTarget> result =
                new AnnotationMapper(pages(), new HashMap<>()).map(annots);

        assertEquals(5, result.getItems().size());
        assertEquals("Link", result.getItems().get(0).getPdfAnnotationType());
        assertEquals("Highlight", result.getItems().get(1).getPdfAnnotationType());
        assertEquals("Stamp", result.getItems().get(2).getPdfAnnotationType());
        assertEquals("Stamp", result.getItems().get(3).getPdfAnnotationType());
        assertEquals("Stamp", result.getItems().get(4).getPdfAnnotationType());
    }

    @Test
    @DisplayName("动作映射：URI 保留链接，Goto 保留目标，Sound/Movie 跳过并告警（决策 18）")
    void mapsActions() {
        Actions soundLike = new Actions().addAction(new CT_Action(EventType.CLICK, new Movie(new ST_RefID(9L))));
        CT_Dest dest = new CT_Dest();
        dest.setType(DestType.XYZ);
        dest.setPageID(new ST_RefID(20L));
        dest.setLeft(5d);
        dest.setTop(6d);
        Actions goTo = new Actions().addAction(new CT_Action(EventType.CLICK, new Goto(dest)));

        List<Annot> annots = new ArrayList<>();
        annots.add(annot(1, AnnotType.Link, uri("https://example.org/a")));
        annots.add(annot(2, AnnotType.Link, goTo));
        annots.add(annot(3, AnnotType.Watermark, soundLike));

        AnnotationMapper.Result<AnnotationMapper.AnnotationTarget> result =
                new AnnotationMapper(pages(), new HashMap<>()).map(annots);

        assertEquals(PdfAction.Type.URI, result.getItems().get(0).getAction().getType());
        assertEquals("https://example.org/a", result.getItems().get(0).getAction().getUri());
        assertEquals(PdfAction.Type.GOTO, result.getItems().get(1).getAction().getType());
        assertEquals(1, result.getItems().get(1).getAction().getDestination().getPageIndex());
        assertNull(result.getItems().get(2).getAction(), "Movie 动作不应生成 PDF 动作");
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("Movie")), result.getWarnings().toString());
    }

    @Test
    @DisplayName("热区：外观外接矩形按页面框换算为 PDF 坐标（Y 翻转）")
    void convertsBoundaryToPdfRect() {
        List<Annot> annots = new ArrayList<>();
        annots.add(annot(1, AnnotType.Stamp, null));
        AnnotationMapper.AnnotationTarget target = new AnnotationMapper(pages(), new HashMap<>()).map(annots).getItems().get(0);

        // 页面高 100mm；Boundary=(10, 20, 30, 40)mm → llx=10mm，lly=100-(20+40)=40mm，urx=40mm，ury=100-20=80mm
        double[] rect = target.toPdfRect(0, 100);
        assertEquals(org.ofdrw.converter.pdf.CoordSystem.mmToPt(10), rect[0], EPS);
        assertEquals(org.ofdrw.converter.pdf.CoordSystem.mmToPt(40), rect[1], EPS);
        assertEquals(org.ofdrw.converter.pdf.CoordSystem.mmToPt(40), rect[2], EPS);
        assertEquals(org.ofdrw.converter.pdf.CoordSystem.mmToPt(80), rect[3], EPS);
    }

    @Test
    @DisplayName("可见性：Visible=false 的注释不映射并告警")
    void skipsInvisible() {
        Annot invisible = annot(1, AnnotType.Watermark, null);
        invisible.setVisible(false);
        List<Annot> annots = new ArrayList<>();
        annots.add(invisible);

        AnnotationMapper.Result<AnnotationMapper.AnnotationTarget> result =
                new AnnotationMapper(pages(), new HashMap<>()).map(annots);
        assertTrue(result.isEmpty());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("不可见")), result.getWarnings().toString());
    }

    @Test
    @DisplayName("零面积外观：热区返回 null（写入侧跳过）")
    void zeroAreaBoundary() {
        Annot annot = new Annot();
        annot.setID(new ST_ID(1L));
        annot.setType(AnnotType.Path);
        annot.setAppearance(new Appearance(new ST_Box(0, 0, 0, 0)));
        List<Annot> annots = new ArrayList<>();
        annots.add(annot);

        AnnotationMapper.AnnotationTarget target =
                new AnnotationMapper(pages(), new HashMap<>()).map(annots).getItems().get(0);
        assertNull(target.toPdfRect(0, 100));
    }
}
