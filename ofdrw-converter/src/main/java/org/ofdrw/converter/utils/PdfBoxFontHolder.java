package org.ofdrw.converter.utils;

import org.apache.fontbox.ttf.NamingTable;
import org.apache.fontbox.ttf.OTFParser;
import org.apache.fontbox.ttf.OpenTypeFont;
import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PdfBoxFontHolder {

    private final Map<String, PDFont> FONT_MAP = new HashMap<>();

    public PdfBoxFontHolder(PDDocument pdDocument) {
        try {
            FONT_MAP.put("宋体", PDType0Font.load(pdDocument, PdfBoxFontHolder.class.getClassLoader().getResourceAsStream("fonts/simsun.ttf")));
            FONT_MAP.put("楷体", PDType0Font.load(pdDocument, PdfBoxFontHolder.class.getClassLoader().getResourceAsStream("fonts/simkai.ttf")));
            FONT_MAP.put("KaiTi_GB2312", PDType0Font.load(pdDocument, PdfBoxFontHolder.class.getClassLoader().getResourceAsStream("fonts/simkai.ttf")));
            FONT_MAP.put("黑体", PDType0Font.load(pdDocument, PdfBoxFontHolder.class.getClassLoader().getResourceAsStream("fonts/simhei.ttf")));
            FONT_MAP.put("Courier New", PDType0Font.load(pdDocument, PdfBoxFontHolder.class.getClassLoader().getResourceAsStream("fonts/cour.ttf")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public PDFont getFont(String fontName) {
        return FONT_MAP.get(fontName);
    }
}
