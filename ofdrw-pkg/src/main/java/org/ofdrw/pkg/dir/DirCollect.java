package org.ofdrw.pkg.dir;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 创建目录
 *
 * @author 权观宇
 * @since 2020-01-20 13:06:26
 */
public interface DirCollect {
    /**
     * 创建目录并复制文件
     *
     * @param base 基础路径
     * @return 创建的目录路径
     * @throws IOException IO异常
     */
    Path collect(String base) throws IOException;
}
