package org.ofdrw.converter.pdf.render;

import org.ofdrw.converter.pdf.model.ContentCommand;

import java.util.List;

/**
 * 渲染管线处理器：把 OFD 页面中的某类内容转换为中间模型指令
 * <p>
 * 设计 4.1 节把管线拆分为 {@link PageRenderer} 与若干专用渲染器。所有渲染器的输入都是 OFD 解析结果，
 * 输出都是 {@link ContentCommand}，彼此只通过 {@link RenderContext} 共享上下文，便于独立单测与并行开发。
 * <p>
 * 设计约束 3：单图元异常与未实现特性必须“跳过 + warning 日志”，不得中断整页；
 * 因此实现类的异常处理由具体渲染器负责，本接口不声明受检异常。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public interface ContentRenderer {

    /**
     * 处理器名称，用于 warning 日志与调试输出
     *
     * @return 名称
     */
    String getName();

    /**
     * 把输入对象渲染为中间模型指令
     *
     * @param input   待渲染的 OFD 对象
     * @param context 渲染上下文
     * @return 指令序列，不允许返回 null；无输出时返回空列表
     */
    List<ContentCommand> render(Object input, RenderContext context);
}
