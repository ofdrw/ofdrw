package org.ofdrw.converter.pdf.model;

/**
 * 中间模型内容指令：渲染器无关的 PDF 内容流抽象指令
 * <p>
 * 每一种指令对应 PDF 内容流中的一组操作符。指令本身只承载数据，不持有任何 PDFBox 或其它渲染库类型，
 * 因此可以对同一份渲染结果做“渲染器无关”的单元测试（设计 4.1 节）。
 * <p>
 * 指令坐标默认处于 {@link CoordSpace#PAGE}：已按设计 3.1 节逐点换算为 PDF 页面坐标（单位 pt，Y 轴向上），
 * 页面 CTM 保持恒等。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public interface ContentCommand {

    /**
     * 获取指令使用的坐标作用域
     *
     * @return 坐标作用域
     */
    CoordSpace getCoordSpace();
}
