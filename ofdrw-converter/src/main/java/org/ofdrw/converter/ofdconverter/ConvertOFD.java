package org.ofdrw.converter.ofdconverter;

import org.ofdrw.converter.GeneralConvertException;

import java.io.Closeable;
import java.nio.file.Path;

/**
 * 其他格式转换为OFD文档
 *
 * @author 权观宇
 * @since 2023-3-14 22:59:16
 */
public interface ConvertOFD extends Closeable {

    /**
     * 转换为OFD页面
     * <p>
     * 1. 页码参数仅在转换源文件类型为类文档文件时有效，页码为不不传或为空时表示导出全部，如 {@code obj.convert(p)}、{@code obj.convert(p, null)}
     * <p>
     * 2. 页码参数支持指定需要导出页码序列，序列应支持乱序，以及重复，如 {@code obj.convert(p, 1,2,3)}、{@code obj.convert(p, 5,1,2,2)}
     * <p>
     * 3. 该方法可以重复调用，通过重复调用可以实现导出不同的页，甚至可以是重复的页。
     * <pre>
     *     obj.convert(p, 1);
     *     obj.convert(p, 2);
     *     obj.convert(p, 2);
     *     obj.convert(p, 4,5);
     * </pre>
     *
     * @param filepath 待转换文件路径
     * @param indexes  【可选】【可变】待转换页码（从0起），该参数仅在转换源文件类型为类文档文件时有效，当该参数不传或为空时表示转换全部内容到OFD。
     * @throws GeneralConvertException 转换异常
     */
    public void convert(Path filepath, int... indexes) throws GeneralConvertException;
}
