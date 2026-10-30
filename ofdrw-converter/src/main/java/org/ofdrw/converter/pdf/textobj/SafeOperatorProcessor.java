package org.ofdrw.converter.pdf.textobj;

import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.operator.OperatorProcessor;
import org.apache.pdfbox.cos.COSBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * 安全算子处理器基类：包住 PDFBox 原生算子，在不改变绘制语义的前提下切断文字 run
 * <p>
 * 用于两类无法在 OFD 中精确表达的状态：扩展图形状态中的软掩码（SMask）与非 Normal 混合模式（BM）。
 * <p>
 * 处理流程：进入前收起当前文字 run → 若本次参数涉及软掩码/混合模式则暂停文字对象化 →
 * 调用原生算子（其内部绘制的透明组、图案等内容因此全部走路径）→ 退出时恢复为进入前的暂停状态。
 * 暂停状态按“进入前的值”恢复，连续或嵌套调用都不会泄漏。
 *
 * @author 权观宇
 * @since 2.5.1
 */
abstract class SafeOperatorProcessor extends OperatorProcessor {

    private static final Logger logger = LoggerFactory.getLogger(SafeOperatorProcessor.class);

    /**
     * 算子名称
     */
    private final String name;

    /**
     * @param engine 所属页面绘制器
     * @param name   算子名称
     */
    protected SafeOperatorProcessor(PDFStreamEngine engine, String name) {
        super(engine);
        this.name = name;
    }

    /**
     * 获取页面绘制器
     *
     * @return 页面绘制器；上下文不是 OfdPageDrawer 时返回 null
     */
    protected OfdPageDrawer drawer() {
        return getContext() instanceof OfdPageDrawer ? (OfdPageDrawer) getContext() : null;
    }

    /**
     * 执行包装：收起 run、按需暂停、委托原生算子、恢复暂停状态
     *
     * @param operator  算子
     * @param arguments 参数
     * @param safe      本次参数是否不含软掩码/混合模式
     * @param delegate  原生算子处理器，为 null 时只做暂停状态切换
     * @throws IOException 解析异常
     */
    protected void runSafely(Operator operator, List<COSBase> arguments,
                             boolean safe, OperatorProcessor delegate) throws IOException {
        OfdPageDrawer drawer = drawer();
        if (delegate == null) {
            logger.debug("缺少原生算子 {}，跳过", name);
            return;
        }
        boolean previous = false;
        if (drawer != null) {
            drawer.flushRun();
            previous = drawer.isTextSuspended();
            if (!safe) {
                drawer.setTextSuspended(true);
            }
        }
        try {
            delegate.process(operator, arguments);
        } finally {
            if (drawer != null) {
                drawer.setTextSuspended(previous);
            }
        }
    }

    @Override
    public String getName() {
        return name;
    }
}
