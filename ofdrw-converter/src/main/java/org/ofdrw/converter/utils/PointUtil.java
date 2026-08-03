package org.ofdrw.converter.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ofdrw.converter.point.PathPoint;
import org.ofdrw.converter.point.TextCodePoint;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.graph.pathObj.AbbreviatedData;
import org.ofdrw.core.graph.pathObj.OptVal;
import org.ofdrw.core.text.CT_CGTransform;
import org.ofdrw.core.text.TextCode;
import org.ofdrw.reader.DeltaTool;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static org.ofdrw.converter.utils.CommonUtil.converterDpi;


/**
 * @author dltech21
 * @since 2020/8/11
 */
public class PointUtil {
    private final static Logger logger = LoggerFactory.getLogger(PointUtil.class);

    /**
     * Early Foxit OFD files store path coordinates in a 300 DPI device space.
     */
    private static final double LEGACY_PATH_DPI = 300d;
    private static final double LEGACY_PATH_MM_SCALE = 25.4d / LEGACY_PATH_DPI;

    /**
     * 解析压缩路径为点坐标
     *
     * @param abbreviatedData 压缩路径
     * @return 绘制点坐标序列
     */
    public static List<PathPoint> convertPathAbbreviatedDatatoPoint(String abbreviatedData) {
        // 解析成各个操作符和操作数
        final LinkedList<OptVal> optValArr = AbbreviatedData.parse(abbreviatedData);
        List<PathPoint> pointList = new ArrayList<>();
        for (OptVal optVal : optValArr) {
            double[] array = optVal.expectValues();
            switch (optVal.opt) {
                case "M":
                    pointList.add(new PathPoint("M",
                            (float) array[0], (float) array[1],
                            0, 0,
                            0, 0));
                    break;
                case "L":
                    pointList.add(new PathPoint("L",
                            (float) array[0], (float) array[1],
                            0, 0,
                            0, 0));
                    break;
                case "C":
                    pointList.add(new PathPoint("C", 0, 0, 0, 0, 0, 0));
                    break;
                case "S":
                    pointList.add(new PathPoint("S",
                            (float) array[0], (float) array[1],
                            0, 0,
                            0, 0));
                    break;
                case "B":
                    pointList.add(new PathPoint("B",
                            (float) array[0], (float) array[1],
                            (float) array[2], (float) array[3],
                            (float) array[4], (float) array[5]));
                    break;
                case "Q":
                    pointList.add(new PathPoint("Q",
                            (float) array[0], (float) array[1],
                            (float) array[2], (float) array[3],
                            0, 0));
                    break;
                case "A":
                    pointList.add(new PathPoint("A",
                            (float) array[0], (float) array[1],
                            (float) array[2], (float) array[3],
                            (float) array[4], (float) array[5],
                            (float) array[6]));
                    break;
            }
        }
        return pointList;
    }

    public static List<PathPoint> calPathPoint(List<PathPoint> abbreviatedPoint) {
        List<PathPoint> pointList = new ArrayList<>();
        for (PathPoint point : abbreviatedPoint) {
            if (point.type.equals("M") || point.type.equals("L") || point.type.equals("C") || point.type.equals("S")) {
                double x = 0, y = 0;
                x = point.x1;
                y = point.y1;
                point.x1 = (float) converterDpi(x);
                point.y1 = (float) converterDpi(y);
                pointList.add(point);
            } else if (point.type.equals("B")) {
                double x1 = point.x1, y1 = point.y1;
                double x2 = point.x2, y2 = point.y2;
                double x3 = point.x3, y3 = point.y3;
                PathPoint realPoint = new PathPoint("B", (float) converterDpi(x1), (float) converterDpi(y1),
                        (float) converterDpi(x2), (float) converterDpi(y2),
                        (float) converterDpi(x3), (float) converterDpi(y3));
                pointList.add(realPoint);
            }
        }
        return pointList;
    }

