package org.ofdrw.reader;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.pkg.container.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 资源定位器
 * <p>
 * 通过给与的资源地址获取对应的资源文件或对象
 *
 * @author 权观宇
 * @since 2020-04-08 20:05:14
 */
public class ResourceLocator {

    private static final Logger logger = LoggerFactory.getLogger(ResourceLocator.class);

    /**
     * 路径匹配正则列表
     */
    public static Pattern PtDoc = Pattern.compile("/(Doc_\\d+)");
    public static Pattern PtSigns = Pattern.compile("/(Doc_\\d+)/Signs");
    public static Pattern PtSign = Pattern.compile("/(Doc_\\d+)/Signs/(Sign_\\d+)");
    public static Pattern PtPages = Pattern.compile("/(Doc_\\d+)/Pages");
    public static Pattern PtPage = Pattern.compile("/(Doc_\\d+)/Pages/(Page_\\d+)");
    public static Pattern PtPageRes = Pattern.compile("/(Doc_\\d+)/Pages/(Page_\\d+)/Res");
    public static Pattern PtDocRes = Pattern.compile("/(Doc_\\d+)/Res");

    /**
     * 资源容器
     * <p>
     * 该容器带有缓存功能
     */
    private OFDDir ofdDir;

    /**
     * 当前目录
     */
    private LinkedList<String> workDir = new LinkedList<>();


    /**
     * 保存的路径栈
     * <p>
     * 每次调用Save都会入栈
     */
    private LinkedList<LinkedList<String>> savedStack = new LinkedList<>();


    private ResourceLocator() {
    }

    public ResourceLocator(OFDDir ofdDir) {
        this.ofdDir = ofdDir;
        // 默认工作目录为OFD容器的根目录
        this.workDir.add("/");
    }

    /***
     * 通过虚拟容器创建资源加载器
     *
     * 创建资源加载器的同时切换路径至虚拟容器的目录
     *
     * @param vc 虚拟容器
     */
    public ResourceLocator(VirtualContainer vc) {
        VirtualContainer p = vc;
        // 找到根路径
        while (p != p.getParent()) {
            p = p.getParent();
        }
        if ((p instanceof OFDDir) == false) {
            throw new IllegalArgumentException("根路径不是 OFDDir");
        }
        this.ofdDir = (OFDDir) p;
        this.cd(vc.getAbsLoc().getLoc());
    }

    /**
     * 保存当前工作路径
     *
     * @return this
     */
    public ResourceLocator save() {
        LinkedList<String> toBeSaveWd = new LinkedList<>(workDir);
        // 入栈
        savedStack.addFirst(toBeSaveWd);
        return this;
    }

    /**
     * 还原原有工作区
     * <p>
     * 如果没有保存过工作区，那么不会造成任何影响
     *
     * @return this
     */
    public ResourceLocator restore() {
        if (!savedStack.isEmpty()) {
            workDir.clear();
            // 出栈
            LinkedList<String> lastSaved = savedStack.removeFirst();
            workDir.addAll(lastSaved);
        }
        return this;
    }

    /**
     * 转换路径对象为绝对路径字符串
     *
     * @param path 路径对象
     * @return 绝对路径字符串
     */
    public String toAbsolutePath(ST_Loc path) {
        if (path == null) {
            return pwd();
        }
        return toAbsolutePath(path.getLoc());
    }

