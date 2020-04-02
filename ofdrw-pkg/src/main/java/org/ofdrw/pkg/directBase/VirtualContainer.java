package org.ofdrw.pkg.directBase;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.ofdrw.core.DefaultElementProxy;
import org.ofdrw.pkg.tool.EleCup;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 虚拟容器对象
 *
 * @author 权观宇
 * @since 2020-04-02 19:01:04
 */
public class VirtualContainer {

    /**
     * 文件根路径
     */
    private String base;

    /**
     * 目录名称
     */
    private String name;

    /**
     * 文件缓存
     */
    private Map<String, Element> fileCache;

    /**
     * 目录中的虚拟容器缓存
     */
    private Map<String, VirtualContainer> dirCache;

    /**
     * 获取虚拟容器的名称
     *
     * @return 名称
     */
    public String getContainerName() {
        return name;
    }

    private VirtualContainer() {
    }

    /**
     * 创建一个虚拟容器
     *
     * @param base 基础路径对象
     * @throws IOException 目录创建异常
     */
    public VirtualContainer(Path base) throws IOException {
        if (base == null) {
            throw new IllegalArgumentException("文件路径为空");
        }
        if (Files.notExists(base)) {
            base = Files.createDirectories(base);
        }
        if (!Files.isDirectory(base)) {
            throw new IllegalStateException("请传入基础目录路径，而不是文件");
        }
        this.base = base.toAbsolutePath().toString();
        this.name = base.getFileName().toString();
        fileCache = new HashMap<>(7);
        dirCache = new HashMap<>(5);
    }

    /**
     * 向虚拟容器中加入文件
     *
     * @param file 文件路径对象
     * @return this
     * @throws IOException IO异常
     */
    public VirtualContainer putFile(Path file) throws IOException {
        if (file == null || Files.notExists(file) || Files.isDirectory(file)) {
            // 为空或是一个文件夹，或者不存在
            return this;
        }
        String fileName = file.getFileName().toString();
        Files.copy(file, Paths.get(base, fileName));
        return this;
    }

    /**
     * 向虚拟容器加入对象
     *
     * @param fileName 文件名
     * @param element  元素对象
     * @return this
     */
    public VirtualContainer putObj(String fileName, Element element) {
        if (fileName == null || fileName.length() == 0) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        if (element == null) {
            return this;
        }
        if (element instanceof DefaultElementProxy) {
            // 如果是代理元素对象那么取出被代理的对象存储
            element = ((DefaultElementProxy) element).getProxy();
        }
        fileCache.put(fileName, element);
        return this;
    }

    /**
     * 通过文件名获取元素对象
     *
     * @param fileName 文件名
     * @return 元素对象（不含代理）
     * @throws FileNotFoundException 文件不存在
     * @throws DocumentException     元素序列化异常
     */
    public Element getObj(String fileName) throws FileNotFoundException, DocumentException {
        if (fileName == null || fileName.length() == 0) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        Element element = fileCache.get(fileName);
        if (element == null) {
            // 缓存中不存在，从文件目录中尝试读取
            Path file = getFile(fileName);
            // 反序列化文件为对象
            element = EleCup.inject(file);
            // 从文件加载元素，那么缓存该元素对象
            fileCache.put(fileName, element);
        }
        // 如果是代理元素对象那么取出被代理的对象存储
        if (element instanceof DefaultElementProxy) {
            element = ((DefaultElementProxy) element).getProxy();
        }
        return element;

    }

    /**
     * 获取容器中的文件对象
     *
     * @param fileName 文件名称
     * @return 文件路径对象
     * @throws FileNotFoundException 文件不存在
     */
    public Path getFile(String fileName) throws FileNotFoundException {
        if (fileName == null || fileName.length() == 0) {
            throw new IllegalArgumentException("文件名为空");
        }
        Path res = Paths.get(base, fileName);
        if (Files.isDirectory(res) || Files.notExists(res)) {
            throw new FileNotFoundException("无法在目录: " + base + "中找到，文件 [ " + fileName + " ]");
        }
        return res;
    }


    /**
     * 获取一个虚拟容器对象
     * <p>
     * 如果容器存在，那么取出元素
     * <p>
     * 如果容器不存在，那么创建一个新的对象
     *
     * @param name   容器名称
     * @param mapper 容器构造器引用
     * @param <R>    容器子类
     * @return 新建或已经存在的容器
     */
    public <R extends VirtualContainer> R obtainContainer(String name, Function<Path, R> mapper) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("容器名称（name）为空");
        }
        if (mapper == null) {
            throw new IllegalArgumentException("容器构建对象（mapper）为空");
        }
        Path p = Paths.get(base, name);
        // 检查缓存
        VirtualContainer target = dirCache.get(name);
        if (target == null) {
            // 如果目录不存在那么创建，如果已经存在那么就是加载
            R ct = mapper.apply(p);
            // 加入缓存中
            dirCache.put(name, ct);
            return ct;
        } else {
            return (R) target;
        }
    }

    /**
     * 获取虚拟容器
     * <p>
     * 如果容器不存在那么返还null
     *
     * @param name   容器名称
     * @param mapper 容器构造器引用
     * @param <R>    容器子类
     * @return null或容器对象
     */
    public <R extends VirtualContainer> R getContainer(String name, Function<Path, R> mapper) {
        Path p = Paths.get(base, name);
        if (Files.notExists(p) || !Files.isDirectory(p)) {
            return null;
        }

        // 检查缓存
        VirtualContainer target = dirCache.get(name);
        if (target == null) {
            // 调用指定构造器创建容器对象
            R ct = mapper.apply(p);
            // 加入缓存中
            dirCache.put(name, ct);
            return ct;
        } else {
            return (R) target;
        }
    }


}
