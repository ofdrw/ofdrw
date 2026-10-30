package org.ofdrw.converter.pdf.render;

/**
 * 文字渲染器：OFD TextObject → 中间模型文字指令
 * <p>
 * 职责（设计 3.6 节、决策 3）：
 * <ul>
 *     <li>按 X/Y 与 DeltaX/DeltaY 双数组逐字定位，支持带末尾增量（表 46）；</li>
 *     <li>处理 CharDirection × ReadDirection 组合（表 47），必要时生成 WMode=1 的 CMap；</li>
 *     <li>按 CGTransform 完成一对一/多对一/一对多/多对多字形映射；</li>
 *     <li>字形优先 + ToUnicode：输出 {@link org.ofdrw.converter.pdf.model.GlyphRun} 序列，保证视觉一致且文本可提取。</li>
 * </ul>
 * 本接口在 P0 阶段仅作为契约占位，实现归 P2 阶段。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public interface TextRenderer extends ContentRenderer {
}
