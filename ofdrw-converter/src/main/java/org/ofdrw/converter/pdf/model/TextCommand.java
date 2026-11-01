package org.ofdrw.converter.pdf.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 文字绘制指令
 * <p>
 * 对应 PDF 的 {@code BT ... Tf ... Tm TJ ... ET}。设计 3.6 节与本类约定的写法：
 * <ul>
 *     <li>{@link #getTextMatrix()} 写入 {@code Tm}，包含字符方向与文本原点，已按 {@link #getCoordSpace()} 换算；</li>
 *     <li>字形按 {@link GlyphRun} 顺序输出，每个字形先按 {@code TJ} 位移到其文本空间原点，再以字形索引（GID）绘制，
 *     从而精确还原 OFD 的 X/Y + DeltaX/DeltaY 定位；</li>
 *     <li>{@link #getFontRef()} 指向字体映射器产出的 PDF 字体对象标识，由渲染器解析，本模型不持有字体对象。</li>
 * </ul>
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class TextCommand extends AbstractCommand {

    /**
     * 字体引用（由 font 包生成，用于在同一页面内复用 PDF 字体对象）
     */
    private final String fontRef;

    /**
     * 字号，单位 pt
     */
    private final double fontSizePt;

    /**
     * 文本矩阵，对应 PDF 的 {@code Tm}
     */
    private final AffineMatrix textMatrix;

    /**
     * 字形序列
     */
    private final List<GlyphRun> glyphs;

    /**
     * 文字颜色
     */
    private final PdfColor color;

    /**
     * 加粗描边线宽，单位 pt；0 表示不描边（非加粗）
     */
    private final double strokeWidthPt;

    /**
     * 构造文字指令（页面坐标作用域）
     *
     * @param fontRef      字体引用，不能为空
     * @param fontSizePt   字号，单位 pt，必须大于 0
     * @param textMatrix   文本矩阵，不能为空
     * @param glyphs       字形序列，不能为空
     * @param color        文字颜色，不能为空
     */
    public TextCommand(String fontRef, double fontSizePt, AffineMatrix textMatrix, List<GlyphRun> glyphs, PdfColor color) {
        this(CoordSpace.PAGE, fontRef, fontSizePt, textMatrix, glyphs, color, 0);
    }

    /**
     * 构造文字指令（带加粗描边）
     *
     * @param fontRef       字体引用，不能为空
     * @param fontSizePt    字号，单位 pt，必须大于 0
     * @param textMatrix    文本矩阵，不能为空
     * @param glyphs        字形序列，不能为空
     * @param color         文字颜色，不能为空
     * @param strokeWidthPt 加粗描边线宽，单位 pt；0 表示不描边
     */
    public TextCommand(String fontRef, double fontSizePt, AffineMatrix textMatrix, List<GlyphRun> glyphs,
                       PdfColor color, double strokeWidthPt) {
        this(CoordSpace.PAGE, fontRef, fontSizePt, textMatrix, glyphs, color, strokeWidthPt);
    }

    /**
     * 构造文字指令
     *
     * @param coordSpace 坐标作用域
     * @param fontRef    字体引用，不能为空
     * @param fontSizePt 字号，单位 pt，必须大于 0
     * @param textMatrix 文本矩阵，不能为空
     * @param glyphs     字形序列，不能为空
     * @param color      文字颜色，不能为空
     */
    public TextCommand(CoordSpace coordSpace, String fontRef, double fontSizePt, AffineMatrix textMatrix,
                       List<GlyphRun> glyphs, PdfColor color) {
        this(coordSpace, fontRef, fontSizePt, textMatrix, glyphs, color, 0);
    }

    /**
     * 构造文字指令（带加粗描边）
     *
     * @param coordSpace    坐标作用域
     * @param fontRef       字体引用，不能为空
     * @param fontSizePt    字号，单位 pt，必须大于 0
     * @param textMatrix    文本矩阵，不能为空
     * @param glyphs        字形序列，不能为空
     * @param color         文字颜色，不能为空
     * @param strokeWidthPt 加粗描边线宽，单位 pt；0 表示不描边
     */
    public TextCommand(CoordSpace coordSpace, String fontRef, double fontSizePt, AffineMatrix textMatrix,
                       List<GlyphRun> glyphs, PdfColor color, double strokeWidthPt) {
        super(coordSpace);
        if (fontRef == null || fontRef.isEmpty()) {
            throw new IllegalArgumentException("字体引用不能为空");
        }
        if (!(fontSizePt > 0)) {
            throw new IllegalArgumentException("字号必须大于 0: " + fontSizePt);
        }
        if (textMatrix == null) {
            throw new IllegalArgumentException("文本矩阵不能为空");
        }
        if (glyphs == null || glyphs.isEmpty()) {
            throw new IllegalArgumentException("字形序列不能为空");
        }
        if (color == null) {
            throw new IllegalArgumentException("文字颜色不能为空");
        }
        this.fontRef = fontRef;
        this.fontSizePt = fontSizePt;
        this.textMatrix = textMatrix;
        this.glyphs = Collections.unmodifiableList(new ArrayList<>(glyphs));
        this.color = color;
        this.strokeWidthPt = strokeWidthPt > 0 ? strokeWidthPt : 0;
    }

    /**
     * @return 字体引用
     */
    public String getFontRef() {
        return fontRef;
    }

    /**
     * @return 字号，单位 pt
     */
    public double getFontSizePt() {
        return fontSizePt;
    }

    /**
     * @return 文本矩阵
     */
    public AffineMatrix getTextMatrix() {
        return textMatrix;
    }

    /**
     * @return 字形序列（只读）
     */
    public List<GlyphRun> getGlyphs() {
        return glyphs;
    }

    /**
     * @return 文字颜色
     */
    public PdfColor getColor() {
        return color;
    }

    /**
     * 加粗描边线宽
     *
     * @return 描边线宽，单位 pt；0 表示不描边
     */
    public double getStrokeWidthPt() {
        return strokeWidthPt;
    }

    /**
     * @return 是否加粗（以描边模拟粗体）
     */
    public boolean isBold() {
        return strokeWidthPt > 0;
    }

    /**
     * 拼接全部字形的 Unicode 文本
     *
     * @return 可提取文本
     */
    public String getUnicodeText() {
        StringBuilder sb = new StringBuilder();
        for (GlyphRun glyph : glyphs) {
            sb.append(glyph.getUnicode());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Text(" + fontRef + " @" + fontSizePt + "pt, " + glyphs.size() + " glyphs, '" + getUnicodeText() + "')";
    }
}