    /**
     * 路径转换为绝对路径
     *
     * @param path 容器路径
     * @return 绝对路径字符串
     */
    public String toAbsolutePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return pwd();
        }
        LinkedList<String> workDirCopy = new LinkedList<>(workDir);
        if (path.startsWith("/")) {
            workDirCopy.clear();
            workDirCopy.add("/");
        }
        for (String item : path.split("/")) {
            item = item.trim();
            if (item.equals(".") || item.isEmpty()) {
                // 表示但前目录不做任何操作
                continue;
            } else if (item.equals("..")) {
                workDirCopy.removeLast();
                if (workDirCopy.isEmpty()) {
                    workDirCopy.add("/");
                }
            } else {
                workDirCopy.add(item);
            }
        }
        return pwd(workDirCopy);
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
     * 切换到制定的虚拟容器目录下
     *
     * @param vc 虚拟容器
     * @return this
     */
    public ResourceLocator cd(VirtualContainer vc) {
        if (vc == null) {
            throw new IllegalArgumentException("改变目录的容器对象（vc）为空");
        }
        return cd(vc.getAbsLoc().getLoc());
    }

    /**
     * 改变目录  Change Directory
     *
     * @param path 路径位置
     * @return this
     * @throws ErrorPathException 路径不存在
     */
    public ResourceLocator cd(String path) {
        return cd(workDir, path);
    }

    /**
     * 改变目录  Change Directory
     * <p>
     * 路径最后如果是目录也不加 "/"
     *
     * @param path    路径位置
     * @param workDir 已有工作目录
     * @return this
     * @throws ErrorPathException 路径不存在
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
        /*
         * 转换路径为绝对路径：
         * 绝对路径与当前工作目录无关，直接按自身解析，避免被拼接为“当前目录 + 绝对路径”；
         * 相对路径以传入的工作目录（而不是成员 workDir）为基准。
         */
        final String base = pwd(workDir);
        final String absPath = normalize(path.startsWith("/")
                ? path
                : (base.endsWith("/") ? base + path : base + "/" + path));
        String ofwTmp = ofdDir.getSysAbsPath();
        Path sysPath = Paths.get(ofwTmp + absPath);
        if (Files.exists(sysPath) && Files.isDirectory(sysPath)) {
            setWorkDir(workDir, absPath);
            return this;
        }
        // 精确匹配失败：按大小写不敏感的方式逐级兜底匹配目录
        final List<String> startWorkDir = new LinkedList<>(workDir);
        final String tolerantAbsPath = resolveDirCaseInsensitive(ofwTmp, startWorkDir, path);
        if (tolerantAbsPath != null) {
            // 兜底命中的是系统绝对路径，工作目录需要去掉 OFD 解压临时目录前缀
            setWorkDir(workDir, ofdTmpRelative(ofwTmp, tolerantAbsPath));
            syncContainerCache(startWorkDir, workDir);
            return this;
        }
        // 如果路径不存在，那么报错
        throw new ErrorPathException("无法切换路径到" + path + "，目录不存在。");
    }

    /**
     * 规范化容器内绝对路径：消除 "." 与 ".." 段
     *
     * @param path 绝对路径
     * @return 规范化后的绝对路径
     */
    private static String normalize(String path) {
        final LinkedList<String> stack = new LinkedList<>();
        for (String item : path.split("/")) {
            item = item.trim();
            if (item.isEmpty() || item.equals(".")) {
                continue;
            }
            if (item.equals("..")) {
                if (!stack.isEmpty()) {
                    stack.removeLast();
                }
                continue;
            }
            stack.add(item);
        }
        if (stack.isEmpty()) {
            return "/";
        }
        final StringBuilder sb = new StringBuilder();
        for (String item : stack) {
            sb.append('/').append(item);
        }
        return sb.toString();
    }

    /**
     * 把 OFD 解压临时目录下的系统绝对路径转换为容器内路径
     *
     * @param ofwTmp        OFD 解压临时目录的系统绝对路径
     * @param actualSysPath 系统绝对路径
     * @return 容器内路径（以 "/" 开头），不以 ofwTmp 开头时原样返回
     */
    private static String ofdTmpRelative(String ofwTmp, String actualSysPath) {
        if (actualSysPath == null) {
            return null;
        }
        if (!actualSysPath.startsWith(ofwTmp)) {
            return actualSysPath;
        }
        final String relative = actualSysPath.substring(ofwTmp.length());
        return relative.isEmpty() ? "/" : relative;
    }

    /**
     * 按大小写不敏感方式逐级解析目录的真实路径
     * <p>
     * 仅在按文档路径精确切换目录失败时调用。路径中任意一级目录名的大小写与实际
     * 文件系统不一致，都会导致整条路径无法精确定位，因此这里逐级解析：
     * 先按原名称精确定位，失败时再在该层父目录中做一次大小写不敏感的匹配。
     *
     * @param ofwTmp        OFD 解压临时目录
     * @param startWorkDir  切换前的工作目录
     * @param path          文档中的目录路径（绝对路径或以 startWorkDir 为基准的相对路径）
     * @return 实际存在的目录系统绝对路径；无法解析时返回 null
     */
    private static String resolveDirCaseInsensitive(String ofwTmp, List<String> startWorkDir, String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        final String trimmed = path.trim();
        final List<String> parts = new ArrayList<>();
        if (trimmed.startsWith("/")) {
            for (String item : trimmed.split("/")) {
                item = item.trim();
                if (!item.isEmpty()) {
                    parts.add(item);
                }
            }
        } else {
            for (String item : startWorkDir) {
                item = item.trim();
                if (!item.isEmpty() && !item.equals("/")) {
                    parts.add(item);
                }
            }
            // 相对路径需要按 . 与 .. 规整后再逐级解析
            final List<String> relatives = new ArrayList<>();
            for (String item : trimmed.split("/")) {
                item = item.trim();
                if (item.isEmpty() || item.equals(".")) {
                    continue;
                }
                if (item.equals("..")) {
                    if (!parts.isEmpty()) {
                        parts.remove(parts.size() - 1);
                    }
                    continue;
                }
                relatives.add(item);
            }
            parts.addAll(relatives);
        }
        if (parts.isEmpty()) {
            return null;
        }
        String current = ofwTmp;
        for (int i = 0; i < parts.size(); i++) {
            final String item = parts.get(i);
            final Path exact = Paths.get(current, item);
            if (Files.isDirectory(exact)) {
                current = exact.toAbsolutePath().toString();
                continue;
            }
            final String actual = resolveCaseInsensitive(current, item, true);
            if (actual == null) {
                return null;
            }
            logger.warn("文档资源路径大小写与压缩包内实际路径不一致，已按实际路径加载：原始路径 [{}]，实际路径 [{}]",
                    ofdTmpRelative(ofwTmp, current) + "/" + item, ofdTmpRelative(ofwTmp, actual));
            current = actual;
        }
        return current;
    }

    /**
     * 清理工作目录链路上缓存键与文件系统不一致的虚拟容器
     * <p>
     * 仅在按文档路径精确切换目录失败、并已按大小写不敏感方式命中实际目录时调用：
     * 精确匹配失败时缓存中可能残留以文档路径（大小写）登记的目录项，
     * 而该目录名在文件系统中并不存在，会导致后续切换到实际目录时被旧缓存阻断。
     *
     * @param startWorkDir 链路起点工作目录（切换前的工作目录）
     * @param fullWorkDir  切换后的完整工作目录
     */
    private void syncContainerCache(List<String> startWorkDir, List<String> fullWorkDir) {
        VirtualContainer vc = ofdDir;
        int i = 0;
        for (; i < startWorkDir.size(); i++) {
            final String item = startWorkDir.get(i).trim();
            if (item.isEmpty() || item.equals("/")) {
                continue;
            }
            VirtualContainer child = null;
            try {
                child = vc.getContainer(item, VirtualContainer::new);
            } catch (FileNotFoundException e) {
                child = null;
            }
            if (child == null) {
                break;
            }
            vc = child;
        }
        for (; i < fullWorkDir.size(); i++) {
            final String item = fullWorkDir.get(i).trim();
            if (item.isEmpty() || item.equals("/")) {
                continue;
            }
            final String actualDir = resolveCaseInsensitive(vc.getSysAbsPath(), item, true);
            if (actualDir == null) {
                return;
            }
            vc.syncCacheWithFileSystem();
            try {
                vc = vc.getContainer(fileNameFrom(actualDir), VirtualContainer::new);
            } catch (FileNotFoundException e) {
                return;
            }
            vc.setContainerPath(actualDir);
        }
    }

    /**
     * 刷新工作区到指定绝对路径
     *
     * @param workDir 已有工作目录
     * @param absPath 绝对路径
     */
    private static void setWorkDir(LinkedList<String> workDir, String absPath) {
        workDir.clear();
        workDir.add("/");
        for (String item : absPath.split("/")) {
            item = item.trim();
            if (item.isEmpty()) {
                continue;
            }
            workDir.add(item);
        }
    }

    /**
     * 路径是否存在
     *
     * @param path 末端路径
     * @return true -存在，false - 不存在
     */
    public boolean exist(String path) {
        String ofwTmp = ofdDir.getSysAbsPath();
        String fullPath = "";
        if (path.startsWith("/")) {
            // 绝对路径
            fullPath = ofwTmp + path;
        } else {
            // 相对路径
            fullPath = Paths.get(ofwTmp, pwd(), path).toAbsolutePath().toString();
        }
        return Files.exists(Paths.get(fullPath));
    }

    /**
     * 判断路径是否存在
     *
     * @param workDir 路径集合
     * @return true -存在，false - 不存在
     */
    public boolean exist(LinkedList<String> workDir) {
        String pwd = pwd(workDir);
        String ofwTmp = ofdDir.getSysAbsPath();
        Path path = Paths.get(ofwTmp + pwd);
        return Files.exists(path);
    }


    /**
     * 判断路径是否存在
     *
     * @param workDir 工作路径
     * @return true -存在，false - 不存在
     */
    public boolean dirExit(LinkedList<String> workDir) {
        String pwd = pwd(workDir);
        String ofwTmp = ofdDir.getSysAbsPath();
        Path path = Paths.get(ofwTmp + pwd);
        return Files.exists(path) && Files.isDirectory(path);
    }


    /**
     * 打印工作目录 Print Work Directory
     * <p>
     * 路径最后如果是目录也不加 "/"
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
        for (int i = 0, len = workDir.size(); i < len; i++) {
            String item = workDir.get(i);
            item = item.trim();
            if (item.isEmpty()) {
                continue;
            }
            sb.append(item);
            // 最后一个路径不加"/"
            if (!item.equals("/") && (i != len - 1)) {
                sb.append("/");
            }
        }
        return sb.toString();
    }


    /**
     * 获取以当前路径为基础的容器内绝对路径
     *
     * @param to 目标路径
     * @return 容器内绝对路径
     */
    public ST_Loc getAbsTo(ST_Loc to) {
        if (to == null) {
            return new ST_Loc(pwd());
        }
        String loc = to.getLoc();
        if (loc.startsWith("/")) {
            return new ST_Loc(loc);
        }

        // 查询工作目录
        LinkedList<String> wd = new LinkedList<>(this.workDir);
        // 文件名称
        String fileName;
        int indexOf = loc.lastIndexOf('/');
        if (indexOf != -1) {
            // 切换工作目录
            this.cd(wd, loc.substring(0, indexOf + 1));
            fileName = loc.substring(indexOf + 1);
        } else {
            fileName = loc;
        }
        String p = pwd(wd);
        if (p.endsWith("/")) {
            loc = p + fileName;
        } else {
            loc = p + "/" + fileName;
        }
        return new ST_Loc(loc);
    }

    /**
     * 根据路径获取获取对应的资源对象
     *
     * @param loc    路径地址
     * @param mapper 对象映射构造器
     * @param <R>    映射对象
     * @return 对象
     * @throws FileNotFoundException 文件不存在
     * @throws DocumentException     文件解析异常
     */
    public <R> R get(ST_Loc loc, Function<Element, R> mapper) throws FileNotFoundException, DocumentException {
        return get(loc.getLoc(), mapper);
    }

    /**
     * 根据路径获取获取对应的资源对象
     *
     * @param loc    路径地址
     * @param mapper 对象映射构造器
     * @param <R>    映射对象
     * @return 对象
     * @throws FileNotFoundException 文件不存在
     * @throws DocumentException     文件解析异常
     */
    public <R> R get(String loc, Function<Element, R> mapper) throws FileNotFoundException, DocumentException {
        if (loc == null || loc.trim().equals("")) {
            throw new FileNotFoundException("路径为空（loc）");
        }
        // 查找并获取文件所处路径
        final String trimmedLoc = loc.trim();
        final int indexOf = trimmedLoc.lastIndexOf('/');
        final String fileName = indexOf < 0 ? trimmedLoc : trimmedLoc.substring(indexOf + 1);
        // 查询工作目录：绝对路径直接从根目录出发
        final LinkedList<String> wd = new LinkedList<>();
        if (trimmedLoc.startsWith("/")) {
            wd.add("/");
        } else {
            wd.addAll(this.workDir);
        }
        if (indexOf > 0) {
            final String dirPath = trimmedLoc.substring(0, indexOf + 1);
            if (!dirPath.trim().equals("/")) {
                final LinkedList<String> savedWorkDir = new LinkedList<>(this.workDir);
                try {
                    this.cd(wd, dirPath);
                } finally {
                    // 仅借用工作目录计算，恢复原工作目录
                    this.workDir.clear();
                    this.workDir.addAll(savedWorkDir);
                }
            }
        } else if (indexOf == 0) {
            wd.clear();
            wd.add("/");
        }
        // 根目录：资源直接位于 OFD 根容器中
        VirtualContainer vc = wd.size() == 1 ? ofdDir : getContainer(wd);
        Element element = vc.getObj(fileName);
        if (element != null) {
            return mapper.apply(element);
        }
        // 精确匹配失败：按大小写不敏感的方式兜底获取
        final VirtualContainer actualVc = getContainerTolerant(wd, loc, false);
        if (actualVc == null) {
            return mapper.apply(null);
        }
        final Element actualElement = actualVc.getObj(fileNameFrom(loc.trim()));
        if (actualElement != null) {
            logger.warn("文档资源路径大小写与压缩包内实际路径不一致，已按实际路径加载：原始路径 [{}]，实际路径 [{}]",
                    getSysWd(wd) + "/" + loc, actualVc.getSysAbsPath() + "/" + fileNameFrom(loc.trim()));
        }
        return mapper.apply(actualElement);
    }

    /**
     * 按大小写不敏感方式获取资源所在的虚拟容器
     * <p>
     * 仅在精确匹配失败时调用：以当前工作目录为基准逐级解析路径，
     * 命中后返回实际存在的目录对应的虚拟容器。
     *
     * @param baseWorkDir   基准工作目录
     * @param loc           文档中的资源路径（相对路径以 baseWorkDir 为基准）
     * @param needDirectory true - 资源为目录；false - 资源为文件
     * @return 虚拟容器；未命中返回 null
     */
    private VirtualContainer getContainerTolerant(LinkedList<String> baseWorkDir, String loc, boolean needDirectory) {
        if (loc == null || loc.trim().isEmpty()) {
            return null;
        }
        final String trimmed = loc.trim();
        final int index = trimmed.lastIndexOf('/');
        final String dirPath = index < 0 ? "" : trimmed.substring(0, index + 1);
        final String name = index < 0 ? trimmed : trimmed.substring(index + 1);
        final String parentSysPath = dirPath.isEmpty()
                ? getSysWd(baseWorkDir)
                : resolveDirCaseInsensitive(ofdDir.getSysAbsPath(), baseWorkDir, dirPath);
        if (parentSysPath == null) {
            return null;
        }
        if (needDirectory) {
            return getContainerByActualLoc(parentSysPath);
        }
        return resolveCaseInsensitive(parentSysPath, name, false) == null ? null : getContainerByActualLoc(parentSysPath);
    }

    /**
     * 获取路径下的文件
     *
     * @param stLoc 路径
     * @return 系统文件路径
     * @throws FileNotFoundException 文件或路径不存在
     */
    public Path getFile(ST_Loc stLoc) throws FileNotFoundException {
        String loc = stLoc.getLoc();
        return getFile(loc);
    }

    /**
     * 获取路径下的文件
     *
     * @param loc 路径
     * @return 系统文件路径
     * @throws FileNotFoundException 文件或路径不存在
     */
    public Path getFile(String loc) throws FileNotFoundException {
        if (loc == null || loc.trim().equals("")) {
            throw new FileNotFoundException("路径为空（loc）");
        }
        // 查询工作目录：绝对路径直接从根目录出发
        final String trimmedLoc = loc.trim();
        final int indexOf = trimmedLoc.lastIndexOf('/');
        final String fileName = indexOf < 0 ? trimmedLoc : trimmedLoc.substring(indexOf + 1);
        final LinkedList<String> wd = new LinkedList<>();
        if (trimmedLoc.startsWith("/")) {
            wd.add("/");
        } else {
            wd.addAll(this.workDir);
        }
        if (indexOf > 0) {
            final String dirPath = trimmedLoc.substring(0, indexOf + 1);
            if (!dirPath.trim().equals("/")) {
                final LinkedList<String> savedWorkDir = new LinkedList<>(this.workDir);
                try {
                    this.cd(wd, dirPath);
                } finally {
                    // 仅借用工作目录计算，恢复原工作目录
                    this.workDir.clear();
                    this.workDir.addAll(savedWorkDir);
                }
            }
        } else if (indexOf == 0) {
            wd.clear();
            wd.add("/");
        }
        // 查找并获取文件所处路径
        VirtualContainer vc = wd.size() == 1 ? ofdDir : getContainer(wd);
        Path file = vc.getFile(fileName);
        if (file != null) {
            return file;
        }
        // 精确匹配失败：按大小写不敏感的方式兜底获取
        final VirtualContainer actualVc = getContainerTolerant(wd, loc, false);
        if (actualVc != null) {
            final Path actualFile = actualVc.getFile(fileNameFrom(loc.trim()));
            if (actualFile != null) {
                logger.warn("文档资源路径大小写与压缩包内实际路径不一致，已按实际路径加载：原始路径 [{}]，实际路径 [{}]",
                        getSysWd(wd) + "/" + loc, actualFile);
                return actualFile;
            }
        }
        // 兜底未命中，保持原有异常行为
        return vc.getFile(fileName);
    }


    /**
     * 通过路径获取容器
     *
     * @param workDir 路径序列
     * @return 虚拟容器
     * @throws FileNotFoundException 路径不存在
     */
    public VirtualContainer getContainer(LinkedList<String> workDir) throws FileNotFoundException {
        VirtualContainer vc = ofdDir;
        for (String item : workDir) {
            if (item.equals("/")) {
                continue;
            }
            try {
                vc = getChildContainer(vc, item);
            } catch (FileNotFoundException e) {
                // 精确匹配失败：按大小写不敏感的方式兜底定位实际目录
                final VirtualContainer tolerant = getChildContainerTolerant(vc, item);
                if (tolerant == null) {
                    throw e;
                }
                vc = tolerant;
            }
        }
        return vc;
    }

    /**
     * 根据容器名称获取子容器（按容器类型选择容器实现）
     *
     * @param vc   父容器
     * @param item 子容器名称
     * @return 子容器
     * @throws FileNotFoundException 容器不存在
     */
    private static VirtualContainer getChildContainer(VirtualContainer vc, String item) throws FileNotFoundException {
        if (item.startsWith("Doc_")) {
            return vc.getContainer(item, DocDir::new);
        } else if (item.equals("Signs")) {
            return vc.getContainer(item, SignsDir::new);
        } else if (item.startsWith("Sign_")) {
            return vc.getContainer(item, SignDir::new);
        } else if (item.equals("Pages")) {
            return vc.getContainer(item, PagesDir::new);
        } else if (item.startsWith("Page_")) {
            return vc.getContainer(item, PageDir::new);
        } else if (item.equals("Res")) {
            return vc.getContainer(item, ResDir::new);
        } else if (item.startsWith("Temp_")) {
            return vc.getContainer(item, TempsDir::new);
        }
        return vc.getContainer(item, VirtualContainer::new);
    }

    /**
     * 按大小写不敏感的方式获取已实际存在的子容器
     * <p>
     * 仅在按文档中的名称精确获取子容器失败时调用：先使父容器缓存与文件系统一致，
     * 再按实际目录名获取子容器并记录告警。
     *
     * @param vc      父容器
     * @param item    文档中的目录名称
     * @param sysPath 父容器对应的系统绝对路径
     * @return 子容器；实际目录不存在时返回 null
     */
    private static VirtualContainer getChildContainerTolerant(VirtualContainer vc, String item) {
        final String sysPath = vc.getSysAbsPath();
        final String actualDir = resolveCaseInsensitive(sysPath, item, true);
        if (actualDir == null) {
            return null;
        }
        final String actualName = fileNameFrom(actualDir);
        try {
            // 清理缓存中按文档名称登记的旧键，避免阻断按实际名称查找
            vc.syncCacheWithFileSystem();
            final VirtualContainer child = getChildContainer(vc, actualName);
            logger.warn("文档资源路径大小写与压缩包内实际路径不一致，已按实际路径加载：原始路径 [{}]，实际路径 [{}]",
                    sysPath.substring(ofdSysPrefix(vc).length()) + "/" + item,
                    sysPath.substring(ofdSysPrefix(vc).length()) + "/" + actualName);
            return child;
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    /**
     * 获取容器所属 OFD 解压临时目录
     *
     * @param vc 任意虚拟容器
     * @return OFD 解压临时目录的系统绝对路径
     */
    private static String ofdSysPrefix(VirtualContainer vc) {
        VirtualContainer p = vc;
        while (p.getParent() != p) {
            p = p.getParent();
        }
        return p.getSysAbsPath();
    }

    /**
     * 根据路径获取虚拟容器对象
     * <p>
     * 获取的同时会缓存整个容器链路
     *
     * @param containerPath 容器目录
     * @return 虚拟容器
     * @throws FileNotFoundException 路径不存在
     */
    public VirtualContainer getContainer(String containerPath) throws FileNotFoundException {
        this.save();
        try {
            this.cd(containerPath);
            return getContainer(this.workDir);
        } finally {
            this.restore();
        }
    }

    @Override
    public String toString() {
        return pwd();
    }

    /**
     * 获取工作目录对应的系统绝对路径
     *
     * @param workDir 工作目录
     * @return 系统绝对路径，例如：/tmp/ofd-xxx/Doc_0
     */
    private String getSysWd(LinkedList<String> workDir) {
        return ofdDir.getSysAbsPath() + pwd(workDir);
    }

    /**
     * 从路径中取出文件名部分
     *
     * @param loc 路径字符串
     * @return 文件名（不含目录部分）
     */
    private static String fileNameFrom(String loc) {
        final int index = loc.lastIndexOf('/');
        return index < 0 ? loc : loc.substring(index + 1);
    }

    /**
     * 按大小写不敏感方式解析末端名称在文件系统中的实际路径
     * <p>
     * 仅在精确匹配失败时调用：比较末端名称所在父目录中的条目（忽略大小写），
     * 命中后返回实际的系统绝对路径，未命中返回 null。
     *
     * @param parentSysPath 末端名称所在父目录的系统绝对路径
     * @param name          末端名称（文件或目录名）
     * @param needDirectory true - 要求末端为目录；false - 要求末端为文件
     * @return 实际存在的系统绝对路径；未命中返回 null
     */
    private static String resolveCaseInsensitive(String parentSysPath, String name, boolean needDirectory) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        final Path parent = Paths.get(parentSysPath);
        if (Files.notExists(parent) || !Files.isDirectory(parent)) {
            return null;
        }
        try (Stream<Path> children = Files.list(parent)) {
            return children
                    .filter(p -> name.equalsIgnoreCase(p.getFileName().toString()))
                    .filter(p -> needDirectory ? Files.isDirectory(p) : Files.isRegularFile(p))
                    .findFirst()
                    .map(p -> p.toAbsolutePath().toString())
                    .orElse(null);
        } catch (IOException e) {
            logger.debug("按大小写不敏感方式查找 [{}] 失败：{}", parentSysPath, e.getMessage());
            return null;
        }
    }

    /**
     * 获取已存在实际路径对应的虚拟容器
     * <p>
     * 用于大小写不敏感兜底：沿实际路径逐级向下复用虚拟容器，使解析结果与精确匹配时一致。
     *
     * @param actualSysPath 实际存在的系统绝对路径
     * @return 虚拟容器；路径不可用时返回 null
     */
    private VirtualContainer getContainerByActualLoc(String actualSysPath) {
        final String ofwTmp = ofdDir.getSysAbsPath();
        if (actualSysPath == null || !actualSysPath.startsWith(ofwTmp)) {
            return null;
        }
        final String loc = actualSysPath.substring(ofwTmp.length());
        VirtualContainer vc = ofdDir;
        try {
            for (String item : loc.split("/")) {
                item = item.trim();
                if (!item.isEmpty()) {
                    vc = getChildContainer(vc, item);
                }
            }
            return vc;
        } catch (FileNotFoundException e) {
            logger.debug("按实际路径 [{}] 获取容器失败：{}", loc, e.getMessage());
            return null;
        }
    }

}
