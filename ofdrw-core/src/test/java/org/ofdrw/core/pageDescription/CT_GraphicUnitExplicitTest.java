package org.ofdrw.core.pageDescription;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ofdrw.core.basicStructure.pageObj.layer.block.PathObject;
import org.ofdrw.core.graph.pathObj.CT_Path;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.pageDescription.color.colorSpace.BitsPerComponent;
import org.ofdrw.core.image.Border;
import org.ofdrw.core.pageDescription.color.color.CT_Color;
import org.ofdrw.core.pageDescription.color.colorSpace.CT_ColorSpace;
import org.ofdrw.core.pageDescription.drawParam.CT_DrawParam;
import org.ofdrw.core.pageDescription.drawParam.LineCapType;
import org.ofdrw.core.pageDescription.drawParam.LineJoinType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 图元对象“仅取显式属性”访问器测试
 * <p>
 * 用于锁定 {@code getCapExplicit()} / {@code getJoinExplicit()} 与既有 {@code getCap()} / {@code getJoin()}
 * 在“属性缺失”与“属性显式书写”两种情形下的差异：既有访问器缺失时返回规范默认值，显式访问器返回 null。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public class CT_GraphicUnitExplicitTest {

    /**
     * 构造一个不含 Cap / Join 属性的路径图元
     *
     * @return 路径图元
     */
    private static CT_Path emptyGraphicUnit() {
        return new PathObject(ST_ID.getInstance("10"));
    }

    @Test
    @DisplayName("属性缺失：既有访问器返回规范默认值，显式访问器返回 null")
    void missingAttributes() {
        CT_Path unit = emptyGraphicUnit();
        // 规范默认值：Cap = Butt，Join = Miter
        assertEquals(LineCapType.Butt, unit.getCap());
        assertEquals(LineJoinType.Miter, unit.getJoin());
        // 显式访问器：无法区分“未定义”，故返回 null
        assertNull(unit.getCapExplicit());
        assertNull(unit.getJoinExplicit());
        assertNull(unit.getLineWidth());
    }

    @Test
    @DisplayName("属性显式书写：两个访问器取值一致")
    void explicitAttributes() {
        CT_Path unit = emptyGraphicUnit()
                .setCap(LineCapType.Round)
                .setJoin(LineJoinType.Bevel)
                .setLineWidth(2.5d);
        assertEquals(LineCapType.Round, unit.getCap());
        assertEquals(LineCapType.Round, unit.getCapExplicit());
        assertEquals(LineJoinType.Bevel, unit.getJoin());
        assertEquals(LineJoinType.Bevel, unit.getJoinExplicit());
        assertEquals(2.5d, unit.getLineWidth(), 1e-9);
    }

    @Test
    @DisplayName("属性显式书写为规范默认值：显式访问器仍能识别为已定义")
    void explicitDefaultValue() {
        CT_Path unit = emptyGraphicUnit()
                .setCap(LineCapType.Butt)
                .setJoin(LineJoinType.Miter);
        // 关键差异：显式写成 Butt / Miter 时，显式访问器返回非 null，可打断 DrawParam 继承
        assertEquals(LineCapType.Butt, unit.getCapExplicit());
        assertEquals(LineJoinType.Miter, unit.getJoinExplicit());
    }

    @Test
    @DisplayName("移除属性后显式访问器重新返回 null")
    void removeAttributes() {
        CT_Path unit = emptyGraphicUnit()
                .setCap(LineCapType.Square)
                .setJoin(LineJoinType.Round);
        assertEquals(LineCapType.Square, unit.getCapExplicit());
        unit.setCap(null).setJoin(null);
        assertNull(unit.getCapExplicit());
        assertNull(unit.getJoinExplicit());
    }

    @Test
    @DisplayName("Alpha 属性缺失：既有访问器返回 255，显式访问器返回 null")
    void alphaExplicit() {
        CT_Path unit = emptyGraphicUnit();
        // 规范默认值：不透明
        assertEquals(255, unit.getAlpha());
        assertNull(unit.getAlphaExplicit());
        // 显式写成规范默认值仍识别为已定义
        unit.setAlpha(255);
        assertEquals(255, unit.getAlphaExplicit());
        unit.setAlpha(128);
        assertEquals(128, unit.getAlphaExplicit());
        unit.setAlpha(null);
        assertNull(unit.getAlphaExplicit());
    }

    @Test
    @DisplayName("DrawParam 线宽属性缺失：既有访问器返回 0.353，显式访问器返回 null")
    void drawParamLineWidthExplicit() {
        CT_DrawParam param = new CT_DrawParam();
        // 规范默认值：0.353 mm
        assertEquals(0.353d, param.getLineWidth(), 1e-9);
        assertNull(param.getLineWidthExplicit());
        // 显式写成规范默认值仍识别为已定义
        param.setLineWidth(0.353d);
        assertEquals(0.353d, param.getLineWidthExplicit(), 1e-9);
        param.setLineWidth(2.0d);
        assertEquals(2.0d, param.getLineWidthExplicit(), 1e-9);
        param.setLineWidth(null);
        assertNull(param.getLineWidthExplicit());
    }

    @Test
    @DisplayName("颜色空间位宽属性缺失：既有访问器返回 BIT_1，显式访问器返回 null")
    void bitsPerComponentExplicit() {
        CT_ColorSpace space = new CT_ColorSpace();
        // 既有访问器把缺失映射为 BIT_1（BitsPerComponent.getInstance(null) 的既定行为），
        // 与 GB/T 33190 8.3.2 “未声明位宽按 8 位处理”并不一致，因此渲染侧必须用显式访问器判空后自行取 8 位
        assertEquals(BitsPerComponent.BIT_1, space.getBitsPerComponent());
        assertNull(space.getBitsPerComponentExplicit());
        // 显式写成规范默认值仍识别为已定义
        space.setBitsPerComponent(BitsPerComponent.BIT_8);
        assertEquals(BitsPerComponent.BIT_8, space.getBitsPerComponentExplicit());
        space.setBitsPerComponent(BitsPerComponent.BIT_4);
        assertEquals(BitsPerComponent.BIT_4, space.getBitsPerComponentExplicit());
    }

    @Test
    @DisplayName("边框线宽属性缺失：既有访问器返回 0.353，显式访问器返回 null")
    void borderLineWidthExplicit() {
        Border border = new Border();
        // 规范默认值：0.353 mm
        assertEquals(0.353d, border.getLineWidth(), 1e-9);
        assertNull(border.getLineWidthExplicit());
        // 边框“线宽为 0 表示不绘制”，因此显式 0 必须能与“未声明”区分
        border.setLineWidth(0d);
        assertEquals(0d, border.getLineWidthExplicit(), 1e-9);
        border.setLineWidth(1.5d);
        assertEquals(1.5d, border.getLineWidthExplicit(), 1e-9);
    }

    @Test
    @DisplayName("颜色透明度属性缺失：既有访问器返回 255，显式访问器返回 null")
    void colorAlphaExplicit() {
        CT_Color color = new CT_Color();
        // 规范默认值：完全不透明
        assertEquals(255, color.getAlpha());
        assertNull(color.getAlphaExplicit());
        // 显式写成规范默认值仍识别为已定义
        color.setAlpha(255);
        assertEquals(255, color.getAlphaExplicit());
        color.setAlpha(64);
        assertEquals(64, color.getAlphaExplicit());
    }
}
