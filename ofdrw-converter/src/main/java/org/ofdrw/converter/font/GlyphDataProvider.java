package org.ofdrw.converter.font;

import java.io.IOException;

/**
 * 字形数据提供者
 *
 * @author 权观宇
 * @since 2021-09-29 20:27:35
 */
@FunctionalInterface
public interface GlyphDataProvider {
    /**
     * 根据字形Index位置获取字形数据
     *
     * @param gid 字形Index
     * @return 字形数据
     * @throws IOException IO操作造成错误
     */
    GlyphData getGlyph(int gid)  throws IOException;
}