    public static List<TextCodePoint> calTextPoint(List<TextCode> textCodes) {
        double x = 0, y = 0;
        List<TextCodePoint> textCodePointList = new ArrayList<>();
        for (TextCode textCode : textCodes) {
            x = textCode.getX();
            y = textCode.getY();

            Double[] deltaXList = null;
            Double[] deltaYList = null;
            if (textCode.getDeltaX() != null && textCode.getDeltaX().getArray().size() > 0) {
                deltaXList = textCode.getDeltaX().toDouble();
            }
            if (textCode.getDeltaY() != null && textCode.getDeltaY().getArray().size() > 0) {
                deltaYList = textCode.getDeltaY().toDouble();
            }
            for (int i = 0; i < textCode.getText().length(); i++) {
                if (i > 0 && Objects.nonNull(deltaXList)) {
                    x += deltaXList[i - 1];
                }
                if (i > 0 && Objects.nonNull(deltaYList)) {
                    y += deltaYList[i - 1];
                }
                String text = textCode.getText().substring(i, i + 1);
                TextCodePoint textCodePoint = new TextCodePoint(converterDpi(x), converterDpi(y), text);
                textCodePointList.add(textCodePoint);
            }
        }
        return textCodePointList;
    }


    public static double[] adjustPos(double width, double height, double x, double y, ST_Box boundary) {
        if (boundary == null) {
            return new double[] {x, y};
        }
        double realX = boundary.getTopLeftX() + x;
//        if (realX > (boundary.getTopLeftX() + boundary.getWidth())) {
//            realX = boundary.getTopLeftX() + boundary.getWidth();
//        }
        double realY = boundary.getTopLeftY() + y;
//        if (realY > height) {
//            realY = height;
//        }
        return new double[]{realX, realY};
    }

    /**
     * 将子图元的外接矩形转换到父图元坐标系。
     *
     * @param parentBoundary 父图元外接矩形
     * @param childBoundary  子图元在父图元坐标系中的外接矩形
     * @return 子图元在页面坐标系中的外接矩形
     */
    public static ST_Box combineBoundary(ST_Box parentBoundary, ST_Box childBoundary) {
        if (parentBoundary == null) {
            return childBoundary;
        }
        if (childBoundary == null) {
            return parentBoundary;
        }
        return new ST_Box(
                parentBoundary.getTopLeftX() + childBoundary.getTopLeftX(),
                parentBoundary.getTopLeftY() + childBoundary.getTopLeftY(),
                childBoundary.getWidth(),
                childBoundary.getHeight());
    }

    public static double[] ctmCalPoint(double x, double y, Double[] ctm) {
        double ctmX = x * ctm[0] + y * ctm[2] + 1 * ctm[4];
        double ctmY = x * ctm[1] + y * ctm[3] + 1 * ctm[5];
        return new double[]{ctmX, ctmY};
    }

    private static double[] ctmCalTextVector(double deltaX, double deltaY, Double[] ctm) {
        double ctmX = deltaX * ctm[0] + deltaY * ctm[2];
        double ctmY = deltaX * ctm[1] + deltaY * ctm[3];
        return new double[]{ctmX, ctmY};
    }

    /**
     * Detects the absolute 300 DPI path coordinates used by pre-standard Foxit OFD files.
     * Their declared CTM places the complete path outside its Boundary, while interpreting
     * the raw coordinates as 300 DPI device coordinates places it inside the Boundary.
     */
    public static boolean isLegacyAbsolutePath(double width, double height, ST_Box boundary,
                                               List<PathPoint> points, boolean hasCtm, ST_Array ctm) {
        if (boundary == null || !hasCtm || ctm == null || points == null || points.isEmpty()) {
            return false;
        }

        PathBounds raw = pathBounds(points, null, 1d);
        if (raw == null || (raw.maxAbsX() <= width * 2 && raw.maxAbsY() <= height * 2)) {
            return false;
        }

        PathBounds standard = pathBounds(points, ctm.toDouble(), 1d);
        if (standard != null && standard.intersects(0, 0, boundary.getWidth(), boundary.getHeight())) {
            return false;
        }

        PathBounds legacy = pathBounds(points, null, LEGACY_PATH_MM_SCALE);
        double tolerance = Math.max(0.5d, Math.max(boundary.getWidth(), boundary.getHeight()) * 0.1d);
        return legacy != null && legacy.inside(
                boundary.getTopLeftX() - tolerance,
                boundary.getTopLeftY() - tolerance,
                boundary.getTopLeftX() + boundary.getWidth() + tolerance,
                boundary.getTopLeftY() + boundary.getHeight() + tolerance);
    }

