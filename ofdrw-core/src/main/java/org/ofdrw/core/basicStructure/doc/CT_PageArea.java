package org.ofdrw.core.basicStructure.doc;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.ST_Box;

/**
 * 页面区域结构
 * <p>
 * ————《GB/T 33190-2016》 图 7
 *
 * @author 权观宇
 * @since 2019-10-04 08:58:55
 */
public class CT_PageArea extends OFDElement {
    public CT_PageArea(Element proxy) {
        super(proxy);
    }

    public CT_PageArea() {
        super("PageArea");
    }

    /**
     * 页面物理区域 创建区域
     *
     * @param topLeftX 页面物理区域 左上角X坐标
     * @param topLeftY 页面物理区域 左上角Y坐标
     * @param width    页面物理区域 宽度
     * @param height   页面物理区域 高度
     */
    public CT_PageArea(double topLeftX,
                       double topLeftY,
                       double width,
                       double height) {
        this();
        this.setPhysicalBox(topLeftX, topLeftY, width, height);
    }

    /**
     * 【必选】
     * 设置 页面物理区域
     * <p>
     * 左上角为页面坐标系的原点
     *
     * @param physicalBox 页面物理区域
     * @return this
     */
    public CT_PageArea setPhysicalBox(ST_Box physicalBox) {
        this.setOFDEntity("PhysicalBox", physicalBox.toString());
        return this;
    }

    /**
     * 【必选】
     * 设置 页面物理区域
     * <p>
     * 左上角为页面坐标系的原点
     *
     * @param topLeftX 左上角X坐标
     * @param topLeftY 左上角Y坐标
     * @param width    宽度
     * @param height   高度
     * @return this
     */
    public CT_PageArea setPhysicalBox(double topLeftX,
                                      double topLeftY,
                                      double width,
                                      double height) {
        ST_Box physicalBox = new ST_Box(topLeftX, topLeftY, width, height);
        return setPhysicalBox(physicalBox);
    }


    /**
     * 【必选】
     * 获取 页面物理区域 左上角为页面坐标系的原点
     *
     * @return 页面物理区域
     */
    public ST_Box getPhysicalBox() {
        return ST_Box.getInstance(this.getOFDElementText("PhysicalBox"));
    }

    /**
     * 【可选】
     * 设置 显示区域
     * <p>
     * 页面内容实际显示或打印输出的区域，位于页面物理区域内，
     * 包含页眉、页脚、页心等内容
     * <p>
     * [例外处理] 如果显示区域不完全位于页面物理区域内，
     * 页面物理区域外的部分则被忽略。如果显示区域完全位于页面物理区域外，
     * 则设置该页为空白页。
     *
     * @param applicationBox 显示区域
     * @return this
     */
    public CT_PageArea setApplicationBox(ST_Box applicationBox) {
        this.setOFDEntity("ApplicationBox", applicationBox.toString());
        return this;
    }

    /**
     * 【可选】
     * 设置 显示区域
     * <p>
     * 页面内容实际显示或打印输出的区域，位于页面物理区域内，
     * 包含页眉、页脚、页心等内容
     * <p>
     * [例外处理] 如果显示区域不完全位于页面物理区域内，
     * 页面物理区域外的部分则被忽略。如果显示区域完全位于页面物理区域外，
     * 则设置该页为空白页。
     *
     * @param topLeftX 左上角X坐标
     * @param topLeftY 左上角Y坐标
     * @param width    宽度
     * @param height   高度
     * @return this
     */
    public CT_PageArea setApplicationBox(double topLeftX,
                                         double topLeftY,
                                         double width,
                                         double height) {
        ST_Box applicationBox = new ST_Box(topLeftX, topLeftY, width, height);
        return setApplicationBox(applicationBox);
    }

    /**
     * 【可选】
     * 获取 显示区域
     * <p>
     * 页面内容实际显示或打印输出的区域，位于页面物理区域内，
     * 包含页眉、页脚、页心等内容
     * <p>
     * [例外处理] 如果显示区域不完全位于页面物理区域内，
     * 页面物理区域外的部分则被忽略。如果显示区域完全位于页面物理区域外，
     * 则设置该页为空白页。
     *
     * @return 显示区域
     */
    public ST_Box getApplicationBox() {
        return ST_Box.getInstance(this.getOFDElementText("ApplicationBox"));
    }

