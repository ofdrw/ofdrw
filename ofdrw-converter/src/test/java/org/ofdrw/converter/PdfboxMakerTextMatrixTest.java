package org.ofdrw.converter;

import org.apache.pdfbox.util.Matrix;
import org.junit.jupiter.api.Test;
import org.ofdrw.converter.point.TextCodePoint;
import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.core.basicType.ST_Array;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PdfboxMakerTextMatrixTest {

    private static final float EPSILON = 0.000001f;

    @Test
    void shouldBuildPdfTextMatrixFromOfdCtm() {
        TextObject textObject = new TextObject(1);
        textObject.setCTM(new ST_Array(
                2, 3,
                4, 5,
                10, 20
        ));
        textObject.setHScale(0.5);

        TextCodePoint point = new TextCodePoint(123, 456, "A");

        Matrix matrix = PdfboxMaker.textMatrix(textObject, point);

        assertEquals(1f, matrix.getScaleX(), EPSILON);
        assertEquals(-1.5f, matrix.getShearY(), EPSILON);
        assertEquals(-4f, matrix.getShearX(), EPSILON);
        assertEquals(5f, matrix.getScaleY(), EPSILON);
        assertEquals(123f, matrix.getTranslateX(), EPSILON);
        assertEquals(456f, matrix.getTranslateY(), EPSILON);
    }
}
