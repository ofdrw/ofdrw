package org.ofdrw.archive.convert.handler;

import org.dom4j.Element;
import org.ofdrw.archive.convert.ArchiveHandler;
import org.ofdrw.core.basicStructure.res.CT_MultiMedia;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.ResourceManage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 处理器 3：删除外部资源引用（GB/T 42133-2022 6.1）
 * <p>
 * 删除指向外部的资源引用（URL 和包外路径），安全不下载。
 * <ul>
 *   <li>http/https URL → 删除对应的资源条目</li>
 *   <li>包外文件路径 → 删除引用</li>
 * </ul>
 *
 * @author 权观宇
 * @since 2.3.9
 */
public class ExternalResourceHandler implements ArchiveHandler {

    /** 外部 URL 模式 */
    private static final Pattern EXTERNAL_URL = Pattern.compile("^https?://");

    @Override
    public void handle(OFDReader reader, OFDDir ofdDir) throws IOException {
        ResourceManage resMgt = reader.getResMgt();
        if (resMgt == null) return;

        Path workDir = reader.getWorkDir();
        List<CT_MultiMedia> mediaList = resMgt.getMultiMedias();
        if (mediaList == null || mediaList.isEmpty()) return;

        List<CT_MultiMedia> toRemove = new ArrayList<>();
        for (CT_MultiMedia media : mediaList) {
            ST_Loc mediaFile = media.getMediaFile();
            if (mediaFile == null) continue;

            String path = mediaFile.toString();
            // 检测外部 URL 引用
            if (EXTERNAL_URL.matcher(path).find()) {
                toRemove.add(media);
                continue;
            }

            // 检测包外文件路径
            String relPath = path.startsWith("/") ? path.substring(1) : path;
            Path absPath = workDir.resolve(relPath).normalize();
            if (!absPath.startsWith(workDir.normalize())) {
                toRemove.add(media);
                // 删除包外文件引用（如果碰巧存在也删除）
                Files.deleteIfExists(absPath);
            }
        }

        // 从 XML 中移除外部资源条目
        for (CT_MultiMedia media : toRemove) {
            media.detach();
        }
    }
}
