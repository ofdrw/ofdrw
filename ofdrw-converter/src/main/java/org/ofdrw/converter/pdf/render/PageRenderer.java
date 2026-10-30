package org.ofdrw.converter.pdf.render;

/**
 * 页面级渲染器：负责页框、模板页、图层 ZOrder 与图元调度
 * <p>
 * 设计 4.1 与 4.2 节：{@code PageRenderer} 是渲染管线的入口，读取一页 OFD 数据，按下列顺序生成
 * {@link org.ofdrw.converter.pdf.model.PdfPageContent}：
 * <ol>
 *     <li>按决策 26 用 PhysicalBox 生成页面框（MediaBox = CropBox）；</li>
 *     <li>按 ZOrder 渲染模板页（Background → 页面内容 → Foreground）；</li>
 *     <li>按图层 ZOrder 渲染各个 {@code CT_Layer}；</li>
 *     <li>把图元分派给 {@link PathRenderer}、{@link TextRenderer}、{@link ImageRenderer}、
 *     {@link ShadingRenderer}、{@link PatternRenderer}，并应用 {@link ClipApplier}。</li>
 * </ol>
 * 实现见 {@link PageRendererImpl}（{@code @since 2.5.1}）。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public interface PageRenderer {

    /**
     * 获取渲染器名称
     *
     * @return 名称
     */
    default String getName() {
        return "PageRenderer";
    }
}
