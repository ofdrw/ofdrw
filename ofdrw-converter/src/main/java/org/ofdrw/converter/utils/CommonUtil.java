package org.ofdrw.converter.utils;

import org.apache.pdfbox.jbig2.JBIG2ImageReader;
import org.apache.pdfbox.jbig2.JBIG2ImageReaderSpi;
import org.apache.pdfbox.jbig2.io.DefaultInputStreamFactory;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceGray;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.ofdrw.converter.point.Tuple2;
import org.ofdrw.core.basicStructure.doc.CT_PageArea;
import org.ofdrw.core.basicStructure.pageObj.layer.block.ImageObject;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_Box;
import org.ujmp.core.Matrix;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.SimpleTimeZone;
import java.util.UUID;

/**
 * @author dltech21
 * @since 2020/8/11
 */
public class CommonUtil {
    public static double millimetersToPixel(double mm, double dpi) {
        //毫米转像素：mm * dpi / 25.4
        return (double) (mm * dpi / 25.4f);
    }

    public static double pixelToMillimeters(double px, double dpi) {
        //像素转毫米：px * 25.4 / dpi
        return (double) ((px * 25.4f) / dpi);
    }

    public static float[] doubleArrayToFloatArray(Double[] doubleArray) {
        if (Objects.isNull(doubleArray)) return new float[0];

        float[] floatArray = new float[doubleArray.length];

        int index = 0;
        for (Double item : doubleArray) {
            floatArray[index] = item.floatValue();
            index++;
        }

        return floatArray;
    }

    public static ST_Box getPageBox(CT_PageArea pageArea, double width, double height) {
        ST_Box documentBox = null;
        try {
            documentBox = pageArea.getPhysicalBox();
            if (documentBox == null) {
                documentBox = pageArea.getApplicationBox();
            }
            if (documentBox == null) {
                documentBox = pageArea.getContentBox();
            }

        } catch (Exception e) {
            documentBox = new ST_Box(0, 0, width, height);
        }
        return documentBox;
    }

    public static String generateTempFilePath() {
        return System.getProperty("java.io.tmpdir") + "/" + UUID.randomUUID().toString();
    }

    public static double converterDpi(double width) {
        return CommonUtil.millimetersToPixel(width, 72);
    }

    public static PDColor convertPDColor(ST_Array colorArray) {
        PDColor color = null;
        String colorStr = colorArray.toString();
        if (colorStr.indexOf("#") != -1) {
            String[] rgbStr = colorStr.split(" ");
            if (rgbStr.length >= 3) {
                String r = rgbStr[0].replaceAll("#", "");
                String g = rgbStr[1].replaceAll("#", "");
                String b = rgbStr[2].replaceAll("#", "");
                if (r.length() == 1) {
                    r += "0";
                }
                if (g.length() == 1) {
                    g += "0";
                }
                if (b.length() == 1) {
                    b += "0";
                }
                Color jColor = Color.decode(String.format("#%s%s%s", r, g, b));
                color = new PDColor(new float[]{
                        jColor.getRed() / 255f,
                        jColor.getGreen() / 255f,
                        jColor.getBlue() / 255f}, PDDeviceRGB.INSTANCE);
            }
        } else {
            float[] colors = CommonUtil.doubleArrayToFloatArray(colorArray.toDouble());
            if (colors.length == 3) {
                color = new PDColor(new float[]{(int) colors[0] / 255f, (int) colors[1] / 255f, (int) colors[2] / 255f},
                        PDDeviceRGB.INSTANCE);
            } else if (colors.length == 1) {
                color = new PDColor(new float[]{(int) colors[0] / 255f}, PDDeviceGray.INSTANCE);
            }
        }
        return color;
    }

    public static String convertOfdColorToHtml(ST_Array colorArray) {
        String color = null;
        String colorStr = colorArray.toString();
        if (colorStr.indexOf("#") != -1) {
            colorStr = colorStr.replaceAll("#", "");
            colorStr = colorStr.replaceAll(" ", "");
            color = String.format("#%s", colorStr);
        } else {
            float[] colors = CommonUtil.doubleArrayToFloatArray(colorArray.toDouble());
            if (colors.length == 3) {
                color = String.format("rgb(%d,%d,%d)", (int) colors[0], (int) colors[1], (int) colors[2]);
            } else if (colors.length == 1) {
                color = String.format("rgb(%d,%d,%d)", (int) colors[0], (int) colors[0], (int) colors[0]);
            }
        }
        return color;
    }

