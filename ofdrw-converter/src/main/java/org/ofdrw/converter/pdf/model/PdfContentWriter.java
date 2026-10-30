package org.ofdrw.converter.pdf.model;

/**
 * 指令序列导出为文本（渲染器无关的断言载体）
 * <p>
 * 用于单元测试与调试：把 {@link PdfPageContent} 的指令序列转换为稳定的伪操作符文本，
 * 每个 {@link ContentCommand} 输出一行，便于断言生成的绘制顺序与参数（例如 {@code Tm}、{@code TJ} 位移）。
 * 本类不使用任何渲染库，输出不承诺与真实 PDF 内容流逐字一致。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class PdfContentWriter {

    private PdfContentWriter() {
    }

    /**
     * 把页面内容导出为伪操作符文本
     *
     * @param content 页面内容，不能为空
     * @return 每行一条指令的文本，行分隔符为 {@code \n}
     */
    static String dump(PdfPageContent content) {
        if (content == null) {
            throw new IllegalArgumentException("页面内容不能为空");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("% page ").append(content.getWidthPt()).append('x').append(content.getHeightPt()).append("pt\n");
        for (ContentCommand command : content.getCommands()) {
            sb.append(DumpFormatter.format(command)).append('\n');
        }
        return sb.toString();
    }

    /**
     * 判断页面内容中是否存在指定类型的指令
     *
     * @param content 页面内容，不能为空
     * @param type    指令类型
     * @return true 表示存在
     */
    static boolean contains(PdfPageContent content, Class<? extends ContentCommand> type) {
        if (content == null || type == null) {
            return false;
        }
        for (ContentCommand command : content.getCommands()) {
            if (type.isInstance(command)) {
                return true;
            }
        }
        return false;
    }
}