    /**
     * Converts an OFD path line width to PDF points.
     */
    public static double calPdfPathLineWidth(double lineWidth, double scale,
                                             boolean legacyAbsolutePath, ST_Array ctm) {
        if (legacyAbsolutePath) {
            // The legacy Foxit value is already expressed in PDF user units (points).
            return lineWidth * scale;
        }
        double result = converterDpi(lineWidth) * scale;
        if (ctm != null) {
            Double[] values = ctm.toDouble();
            double sx = Math.signum(values[0])
                    * Math.sqrt(values[0] * values[0] + values[2] * values[2]);
            result *= sx;
        }
        return result;
    }

    private static PathBounds pathBounds(List<PathPoint> points, Double[] ctm, double scale) {
        PathBounds bounds = new PathBounds();
        for (PathPoint point : points) {
            switch (point.type) {
                case "M":
                case "L":
                case "S":
                    bounds.add(point.x1, point.y1, ctm, scale);
                    break;
                case "B":
                    bounds.add(point.x1, point.y1, ctm, scale);
                    bounds.add(point.x2, point.y2, ctm, scale);
                    bounds.add(point.x3, point.y3, ctm, scale);
                    break;
                case "Q":
                    bounds.add(point.x1, point.y1, ctm, scale);
                    bounds.add(point.x2, point.y2, ctm, scale);
                    break;
                case "A":
                    bounds.add(point.x, point.y, ctm, scale);
                    break;
                default:
                    break;
            }
        }
        return bounds.empty ? null : bounds;
    }

    private static class PathBounds {
        private boolean empty = true;
        private double minX;
        private double minY;
        private double maxX;
        private double maxY;

        private void add(double x, double y, Double[] ctm, double scale) {
            if (ctm != null) {
                double[] transformed = ctmCalPoint(x, y, ctm);
                x = transformed[0];
                y = transformed[1];
            }
            x *= scale;
            y *= scale;
            if (empty) {
                minX = maxX = x;
                minY = maxY = y;
                empty = false;
                return;
            }
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }

        private double maxAbsX() {
            return Math.max(Math.abs(minX), Math.abs(maxX));
        }

        private double maxAbsY() {
            return Math.max(Math.abs(minY), Math.abs(maxY));
        }

        private boolean intersects(double left, double top, double right, double bottom) {
            return maxX >= left && minX <= right && maxY >= top && minY <= bottom;
        }

        private boolean inside(double left, double top, double right, double bottom) {
            return minX >= left && maxX <= right && minY >= top && maxY <= bottom;
        }
    }

    public static List<PathPoint> calPdfPathPoint(double width, double height, ST_Box boundary, List<PathPoint> abbreviatedPoint, boolean hasCtm, ST_Array ctm, ST_Box compositeObjectBoundary, ST_Array compositeObjectCTM, boolean fixOriginToPdf) {
    	return calPdfPathPoint(width, height, boundary, abbreviatedPoint, hasCtm, ctm, compositeObjectBoundary, compositeObjectCTM, fixOriginToPdf, 1.0);
    }
    
