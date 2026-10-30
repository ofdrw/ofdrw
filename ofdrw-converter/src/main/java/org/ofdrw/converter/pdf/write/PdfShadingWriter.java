package org.ofdrw.converter.pdf.write;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceGray;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceCMYK;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShadingType2;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShadingType3;
import org.ofdrw.converter.pdf.model.PdfColor;
import org.ofdrw.converter.pdf.model.ShadingCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * 渐变写入器（P3）：中间模型 {@link ShadingCommand} → PDF Shading Pattern
 * <p>
 * 写法（设计 3.4/第 2 节表格）：
 * <ol>
 *     <li>轴向渐变写 PDF Type 2（{@code /Coords [x0 y0 x1 y1]}），径向写 Type 3
 *     （{@code /Coords [x0 y0 r0 x1 y1 r1]}）；</li>
 *     <li>颜色空间保持中间模型的原色空间：CMYK 用 {@code /DeviceCMYK}（绝不转 RGB），
 *     颜色 Alpha 以 {@code /ca} 写入图形状态（决策 2）；</li>
 *     <li>{@code /Extend} 数组由 {@link ShadingCommand#getExtend()} 决定（见 ShadingRenderer 的映射推导）；
 *     PDF 只有“平铺/透明”两种延长方式，OFD 的 Repeat/Reflect 已在渲染阶段降级为 PAD 并告警；</li>
 *     <li>{@code sh} 操作符作用于当前裁剪路径，因此绘制顺序固定为
 *     {@code q → W n（填充区域裁剪）→ ca → sh → Q}，不依赖内容流隐式状态（设计约束 1）。</li>
 * </ol>
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class PdfShadingWriter {

    private static final Logger logger = LoggerFactory.getLogger(PdfShadingWriter.class);

    /**
     * 形状对象工厂（供裁剪区域复用页面坐标路径）
     */
    private final PdfPathFactory pathFactory;

    /**
     * 页面内渐变名序号
     */
    private int shadingIndex = 0;

    /**
     * 构造渐变写入器
     *
     * @param pathFactory 路径工厂，不能为空
     */
    public PdfShadingWriter(PdfPathFactory pathFactory) {
        if (pathFactory == null) {
            throw new IllegalArgumentException("路径工厂不能为空");
        }
        this.pathFactory = pathFactory;
    }

    /**
     * 写入渐变填充
     *
     * @param document   PDF 文档
     * @param stream     页面内容流
     * @param resources  页面资源字典
     * @param command    渐变指令
     * @param alpha      不透明度，0~1
     * @throws IOException 写入失败
     */
    public void write(PDDocument document, PDPageContentStream stream, PDResources resources,
                      ShadingCommand command, float alpha) throws IOException {
        if (command == null) {
            return;
        }
        PDShading shading = createShading(document, command);
        // 渐变以页面资源的方式命名并引用（内容流中为 /Sh<n> sh）
        String shadingName = "Sh" + (shadingIndex++);
        COSDictionary shadings = (COSDictionary) resources.getCOSObject().getDictionaryObject(COSName.SHADING);
        if (shadings == null) {
            shadings = new COSDictionary();
            resources.getCOSObject().setItem(COSName.SHADING, shadings);
        }
        shadings.setItem(COSName.getPDFName(shadingName), shading.getCOSObject());
        // 渐变依赖当前裁剪路径：先按填充区域裁剪，再 sh
        stream.saveGraphicsState();
        stream.setNonStrokingColor(new PDColor(new float[]{alpha}, PDDeviceGray.INSTANCE));
        applyAlpha(stream, alpha);
        pathFactory.clip(stream, command.getArea(), command.getFillRule());
        stream.appendRawCommands("/" + shadingName + " sh\n");
        stream.restoreGraphicsState();
    }

    /**
     * 创建 PDF 渐变对象
     *
     * @param document PDF 文档
     * @param command  渐变指令
     * @return 渐变对象
     * @throws IOException 创建失败
     */
    public PDShading createShading(PDDocument document, ShadingCommand command) throws IOException {
        PDShading shading;
        if (command.getShadingType() == ShadingCommand.ShadingType.RADIAL) {
            PDShadingType3 radial = new PDShadingType3(new COSDictionary());
            radial.setShadingType(PDShading.SHADING_TYPE3);
            // PDF Type 3：两个圆的圆心与半径；OFD 的起点为渐变中心（半径缺省 0），终点为外圆圆心
            radial.setCoords(numbers(command.getStartX(), command.getStartY(), 0,
                    command.getEndX(), command.getEndY(), endRadius(command)));
            shading = radial;
        } else {
            PDShadingType2 axial = new PDShadingType2(new COSDictionary());
            axial.setShadingType(PDShading.SHADING_TYPE2);
            axial.setCoords(numbers(command.getStartX(), command.getStartY(),
                    command.getEndX(), command.getEndY()));
            shading = axial;
        }
        ((PDShadingType2) shading).setExtend(pdfExtend(command));
        shading.getCOSObject().setItem(COSName.COLORSPACE, colorSpaceOf(command.getStops()).getCOSObject());
        shading.getCOSObject().setItem(COSName.FUNCTION, function(command));
        return shading;
    }

    /**
     * 径向渐变外圆半径
     * <p>
     * OFD 径向渐变的结束半径是渐变区间长度（由渲染阶段换算为页面坐标），
     * 中间模型以起止圆心之间的距离表示；两者一致时即为 OFD 的 EndRadius。
     *
     * @param command 渐变指令
     * @return 半径，单位 pt；至少为 0
     */
    private static double endRadius(ShadingCommand command) {
        double dx = command.getEndX() - command.getStartX();
        double dy = command.getEndY() - command.getStartY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        return Math.max(0, distance);
    }

    /**
     * PDF 扩展开关
     *
     * @param command 渐变指令
     * @return 长度为 2 的数组
     */
    private static COSArray pdfExtend(ShadingCommand command) {
        boolean[] extend = extendArray(command.getExtend());
        COSArray array = new COSArray();
        array.add(org.apache.pdfbox.cos.COSBoolean.getBoolean(extend[0]));
        array.add(org.apache.pdfbox.cos.COSBoolean.getBoolean(extend[1]));
        return array;
    }

    /**
     * 中间模型扩展方式 → PDF Extend 数组
     *
     * @param extend 扩展方式，可为空
     * @return 长度为 2 的数组：{@code [起始侧, 结束侧]}
     */
    public static boolean[] extendArray(ShadingCommand.Extend extend) {
        if (extend == null || extend == ShadingCommand.Extend.NONE) {
            return new boolean[]{false, false};
        }
        // PAD 表示两端都以边界颜色平铺；REPEAT/REFLECT 在渲染阶段已降级为 PAD（决策 7 告警）
        return new boolean[]{true, true};
    }

    /**
     * 渐变颜色空间（取第一个停止点的颜色空间，全部停止点必须一致）
     *
     * @param stops 停止点
     * @return PDF 颜色空间
     */
    private static PDColorSpace colorSpaceOf(List<ShadingCommand.ColorStop> stops) {
        PdfColor first = stops.get(0).getColor();
        switch (first.getColorSpace()) {
            case GRAY:
                return PDDeviceGray.INSTANCE;
            case CMYK:
                return PDDeviceCMYK.INSTANCE;
            case RGB:
            default:
                return PDDeviceRGB.INSTANCE;
        }
    }

    /**
     * 渐变分段函数：每个相邻停止点之间线性插值（Type 2 指数函数，C0/C1 为端点颜色）
     *
     * @param command 渐变指令
     * @return 函数字典
     */
    private COSDictionary function(ShadingCommand command) {
        List<ShadingCommand.ColorStop> stops = command.getStops();
        COSArray functions = new COSArray();
        COSArray bounds = new COSArray();
        COSArray encode = new COSArray();
        for (int i = 0; i < stops.size() - 1; i++) {
            COSDictionary fn = new COSDictionary();
            fn.setItem(COSName.FUNCTION_TYPE, COSName.getPDFName("2"));
            fn.setItem(COSName.DOMAIN, numbers(0, 1));
            fn.setItem(COSName.getPDFName("C0"), colorArray(stops.get(i).getColor()));
            fn.setItem(COSName.getPDFName("C1"), colorArray(stops.get(i + 1).getColor()));
            fn.setInt(COSName.getPDFName("N"), 1);
            functions.add(fn);
            if (i < stops.size() - 2) {
                // 中间停止点作为分段边界（最后一对不需要边界）
                bounds.add(new COSFloat((float) stops.get(i + 1).getFraction()));
                encode.add(numbers(0, 1));
            }
        }
        if (functions.size() == 1) {
            return (COSDictionary) functions.getObject(0);
        }
        COSDictionary stitched = new COSDictionary();
        stitched.setItem(COSName.FUNCTION_TYPE, COSName.getPDFName("3"));
        stitched.setItem(COSName.DOMAIN, numbers(0, 1));
        stitched.setItem(COSName.getPDFName("Functions"), functions);
        stitched.setItem(COSName.getPDFName("Bounds"), bounds);
        stitched.setItem(COSName.getPDFName("Encode"), encode);
        return stitched;
    }

    /**
     * 颜色 → PDF 通道数组（保持原颜色空间，CMYK 不转 RGB）
     *
     * @param color 颜色
     * @return 通道数组
     */
    private static COSArray colorArray(PdfColor color) {
        COSArray array = new COSArray();
        for (int i = 0; i < color.getComponentCount(); i++) {
            array.add(new COSFloat(color.getChannel(i)));
        }
        return array;
    }

    /**
     * 构造数值数组
     *
     * @param values 数值
     * @return 数组
     */
    private static COSArray numbers(double... values) {
        COSArray array = new COSArray();
        for (double value : values) {
            array.add(new COSFloat((float) value));
        }
        return array;
    }

    /**
     * 构造数值数组
     *
     * @param values 数值
     * @return 数组
     */
    private static COSArray numbers(float... values) {
        COSArray array = new COSArray();
        for (float value : values) {
            array.add(new COSFloat(value));
        }
        return array;
    }

    /**
     * 写入填充不透明度（{@code /ca}）
     *
     * @param stream 内容流
     * @param alpha  不透明度，0~1
     * @throws IOException 写入失败
     */
    private static void applyAlpha(PDPageContentStream stream, float alpha) throws IOException {
        org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState state =
                new org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState();
        state.setNonStrokingAlphaConstant(alpha);
        stream.setGraphicsStateParameters(state);
    }
}
