package org.ofdrw.archive.convert.handler;

import org.ofdrw.archive.convert.ArchiveHandler;
import org.ofdrw.core.basicStructure.res.CT_MultiMedia;
import org.ofdrw.core.basicStructure.res.MediaType;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.ResourceManage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 处理器 15：图像格式转换（GB/T 42133-2022 6.2.6e）
 * <p>
 * 将不在允许列表中的图像格式转为 PNG，更新 MultiMedia 的 Format 和 MediaFile 引用。
 * 允许的格式：BMP/JPEG/PNG/JBIG2/JPEG2000/TIFF。
 *
 * @author 权观宇
 * @since 2.3.9
 */
public class ImageConvertHandler implements ArchiveHandler {

    /** 允许保留的图像格式 */
    private static final Set<String> ALLOWED_FORMATS = new HashSet<>(Arrays.asList(
            "BMP", "JPEG", "PNG", "JBIG2", "JPEG2000", "TIFF"));

    /** 魔数映射 */
    private static final Map<String, byte[]> MAGIC = new LinkedHashMap<>();
    static {
        MAGIC.put("PNG", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});
        MAGIC.put("JPEG", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        MAGIC.put("BMP", new byte[]{(byte) 0x42, (byte) 0x4D});
        MAGIC.put("JBIG2", new byte[]{(byte) 0x97, 0x4A, 0x42, 0x32});
        MAGIC.put("TIFF", new byte[]{0x49, 0x49, 0x2A, 0x00});
    }

    @Override
    public void handle(OFDReader reader, OFDDir ofdDir) throws IOException {
        ResourceManage resMgt = reader.getResMgt();
        if (resMgt == null) return;

        List<CT_MultiMedia> mediaList = resMgt.getMultiMedias();
        if (mediaList == null) return;

        Path workDir = reader.getWorkDir();
        for (CT_MultiMedia media : mediaList) {
            if (media.getType() != MediaType.Image) continue;

            ST_Loc mediaFile = media.getMediaFile();
            if (mediaFile == null) continue;

            String path = mediaFile.toString();
            if (path.startsWith("/")) path = path.substring(1);
            Path imgPath = workDir.resolve(path);
            if (!Files.exists(imgPath)) continue;

            // 检测格式
            String format = detectFormat(imgPath);
            if (format == null || ALLOWED_FORMATS.contains(format.toUpperCase())) {
                continue;  // 格式允许，跳过
            }

            // 尝试转为 PNG
            try (InputStream in = Files.newInputStream(imgPath)) {
                BufferedImage image = ImageIO.read(in);
                if (image == null) continue;  // 无法解码，跳过

                // 写为 PNG
                Path pngPath = imgPath.resolveSibling(
                        imgPath.getFileName().toString().replaceFirst("\\.[^.]+$", "") + ".png");
                ImageIO.write(image, "PNG", pngPath.toFile());

                // 删除原文件
                Files.deleteIfExists(imgPath);

                // 更新 MediaFile 引用
                ST_Loc newLoc = ST_Loc.getInstance(
                        mediaFile.parent() + "/" + pngPath.getFileName().toString());
                media.setMediaFile(newLoc);
                media.setFormat("PNG");
            }
        }
    }

    /** 魔数检测格式 */
    private String detectFormat(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            byte[] header = new byte[12];
            int read = in.read(header);
            for (Map.Entry<String, byte[]> e : MAGIC.entrySet()) {
                if (read >= e.getValue().length && matches(header, e.getValue())) {
                    return e.getKey();
                }
            }
        } catch (IOException ignored) {}
        return null;
    }

    private boolean matches(byte[] header, byte[] magic) {
        for (int i = 0; i < magic.length; i++) {
            if (header[i] != magic[i]) return false;
        }
        return true;
    }
}
