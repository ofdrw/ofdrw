package org.ofdrw.sign;

import org.ofdrw.core.basicType.ST_Loc;

import java.nio.file.Path;

/**
 * 待计算杂凑值的文件信息
 *
 * @author 权观宇
 * @since 2020-04-18 11:04:54
 */
public class ToDigestFileInfo {

    /**
     * 文件在OFD虚拟容器中的绝对路径
     * <p>
     * 如：“/Doc_0/Pages/Page_0/Content.xml”
     */
    private ST_Loc absPath;

    /**
     * 待杂凑的文件在文件系统中的路径
     */
    private Path sysPath;

    /**
     * 创建文件信息对象
     *
     * @param absPath 容器内绝对路径
     * @param sysPath 文件系统中的路径
     */
    public ToDigestFileInfo(String absPath, Path sysPath) {
        this.absPath = new ST_Loc(absPath);
        this.sysPath = sysPath;
    }

    public ST_Loc getAbsPath() {
        return absPath;
    }

    public void setAbsPath(String absPath) {
        this.absPath = new ST_Loc(absPath);
    }

    public Path getSysPath() {
        return sysPath;
    }

    public void setSysPath(Path sysPath) {
        this.sysPath = sysPath;
    }
}
