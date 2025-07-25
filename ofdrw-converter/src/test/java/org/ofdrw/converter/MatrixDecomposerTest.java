package org.ofdrw.converter;

import org.apache.pdfbox.pdfwriter.compress.CompressParameters;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.util.Matrix;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MatrixDecomposerTest {

    @Test
    void decompose() {
        // 原始矩阵示例 (平移+旋转+缩放+斜切)
        Matrix original = new Matrix(1, (float) 0.5, (float) 0.5, 1, 10, 20);

        List<Matrix> decomposed = MatrixDecomposer.decompose(original);

        // 打印分解后的矩阵
        System.out.println("分解结果:");
        System.out.println("K (斜切): " + decomposed.get(0));
        System.out.println("S (缩放): " + decomposed.get(1));
        System.out.println("Q (旋转): " + decomposed.get(2));
        System.out.println("T (平移): " + decomposed.get(3));

        // 按顺序应用变换矩阵 (从右到左: K->S->Q->T)
        Matrix composed = new Matrix(); // 单位矩阵
        for (Matrix matrix : decomposed) {
            composed = composed.multiply(matrix);
        }
        System.out.println();
        // 验证 composed 应与 original 近似相等
        System.out.println("原始矩阵: " + original);
        System.out.println("组合矩阵: " + composed);
        assertEquals(original, composed);
    }

    @Test
    void testpdf() throws Exception {

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, false, false)) {
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN), 12);
                cs.beginText();
                cs.newLineAtOffset(50, 700);  // 起始位置
                // 绘制带位置调整的文本
                cs.showText("Hello World");
                cs.endText();
            }
            doc.save("target/text_positioning_example.pdf", CompressParameters.NO_COMPRESSION);
        }
    }
}