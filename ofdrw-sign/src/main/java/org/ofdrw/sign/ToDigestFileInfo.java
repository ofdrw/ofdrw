package org.ofdrw.sign;

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
     *
     * 如：“/Doc_0/Pages/Page_0/Content.xml”
     */
    private String absPath;

    /**
     * 待杂凑的文件在文件系统中的路径
     */
    private Path loc;

    public ToDigestFileInfo(String absPath, Path loc) {
        this.absPath = absPath;
        this.loc = loc;
    }

    public String getAbsPath() {
        return absPath;
    }

    public void setAbsPath(String absPath) {
        this.absPath = absPath;
    }

    public Path getLoc() {
        return loc;
    }

    public void setLoc(Path loc) {
        this.loc = loc;
    }
}
