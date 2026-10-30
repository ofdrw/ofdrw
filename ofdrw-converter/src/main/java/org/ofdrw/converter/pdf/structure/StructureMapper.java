package org.ofdrw.converter.pdf.structure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 结构映射器：文件级结构信息 → PDF 结构（设计 3.8 节、P5 阶段）
 * <p>
 * 各类结构目标如下：
 * <table border="1">
 *     <tr><th>OFD</th><th>PDF</th><th>决策</th></tr>
 *     <tr><td>Outlines</td><td>/Outlines 书签树，仅标题与目标</td><td>25</td></tr>
 *     <tr><td>Attachments</td><td>/Names /EmbeddedFiles，保留名称、创建时间、MIME</td><td>—</td></tr>
 *     <tr><td>Annots</td><td>页面 /Annots，视觉外观重绘</td><td>—</td></tr>
 *     <tr><td>页面动作 PO/DO/CLICK</td><td>/OpenAction、链接注释；Sound/Movie 跳过并告警</td><td>18</td></tr>
 *     <tr><td>数字签名</td><td>仅转换外观，不保留签名有效性</td><td>—</td></tr>
 *     <tr><td>文档属性</td><td>/Info 与 XMP</td><td>—</td></tr>
 * </table>
 * <p>
 * <b>职责边界</b>：本包的映射器只做“OFD 对象 → 结构描述”，输入输出均为库无关类型
 * （{@link OutlineMapper.BookmarkNode}、{@link AnnotationMapper.AnnotationTarget} 等），
 * 由写入侧（{@code org.ofdrw.converter.pdf.write}）落到 PDFBox 对象上，
 * 因此结构映射可以在没有 PDF 文档的情况下做单元测试。
 * <p>
 * 结构信息在全部页面渲染完成后、文档关闭前统一写入（与 {@code PDFExporter#close()} 一致）。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public interface StructureMapper {

    /**
     * 获取映射器名称，用于 warning 日志
     *
     * @return 名称
     */
    String getName();


    /**
     * 结构映射结果
     * <p>
     * 设计约束 4（可观测）：无法映射的特性不抛异常，统一收集到 {@link #getWarnings()}，由调用方输出 warning 日志。
     *
     * @param <T> 条目类型
     */
    final class Result<T> {

        /**
         * 空的映射结果（接口静态方法不会被实现类继承，因此提供在枚举/结果类型上）
         *
         * @param <T> 条目类型
         * @return 既无条目也无告警的结果
         */
        public static <T> Result<T> empty() {
            return new Result<>(Collections.<T>emptyList(), Collections.<String>emptyList());
        }

        /**
         * 成功映射的条目
         */
        private final List<T> items;

        /**
         * 告警信息
         */
        private final List<String> warnings;

        /**
         * @param items    条目
         * @param warnings 告警
         */
        public Result(Collection<? extends T> items, Collection<String> warnings) {
            this.items = new ArrayList<>(items);
            this.warnings = new ArrayList<>(warnings);
        }

        /**
         * @return 成功映射的条目
         */
        public List<T> getItems() {
            return Collections.unmodifiableList(items);
        }

        /**
         * @return 告警信息
         */
        public List<String> getWarnings() {
            return Collections.unmodifiableList(warnings);
        }

        /**
         * @return 是否没有任何条目
         */
        public boolean isEmpty() {
            return items.isEmpty();
        }

        /**
         * 追加一条告警
         *
         * @param message 告警信息，空白字符串会被忽略
         * @return this，便于链式调用
         */
        public Result<T> warn(String message) {
            if (message != null && !message.trim().isEmpty()) {
                warnings.add(message);
            }
            return this;
        }
    }
}