    public static List<PathPoint> calPdfPathPoint(double width, double height, ST_Box boundary, List<PathPoint> abbreviatedPoint, boolean hasCtm, ST_Array ctm, ST_Box compositeObjectBoundary, ST_Array compositeObjectCTM, boolean fixOriginToPdf, double scale) {
        List<PathPoint> pointList = new ArrayList<>();
        boolean legacyAbsolutePath = scale == 1d && compositeObjectBoundary == null
                && isLegacyAbsolutePath(width, height, boundary, abbreviatedPoint, hasCtm, ctm);
        for (PathPoint point : abbreviatedPoint) {
            if (point.type.equals("M") || point.type.equals("L") || point.type.equals("C") || point.type.equals("S")) {
                double x = 0, y = 0;
                x = point.x1;
                y = point.y1;

                if (legacyAbsolutePath) {
                    x *= LEGACY_PATH_MM_SCALE;
                    y *= LEGACY_PATH_MM_SCALE;
                } else if (hasCtm) {
                    double[] newPoint = ctmCalPoint(x, y, ctm.toDouble());
                    x = newPoint[0];
                    y = newPoint[1];
                }
                double[] realPos = legacyAbsolutePath
                        ? new double[]{x, y}
                        : adjustPos(width, height, x * scale, y * scale, boundary);
                point.x1 = (float) converterDpi(realPos[0]);
                point.y1 = (float) converterDpi(fixOriginToPdf ? (height - realPos[1]) : realPos[1]);
                if (compositeObjectBoundary != null) {
                    if (compositeObjectCTM != null) {
                        realPos = ctmCalPoint(realPos[0], realPos[1], compositeObjectCTM.toDouble());
                    }
                    realPos = adjustPos(width, height, realPos[0], realPos[1], compositeObjectBoundary);
                    point.x1 = (float) converterDpi(realPos[0]);
                    point.y1 = (float) converterDpi(fixOriginToPdf ? (height - realPos[1]) : realPos[1]);
                }
                pointList.add(point);
            } else if (point.type.equals("B")) {
                double x1 = point.x1, y1 = point.y1;
                double x2 = point.x2, y2 = point.y2;
                double x3 = point.x3, y3 = point.y3;
                if (legacyAbsolutePath) {
                    x1 *= LEGACY_PATH_MM_SCALE;
                    y1 *= LEGACY_PATH_MM_SCALE;
                    x2 *= LEGACY_PATH_MM_SCALE;
                    y2 *= LEGACY_PATH_MM_SCALE;
                    x3 *= LEGACY_PATH_MM_SCALE;
                    y3 *= LEGACY_PATH_MM_SCALE;
                } else if (hasCtm) {
                    double[] newPoint = ctmCalPoint(x1, y1, ctm.toDouble());
                    x1 = newPoint[0];
                    y1 = newPoint[1];
                    newPoint = ctmCalPoint(x2, y2, ctm.toDouble());
                    x2 = newPoint[0];
                    y2 = newPoint[1];
                    newPoint = ctmCalPoint(x3, y3, ctm.toDouble());
                    x3 = newPoint[0];
                    y3 = newPoint[1];
                }
                double[] realPos = legacyAbsolutePath
                        ? new double[]{x1, y1}
                        : adjustPos(width, height, x1 * scale, y1 * scale, boundary);
                x1 = realPos[0];
                y1 = realPos[1];
                realPos = legacyAbsolutePath
                        ? new double[]{x2, y2}
                        : adjustPos(width, height, x2 * scale, y2 * scale, boundary);
                x2 = realPos[0];
                y2 = realPos[1];
                realPos = legacyAbsolutePath
                        ? new double[]{x3, y3}
                        : adjustPos(width, height, x3 * scale, y3 * scale, boundary);
                x3 = realPos[0];
                y3 = realPos[1];
                PathPoint realPoint = new PathPoint("B", (float) converterDpi(x1), (float) converterDpi(fixOriginToPdf ? (height - y1) : y1),
                        (float) converterDpi(x2), (float) converterDpi(fixOriginToPdf ? (height - y2) : y2),
                        (float) converterDpi(x3), (float) converterDpi(fixOriginToPdf ? (height - y3) : y3));
                pointList.add(realPoint);
            } else if (point.type.equals("Q")) {
                double x1 = point.x1, y1 = point.y1;
                double x2 = point.x2, y2 = point.y2;
                if (legacyAbsolutePath) {
                    x1 *= LEGACY_PATH_MM_SCALE;
                    y1 *= LEGACY_PATH_MM_SCALE;
                    x2 *= LEGACY_PATH_MM_SCALE;
                    y2 *= LEGACY_PATH_MM_SCALE;
                } else if (hasCtm) {
                    double[] newPoint = ctmCalPoint(x1, y1, ctm.toDouble());
                    x1 = newPoint[0];
                    y1 = newPoint[1];
                    newPoint = ctmCalPoint(x2, y2, ctm.toDouble());
                    x2 = newPoint[0];
                    y2 = newPoint[1];
                }
                double[] realPos = legacyAbsolutePath
                        ? new double[]{x1, y1}
                        : adjustPos(width, height, x1 * scale, y1 * scale, boundary);
                x1 = realPos[0];
                y1 = realPos[1];
                realPos = legacyAbsolutePath
                        ? new double[]{x2, y2}
                        : adjustPos(width, height, x2 * scale, y2 * scale, boundary);
                x2 = realPos[0];
                y2 = realPos[1];
                PathPoint realPoint = new PathPoint("Q", (float) converterDpi(x1), (float) converterDpi(fixOriginToPdf ? (height - y1) : y1),
                        (float) converterDpi(x2), (float) converterDpi(fixOriginToPdf ? (height - y2) : y2),
                        0, 0);
                pointList.add(realPoint);
            } else if (point.type.equals("A")) {
                double rx = point.rx, ry = point.ry;
                float rotation = point.rotation, arc = point.arc, sweep = point.sweep;
                double x = point.x, y = point.y;
                if (legacyAbsolutePath) {
                    rx *= LEGACY_PATH_MM_SCALE;
                    ry *= LEGACY_PATH_MM_SCALE;
                    x *= LEGACY_PATH_MM_SCALE;
                    y *= LEGACY_PATH_MM_SCALE;
                } else if (hasCtm) {
                    double[] newPoint = ctmCalPoint(x, y, ctm.toDouble());
                    x = newPoint[0];
                    y = newPoint[1];
                }
                double[] realPos = legacyAbsolutePath
                        ? new double[]{x, y}
                        : adjustPos(width, height, x * scale, y * scale, boundary);
                x = realPos[0];
                y = realPos[1];
                PathPoint realPoint = new PathPoint("A", (float) converterDpi(rx), (float) converterDpi(ry),
                        rotation, arc, sweep,
                        (float) converterDpi(x), (float) converterDpi(fixOriginToPdf ? (height - y) : y));
                pointList.add(realPoint);
            }
        }
        return pointList;
    }