    /**
     * 【可选】
     * 设置 版心区域
     * <p>
     * 文件的正文区域，位于显示区域内。
     * 左上角坐标决定了其在显示区域内的位置
     * <p>
     * [例外处理] 如果版心区域不完全位于页面物理区域内，
     * 页面物理区域外的部分则被忽略。如果版心区域完全位于页面物理区域外，
     * 则设置该页为空白页。
     *
     * @param contentBox 版心区域
     * @return this
     */
    public CT_PageArea setContentBox(ST_Box contentBox) {
        this.setOFDEntity("ContentBox", contentBox);
        return this;
    }

    /**
     * 【可选】
     * 设置 版心区域
     * <p>
     * 文件的正文区域，位于显示区域内。
     * 左上角坐标决定了其在显示区域内的位置
     * <p>
     * [例外处理] 如果版心区域不完全位于页面物理区域内，
     * 页面物理区域外的部分则被忽略。如果版心区域完全位于页面物理区域外，
     * 则设置该页为空白页。
     *
     * @param topLeftX 左上角X坐标
     * @param topLeftY 左上角Y坐标
     * @param width    宽度
     * @param height   高度
     * @return this
     */
    public CT_PageArea setContentBox(double topLeftX,
                                     double topLeftY,
                                     double width,
                                     double height) {
        ST_Box contentBox = new ST_Box(topLeftX, topLeftY, width, height);
        return setContentBox(contentBox);
    }

    /**
     * 【可选】
     * 获取 版心区域
     * <p>
     * 文件的正文区域，位于显示区域内。
     * 左上角坐标决定了其在显示区域内的位置
     * <p>
     * [例外处理] 如果版心区域不完全位于页面物理区域内，
     * 页面物理区域外的部分则被忽略。如果版心区域完全位于页面物理区域外，
     * 则设置该页为空白页。
     *
     * @return 版心区域
     */
    public ST_Box getContentBox() {
        return ST_Box.getInstance(this.getOFDElementText("ContentBox"));
    }

    /**
     * 【可选】
     * 设置 出血区域
     * <p>
     * 超出设备性能限制的额外出血区域，位于页面物理区域外。
     * 不出现时，默认值为页面物理区域
     * <p>
     * [例外处理] 如果出血区域不完全位于页面物理区域内，
     * 页面物理区域外的部分则被忽略。如果显示出血完全位于页面物理区域外，
     * 则设置该页为空白页。
     *
     * @param bleedBox 出血区域
     * @return this
     */
    public CT_PageArea setBleedBox(ST_Box bleedBox) {
        this.setOFDEntity("BleedBox", bleedBox);
        return this;
    }

    /**
     * 【可选】
     * 设置 出血区域
     * <p>
     * 超出设备性能限制的额外出血区域，位于页面物理区域外。
     * 不出现时，默认值为页面物理区域
     * <p>
     * [例外处理] 如果出血区域不完全位于页面物理区域内，
     * 页面物理区域外的部分则被忽略。如果显示出血完全位于页面物理区域外，
     * 则设置该页为空白页。
     *
     * @param topLeftX 左上角X坐标
     * @param topLeftY 左上角Y坐标
     * @param width    宽度
     * @param height   高度
     * @return this
     */
    public CT_PageArea setBleedBox(double topLeftX,
                                   double topLeftY,
                                   double width,
                                   double height) {
        ST_Box bleedBox = new ST_Box(topLeftX, topLeftY, width, height);
        return setBleedBox(bleedBox);
    }

    /**
     * 【可选】
     * 获取 出血区域
     * <p>
     * 超出设备性能限制的额外出血区域，位于页面物理区域外。
     * 不出现时，默认值为页面物理区域
     * <p>
     * [例外处理] 如果出血区域不完全位于页面物理区域内，
     * 页面物理区域外的部分则被忽略。如果显示出血完全位于页面物理区域外，
     * 则设置该页为空白页。
     *
     * @return 出血区域
     */
    public ST_Box getBleedBox() {
        return ST_Box.getInstance(this.getOFDElementText("BleedBox"));
    }


    /**
     * 尝试获取页面最大区域
     * <p>
     * PhysicalBox >  ApplicationBox > ContentBox
     *
     * @return 区域
     */
    public ST_Box getBox() {
        ST_Box res = this.getPhysicalBox();
        if (res == null) {
            res = this.getApplicationBox();
        }
        if (res == null) {
            res = this.getContentBox();
        }
        return res;
    }
}
