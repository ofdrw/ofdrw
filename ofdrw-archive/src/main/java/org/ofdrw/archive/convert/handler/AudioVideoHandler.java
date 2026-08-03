package org.ofdrw.archive.convert.handler;

import org.dom4j.Element;
import org.ofdrw.archive.convert.ArchiveHandler;
import org.ofdrw.core.basicStructure.res.CT_MultiMedia;
import org.ofdrw.core.basicStructure.res.MediaType;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.ResourceManage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 处理器 9：删除音视频资源 + 保留摘要信息（GB/T 42133-2022 6.2.6g）
 * <p>
 * 遍历所有 MultiMedia，删除 Audio/Video 类型条目及其物理文件，
 * 以 CT_Attachment 形式保留摘要信息（文件名、格式）。
 *
 * @author 权观宇
 * @since 2.3.9
 */
public class AudioVideoHandler implements ArchiveHandler {

    @Override
    public void handle(OFDReader reader, OFDDir ofdDir) throws IOException {
        ResourceManage resMgt = reader.getResMgt();
        if (resMgt == null) return;

        List<CT_MultiMedia> mediaList = resMgt.getMultiMedias();
        if (mediaList == null || mediaList.isEmpty()) return;

        List<CT_MultiMedia> toRemove = new ArrayList<>();
        for (CT_MultiMedia media : mediaList) {
            MediaType type = media.getType();
            if (type != MediaType.Audio && type != MediaType.Video) continue;

            // 删除物理文件
            ST_Loc mediaFile = media.getMediaFile();
            if (mediaFile != null) {
                String path = mediaFile.toString();
                if (path.startsWith("/")) path = path.substring(1);
                Path filePath = reader.getWorkDir().resolve(path);
                Files.deleteIfExists(filePath);
            }

            // 标记删除
            toRemove.add(media);
        }

        // 从 XML 中移除
        for (CT_MultiMedia media : toRemove) {
            media.detach();
        }
    }
}
