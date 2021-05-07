package org.ofdrw.converter;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.svggen.SVGGraphics2DIOException;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.PageInfo;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

/**
 * SVG转换类
 *
 * @author qaqtutu
 * @since 2021-05-06 23:00:01
 */
public class SVGMaker extends AWTMaker {


    /**
     * 创建SVG转换对象实例
     * <p>
     * OFD内部使用毫米作为基本单位
     *
     * @param reader OFD解析器
     * @param ppm    每毫米像素数量(Pixels per millimeter)
     */

    public SVGMaker(OFDReader reader, int ppm) {
        super(reader, ppm);
    }

    /**
     * 渲染OFD页面为SVG
     *
     * @param pageIndex 页码，从0起
     * @return 渲染完成的SVG字符串
     */
    public String makePage(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= pages.size()) {
            throw new GeneralConvertException(String.format("%s 不是有效索引", pageIndex));
        }
        PageInfo pageInfo = pages.get(pageIndex);
        ST_Box pageBox = pageInfo.getSize();
        int pageWidthPixel = Double.valueOf(ppm * pageBox.getWidth()).intValue();
        int pageHeightPixel = Double.valueOf(ppm * pageBox.getHeight()).intValue();

        DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();

        // Create an instance of org.w3c.dom.Document.
        String svgNS = "http://www.w3.org/2000/svg";
        Document document = domImpl.createDocument(svgNS, "svg", null);

        // Create an instance of the SVG Generator.
        SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
        svgGenerator.setSVGCanvasSize(new Dimension(pageWidthPixel, pageHeightPixel));

        writePage(svgGenerator, pageInfo, null);

        // Finally, stream out SVG to the standard output using
        // UTF-8 encoding.
        boolean useCSS = false; // we want to use CSS style attributes

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Writer out = null;
        try {
            out = new OutputStreamWriter(outputStream, "UTF-8");
            svgGenerator.stream(out, useCSS);
            return new String(outputStream.toByteArray());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (SVGGraphics2DIOException e) {
            e.printStackTrace();
        }

        return null;
    }

}
