package org.ofdrw.converter.pdf.write;

import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.ofdrw.converter.pdf.model.ClosePath;
import org.ofdrw.converter.pdf.model.CurveTo;
import org.ofdrw.converter.pdf.model.FillRule;
import org.ofdrw.converter.pdf.model.LineTo;
import org.ofdrw.converter.pdf.model.MoveTo;
import org.ofdrw.converter.pdf.model.PathSegment;

import java.io.IOException;
import java.util.List;

/**
 * 路径与裁剪写入器（P3 写入侧复用）：中间模型页面坐标路径 → PDF 内容流
 * <p>
 * 中间模型的路径段已经是 {@link org.ofdrw.converter.pdf.model.CoordSpace#PAGE} 作用域的 PDF 页面坐标
 * （决策 1 逐点换算 + 约束 2 换算集中），因此这里只做“逐段映射为 m/l/c/h”，不再做任何坐标换算。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class PdfPathFactory {

    /**
     * 构造路径工厂
     */
    public PdfPathFactory() {
    }

    /**
     * 把路径段写入内容流（不执行绘制、不裁剪）
     *
     * @param stream   内容流
     * @param segments 路径段，不能为空
     * @throws IOException 写入失败
     */
    public void append(PDPageContentStream stream, List<PathSegment> segments) throws IOException {
        for (PathSegment segment : segments) {
            if (segment instanceof MoveTo) {
                MoveTo move = (MoveTo) segment;
                stream.moveTo((float) move.getX(), (float) move.getY());
            } else if (segment instanceof LineTo) {
                LineTo line = (LineTo) segment;
                stream.lineTo((float) line.getX(), (float) line.getY());
            } else if (segment instanceof CurveTo) {
                CurveTo curve = (CurveTo) segment;
                stream.curveTo((float) curve.getX1(), (float) curve.getY1(),
                        (float) curve.getX2(), (float) curve.getY2(),
                        (float) curve.getX3(), (float) curve.getY3());
            } else if (segment instanceof ClosePath) {
                stream.closePath();
            }
        }
    }

    /**
     * 写入裁剪路径：路径 + {@code W}/{@code W*} + {@code n}（裁剪后不保留路径）
     *
     * @param stream   内容流
     * @param segments 裁剪路径
     * @param fillRule 填充规则，为空按 NonZero
     * @throws IOException 写入失败
     */
    public void clip(PDPageContentStream stream, List<PathSegment> segments, FillRule fillRule) throws IOException {
        append(stream, segments);
        if (fillRule == FillRule.EVEN_ODD) {
            stream.clipEvenOdd();
        } else {
            stream.clip();
        }
        // 裁剪后结束路径（PDF 的 n）
        stream.appendRawCommands("n\n");
    }
}
