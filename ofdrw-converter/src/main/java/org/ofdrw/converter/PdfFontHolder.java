package org.ofdrw.converter;

import com.itextpdf.io.font.FontProgram;
import com.itextpdf.io.font.FontProgramFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 默认字体的holder
 *
 * @author dltech21
 * @time 2020/8/11
 */
public class PdfFontHolder {
    private final static Logger logger = LoggerFactory.getLogger(PdfFontHolder.class);

    private final static Map<String, FontProgram> fontMap = new HashMap<>();


    static {
        try {
            fontMap.put("宋体", FontProgramFactory.createFont("fonts/simsun.ttf"));
            fontMap.put("楷体", FontProgramFactory.createFont("fonts/simkai.ttf"));
            fontMap.put("kaiti_gb2312", FontProgramFactory.createFont("fonts/simkai.ttf"));
            fontMap.put("楷体_gb2312", FontProgramFactory.createFont("fonts/simkai.ttf"));
            fontMap.put("黑体", FontProgramFactory.createFont("fonts/simhei.ttf"));
            fontMap.put("courier new", FontProgramFactory.createFont("fonts/cour.ttf"));
            fontMap.put("仿宋", FontProgramFactory.createFont("fonts/SIMFANG.TTF"));
            fontMap.put("仿宋_gb2312", FontProgramFactory.createFont("fonts/SIMFANG.TTF"));
            fontMap.put("小标宋体", FontProgramFactory.createFont("fonts/方正小标宋简体.ttf"));
            fontMap.put("方正小标宋简体", FontProgramFactory.createFont("fonts/方正小标宋简体.ttf"));
            fontMap.put("latha", FontProgramFactory.createFont("fonts/Latha.ttf"));
            fontMap.put("tahoma", FontProgramFactory.createFont("fonts/Tahoma.ttf"));
            fontMap.put("times new roman", FontProgramFactory.createFont("fonts/times.ttf"));
            fontMap.put("timesnewromanpsmt", FontProgramFactory.createFont("fonts/times.ttf"));
            fontMap.put("arial", FontProgramFactory.createFont("fonts/arial.ttf"));
            fontMap.put("calibri", FontProgramFactory.createFont("fonts/calibri.ttf"));
            fontMap.put("symbol", FontProgramFactory.createFont("fonts/symbol.ttf"));
            fontMap.put("stcaiyun", FontProgramFactory.createFont("fonts/STCAIYUN.TTF"));
            fontMap.put("mongolian baiti", FontProgramFactory.createFont("fonts/monbaiti.ttf"));
            fontMap.put("wingdings", FontProgramFactory.createFont("fonts/wingding.ttf"));
        } catch (IOException e) {
            logger.error("load font failed", e);
        }
    }

    public static FontProgram getFont(String fontName) {
        if(Objects.nonNull(fontMap.get(fontName))) {
            return fontMap.get(fontName);
        }

        return null;
    }
}
