package org.ofdrw.layout.element.canvas;

import org.ofdrw.core.basicType.STBase;

import java.io.IOException;
import java.util.LinkedList;

/**
 * 类单元格特殊Canvas 绘制器
 * <p>
 * 可以实现类似于单元个效果，用于简化区域占位区块绘制。
 *
 * @author 权观宇
 * @since 2023-11-13 18:48:53
 */
public class CellContentDrawer implements Drawer {

    /**
     * 调试开关，在开启后会绘制辅助线
     */
    public static Boolean DEBUG = false;

    /**
     * Canvas对象
     */
    private final Canvas canvas;

    /**
     * 单元格文字内容
     */
    private String value;

    /**
     * 文字水平浮动方式
     * <p>
     * 默认：左浮动
     */
    private TextAlign textAlign = TextAlign.left;

    /**
     * 文字垂直方向浮动方式
     * <p>
     * 默认：居中
     */
    private VerticalAlign verticalAlign = VerticalAlign.center;


    /**
     * 文字颜色
     * <p>
     * 支持16进制颜色值：#000000
     * <p>
     * RGB：rgb(0,0,0)
     * <p>
     * RGBA：rgba(0,0,0,1)
     * <p>
     * 默认：#000000 （黑色）
     */
    private String color = "#000000";

    /**
     * 字体名称
     * <p>
     * 默认：宋体
     */
    private String fontName = "宋体";

    /**
     * 字号
     * <p>
     * 默认：3 （单位：毫米）
     */
    private double fontSize = 3;

    /**
     * 行间距
     */
    private Double lineSpace = 0.6;


    /**
     * 是否加粗
     */
    private Boolean blob = false;

    /**
     * 是否斜体
     */
    private Boolean italic = false;

    /**
     * 文字之间的间距
     */
    private Double letterSpacing = 0d;


    /**
     * 通过已有Canvas构造单元格
     * <p>
     * 注意该方法将会替换Canvas的绘制器为单元格的绘制器。
     *
     * @param canvas Canvas
     */
    public CellContentDrawer(Canvas canvas) {
        if (canvas == null) {
            throw new IllegalArgumentException("Canvas 不能为空");
        }
        this.canvas = canvas;
        canvas.setDrawer(this);
    }

    /**
     * 创建单元格
     *
     * @param x      左下角x坐标
     * @param y      左下角y坐标
     * @param width  宽度
     * @param height 高度
     */
    public CellContentDrawer(double x, double y, double width, double height) {
        this.canvas = new Canvas(x, y, width, height);
        this.canvas.setDrawer(this);
    }

    private static class TextLine {
        /**
         * 文本内容
         */
        public String text;
        /**
         * 文本宽度
         */
        public double width;

        public TextLine(String text, double width) {
            this.text = text;
            this.width = width;
            if (DEBUG) {
                System.out.println(">> text:" + text + " width:" + width);
            }
        }
    }


    /**
     * 单元格内部绘制
     *
     * @param ctx 绘制上下文
     * @throws IOException 图形绘制异常
     */
    @Override
    public void draw(DrawContext ctx) throws IOException {
        if (this.value == null || this.value.isEmpty()) {
            return;
        }

        // 设置字体样式
        String fontStr = "";
        if (italic) {
            fontStr += "italic ";
        }
        if (blob) {
            fontStr += "bold ";
        }
        ctx.font = fontStr + STBase.fmt(fontSize) + "mm " + fontName;
        if (this.letterSpacing != 0) {
            // 设置字间距
            ctx.getFont().setLetterSpacing(this.letterSpacing);
        }

        double width = canvas.getWidth();
        double height = canvas.getHeight();
        TextMetrics textMetrics = ctx.measureText(this.value);


        LinkedList<TextLine> lines = new LinkedList<>();
        double textLineWidth = 0;
        int offset = 0;
        // 分段
        for (int i = 0; i < this.value.length(); i++) {
            char c = this.value.charAt(i);
            // 换行符提前结束
            if (c == '\n') {
                lines.add(new TextLine(this.value.substring(offset, i), textLineWidth));
                offset = i + 1;
                textLineWidth = 0;
                continue;
            }
            double cWidth = 0;
            if (i == this.value.length() - 1) {
                cWidth = ctx.measureText(String.valueOf(c)).width;
            } else {
                cWidth = textMetrics.offset[i];
            }
            if (textLineWidth + cWidth > width) {
                // 超出宽度，换行
                lines.add(new TextLine(this.value.substring(offset, i), textLineWidth));
                offset = i;
                textLineWidth = cWidth;
            } else {
                textLineWidth += cWidth;
            }
        }
        // 最后一行
        if (offset < this.value.length()) {
            lines.add(new TextLine(this.value.substring(offset), textLineWidth));
        }


        // 内容高度：(字号 + 行间距) *行数
        double contentHeight = (this.fontSize + this.lineSpace) * lines.size();

        double offsetY = 0;
        // 第一行 Y 偏移量计算：字体坐标位于字体的左下角
        if (this.verticalAlign == VerticalAlign.top) {
            offsetY = fontSize;
        } else if (this.verticalAlign == VerticalAlign.center) {
            offsetY = height / 2 - contentHeight / 2 + fontSize;
        } else {
            // 底部对齐 偏移量为：总高度 - 内容高度 + 字号
            offsetY = height - contentHeight + fontSize;
        }

        for (TextLine line : lines) {
            double offsetX = 0;
            // 按照左右浮动方式 依次计算出每行的X 偏移量
            if (this.textAlign == TextAlign.left || this.textAlign == TextAlign.start || this.textAlign == null) {
                // 左浮动
                offsetX = 0;
            } else if (this.textAlign == TextAlign.center) {
                // 居中
                offsetX = (width - line.width) / 2d;
            } else {
                // 右浮动
                offsetX = width - line.width;
            }
            if (!"".equals(line.text)) {
                ctx.fillText(line.text, offsetX, offsetY);
            }

            if (DEBUG) {
                ctx.strokeRect(offsetX, offsetY - fontSize, line.width, fontSize + lineSpace);
                ctx.stroke();
            }
            // 绘制完上一行后将offsetY移动到下一行
            offsetY += this.fontSize + this.lineSpace;
        }

        if (DEBUG) {
            ctx.save();
            double lineWidth = 0.353d;
            ctx.setLineWidth(lineWidth);
            ctx.strokeStyle = "rgb(255,0,0)";
            ctx.moveTo(0, 0);
            ctx.lineTo(width, height);
            ctx.moveTo(width, 0);
            ctx.lineTo(0, height);
            ctx.stroke();
            ctx.restore();
        }
    }

