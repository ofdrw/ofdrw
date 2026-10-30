package org.ofdrw.converter.pdf.font;

import org.ofdrw.converter.pdf.render.RenderContext;

import java.util.List;

/**
 * 字体选择器：OFD 字体资源 → PDF 字体引用
 * <p>
 * 职责（设计 3.9 节、决策 15/16）：把 OFD 内嵌字体的整字体数据（不做子集裁剪）交给写入器嵌入，
 * 缺失或无法解析时按 FamilyName/Charset/Serif/Bold/Italic 匹配替代字体并输出 warning 日志。
 * <p>
 * 本接口在 P0 阶段仅作为契约占位，实现归 P2 阶段。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public interface FontSelector {

    /**
     * 为文字图元选择字体
     *
     * @param fontResources 文字图元引用的 OFD 字体列表，按 Font 属性顺序排列
     * @param context       渲染上下文
     * @return 选中的字体引用；无法确定时返回 null（调用方输出 warning 并跳过该文字图元）
     */
    PdfFontRef select(List<?> fontResources, RenderContext context);
}
