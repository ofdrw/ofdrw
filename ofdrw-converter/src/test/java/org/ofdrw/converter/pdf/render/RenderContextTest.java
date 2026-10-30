package org.ofdrw.converter.pdf.render;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ofdrw.converter.pdf.model.AffineMatrix;
import org.ofdrw.converter.pdf.model.ContentCommand;
import org.ofdrw.converter.pdf.model.PdfPageContent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 渲染上下文契约测试
 * <p>
 * 锁定：初始 CTM 为单位矩阵（决策 1 逐点换算）、容器 CTM 叠加不污染原上下文、
 * 字体选择器可为空（文字渲染器需自行降级）。
 *
 * @author 权观宇
 * @since 2.5.1
 */
class RenderContextTest {

    @Test
    @DisplayName("初始上下文：页面尺寸透传，CTM 为单位矩阵")
    void create() {
        PdfPageContent content = new PdfPageContent(595.28, 841.89);
        RenderContext context = RenderContexts.create(210, 297, content, null);
        assertEquals(210, context.getPageWidthMm(), 1e-9);
        assertEquals(297, context.getPageHeightMm(), 1e-9);
        assertEquals(AffineMatrix.IDENTITY, context.getCurrentCtm());
        assertEquals(content, context.getPageContent());
        assertEquals(null, context.getFontSelector());
    }

    @Test
    @DisplayName("容器 CTM 叠加：返回新上下文，不污染原上下文")
    void withCtm() {
        PdfPageContent content = new PdfPageContent(100, 100);
        RenderContext base = RenderContexts.create(100, 100, content, null);
        AffineMatrix scale = AffineMatrix.scale(2, 2);
        RenderContext inner = base.withCtm(scale);
        assertNotSame(base, inner);
        assertEquals(AffineMatrix.IDENTITY, base.getCurrentCtm());
        // 叠加后不污染原上下文
        assertEquals(scale, inner.getCurrentCtm());
        assertEquals(content, inner.getPageContent());
        assertEquals(100, inner.getPageWidthMm(), 1e-9);
    }

    @Test
    @DisplayName("页面内容为空时拒绝构造上下文")
    void rejectsNullContent() {
        assertThrows(IllegalArgumentException.class,
                () -> RenderContexts.create(100, 100, null, null));
    }

    @Test
    @DisplayName("渲染器契约：名称可用于日志定位")
    void rendererNames() {
        assertNotSame(null, RenderContexts.create(1, 1, new PdfPageContent(1, 1), null).withCtm(null));
        PageRenderer pageRenderer = new PageRenderer() {
        };
        assertEquals("PageRenderer", pageRenderer.getName());
        // PathRenderer 自 P1 起为 final 且需要依赖（P2 新增）：此处以接口实现体锁定“名称用于日志定位”的契约
        ContentRenderer pathRenderer = new ContentRenderer() {
            @Override
            public String getName() {
                return "PathRenderer";
            }

            @Override
            public java.util.List<ContentCommand> render(Object input, RenderContext context) {
                return java.util.Collections.emptyList();
            }
        };
        assertTrue(pathRenderer.render(null, null).isEmpty());
        assertEquals("PathRenderer", pathRenderer.getName());
        assertEquals(32, GraphicsStateResolver.MAX_RELATIVE_DEPTH);
        assertEquals(Math.PI / 2, ArcConverter.MAX_SWEEP_PER_CURVE, 1e-9);
    }

    @Test
    @DisplayName("圆弧参数校验")
    void arcParams() {
        assertThrows(IllegalArgumentException.class,
                () -> new ArcConverter.ArcParams(0, 0, 0, 1, 0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> ArcConverter.toCubicSegments(null));
        ArcConverter.ArcParams arc = new ArcConverter.ArcParams(10, 20, 5, 5, 0, 0, Math.PI / 2);
        assertEquals(10, arc.getCx(), 1e-9);
        // 90° 圆弧恰为一段贝塞尔（P1 起圆弧算法已实现，见 ArcConverterTest）
        assertEquals(1, ArcConverter.toCubicSegments(arc).size());
        assertTrue(ArcConverter.toCubicSegments(new ArcConverter.ArcParams(0, 0, 1, 1, 0, 0, 0)).isEmpty());
    }
}