    /**
     * 获取单元格文字内容
     *
     * @return 单元格文字内容
     */
    public String getValue() {
        return value;
    }

    /**
     * 设置单元格文字内容
     *
     * @param value 单元格文字内容
     * @return this
     */
    public CellContentDrawer setValue(String value) {
        this.value = value;
        return this;
    }

    /**
     * 获取单元格颜色
     *
     * @return 单元格颜色，格式：#000000、rgb(0,0,0)、rgba(0,0,0,1)
     */
    public String getColor() {
        return color;
    }

    /**
     * 设置单元格颜色
     *
     * @param color 单元格颜色，格式：#000000、rgb(0,0,0)、rgba(0,0,0,1)
     * @return this
     */
    public CellContentDrawer setColor(String color) {
        if (color == null || color.isEmpty()) {
            throw new IllegalArgumentException("颜色(color)不能为空");
        }
        this.color = color;
        return this;
    }

    /**
     * 获取字体名称
     *
     * @return 字体名称
     */
    public String getFontName() {
        return fontName;
    }

    /**
     * 设置字体名称
     *
     * @param fontName 字体名称，仅支持系统安装字体，且不会嵌入到OFD中。
     * @return this
     */
    public CellContentDrawer setFontName(String fontName) {
        this.fontName = fontName;
        return this;
    }

    /**
     * 获取字号
     *
     * @return 字号，默认：0.353 （单位：毫米）
     */
    public double getFontSize() {
        return fontSize;
    }

    /**
     * 设置字号
     *
     * @param fontSize 字号，默认：3（单位：毫米）
     * @return this
     */
    public CellContentDrawer setFontSize(double fontSize) {
        if (fontSize <= 0) {
            throw new IllegalArgumentException("字号(fontSize)必须大于0");
        }
        this.fontSize = fontSize;
        return this;
    }

    /**
     * 获取单元格的Canvas对象
     *
     * @return 单元格的Canvas对象
     */
    public Canvas getCanvas() {
        return canvas;
    }

    /**
     * 获取 文字对齐方式
     *
     * @return 文字对齐方式，默认：左对齐
     */
    public TextAlign getTextAlign() {
        return textAlign;
    }

    /**
     * 设置 文字对齐方式
     *
     * @param textAlign 文字对齐方式
     * @return this
     */
    public CellContentDrawer setTextAlign(TextAlign textAlign) {
        this.textAlign = textAlign;
        return this;
    }

    /**
     * 获取 文字垂直方向浮动方式
     *
     * @return 文字垂直方向浮动方式，默认：居中 {@link VerticalAlign#center}
     */
    public VerticalAlign getVerticalAlign() {
        return verticalAlign;
    }

    /**
     * 设置 文字垂直方向浮动方式
     *
     * @param verticalAlign 文字垂直方向浮动方式
     * @return this
     */
    public CellContentDrawer setVerticalAlign(VerticalAlign verticalAlign) {
        this.verticalAlign = verticalAlign;
        return this;
    }

    /**
     * 获取 行间距
     *
     * @return 行间距，默认值 0.6mm
     */
    public Double getLineSpace() {
        return lineSpace;
    }

    /**
     * 设置 行间距
     *
     * @param lineSpace 行间距
     * @return this
     */
    public CellContentDrawer setLineSpace(Double lineSpace) {
        this.lineSpace = lineSpace;
        return this;
    }

    /**
     * 是否加粗
     *
     * @return 是否加粗，默认：不加粗
     */
    public Boolean getBlob() {
        return blob;
    }

    /**
     * 设置 是否加粗
     *
     * @param blob 是否加粗
     * @return this
     */
    public CellContentDrawer setBlob(Boolean blob) {
        this.blob = blob;
        return this;
    }

    /**
     * 是否斜体
     *
     * @return true - 斜体、false - 正常
     */
    public Boolean getItalic() {
        return italic;
    }

    /**
     * 设置 是否斜体
     *
     * @param italic 是否斜体，true - 斜体、false - 正常
     * @return this
     */
    public CellContentDrawer setItalic(Boolean italic) {
        this.italic = italic;
        return this;
    }

    /**
     * 获取 文字之间的间距
     *
     * @return 文字之间的间距，默认为：0
     */
    public Double getLetterSpacing() {
        if (letterSpacing < 0) {
            return 0d;
        }
        return letterSpacing;
    }

    /**
     * 设置 文字之间的间距
     *
     * @param letterSpacing 文字之间的间距，可以为负数，默认为：0。
     * @return this
     */
    public CellContentDrawer setLetterSpacing(Double letterSpacing) {
        this.letterSpacing = letterSpacing;
        return this;
    }
}
