package org.ofdrw.converter;

import com.itextpdf.kernel.geom.Rectangle;
import org.junit.jupiter.api.Test;
import org.ofdrw.core.basicType.ST_Box;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.ofdrw.converter.utils.CommonUtil.converterDpi;

class ItextMakerTest {

    @Test
    void textClipRectangleShouldKeepOneMillimeterMargin() {
        ST_Box pageBox = new ST_Box(0, 0, 210, 297);
        ST_Box boundary = new ST_Box(85.682663, 64.515999, 22.013334, 4.233333);

        Rectangle clip = ItextMaker.textClipRectangle(pageBox, boundary);

        assertEquals(converterDpi(boundary.getTopLeftX() - 1), clip.getX(), 0.001);
        assertEquals(converterDpi(pageBox.getHeight() - boundary.getTopLeftY()
                - boundary.getHeight() - 1), clip.getY(), 0.001);
        assertEquals(converterDpi(boundary.getWidth() + 2), clip.getWidth(), 0.001);
        assertEquals(converterDpi(boundary.getHeight() + 2), clip.getHeight(), 0.001);
    }
}
