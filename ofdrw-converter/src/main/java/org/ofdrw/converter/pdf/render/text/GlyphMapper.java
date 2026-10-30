package org.ofdrw.converter.pdf.render.text;

import org.ofdrw.converter.pdf.font.TrueTypePdfFont;
import org.ofdrw.converter.pdf.model.AffineMatrix;
import org.ofdrw.converter.pdf.model.GlyphRun;
import org.ofdrw.converter.pdf.model.PdfColor;
import org.ofdrw.converter.pdf.model.TextCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * 字形映射：布局结果 + 字体引用 → 中间模型文字指令（决策 3“字形优先 + ToUnicode”）
 * <p>
 * 计算内容：
 * <ul>
 *     <li><b>字形索引</b>：优先采用 CGTransform 的显式 GID，其次由 {@link GlyphIndexProvider} 查 cmap，
 *     最后回退字形 0 并记录 warning（字形优先，保证视觉一致）；</li>
 *     <li><b>定位</b>：{@link TextLayout} 给出的字形原点已是页内坐标系绝对坐标（表 46），经“容器 CTM + mm→pt + Y 翻转”
 *     换算为页面坐标，并把字形相对首个字形的位置差写入 {@link GlyphRun#getX()}/{@link GlyphRun#getY()}
 *     （文本空间位移），首个字形位置写入 {@code Tm}；每个字形先 {@code Tf} 再 {@code Tm}，不受字宽与 TJ 累积误差影响；</li>
 *     <li><b>字符方向</b>（表 47）：以文字流起始点（外接矩形竖直中线）为旋转中心，CharDirection 0/90/180/270 依次
 *     对应顺时针 0°/90°/180°/270°；由于 PDF 的 Y 轴向上，OFD 的顺时针旋转在 PDF 中对应同一角度值，无镜像；</li>
 *     <li><b>ToUnicode</b>：把“字形 → Unicode”登记到字体引用，保证文本可提取（决策 3）。</li>
 * </ul>
 * 本类不依赖 PDFBox，输出为中间模型指令，可直接用 {@code PdfContentWriter.dump} 断言。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class GlyphMapper {

    /**
     * 进行字形映射
     *
     * @param text        文字图元视图（提供外接矩形与方向）
     * @param layout      布局结果
     * @param font        字体引用
     * @param fontSizePt  字号，单位 pt
     * @param baseMatrix  容器 CTM 与页面换算组成的页内坐标 → 页面坐标矩阵（mm→pt 与 Y 翻转已在其中）；
     *                    字形原点取自 TextCode 的绝对坐标，故其中不得再叠加外接矩形平移
     * @param color       文字颜色
     * @param warnings    降级事件收集器
     * @return 文字指令；没有可写出的字形时返回 null
     */
    public TextCommand map(OfdText text, TextLayout layout, TrueTypePdfFont font, double fontSizePt,
                           AffineMatrix baseMatrix, PdfColor color,
                           List<String> warnings) {
        if (layout == null || layout.isEmpty()) {
            return null;
        }
        // 1) 字形索引与 ToUnicode
        List<int[]> glyphIds = new ArrayList<>(layout.getGlyphs().size());
        for (TextLayout.PlacedGlyph glyph : layout.getGlyphs()) {
            int gid = resolveGlyph(font, glyph, warnings);
            glyphIds.add(new int[]{gid});
            font.registerUnicode(gid, glyph.getCodePoints());
        }

        /*
         * 2) 位置：Tm 负责把文字放到页面上的首字形原点（页内绝对坐标 + 页面换算，单位 pt），
         *    线性部分只保留字符方向旋转；每个字形给出相对首字形的文本空间位移（单位 pt）。
         *    Tm 的线性部分不能带 mm→pt 缩放，否则字号会被二次放大。
         */
        AffineMatrix direction = rotationMatrix(text.getCharDirectionDeg());
        TextLayout.PlacedGlyph first = layout.getGlyphs().get(0);
        /*
         * 首字形原点的页面坐标（pt）：页内绝对坐标经 baseMatrix 换算而来，作为 Tm 的平移量。
         * 不能直接取 baseMatrix 的平移量：它对应页内坐标原点，不含 TextCode 的 X/Y，文字会全部堆到页角。
         */
        double[] origin = baseMatrix.transform(first.getX(), first.getY());
        // Tm = 首字形页面原点 × 字符方向旋转；线性部分不带 mm→pt 缩放
        AffineMatrix textMatrix = new AffineMatrix(1, 0, 0, 1, origin[0], origin[1]).multiply(direction);
        /*
         * 字形定位：baseMatrix 只把页内坐标差换算为“相对首字形”的页面位移，
         * 再叠加字符方向旋转得到文本空间位移。首字形位移为 0，其余字形按各自的绝对位置相对首字形给出，
         * 因此既不受 Tm 平移量影响，也不会让字距被 Tf 字号放大（Td 会，故这里不用 Td 累加）。
         */
        /*
         * 页面坐标差已是 pt，换算回文本空间只需叠加“方向旋转的逆”，
         * 不能再乘 baseMatrix 的线性部分（其中含 mm→pt 缩放），否则字距被放大 2.83 倍。
         */
        AffineMatrix linear = direction.inverse();
        List<GlyphRun> runs = new ArrayList<>(layout.getGlyphs().size());
        for (int i = 0; i < layout.getGlyphs().size(); i++) {
            TextLayout.PlacedGlyph glyph = layout.getGlyphs().get(i);
            // 先在页面坐标里求“相对首字形”的位移，再换算回文本空间（线性部分只含方向旋转）
            double[] page = baseMatrix.transform(glyph.getX(), glyph.getY());
            double[] local = linear.transform(page[0] - origin[0], page[1] - origin[1]);
            /*
             * 字形宽度 = 文档声明的排版步进（TextCode 的 DeltaX/DeltaY），并登记到字体供 /W 使用。
             * 原因：OFD 用 Delta 精确摆放每个字形，若 /W 改用替代字体自带字宽（与步进不一致），
             * 阅读器会判“字宽 < 字距”而在字间插入空格，导致复制文本多空格、错行（实测 1.ofd 多 19 个空格）。
             * 位置始终由显式 Tm 给出，因此这里只影响 /W 与词边界判定，不影响渲染位置。
             */
            double advancePt = fontAdvancePt(font, glyph.getAdvanceMm());
            font.registerDocumentAdvance(glyphIds.get(i)[0], (float) (advancePt * 1000d / fontSizePt));
            runs.add(new GlyphRun(glyphIds.get(i)[0], glyph.getText(), local[0], local[1], advancePt));
        }
        return new TextCommand(font.getResourceName(), fontSizePt, textMatrix, runs, color);
    }

    /**
     * 字形标称步进（供写入器按 TJ 位移时参考）
     *
     * @param font      字体引用
     * @param advanceMm 布局给出的步进，单位 mm
     * @return 步进，单位 pt
     */
    /**
     * 字形宽度：文档声明的步进（Delta 换算为 mm）优先，未声明时退回字体标称步进
     * <p>
     * 该值用于 {@code /W}（反馈给阅读器的字宽）。OFD 已用 Delta 精确摆放字形，
     * 因此写出文档步进才能让阅读器的词边界判定与实际排版一致。
     *
     * @param font      实际写出的字体
     * @param advanceMm 文档声明的步进，单位 mm；未声明时为 0
     * @return 宽度，单位 pt
     */
    static double fontAdvancePt(TrueTypePdfFont font, double advanceMm) {
        if (advanceMm > 0) {
            return org.ofdrw.converter.pdf.CoordSystem.mmToPt(advanceMm);
        }
        return metricsAdvancePt(font, 0d);
    }

    /**
     * 文档步进换算为 pt（字体缺字宽表时使用）
     *
     * @param font      字体引用，可为空
     * @param advanceMm 步进，单位 mm
     * @return 宽度，单位 pt
     */
    static double metricsAdvancePt(TrueTypePdfFont font, double advanceMm) {
        return org.ofdrw.converter.pdf.CoordSystem.mmToPt(advanceMm);
    }

    /**
     * 解析字形索引：显式 GID 优先，其次 cmap，最后回退字形 0
     *
     * @param font     字体引用
     * @param glyph    布局字形
     * @param warnings 降级事件收集器
     * @return 字形索引
     */
    private static int resolveGlyph(TrueTypePdfFont font, TextLayout.PlacedGlyph glyph, List<String> warnings) {
        if (glyph.isShared()) {
            // 一对多映射的附属字形：与首字形同址，不重复登记文字
            return font.encodeGlyph(glyph.getExplicitGlyphId() >= 0 ? glyph.getExplicitGlyphId() : 0);
        }
        int gid = font.resolveGlyphId(glyph.getExplicitGlyphId(), glyph.getCodePoints(), warnings);
        return font.encodeGlyph(gid);
    }

    /**
     * 字符方向旋转矩阵（表 47）
     * <p>
     * 只包含线性部分、不含平移：文字原点已由 {@code baseMatrix}（外接矩形平移 + 页面换算）给出，
     * 旋转以该原点为中心生效。OFD 的 CharDirection 为顺时针角度，PDF 坐标 Y 轴向上，
     * 两者表现出的视觉旋转方向一致，因此角度值直接对应 PDF 的旋转矩阵。
     *
     * @param angleDeg 字符方向，单位 °，取值 0/90/180/270
     * @return 旋转矩阵（无平移分量）
     */
    static AffineMatrix rotationMatrix(double angleDeg) {
        if (angleDeg == 0) {
            return AffineMatrix.IDENTITY;
        }
        double rad = Math.toRadians(-angleDeg);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        // AffineMatrix 采用行向量写法：x' = a·x + c·y + e，y' = b·x + d·y + f
        return new AffineMatrix(cos, sin, -sin, cos, 0, 0);
    }
}
