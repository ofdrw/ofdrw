package org.ofdrw.layout.element.canvas;


import java.io.IOException;
import java.nio.file.Path;

/**
 * 单元对象
 * <p>
 * 绘制行为详见渲染器：{@link org.ofdrw.layout.element.canvas.CellContentDrawer}
 *
 * @author 权观宇
 * @since 2023-11-21 19:22:28
 */
public class Cell extends Canvas {

    /**
     * 单元格内容绘制器
     */
    private final CellContentDrawer cellDrawer;

    /**
     * 创建单元对象
     *
     * @param width  宽度（单位：毫米mm）
     * @param height 高度（单位：毫米mm）
     */
    public Cell(Double width, Double height) {
        super(width, height);
        this.cellDrawer = new CellContentDrawer(this);
        this.setDrawer(this.cellDrawer);
    }

    /**
     * 创建单元对象
     *
     * @param x x坐标（单位：毫米mm）
     * @param y y坐标（单位：毫米mm）
     * @param w 宽度（单位：毫米mm）
     * @param h 高度（单位：毫米mm）
     */
    public Cell(double x, double y, double w, double h) {
        super(x, y, w, h);
        this.cellDrawer = new CellContentDrawer(this);
        this.setDrawer(this.cellDrawer);
    }

    /**
     * 设置单元格内容绘制器
     *
     * @param drawer 新的绘制器
     * @return this
     */
    @Override
    public Cell setDrawer(Drawer drawer) {
        if (!(drawer instanceof CellContentDrawer)) {
            throw new IllegalArgumentException("Cell的绘制器必须是CellContentDrawer");
        }
        super.setDrawer(drawer);
        return this;
    }

    /**
     * 获取单元格内容绘制器
     *
     * @return 单元格内容绘制器
     */
    @Override
    public CellContentDrawer getDrawer() {
        return cellDrawer;
    }


    /**
     * 获取单元格文字内容
     *
     * @return 单元格文字内容
     */
    public String getValue() {
        return cellDrawer.getValue();
    }