    public static List<TextCodePoint> calPdfTextCoordinate(double width, double height, ST_Box boundary, float fontSize, List<TextCode> textCodes, boolean hasCtm, ST_Array ctm, boolean fixOriginToPdf, double scale) {
        List<TextCodePoint> textCodePointList = new ArrayList<>();
        Double[] textCtm = hasCtm && ctm != null ? ctm.toDouble() : null;
        for (TextCode textCode : textCodes) {
            double localX = textCode.getX() == null ? 0 : textCode.getX();
            double localY = textCode.getY() == null ? 0 : textCode.getY();
            double transformedX = localX;
            double transformedY = localY;
            if (textCtm != null) {
                // The TextCode origin is a point, so the CTM translation is applied once here.
                double[] transformedPoint = ctmCalPoint(localX, localY, textCtm);
                transformedX = transformedPoint[0];
                transformedY = transformedPoint[1];
            }
            List<Float> deltaXList = null;
            List<Float> deltaYList = null;
            String textStr = textCode.getText();
            if (textCode.getDeltaX() != null && textCode.getDeltaX().getArray().size() > 0) {
                deltaXList = DeltaTool.getDelta(textCode.getDeltaX(), textStr.length());
            }
            if (textCode.getDeltaY() != null && textCode.getDeltaY().getArray().size() > 0) {
                deltaYList = DeltaTool.getDelta(textCode.getDeltaY(), textStr.length());
            }

            textStr = textStr.replaceAll("&lt;", "<");
            textStr = textStr.replaceAll("&gt;", ">");
            textStr = textStr.replaceAll("&amp;", "&");
            textStr = textStr.replaceAll("\n", "");
            textStr = textStr.replaceAll("&nbsp;", " ");
            textStr = textStr.replaceAll("&quot;", "\"");
            textStr = textStr.replaceAll("&copy;", "");
            textStr = textStr.replaceAll("&apos;", "'");
            for (int i = 0; i < textStr.length(); i++) {
                if (i > 0) {
                    double deltaX = Objects.nonNull(deltaXList) ? deltaXList.get(i - 1) : 0;
                    double deltaY = Objects.nonNull(deltaYList) ? deltaYList.get(i - 1) : 0;
                    localX += deltaX;
                    localY += deltaY;
                    if (textCtm != null) {
                        // DeltaX and DeltaY form one vector and must not include CTM translation.
                        double[] transformedDelta = ctmCalTextVector(deltaX, deltaY, textCtm);
                        transformedX += transformedDelta[0];
                        transformedY += transformedDelta[1];
                    } else {
                        transformedX = localX;
                        transformedY = localY;
                    }
                }
                double[] realPos = adjustPos(width, height, transformedX * scale, transformedY * scale, boundary);
                String text = textStr.substring(i, i + 1);
                TextCodePoint textCodePoint = new TextCodePoint(converterDpi(realPos[0]), converterDpi(fixOriginToPdf ? (height - realPos[1]) : realPos[1]), text);
                textCodePointList.add(textCodePoint);
            }
        }
        return textCodePointList;
    }