    public static String matrixFrom(ST_Array ctms) {
        Double[] ctm = ctms.toDouble();
        double a = ctm[0].doubleValue();
        double b = ctm[1].doubleValue();
        double c = ctm[2].doubleValue();
        double d = ctm[3].doubleValue();
        double e = ctm[4].doubleValue();
        double f = ctm[5].doubleValue();
        String matrix = String.format("matrix(%f %f %f %f %f %f)", a, b, c, d, converterDpi(e), converterDpi(f));
        return matrix;
    }

    public static byte[] converJbig2(File imgFile) throws IOException {
        JBIG2ImageReader imageReader = new JBIG2ImageReader(new JBIG2ImageReaderSpi());
        InputStream inputStream = new FileInputStream(imgFile);
        DefaultInputStreamFactory disf = new DefaultInputStreamFactory();
        ImageInputStream imageInputStream = disf.getInputStream(inputStream);
        imageReader.setInput(imageInputStream);
        BufferedImage bufferedImage = imageReader.read(0, imageReader.getDefaultReadParam());
        ByteArrayOutputStream bosImage = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "PNG", bosImage);
        return bosImage.toByteArray();
    }


    public static long convertV1SignTime(String time) throws ParseException {
        SimpleDateFormat var1;
        time = time.replaceAll("T", "");
        time = time.replaceAll("-", "");
        time = time.replaceAll(":", "");
        if (time.endsWith("Z")) {
            if (hasFractionalSeconds(time)) {
                var1 = new SimpleDateFormat("yyyyMMddHHmmss.SSS'Z'");
            } else if (hasSeconds(time)) {
                var1 = new SimpleDateFormat("yyyyMMddHHmmss'Z'");
            } else if (hasMinutes(time)) {
                var1 = new SimpleDateFormat("yyyyMMddHHmm'Z'");
            } else {
                var1 = new SimpleDateFormat("yyyyMMddHH'Z'");
            }

            var1.setTimeZone(new SimpleTimeZone(0, "Z"));
            Date date = var1.parse(time);
            return date.getTime();
        }
        return 0;
    }

    private static boolean hasFractionalSeconds(String time) {
        for (int var1 = 0; var1 != time.length(); ++var1) {
            if (time.charAt(var1) == 46 && var1 == 14) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasSeconds(String time) {
        return isDigit(time, 12) && isDigit(time, 13);
    }

    private static boolean hasMinutes(String time) {
        return isDigit(time, 10) && isDigit(time, 11);
    }

    private static boolean isDigit(String time, int var1) {
        return time.length() > var1 && time.charAt(var1) >= 48 && time.charAt(var1) <= 57;
    }

    public static Matrix getImageMatrixFromOfd(ImageObject nImageObject, ST_Box pageBox) {
        Matrix matrix = MatrixUtils.base();
        matrix = MatrixUtils.imageMatrix(matrix, 0, 1, 0);
        matrix = MatrixUtils.move(matrix, 0, 1);
        if (nImageObject.getCTM() != null) {
            Matrix ctm = MatrixUtils.base();
            ctm = ctm.mtimes(MatrixUtils.ctm(nImageObject.getCTM().toDouble()));
            matrix = matrix.mtimes(ctm);
        }

        ST_Box boundary = nImageObject.getBoundary();
        if (boundary == null) {
            boundary = pageBox;
        }

        Tuple2<Double, Double> tuple2 = MatrixUtils.leftTop(matrix);
//                    matrix = matrix.mtimes(MatrixUtils.create(1, 0, 0, 1, tuple2.getFirst().floatValue(),tuple2.getSecond().floatValue()));

        matrix = matrix.mtimes(MatrixUtils.create(1, 0, 0, 1, boundary.getTopLeftX(), boundary.getTopLeftY()));

//                    matrix=matrix.mtimes(MatrixUtils.create(1,0,0,1,0,boundary.getH()));

        matrix = MatrixUtils.imageMatrix(matrix, 0, 1, 0);
        matrix = matrix.mtimes(MatrixUtils.create(1, 0, 0, 1, 0, pageBox.getHeight()));

        matrix = matrix.mtimes(MatrixUtils.create(converterDpi(1), 0, 0, converterDpi(1), 0, 0));
        return matrix;
    }

    public static org.apache.pdfbox.util.Matrix toPFMatrix(Matrix source) {
        org.apache.pdfbox.util.Matrix target = new org.apache.pdfbox.util.Matrix();
        target.setValue(0, 0, source.getAsFloat(0, 0));
        target.setValue(0, 1, source.getAsFloat(0, 1));
        target.setValue(1, 0, source.getAsFloat(1, 0));
        target.setValue(1, 1, source.getAsFloat(1, 1));
        target.setValue(2, 0, source.getAsFloat(2, 0));
        target.setValue(2, 1, source.getAsFloat(2, 1));
        return target;
    }

//    private static PDFont getCFFFont(PDDocument doc, File fontFile)
//            throws IOException
//    {
//        PDFont pdFont = null;
//
//        COSDictionary dict = new COSDictionary();
//        dict.setItem(COSName.SUBTYPE, COSName.TYPE1);
//        FileInputStream fis = new FileInputStream(fontFile);
//        List<CFFFont> fonts = readCFFFont(fis);
//        CFFFont cffFont = fonts.get(0);
//        IOUtils.closeQuietly(fis);
//
//        CFFType1Font font = (CFFType1Font) fonts.get(0);
//
//        dict.setName(COSName.BASE_FONT, font.getName());
//        Encoding encoding = font.getEncoding();
//
//        COSDictionary fontEncoding = new COSDictionary();
//        fontEncoding.setItem(COSName.NAME, COSName.ENCODING);
//        dict.setItem(COSName.ENCODING, fontEncoding);
//
//        PDFontDescriptor fd = buildFontDescriptor(font);
//        // full embedding
//        PDStream stream = new PDStream(doc, new FileInputStream(fontFile),
//                COSName.FLATE_DECODE);
//        stream.getCOSObject().setLong(COSName.LENGTH1, fontFile.length());
//        fd.setFontFile3(stream);
//
//        dict.setItem(COSName.FONT_DESC, fd);
//
//        // widths
//        List<Integer> widths = new ArrayList<Integer>();
//        if (encoding instanceof CFFStandardEncoding)
//        {
//            CFFStandardEncoding sEncoding = (CFFStandardEncoding) encoding;
//            Map<Integer, String> codeToNames = sEncoding.getCodeToNameMap();
//            Map<Integer, Integer> newGIDToOldCID = new HashMap<Integer, Integer>();
//            for (Iterator<Integer> iterator = codeToNames.keySet()
//                    .iterator(); iterator.hasNext();)
//            {
//                Integer code = iterator.next();
//                // String name = codeToNames.get(code);
//                // if (!".notdef".equals(name))
//                // {
//                // int width = Math.round(font.getWidth(name));
//                // // widths.add(width);
//                // }
//                // else
//                // {
//                // // widths.add(0);
//                // }
//                widths.add(0);
//                newGIDToOldCID.put(code, code);
//            }
//
//            // buildToUnicodeCMap(doc, dict, newGIDToOldCID);
//
//        }
//
//        dict.setInt(COSName.FIRST_CHAR, 0);
//        dict.setInt(COSName.LAST_CHAR, 255);
//        dict.setItem(COSName.WIDTHS,
//                COSArrayList.converterToCOSArray(widths));
//
//        pdFont = PDFontFactory.createFont(dict);
//
//        return pdFont;
//    }
//
//
//    /**
//     * Returns a PDFontDescriptor for the given PFB.
//     */
//    private static PDFontDescriptor buildFontDescriptor(CFFType1Font type1)
//    {
//        PDFontDescriptor fd = new PDFontDescriptor();
//        Map<String, Object> privateDict = type1.getPrivateDict();
//        Map<String, Object> topDict = type1.getTopDict();
//        Integer italicAngle = (Integer) topDict.get("ItalicAngle");
//        List<Number> blueValues = (List<Number>) privateDict.get("BlueValues");
//        fd.setFontName(type1.getName());
//        fd.setFontFamily(type1.getName());
//        fd.setFontBoundingBox(new PDRectangle(type1.getFontBBox()));
//        fd.setItalicAngle(italicAngle);
//        fd.setAscent(type1.getFontBBox().getUpperRightY());
//        fd.setDescent(type1.getFontBBox().getLowerLeftY());
//        fd.setCapHeight(blueValues.get(2).floatValue());
//        fd.setStemV(0); // for PDF/A
//
//        return fd;
//    }
//
//
//    private static List<CFFFont> readCFFFont(InputStream in) throws IOException
//    {
//        ByteArrayOutputStream content = new ByteArrayOutputStream();
//        byte[] buf = new byte[1024];
//        int len;
//        while ((len = in.read(buf)) > -1)
//        {
//            content.write(buf, 0, len);
//        }
//
//        CFFParser parser = new CFFParser();
//        return parser.parse(content.toByteArray());
//    }

}