    /**
     * 设置单元格文字内容
     *
     * @param value 单元格文字内容
     * @return this
     */
    public Cell setValue(String value) {
        this.cellDrawer.setValue(value);
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
    public Cell setValue(Path imgPath, double w, double h) {
        this.cellDrawer.setValue(imgPath, w, h);
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
    public Cell setValue(Path imgPath) throws IOException {
        this.cellDrawer.setValue(imgPath);
        return this;
    }

    /**
     * 获取单元格颜色
     *
     * @return 单元格颜色，格式：#000000、rgb(0,0,0)、rgba(0,0,0,1)
     */
    public String getColor() {
        return cellDrawer.getColor();
    }

    /**
     * 设置单元格颜色
     *
     * @param color 单元格颜色，格式：#000000、rgb(0,0,0)、rgba(0,0,0,1)
     * @return this
     */
    public Cell setColor(String color) {
        if (color == null || color.isEmpty()) {
            throw new IllegalArgumentException("颜色(color)不能为空");
        }
        this.cellDrawer.setColor(color);
        return this;
    }

    /**
     * 获取字体名称
     *
     * @return 字体名称
     */
    public String getFontName() {
        return this.cellDrawer.getFontName();
    }

    /**
     * 设置字体名称
     *
     * @param fontName 字体名称，仅支持系统安装字体，且不会嵌入到OFD中。
     * @return this
     */
    public Cell setFontName(String fontName) {
        this.cellDrawer.setFontName(fontName);
        return this;
    }

    /**
     * 设置单元格使用的外部字体
     * <p>
     * 注意OFDRW不会提供任何字体裁剪功能，您的字体文件将直接加入OFD文件中，这可能造成文件体积剧增。
     *
     * @param fontName 字体名称，如“思源宋体”
     * @param fontPath 字体文件所在路径
     * @return this
     */
    public Cell setFont(String fontName, Path fontPath) {
        this.cellDrawer.setFont(fontName, fontPath);
        return this;
    }

    /**
     * 获取字号
     *
     * @return 字号，默认：0.353 （单位：毫米）
     */
    public double getFontSize() {
        return this.cellDrawer.getFontSize();
    }

    /**
     * 设置字号
     *
     * @param fontSize 字号，默认：3（单位：毫米）
     * @return this
     */
    public Cell setFontSize(double fontSize) {
        this.cellDrawer.setFontSize(fontSize);
        return this;
    }

    /**
     * 获取 文字对齐方式
     *
     * @return 文字对齐方式，默认：左对齐
     */
    public TextAlign getTextAlign() {
        return this.cellDrawer.getTextAlign();
    }

    /**
     * 设置 文字对齐方式
     *
     * @param textAlign 文字对齐方式
     * @return this
     */
    public Cell setTextAlign(TextAlign textAlign) {
        this.cellDrawer.setTextAlign(textAlign);
        return this;
    }

    /**
     * 获取 文字垂直方向浮动方式
     *
     * @return 文字垂直方向浮动方式，默认：居中 {@link VerticalAlign#center}
     */
    public VerticalAlign getVerticalAlign() {
        return this.cellDrawer.getVerticalAlign();
    }

    /**
     * 设置 文字垂直方向浮动方式
     *
     * @param verticalAlign 文字垂直方向浮动方式
     * @return this
     */
    public Cell setVerticalAlign(VerticalAlign verticalAlign) {
        this.cellDrawer.setVerticalAlign(verticalAlign);
        return this;
    }

    /**
     * 获取 行间距
     *
     * @return 行间距，默认值 0.6mm
     */
    public Double getLineSpace() {
        return this.cellDrawer.getLineSpace();
    }

    /**
     * 设置 行间距
     *
     * @param lineSpace 行间距
     * @return this
     */
    public Cell setLineSpace(Double lineSpace) {
        this.cellDrawer.setLineSpace(lineSpace);
        return this;
    }

    /**
     * @return 是否加粗，默认：不加粗
     * @deprecated 单词错误 {@link #getBold()}
     * 是否加粗
     */
    @Deprecated
    public Boolean getBlob() {
        return this.cellDrawer.getBlob();
    }

    /**
     * @param blob 是否加粗
     * @return this
     * @deprecated 单词错误 {@link #getBold()}
     * 设置 是否加粗
     */
    @Deprecated
    public Cell setBlob(Boolean blob) {
        this.cellDrawer.setBlob(blob);
        return this;
    }

    /**
     * 是否加粗
     *
     * @return 是否加粗，默认：不加粗
     */
    public Boolean getBold() {
        return this.cellDrawer.getBold();
    }

    /**
     * 设置 是否加粗
     *
     * @param blob 是否加粗
     * @return this
     */
    public Cell setBold(Boolean blob) {
        this.cellDrawer.setBold(blob);
        return this;
    }


    /**
     * 是否斜体
     *
     * @return true - 斜体、false - 正常
     */
    public Boolean getItalic() {
        return this.cellDrawer.getItalic();
    }

    /**
     * 设置 是否斜体
     *
     * @param italic 是否斜体，true - 斜体、false - 正常
     * @return this
     */
    public Cell setItalic(Boolean italic) {
        this.cellDrawer.setItalic(italic);
        return this;
    }

    /**
     * 获取 文字之间的间距
     *
     * @return 文字之间的间距，默认为：0
     */
    public Double getLetterSpacing() {
        return this.cellDrawer.getLetterSpacing();
    }

    /**
     * 设置 文字之间的间距
     *
     * @param letterSpacing 文字之间的间距，可以为负数，默认为：0。
     * @return this
     */
    public Cell setLetterSpacing(Double letterSpacing) {
        this.cellDrawer.setLetterSpacing(letterSpacing);
        return this;
    }

    /**
     * 设置是否开启下划线
     *
     * @param underline true - 启下划线，false - 禁用下划线
     * @return this
     */
    public Cell setUnderline(boolean underline) {
        this.cellDrawer.setUnderline(underline);
        return this;
    }

    /**
     * 获取是否开启下划线
     *
     * @return true - 启下划线，false - 不启用下划线
     */
    public boolean getUnderline() {
        return this.cellDrawer.getUnderline();
    }

    /**
     * 设置是否开启删除线
     *
     * @param deleteLine true - 启删除线，false - 禁用删除线
     * @return this
     */
    public Cell setDeleteLine(boolean deleteLine) {
        this.cellDrawer.setDeleteLine(deleteLine);
        return this;
    }

    /**
     * 获取是否开启删除线
     *
     * @return true - 启删除线，false - 禁用删除线
     */
    public boolean getDeleteLine() {
        return this.cellDrawer.getDeleteLine();
    }

    /**
     * 获取图片路径
     *
     * @return 图片路径，可能为空。
     */
    public Path getImgPath() {
        return this.cellDrawer.getImgPath();
    }

    /**
     * 获取图片高度
     *
     * @return 图片高度，可能为0。
     */
    public double getImgWidth() {
        return this.cellDrawer.getImgWidth();
    }

    /**
     * 获取图片宽度
     *
     * @return 图片宽度，可能为0。
     */
    public double getImgHeight() {
        return this.cellDrawer.getImgHeight();
    }

    /**
     * 获取字体宽度
     *
     * @return 字体宽度，遵循 CSS3标准，可选值为 normal | bold | bolder | lighter | 100 | 200 | 300 | 400 | 500 | 600 | 700 | 800 | 900
     */
    public String getFontWeight() {
        return this.cellDrawer.getFontWeight();
    }

    /**
     * 设置字体宽度
     * <p>
     *
     * @param fontWeight 宽度，应遵循 CSS3标准: normal | bold | bolder | lighter | 100 | 200 | 300 | 400 | 500 | 600 | 700 | 800 | 900
     */
    public Cell setFontWeight(String fontWeight) {
        this.cellDrawer.setFontWeight(fontWeight);
        return this;
    }
}
