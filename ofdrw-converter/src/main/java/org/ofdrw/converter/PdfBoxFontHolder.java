package org.ofdrw.converter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PdfBoxFontHolder {

    private final static Map<String, PDFont> fontMap = new HashMap<>();

    private static volatile PdfBoxFontHolder mInstance;

    public static PdfBoxFontHolder getInstance(PDDocument pdDocument) {
        if (mInstance == null) {
            synchronized (PdfBoxFontHolder.class) {
                if (mInstance == null) {
                    mInstance = new PdfBoxFontHolder(pdDocument);
                }
            }
        }
        return mInstance;
    }

    public PdfBoxFontHolder(PDDocument pdDocument) {
        try {
            fontMap.put("宋体", PDType0Font.load(pdDocument, PdfBoxFontHolder.class.getClassLoader().getResourceAsStream("fonts/simsun.ttf")));
            fontMap.put("楷体", PDType0Font.load(pdDocument, PdfBoxFontHolder.class.getClassLoader().getResourceAsStream("fonts/simkai.ttf")));
            fontMap.put("KaiTi_GB2312", PDType0Font.load(pdDocument, PdfBoxFontHolder.class.getClassLoader().getResourceAsStream("fonts/simkai.ttf")));
            fontMap.put("黑体", PDType0Font.load(pdDocument, PdfBoxFontHolder.class.getClassLoader().getResourceAsStream("fonts/simhei.ttf")));
            fontMap.put("Courier New", PDType0Font.load(pdDocument, PdfBoxFontHolder.class.getClassLoader().getResourceAsStream("fonts/cour.ttf")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public PDFont getFont(String fontName) {
        return fontMap.get(fontName);
    }
}
