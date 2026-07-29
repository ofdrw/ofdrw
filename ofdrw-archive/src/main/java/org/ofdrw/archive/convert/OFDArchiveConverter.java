package org.ofdrw.archive.convert;

import org.ofdrw.archive.convert.handler.*;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * OFD-A 转换器
 * <p>
 * 将普通 OFD 文件转换为符合 GB/T 42133-2022 的 OFD-A 归档文件。
 * 转换流程：
 * <ol>
 *   <li>解压源文件到临时目录 src/</li>
 *   <li>按编排顺序执行所有去技术化处理器</li>
 *   <li>从 OFD.xml 出发 DFS 收集可达文件白名单</li>
 *   <li>按白名单复制文件到 output/，排除非法夹带</li>
 *   <li>打包 output/ 为 OFD-A 文件</li>
 *   <li>清理临时目录</li>
 * </ol>
 * <p>
 * 使用示例：
 * <pre>{@code
 *     OFDArchiveConverter converter = new OFDArchiveConverter();
 *     converter.convert(Paths.get("input.ofd"), Paths.get("output.ofd"));
 * }</pre>
 *
 * @author xxx
 * @since 2.3.9
 */
public class OFDArchiveConverter {

    /**
     * 处理器管道
     * <p>
     * 按顺序执行，顺序至关重要（如解密必须在最前）
     */
    private final List<ArchiveHandler> handlers;

    /**
     * 创建转换器，加载默认处理器管道
     */
    public OFDArchiveConverter() {
        this(loadDefaultHandlers());
    }

    /**
     * 创建转换器，使用自定义处理器管道
     *
     * @param handlers 自定义处理器列表（按顺序执行）
     */
    public OFDArchiveConverter(List<ArchiveHandler> handlers) {
        this.handlers = new ArrayList<>(handlers);
    }

    /**
     * 将 OFD 文件转换为 OFD-A 格式
     * <p>
     * 处理完成后输出到 dstPath，源文件不受影响。
     *
     * @param srcPath 源 OFD 文件路径
     * @param dstPath 输出 OFD-A 文件路径
     * @throws IOException 文件操作异常
     */
    public void convert(Path srcPath, Path dstPath) throws IOException {
        if (srcPath == null || Files.notExists(srcPath)) {
            throw new IllegalArgumentException("源文件不存在: " + srcPath);
        }
        if (dstPath == null) {
            throw new IllegalArgumentException("输出路径为空");
        }

        // 创建临时目录结构：src/ 存放解压内容，output/ 存放输出
        Path tempDir = Files.createTempDirectory("ofd-archive-");
        Path srcDir = tempDir.resolve("src");
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);

