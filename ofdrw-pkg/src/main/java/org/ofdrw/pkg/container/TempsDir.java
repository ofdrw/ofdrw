package org.ofdrw.pkg.container;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.ofdrw.core.Holder;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicType.ST_Loc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * 模板文件存放目录
 *
 * @author 权观宇
 * @since 2020-4-3 19:41:32
 */
public class TempsDir extends VirtualContainer {

    public static final String TempFilePrefix = "Temp_";

    public static final Pattern TempFileNameRegex = Pattern.compile("Temp_(\\d+).xml");

    private int maxTempIndex = -1;

    public TempsDir(Path fullDir) throws IllegalArgumentException {
        super(fullDir);
    }

    /**
     * 向目录中加入模板文件
     * <p>
     * 加入的资源将会被复制到指定目录，与原有文件
     *
     * @param res 资源
     * @return this
     * @throws IOException 文件复制过程中发生的异常
     */
    public TempsDir add(Path res) throws IOException {
        if (Files.notExists(res)) {
            return this;
        }
        this.putFile(res);
        return this;
    }

    /**
     * 向目录中加入模板页面
     *
     * @param fileName 模板文件名称
     * @param page     模板页面
     * @return 加入页面的容器内绝对路径
     */
    public ST_Loc add(String fileName, Page page) {
        this.putObj(fileName, page);
        return this.getAbsLoc().cat(fileName);
    }

    /**
     * 向容器内加入模板
     *
     * @param page 模板页面
     * @return 模板的容器内绝对路径
     * @throws IOException 文件读写异常
     */
    public ST_Loc add(Page page) throws IOException {
        if (page == null) {
            return null;
        }
        this.maxTempIndex = getMaxTempIndex() + 1;
        String fileName = String.format("%s%d.xml", TempFilePrefix, maxTempIndex);
        return add(fileName, page);
    }

    /**
     * 根据文件名获取模板页面对象
     *
     * @param fileName 文件名
     * @return 模板原页面
     * @throws DocumentException     文档无法解析
     * @throws FileNotFoundException 文件不存在
     */
    public Page get(String fileName) throws DocumentException, FileNotFoundException {
        final Element element = this.getObj(fileName);
        return new Page(element);
    }

    /**
     * 获取当前容器内最大的模板文件索引号
     *
     * @return 索引数字
     * @throws IOException 文件读取异常
     */
    public Integer getMaxTempIndex() throws IOException {
        if (maxTempIndex < 0) {
            Holder<Integer> maxIndexHolder = new Holder<>(-1);
            Files.list(this.getContainerPath()).forEach((item) -> {
                String fileName = item.getFileName().toString().toLowerCase();
                // 不是目录 并且 文件名以 Annot_ 开头
                if (fileName.startsWith(TempFilePrefix.toLowerCase())) {
                    String numStr = fileName.replace(TempFilePrefix.toLowerCase(), "")
                            .split("\\.")[0];
                    try {
                        int n = Integer.parseInt(numStr);
                        if (n > maxIndexHolder.value) {
                            maxIndexHolder.value = n;
                        }
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
            });
            maxTempIndex = maxIndexHolder.value;
        }
        return maxTempIndex;
    }
}
