package org.ofdrw.converter.utils;

import org.ofdrw.converter.FontLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FontConfig 辅助类，用于在 Linux 系统上通过 fontconfig 框架查询字体匹配
 * 仅在 Linux 环境下生效，其他平台返回 null
 */
public class FontConfigHelper {
    private static final Logger log = LoggerFactory.getLogger(FontConfigHelper.class);
    // 使用 WeakHashMap 防止内存泄漏，当不再使用时会被GC回收
    private static final Map<String, Integer> ttcIndexCache =
        Collections.synchronizedMap(new WeakHashMap<>());
    // 预编译正则表达式，避免重复编译
    private static final java.util.regex.Pattern FILE_PATH_PATTERN =
        java.util.regex.Pattern.compile("\"([^\"]+)\"");

    /**
     * 使用 fontconfig 查询字体匹配
     *
     * @param fontName 字体名称（如 "SourceHanSansSC-Regular"）
     * @param familyName 字族名称（可选，可为 null）
     * @return 匹配的字体文件绝对路径，如果未找到或非 Linux 平台则返回 null
     */
    public static String queryFontConfig(String fontName, String familyName) {
        if (!OSinfo.isLinux()) {
            return null;
        }

        // 优先使用 fontName，如果为 null 或空字符串则使用 familyName
        String queryName = (fontName != null && !fontName.trim().isEmpty()) ? fontName : familyName;
        if (queryName == null || queryName.trim().isEmpty()) {
            return null;
        }

        if (FontLoader.DEBUG) {
            log.debug("fontconfig 查询: fontName={}, familyName={}, queryName={}", fontName, familyName, queryName);
        }

        Process process = null;
        try {
            // 构建 fc-match 命令
            ProcessBuilder pb = new ProcessBuilder("fc-match", "-v", queryName);
            process = pb.start();

            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // 读取错误流，防止进程阻塞
            StringBuilder error = new StringBuilder();
            try (BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    error.append(line).append("\n");
                }
            }

            // 添加超时机制，防止命令卡住
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                if (FontLoader.DEBUG) {
                    log.debug("fc-match 命令超时（5秒），已强制终止");
                }
                return null;
            }

            int exitCode = process.exitValue();

            if (exitCode != 0) {
                if (FontLoader.DEBUG) {
                    log.debug("fc-match 执行失败，退出码: {}，错误: {}", exitCode, error.toString());
                }
                return null;
            }

