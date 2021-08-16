package org.ofdrw.reader.extractor;

import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.core.text.TextCode;

/**
 * 抽取过滤器
 *
 * @author minghu-zhang
 * @since 2021-08-09 20:53:09
 */
@FunctionalInterface
public interface ExtractorFilter {

    /**
     * 是否运行返回文本
     *
     * @param textObject 文本域对象
     * @param textCode   文字定位对象
     * @return true/false
     */
    String getAllowText(TextObject textObject, TextCode textCode);
}
