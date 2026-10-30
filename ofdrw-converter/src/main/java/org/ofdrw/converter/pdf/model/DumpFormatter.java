package org.ofdrw.converter.pdf.model;

/**
 * 指令文本格式化：把中间模型指令转换为便于断言的一行文本
 * <p>
 * 由 {@link PdfContentWriter#dump(PdfPageContent)} 调用，供渲染器无关的单元测试断言绘制顺序与参数。
 * 输出只保证稳定、可读，不承诺与真实 PDF 内容流逐字一致。
 *
 * @author 权观宇
 * @since 2.5.1
 */
final class DumpFormatter {

    private DumpFormatter() {
    }

    /**
     * 格式化单条指令
     *
     * @param command 指令，不能为空
     * @return 单行文本
     */
    static String format(ContentCommand command) {
        if (command instanceof SaveState) {
            return "q";
        }
        if (command instanceof RestoreState) {
            return "Q";
        }
        if (command instanceof ConcatMatrix) {
            return "cm " + ((ConcatMatrix) command).getMatrix();
        }
        if (command instanceof StrokeStyle) {
            StrokeStyle style = (StrokeStyle) command;
            return "w " + style.getLineWidthPt() + " J " + style.getLineCap() + " j " + style.getLineJoin()
                    + " M " + style.getMiterLimit() + " d " + style.getDashPattern();
        }
        if (command instanceof SetAlpha) {
            SetAlpha alpha = (SetAlpha) command;
            return "gs ca=" + alpha.getFillAlpha() + " CA=" + alpha.getStrokeAlpha();
        }
        if (command instanceof ClipCommand) {
            ClipCommand clip = (ClipCommand) command;
            return "W " + clip.getFillRule() + (clip.isUnionArea() ? " union" : "") + " " + pathText(clip.getSegments());
        }
        if (command instanceof PathCommand) {
            PathCommand path = (PathCommand) command;
            return "path " + pathText(path.getSegments()) + " paint=" + paintOperator(path.getFillRule(), path.isFill(), path.isStroke());
        }
        if (command instanceof RectCommand) {
            RectCommand rect = (RectCommand) command;
            return "rect " + rect.getRect() + " fill " + rect.getFillColor();
        }
        if (command instanceof TextCommand) {
            TextCommand text = (TextCommand) command;
            StringBuilder sb = new StringBuilder();
            sb.append("text ").append(text.getFontRef()).append(' ').append(text.getFontSizePt())
                    .append(" Tm ").append(text.getTextMatrix())
                    .append(" color ").append(text.getColor());
            for (GlyphRun glyph : text.getGlyphs()) {
                sb.append(" gid=").append(glyph.getGlyphId())
                        .append("@").append(glyph.getX()).append(',').append(glyph.getY())
                        .append(" adv=").append(glyph.getAdvancePt());
            }
            return sb.toString();
        }
        if (command instanceof ImageCommand) {
            ImageCommand image = (ImageCommand) command;
            return "image " + image.getResourceKey() + " " + image.getEncoding()
                    + " " + image.getPixelWidth() + "x" + image.getPixelHeight()
                    + " cm " + image.getPlacementMatrix() + (image.isMasked() ? " mask" : "");
        }
        if (command instanceof ImageBorderCommand) {
            ImageBorderCommand border = (ImageBorderCommand) command;
            return "border " + border.getRect() + " w=" + border.getLineWidthPt()
                    + " r=" + border.getEffectiveCornerRadiusPt() + " " + border.getColor() + " " + border.getDashPattern();
        }
        if (command instanceof ShadingCommand) {
            ShadingCommand shading = (ShadingCommand) command;
            return "sh " + shading.getShadingType() + " from=(" + shading.getStartX() + "," + shading.getStartY()
                    + ") to=(" + shading.getEndX() + "," + shading.getEndY() + ") extend=" + shading.getExtend()
                    + " stops=" + shading.getStops();
        }
        if (command instanceof PatternCommand) {
            PatternCommand pattern = (PatternCommand) command;
            return "pattern " + pattern.getName() + " cell=" + pattern.getCell()
                    + (pattern.isApproximate() ? " approximate" : "") + " items=" + pattern.getCellCommands().size();
        }
        if (command instanceof FormCommand) {
            FormCommand form = (FormCommand) command;
            return "form " + form.getFormRef() + " cm " + form.getPlacementMatrix();
        }
        if (command instanceof MarkerCommand) {
            return "% " + ((MarkerCommand) command).getLabel();
        }
        return command.toString();
    }

    /**
     * 路径文本
     *
     * @param segments 子路径序列
     * @return 以空格分隔的操作文本
     */
    private static String pathText(java.util.List<PathSegment> segments) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segments.size(); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(segments.get(i));
        }
        return sb.toString();
    }

    /**
     * 根据填充/描边组合给出 PDF 绘制操作符
     *
     * @param fillRule 填充规则
     * @param fill     是否填充
     * @param stroke   是否描边
     * @return PDF 操作符
     */
    static String paintOperator(FillRule fillRule, boolean fill, boolean stroke) {
        if (fill && stroke) {
            return fillRule == FillRule.EVEN_ODD ? "B*" : "B";
        }
        if (fill) {
            return fillRule == FillRule.EVEN_ODD ? "f*" : "f";
        }
        return "S";
    }
}
