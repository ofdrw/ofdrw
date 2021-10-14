package org.ofdrw.converter.font;

import java.awt.geom.GeneralPath;
import java.io.IOException;

/**
 * 字形绘制路径提供器
 *
 * @author 权观宇
 * @since 2021-10-14 20:53:49
 */
@FunctionalInterface
public interface FontDrawPathProvider {

    /**
     * 通过字体索引号获取字形的绘制路径
     *
     * @param gid 字形索引号
     * @return 绘制路径或null（找不到时）
     * @throws IOException 解析异常
     */
    GeneralPath getPath(int gid) throws IOException;
}
