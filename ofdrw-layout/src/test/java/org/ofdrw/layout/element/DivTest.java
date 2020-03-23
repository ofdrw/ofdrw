package org.ofdrw.layout.element;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DivTest {

    @Test
    void split() {
        Div div = new Div()
                .setWidth(400d)
                .setHeight(800d)
                .setBackgroundColor(0, 0, 0)
                .setBorder(1d)
                .setMargin(15d)
                .setPadding(8d);

        Div[] sDivs = div.split(100);
        assertEquals(sDivs.length, 2);
        Div d1 = sDivs[0];
        Div d2 = sDivs[1];
        int h1 = 100 - 1 - 15 - 8;
        assertEquals(d1.getHeight(), h1);
        assertEquals(d2.getHeight(),800d-h1 );

        assertEquals(d1.getPaddingBottom(), 0);
        assertEquals(d1.getBorderBottom(), 0);
        assertEquals(d1.getMarginBottom(), 0);

        assertEquals(d2.getPaddingTop(), 0);
        assertEquals(d2.getBorderTop(), 0);
        assertEquals(d2.getMarginTop(), 0);

        div = new Div()
                .setBackgroundColor(0, 0, 0)
                .setWidth(300d)
                .setHeight(100d)
                .setPadding(6d)
                .setBorder(8d)
                .setMargin(10d);


        Div[] sp = div.split(5);
        assertEquals(sp[0].getMarginTop(), 5);
        assertEquals(sp[1].getMarginTop(), 5);

        sp = div.split(10);
        assertEquals(sp[0].getMarginTop(), 10);
        assertEquals(sp[1].getMarginTop(), 0);

        sp = div.split(14);
        assertEquals(sp[0].getMarginTop(), 10);
        assertEquals(sp[0].getBorderTop(), 4);
        assertEquals(sp[1].getMarginTop(), 0);
        assertEquals(sp[1].getBorderTop(), 4);

        sp = div.split(21);
        assertEquals(sp[0].getMarginTop(), 10);
        assertEquals(sp[0].getBorderTop(), 8);
        assertEquals(sp[0].getPaddingTop(), 3);
        assertEquals(sp[1].getMarginTop(), 0);
        assertEquals(sp[1].getBorderTop(), 0);
        assertEquals(sp[1].getPaddingTop(), 3);

    }
}