    public static List<TextCodePoint> calPdfTextCoordinate(double width, double height, ST_Box boundary, float fontSize, List<TextCode> textCodes, List<CT_CGTransform> cgTransforms, ST_Box compositeObjectBoundary, ST_Array compositeObjectCTM, boolean hasCtm, ST_Array ctm, boolean fixOriginToPdf) {
    	return calPdfTextCoordinate(width, height, boundary, fontSize, textCodes, cgTransforms, compositeObjectBoundary, compositeObjectCTM, hasCtm, ctm, fixOriginToPdf, 1.0);
    }
    
    public static List<TextCodePoint> calPdfTextCoordinate(double width, double height, ST_Box boundary, float fontSize, List<TextCode> textCodes, List<CT_CGTransform> cgTransforms, ST_Box compositeObjectBoundary, ST_Array compositeObjectCTM, boolean hasCtm, ST_Array ctm, boolean fixOriginToPdf, double scale) {
        double x = 0, y = 0;
        List<TextCodePoint> textCodePointList = new ArrayList<>();
        for (TextCode textCode : textCodes) {
            x = textCode.getX() == null ? 0 : textCode.getX();
            y = textCode.getY() == null ? 0 : textCode.getY();

            if (hasCtm) {
                double[] newPoint = ctmCalPoint(x, y, ctm.toDouble());
                x = newPoint[0];
                y = newPoint[1];
            }
            List<String> deltaXList = null;
            List<String> deltaYList = null;
            String textStr = textCode.getText();
            if (textCode.getDeltaX() != null && textCode.getDeltaX().getArray().size() > 0) {
                deltaXList = textCode.getDeltaX().getArray();
            }
            if (textCode.getDeltaY() != null && textCode.getDeltaY().getArray().size() > 0) {
                deltaYList = textCode.getDeltaY().getArray();
            }

            textStr = textStr.replaceAll("&lt;", "<");
            textStr = textStr.replaceAll("&gt;", ">");
            textStr = textStr.replaceAll("&amp;", "&");
            textStr = textStr.replaceAll("\n", "");
            textStr = textStr.replaceAll("&nbsp;", " ");
            textStr = textStr.replaceAll("&quot;", "\"");
            textStr = textStr.replaceAll("&copy;", "");
            textStr = textStr.replaceAll("&apos;", "'");

            int skipCount = 0;
            for (int i = 0; i < textStr.length(); i++) {
                String text = textStr.substring(i, i + 1);
                boolean skipPosition = false;
                if (i > 0 && Objects.nonNull(deltaXList)) {
                    for (CT_CGTransform cgTransform : cgTransforms) {
                        int pos = cgTransform.getCodePosition();
                        int codeCount = cgTransform.getCodeCount();
                        if (i > pos && i < (pos + codeCount)) {
                            if (cgTransform.getGlyphs().size() < codeCount) {
                                skipPosition = true;

                                skipCount++;
                            }
                            break;
                        }
                    }
                    int index = i - 1;
//                    boolean keepSameDX = false;
                    if (index >= deltaXList.size()) {
                        index = deltaXList.size() - 1;
//                        keepSameDX = true;
                    }
                    double dx = Double.parseDouble(deltaXList.get(index));
                    if (dx != 0) {
                        if (hasCtm) {
                            Double[] ctms = ctm.toDouble();
                            double a = ctms[0].doubleValue();
                            double b = ctms[1].doubleValue();
                            double c = ctms[2].doubleValue();
                            double d = ctms[3].doubleValue();
                            double e = ctms[4].doubleValue();
                            double f = ctms[5].doubleValue();
                            double angel = Math.atan2(-b, d);

                            double[] newPoint = ctmCalPoint(dx, 0, ctm.toDouble());
                            // 无旋转时直接应用CTM
                            if (angel == 0) {
                                dx = newPoint[0];
                            } else {
                                // 如果竖排文字，水平偏移转化为变换后坐标的垂直分量
                                if (a == 0 && d == 0) {
                                    dx = Math.abs(newPoint[1]); // 取绝对值防止负数
                                }
                            }
                        } else {
                            if (skipPosition) {
                                deltaXList.add(i - 1, "0");
                            }
                        }
                    }
//                    x += keepSameDX ? dx : !skipPosition ? dx : 0;
                    x += !skipPosition ? dx : 0;
                }
                if (i > 0 && Objects.nonNull(deltaYList)) {
                    int index = i - 1;
//                    boolean keepSameDY = false;
                    if ((i - 1) >= deltaYList.size()) {
                        index = deltaYList.size() - 1;
//                        keepSameDY = true;
                    }
                    double dy = Double.parseDouble(deltaYList.get(index));
                    if (dy != 0) {
                        if (hasCtm) {
                            Double[] ctms = ctm.toDouble();
                            double a = ctms[0].doubleValue();
                            double b = ctms[1].doubleValue();
                            double c = ctms[2].doubleValue();
                            double d = ctms[3].doubleValue();
                            double e = ctms[4].doubleValue();
                            double f = ctms[5].doubleValue();
                            double angel = Math.atan2(-b, d);
                            if (angel == 0) {
                                double[] newPoint = ctmCalPoint(0, dy, ctm.toDouble());
                                dy = newPoint[1];
                            } else {
                                if (a == 0 && d == 0) {
                                    dy = dy * fontSize;
                                }
                            }
                        }
                    }
//                    y += keepSameDY ? 0 : dy;
                    y += dy;
                }
                double[] realPos = adjustPos(width, height, x * scale, y * scale, boundary);
                if (compositeObjectCTM != null) {
                    realPos = ctmCalPoint(realPos[0], realPos[1], compositeObjectCTM.toDouble());
                }
                if (skipPosition) {
                    text = "";
                }
                TextCodePoint textCodePoint = new TextCodePoint(converterDpi(realPos[0]), converterDpi(fixOriginToPdf ? (height - realPos[1]) : realPos[1]), text);
                textCodePointList.add(textCodePoint);
            }
        }//todo 先按textcode有值
        if (textCodePointList.size() > 0) {
            for (CT_CGTransform cgTransform : cgTransforms) {
                int pos = cgTransform.getCodePosition();
                int glyphCount = cgTransform.getGlyphCount();
                int codeCount = cgTransform.getCodeCount();
                for (int i = pos; i < glyphCount + pos; i++) {
                    if (textCodePointList.size() <= i) {
                        String glyphs = textCodePointList.get(textCodePointList.size() - 1).getGlyph() + " " + cgTransform.getGlyphs().getArray().get(i - pos);
                        textCodePointList.get(textCodePointList.size() - 1).setGlyph(glyphs);
                    } else {
                        textCodePointList.get(i).setGlyph(cgTransform.getGlyphs().getArray().get(i - pos));
                    }
                }
            }
        }
        return textCodePointList;
    }
}
