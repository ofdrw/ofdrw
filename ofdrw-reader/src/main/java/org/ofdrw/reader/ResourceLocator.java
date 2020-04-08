package org.ofdrw.reader;

import org.dom4j.Element;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.pkg.container.OFDDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

/**
 * 资源定位器
 * <p>
 * 通过给与的资源地址获取对应的资源文件或对象
 *
 * @author 权观宇
 * @since 2020-04-08 20:05:14
 */
public class ResourceLocator {

    /**
     * 当前目录
     */
    private LinkedList<String> workDir;
    /**
     * 资源容器
     * <p>
     * 该容器带有缓存功能
     */
    private OFDDir dirCache;

    private ResourceLocator() {

    }

    public ResourceLocator(OFDDir dirCache) {
        this.dirCache = dirCache;
        // 默认工作目录为OFD容器的根目录
        this.workDir = new LinkedList<>();
        this.workDir.add("/");
    }

    /**
     * 重置工作路径
     * <p>
     * 重置后将回到根路径
     *
     * @return this
     */
    public ResourceLocator restWd() {
        return cd("/");
    }

    /**
     * 改变目录  Change Directory
     *
     * @param path 路径位置
     * @return this
     */
    public ResourceLocator cd(String path) {
        return cd(workDir, path);
    }

    /**
     * 改变目录  Change Directory
     *
     * @param path    路径位置
     * @param workDir 已有工作目录
     * @return this
     */
    public ResourceLocator cd(LinkedList<String> workDir, String path) {
        if (path == null || path.equals("")) {
            return this;
        }
        path = path.trim();
        if (path.equals("/")) {
            workDir.clear();
            workDir.add("/");
            return this;
        }

        LinkedList<String> workDirCopy = new LinkedList<>(workDir);
        for (String item : path.split("/")) {
            item = item.trim();
            if (item.equals(".") || item.equals("")) {
                // 表示但前目录不做任何操作
            } else if (item.equals("..")) {
                workDirCopy.removeLast();
                if (workDirCopy.isEmpty()) {
                    workDirCopy.add("/");
                }
            } else {
                workDirCopy.add(item);
                // 如果工作路径不存在，那么报错
                if (!exist(workDirCopy)) {
                    System.err.println(">> 无法切换路径至:" + path + "，因为路径不存在");
                    return this;
                }
            }
            workDir.clear();
            workDir.addAll(workDirCopy);
        }
        return this;
    }

    /**
     * 判断路径是否存在
     *
     * @param workDir 路径集合
     * @return true -存在，false - 不存在
     */
    public boolean exist(LinkedList<String> workDir) {
        String pwd = pwd(workDir);
        String ofwTmp = dirCache.getFullPath();
        Path path = Paths.get(ofwTmp + pwd);
        return Files.exists(path);
    }


    /**
     * 打印工作目录 Print Work Directory
     *
     * @return 工作目录路径
     */
    public String pwd() {
        return pwd(this.workDir);
    }

    /**
     * 打印工作目录 Print Work Directory
     *
     * @param workDir 工作目录
     * @return 工作目录路径
     */
    public String pwd(List<String> workDir) {
        if (workDir.size() == 1) {
            return "/";
        }
        StringBuilder sb = new StringBuilder();
        for (String item : workDir) {
            sb.append(item);
            if (!item.equals("/")) {
                sb.append("/");
            }
        }
        return sb.toString();
    }


    public <R> R get(ST_Loc loc, Function<Element, R> mapper) {
        // 查询工作目录
        LinkedList<String> searchWd = new LinkedList<>(this.workDir);
        // 切换工作目录
        this.cd(searchWd, loc.getLoc());
        return null;
    }
}