        try {
            // 1. 解压源文件到 src/
            ZipUtil.unZipFiles(srcPath.toFile(), srcDir.toAbsolutePath() + File.separator);

            // 2. 加载 OFD 容器和阅读器
            OFDDir ofdDir = new OFDDir(srcDir.toAbsolutePath());
            try (OFDReader reader = new OFDReader(srcDir.toAbsolutePath().toString(), false)) {
                // 3. 按顺序执行所有处理器
                for (ArchiveHandler handler : handlers) {
                    handler.handle(reader, ofdDir);
                }
            }

            // 4. 从 OFD.xml 出发 DFS 收集可达文件白名单
            Set<Path> reachableFiles = collectReachableFiles(srcDir);

            // 5. 按白名单复制文件到 output/
            for (Path file : reachableFiles) {
                Path relPath = srcDir.relativize(file);
                Path destFile = outputDir.resolve(relPath);
                Files.createDirectories(destFile.getParent());
                Files.copy(file, destFile, StandardCopyOption.REPLACE_EXISTING);
            }

            // 6. 打包 output/ 为 OFD 文件
            OFDDir outputOfdDir = new OFDDir(outputDir.toAbsolutePath());
            outputOfdDir.jar(dstPath);
        } finally {
            // 7. 清理临时目录
            deleteRecursively(tempDir);
        }
    }

    /**
     * 从 OFD.xml 出发，DFS 收集所有被引用的文件
     * <p>
     * BFS/DFS 遍历文档树中所有 ST_Loc 引用，构建可达文件白名单。
     * 未被引用的文件（非法夹带）自然被排除。
     *
     * @param rootDir OFD 包根目录
     * @return 可达文件的绝对路径集合
     * @throws IOException 文件遍历异常
     */
    private Set<Path> collectReachableFiles(Path rootDir) throws IOException {
        Set<Path> visited = new HashSet<>();      // 已处理的文件（防循环引用）
        Set<Path> reachable = new HashSet<>();     // 可达文件白名单

        // 从 OFD.xml 入口开始 DFS
        Path ofdXml = rootDir.resolve("OFD.xml");
        if (Files.exists(ofdXml)) {
            dfsCollect(ofdXml, rootDir, visited, reachable);
        }

        return reachable;
    }

    /**
     * DFS 递归收集可达文件
     * <p>
     * 解析 XML 文件中的所有 ST_Loc 引用路径，递归加入白名单。
     * visited 集合防止两个文档互引时的无限循环。
     *
     * @param currentFile 当前处理的文件
     * @param rootDir     OFD 包根目录
     * @param visited     已处理文件集合
     * @param reachable   可达文件白名单
     * @throws IOException 文件读取异常
     */
    private void dfsCollect(Path currentFile, Path rootDir,
                            Set<Path> visited, Set<Path> reachable) throws IOException {
        // 防循环引用：已处理过则跳过
        if (!visited.add(currentFile.toRealPath())) {
            return;
        }
        // 加入可达白名单
        reachable.add(currentFile);

        // 只对 XML 文件进行引用解析
        String fileName = currentFile.getFileName().toString().toLowerCase();
        if (!fileName.endsWith(".xml")) {
            return;
        }

        // 读取文件内容，提取所有 ST_Loc 路径引用
        // ST_Loc 路径以 "/" 开头表示容器内绝对路径
        try {
            String content = new String(Files.readAllBytes(currentFile), java.nio.charset.StandardCharsets.UTF_8);
            // 提取形如 "Doc_0/xxx" 或 "/Doc_0/xxx" 的路径引用
            // 同时提取 Loc 属性中的路径
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                    "\"(/?[A-Za-z0-9_/]+(?:\\.[A-Za-z]+)?)\"").matcher(content);
            while (m.find()) {
                String refPath = m.group(1);
                // 跳过明显不是文件路径的（如版本号、命名空间等）
                if (refPath.length() < 2 || refPath.startsWith("http") || refPath.startsWith("//")) {
                    continue;
                }
                // 去掉开头的 "/"
                if (refPath.startsWith("/")) {
                    refPath = refPath.substring(1);
                }
                // 解析为文件系统路径
                Path resolved = rootDir.resolve(refPath).normalize();
                // 安全检查：必须在 rootDir 子树内
                if (resolved.startsWith(rootDir) && Files.exists(resolved)) {
                    dfsCollect(resolved, rootDir, visited, reachable);
                }
            }
        } catch (Exception e) {
            // XML 解析失败不中断，尽力而为
        }
    }

    /**
     * 递归删除目录
     *
     * @param dir 待删除目录
     */
    private void deleteRecursively(Path dir) {
        if (Files.notExists(dir)) {
            return;
        }
        try {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    /**
     * 加载默认处理器管道
     * <p>
     * 处理器按编排顺序执行：
     * <ol>
     *   <li>门禁：加密解密在最前</li>
     *   <li>结构变更：单文档处理</li>
     *   <li>元数据：DocType / 权限 / 视图首选项 / 扩展</li>
     *   <li>动作：文档动作 / 大纲动作</li>
     *   <li>资源：外部资源 / 音视频 / 图像扩展 / 资源位置</li>
     *   <li>内容：PageBlock 展平 / 裁剪区 / 图层 / 图像属性清理</li>
     * </ol>
     *
     * @return 默认处理器列表
     */
    private static List<ArchiveHandler> loadDefaultHandlers() {
        List<ArchiveHandler> handlers = new ArrayList<>();

        // 0. 门禁（最先执行，加密不解密则中断）
        handlers.add(new EncryptionHandler());

        // 1. 结构变更
        handlers.add(new SingleDocHandler());
        handlers.add(new DocTypeHandler());

        // 2. 元数据清理
        handlers.add(new ExternalResourceHandler());
        handlers.add(new PermissionHandler());
        handlers.add(new VPrefsHandler());

        // 3. 动作清理
        handlers.add(new NonGotoActionHandler());
        handlers.add(new OutlineActionHandler());

        // 4. 扩展清理
        handlers.add(new ExtensionHandler());

        // 5. 资源处理
        handlers.add(new AudioVideoHandler());
        handlers.add(new ImageExtensionHandler());

        // 6. 内容处理
        handlers.add(new PageBlockFlattenHandler());
        handlers.add(new LayerNameHandler());
        handlers.add(new ImageInterpolateHandler());

        // 7. 属性清理
        handlers.add(new CleanStrokeAttrHandler());
        handlers.add(new CleanFillAttrHandler());

        // 8. 注释处理
        handlers.add(new AnnotationHandler());

        return handlers;
    }
}
