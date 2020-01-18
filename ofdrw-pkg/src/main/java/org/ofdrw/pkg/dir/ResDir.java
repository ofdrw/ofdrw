package org.ofdrw.pkg.dir;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 资源目录
 *
 * @author 权观宇
 * @since 2020-01-18 02:49:24
 */
public class ResDir {
    /**
     * 目录内容
     */
    private Map<String, Path> content;

    public ResDir() {
        content = new HashMap<>(5);
    }

    /**
     * 向目录中加入资源
     *
     * @param res 资源
     * @return this
     */
    public ResDir add(Path res) {
        if (res == null) {
            return this;
        }
        this.content.put(res.getFileName().toString(), res);
        return this;
    }

    /**
     * 获取资源
     *
     * @param name 资源名称（包含后缀）
     * @return 资源路径
     */
    public Path get(String name) {
        return content.get(name);
    }
}
