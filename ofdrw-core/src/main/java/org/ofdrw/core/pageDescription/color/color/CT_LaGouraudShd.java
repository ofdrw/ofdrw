package org.ofdrw.core.pageDescription.color.color;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;

/**
 * 网格高洛德渐变
 * <p>
 * 网格高洛德渐变是高洛德渐变的一种特殊形式，
 * 允许定义 4 个以上的控制点，按照每行固定的网格数（VerticesPerRow）
 * 形成若干行列，相邻的 4 个控制点定义一个网格单元，在
 * 一个网格单元内 EdgeFlag 固定为 1，网格单元及多个单元组成的网格区域的规则如图42所示。
 * <p>
 * 8.3.4.5 图 43 表 32
 *
 * @author 权观宇
 * @since 2019-11-09 12:54:50
 */
public class CT_LaGouraudShd extends CT_GouraudShd {
    public CT_LaGouraudShd(Element proxy) {
        super(proxy);
    }

    public CT_LaGouraudShd() {
        super("LaGouraudShd");
    }

    /**
     * 【必选 属性】
     * 设置 渐变区域内每行的网格数
     *
     * @param verticesPerRow 渐变区域内每行的网格数
     * @return this
     */
    public CT_LaGouraudShd setVerticesPerRow(Integer verticesPerRow) {
        if (verticesPerRow == null) {
            throw new IllegalArgumentException("渐变区域内每行的网格数（VerticesPerRow）不能为空");
        }
        this.addAttribute("VerticesPerRow", verticesPerRow.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 渐变区域内每行的网格数
     *
     * @return 渐变区域内每行的网格数
     */
    public Integer getVerticesPerRow() {
        String str = this.attributeValue("VerticesPerRow");
        if (str == null || str.trim().length() == 0) {
            throw new IllegalArgumentException("渐变区域内每行的网格数（VerticesPerRow）不能为空");
        }
        return Integer.parseInt(str);
    }

    /**
     * 【必选】
     * 增加  渐变控制点
     * <p>
     * 至少出现四个
     *
     * @param point 渐变控制点，至少出现四个
     * @return this
     */
    @Override
    public CT_LaGouraudShd addPoint(Point point) {
        super.addPoint(point);
        return this;
    }
}
