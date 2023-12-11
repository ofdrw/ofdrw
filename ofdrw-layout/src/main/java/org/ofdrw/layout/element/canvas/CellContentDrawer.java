package org.ofdrw.layout.element.canvas;

import org.ofdrw.core.basicType.STBase;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
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
    private Boolean bold = false;

    /**
     * 是否斜体
     */
    private Boolean italic = false;

    /**
     * 文字之间的间距
     */
    private Double letterSpacing = 0d;

    /**
     * 图片
     */
    private Img img = null;

    /**
     * 是否有下划线
     */
    private boolean underline = false;

    /**
     * 是否删除线
     */
    private boolean deleteLine = false;


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

    /**
     * 文字行
     */
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
     * 图片
     */
    private static class Img {
        /**
         * 图片路径
         */
        public Path path;
        /**
         * 图片宽度
         */
        public double width;

        /**
         * 图片高度
         */
        public double height;

        public Img(Path path, double width, double height) {
            this.path = path;
            this.width = width;
            this.height = height;
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
        if (this.img != null) {
            // 绘制图片
            drawImg(ctx);
        } else {
            // 绘制文字
            drawText(ctx);
        }
    }

    /**
     * 单元格图片绘制
     *
     * @param ctx 绘制上下文
     * @throws IOException 图片绘制异常
     */
    private void drawImg(DrawContext ctx) throws IOException {
        if (this.img == null) {
            return;
        }
        if (this.img.width <= 0 || this.img.height <= 0) {
            // 若未对图片进行宽高设置，则从图片中获取宽高
            BufferedImage gImg = ImageIO.read(img.path.toFile());
            this.img.width = ctx.mm(gImg.getWidth());
            this.img.height = ctx.mm(gImg.getHeight());
            if (DEBUG) {
                System.out.printf(">> 从图片中获取宽高 img.width:%.2f img.height:%.2f\n", img.width, img.height);
            }
        }

        double x = 0;
        switch (this.textAlign) {

            case right:
            case end:
                // 右浮动
                x = canvas.getWidth() - img.width;
                break;
            case center:
                // 居中
                x = (canvas.getWidth() - img.width) / 2d;
                break;
            case start:
            case left:
            default:
                // 左浮动
                x = 0d;
                break;
        }
        double y = 0;
        switch (this.verticalAlign) {
            case bottom:
                y = canvas.getHeight() - img.height;
                break;
            case center:
                y = (canvas.getHeight() - img.height) / 2d;
                break;
            case top:
            default:
                y = 0d;
                break;
        }
        ctx.drawImage(img.path, x, y, img.width, img.height);

        if (DEBUG) {
            debugBorder(ctx);
        }
    }

    /**
     * 单元格文字内容绘制
     *
     * @param ctx 绘制上下文
     * @throws IOException 文字绘制异常
     */
    private void drawText(DrawContext ctx) throws IOException {
        if (this.value == null || this.value.isEmpty()) {
            return;
        }

        // 设置字体样式
        String fontStr = "";
        if (italic) {
            fontStr += "italic ";
        }
        if (bold) {
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
            // 文字装饰线条宽度
            // 比例系数由经验指定无特定规则
            double fontLineWidth = fontSize / 30;
            double underlineOffset = fontLineWidth * 3.2;
            // 下划线
            if (this.underline) {
                ctx.save();
                ctx.setLineWidth(fontLineWidth);
                ctx.beginPath();
                ctx.moveTo(offsetX, offsetY + underlineOffset);
                ctx.lineTo(offsetX + line.width, offsetY + underlineOffset);
                ctx.stroke();
                ctx.restore();
            }
            // 删除线
            if (this.deleteLine) {
                ctx.save();
                ctx.setLineWidth(fontLineWidth);
                ctx.beginPath();
                // 由于文字定位在基线位置，此处以 5/18 比例计算出删除线位置，5/18为经验值，无特殊意义。
                ctx.moveTo(offsetX, offsetY - fontSize * 5/18 );
                ctx.lineTo(offsetX + line.width, offsetY - fontSize * 5/18);
                ctx.stroke();
                ctx.restore();
            }

            if (DEBUG) {
                ctx.save();
                ctx.setLineDash(1.5d, 1.5d);
                ctx.setLineWidth(0.1);
                ctx.setGlobalAlpha(0.53);
                ctx.strokeStyle = "rgb(255,0,0)";
                ctx.strokeRect(offsetX, offsetY - fontSize, line.width, fontSize + lineSpace);
                ctx.stroke();
                ctx.restore();
            }
            // 绘制完上一行后将offsetY移动到下一行
            offsetY += this.fontSize + this.lineSpace;
        }

        if (DEBUG) {
            debugBorder(ctx);
        }
    }

    /**
     * 绘制辅助线
     *
     * @param ctx 绘制上下文
     */
    private void debugBorder(DrawContext ctx) {

        double width = canvas.getWidth();
        double height = canvas.getHeight();

        ctx.save();
        double lineWidth = 0.353d;
        ctx.setLineDash(1.5d, 1.5d);
        ctx.setLineWidth(lineWidth);
        ctx.setGlobalAlpha(0.53);
        ctx.strokeStyle = "rgb(255,0,0)";
        ctx.moveTo(0, 0);
        ctx.lineTo(width, height);
        ctx.moveTo(width, 0);
        ctx.lineTo(0, height);
        ctx.rect(0, 0, width, height);
        ctx.stroke();

        ctx.restore();
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
     * 设置图片
     *
     * @param imgPath 图片路径，仅支持png、jpg、jpeg、gif、bmp格式
     * @param w       图片宽度，单位：毫米
     * @param h       图片高度，单位：毫米
     * @return this
     */
    public CellContentDrawer setValue(Path imgPath, double w, double h) {
        this.img = new Img(imgPath, w, h);
        return this;
    }

    /**
     * 设置图片
     * <p>
     * 图片宽度与高度通过 {@link DrawContext#mm(int)} } 方法转换为毫米
     *
     * @param imgPath 图片路径，仅支持png、jpg、jpeg、gif、bmp格式
     * @return this
     * @throws IOException 图片加载异常
     */
    public CellContentDrawer setValue(Path imgPath) throws IOException {
        this.img = new Img(imgPath, 0, 0);
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
     * @return 是否加粗，默认：不加粗
     * @deprecated 单词错误 {@link #getBold()}
     * 是否加粗
     */
    @Deprecated
    public Boolean getBlob() {
        return bold;
    }

    /**
     * @param bolb 是否加粗
     * @return this
     * @deprecated 单词错误 {@link #setBold(Boolean)}
     * <p>
     * 设置 是否加粗
     */
    @Deprecated
    public CellContentDrawer setBlob(Boolean bolb) {
        this.bold = bolb;
        return this;
    }

    /**
     * 是否加粗
     *
     * @return 是否加粗，默认：不加粗
     */
    public Boolean getBold() {
        return bold;
    }

    /**
     * 设置 是否加粗
     *
     * @param bold 是否加粗
     * @return this
     */
    public CellContentDrawer setBold(Boolean bold) {
        this.bold = bold;
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

    /**
     * 获取图片路径
     *
     * @return 图片路径，可能为空。
     */
    public Path getImgPath() {
        if (img == null) {
            return null;
        }
        return img.path;
    }

    /**
     * 获取图片高度
     *
     * @return 图片高度，可能为0。
     */
    public double getImgWidth() {
        if (img == null) {
            return 0;
        }
        return img.width;
    }

    /**
     * 获取图片宽度
     *
     * @return 图片宽度，可能为0。
     */
    public double getImgHeight() {
        if (img == null) {
            return 0;
        }
        return img.height;
    }

    /**
     * 设置是否开启下划线
     *
     * @param underline true - 启下划线，false - 禁用下划线
     * @return this
     */
    public CellContentDrawer setUnderline(boolean underline) {
        this.underline = underline;
        return this;
    }

    /**
     * 获取是否开启下划线
     *
     * @return true - 启下划线，false - 不启用下划线
     */
    public boolean getUnderline() {
        return this.underline;
    }

    /**
     * 设置是否开启删除线
     *
     * @param deleteLine true - 启删除线，false - 禁用删除线
     * @return this
     */
    public CellContentDrawer setDeleteLine(boolean deleteLine) {
        this.deleteLine = deleteLine;
        return this;
    }

    /**
     * 获取是否开启删除线
     *
     * @return true - 启删除线，false - 禁用删除线
     */
    public boolean getDeleteLine() {
        return this.deleteLine;
    }
}