            // 解析输出，寻找 file: 或 filename: 行
            String result = parseFontConfigOutput(output.toString());
            if (result != null && !result.isEmpty()) {
                if (FontLoader.DEBUG) {
                    log.debug("fontconfig 匹配到字体: {} -> {}", queryName, result);
                }
                return result;
            }

        } catch (IOException e) {
            if (FontLoader.DEBUG) {
                log.debug("执行 fc-match 命令失败: {}", e.getMessage());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (FontLoader.DEBUG) {
                log.debug("fc-match 命令被中断");
            }
        } finally {
            if (process != null) {
                process.destroy();
            }
        }

        return null;
    }

    /**
     * 解析 fc-match 输出，提取字体文件路径，并缓存 TTC 索引（如果存在）
     */
    private static String parseFontConfigOutput(String output) {
        if (output == null || output.isEmpty()) {
            return null;
        }

        if (FontLoader.DEBUG) {
            log.debug("parseFontConfigOutput 输入: {}", output);
        }

        String[] lines = output.split("\n");
        String foundPath = null;
        Integer foundIndex = null;

        for (String line : lines) {
            line = line.trim();
            // 查找包含文件路径的行
            if (line.startsWith("file:") || line.startsWith("filename:")) {
                // 提取路径部分
                String[] parts = line.split(":", 2);
                if (parts.length >= 2) {
                    String path = parts[1].trim();
                    if (FontLoader.DEBUG) {
                        log.debug("解析路径行: {}, 原始路径: {}", line, path);
                    }
                    // 去除可能的引号以及引号外的括号后缀，例如 "/path/to/font.ttc"(s)
                    // 使用预编译的正则表达式匹配引号内的内容
                    java.util.regex.Matcher matcher = FILE_PATH_PATTERN.matcher(path);
                    if (matcher.find()) {
                        foundPath = matcher.group(1);
                        if (FontLoader.DEBUG) {
                            log.debug("正则匹配成功，提取路径: {}", foundPath);
                        }
                    } else {
                        // 如果没有引号，直接使用路径（去除可能的后缀）
                        // 移除括号及其内容，例如 (s)、(w) 等
                        foundPath = path.replaceAll("\\(.*\\)", "").trim();
                        // 如果路径仍然包含引号，移除它们
                        if (foundPath.startsWith("\"") && foundPath.endsWith("\"")) {
                            foundPath = foundPath.substring(1, foundPath.length() - 1);
                        }
                        if (FontLoader.DEBUG) {
                            log.debug("正则匹配失败，处理后路径: {}", foundPath);
                        }
                    }
                }
            }
            // 查找索引行
            else if (line.startsWith("index:")) {
                // 格式: index: 2(i)(w)
                String[] parts = line.split(":");
                if (parts.length >= 2) {
                    String indexStr = parts[1].trim();
                    // 提取数字部分（可能后面有括号）
                    String numStr = indexStr.replaceAll("\\(.*\\)", "").trim();
                    try {
                        foundIndex = Integer.parseInt(numStr);
                        if (FontLoader.DEBUG) {
                            log.debug("解析到索引: {}", foundIndex);
                        }
                    } catch (NumberFormatException e) {
                        // 忽略格式错误
                        if (FontLoader.DEBUG) {
                            log.debug("索引解析失败: {}", indexStr);
                        }
                    }
                }
            }
        }

        // 如果找到路径且索引有效，缓存索引
        if (foundPath != null && foundIndex != null && foundIndex >= 0) {
            ttcIndexCache.put(foundPath, foundIndex);
            if (FontLoader.DEBUG) {
                log.debug("缓存 TTC 索引: {} -> {}", foundPath, foundIndex);
            }
        }

        if (FontLoader.DEBUG) {
            log.debug("parseFontConfigOutput 返回路径: {}", foundPath);
        }
        return foundPath;
    }

    /**
     * 查询字体在 TTC 集合中的索引
     * 如果匹配到的字体是 TTC 文件，尝试确定其中哪个字体变体最匹配
     *
     * @param ttcPath TTC 文件路径
     * @param fontName 目标字体名称
     * @param familyName 目标字族名称
     * @return 匹配的字体索引，如果无法确定则返回 -1
     */
    public static int queryTtcFontIndex(String ttcPath, String fontName, String familyName) {
        if (!OSinfo.isLinux() || ttcPath == null || !ttcPath.toLowerCase().endsWith(".ttc")) {
            return -1;
        }

        // 检查是否有缓存的索引
        Integer cachedIndex = ttcIndexCache.get(ttcPath);
        if (cachedIndex != null && cachedIndex >= 0) {
            if (FontLoader.DEBUG) {
                log.debug("使用缓存的 TTC 索引: {} -> {}", ttcPath, cachedIndex);
            }
            return cachedIndex;
        }

        // 尝试使用 fc-query 查询 TTC 文件中的字体信息
        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder("fc-query", "--format=%{family}:%{style}:%{fullname}", ttcPath);
            process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // 添加超时机制，防止命令卡住
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                if (FontLoader.DEBUG) {
                    log.debug("fc-query 命令超时（5秒），已强制终止");
                }
                return -1;
            }

            // 解析输出，每行格式: 字族:样式:全名
            String[] lines = output.toString().split("\n");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(":", 3);
                if (parts.length >= 3) {
                    String family = parts[0].trim();
                    String style = parts[1].trim();
                    String fullname = parts[2].trim();

                    // 匹配逻辑
                    if (fontName != null && fullname.equalsIgnoreCase(fontName)) {
                        return i; // 精确匹配全名
                    }
                    if (familyName != null && family.equalsIgnoreCase(familyName)) {
                        // 字族匹配，进一步检查样式
                        if (fontName != null && style.toLowerCase().contains(fontName.toLowerCase())) {
                            return i;
                        }
                        // 如果样式包含中文变体标识，优先选择
                        if (isChineseStyle(style)) {
                            return i;
                        }
                    }
                }
            }

        } catch (Exception e) {
            if (FontLoader.DEBUG) {
                log.debug("查询 TTC 字体索引失败: {}", e.getMessage());
            }
        } finally {
            if (process != null) {
                process.destroy();
            }
        }

        return -1;
    }

    /**
     * 判断字体样式是否包含中文变体标识
     */
    private static boolean isChineseStyle(String style) {
        if (style == null) return false;
        String styleLower = style.toLowerCase();
        return styleLower.contains("sc") || styleLower.contains("cn") ||
               styleLower.contains("chs") || styleLower.contains("gb") ||
               styleLower.contains("big5") || styleLower.contains("hk") ||
               styleLower.contains("tw") || styleLower.contains("简体") ||
               styleLower.contains("中文");
    }
}