package org.ofdrw.archive.check.rule;

import org.ofdrw.archive.check.ArchiveRule;
import org.ofdrw.archive.check.ArchiveViolation;
import org.ofdrw.core.basicStructure.res.CT_MultiMedia;
import org.ofdrw.core.basicStructure.res.MediaType;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.ResourceManage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 规则 10：图像格式仅限 BMP/JPEG/PNG/JBIG2/JPEG2000/TIFF（GB/T 42133-2022 6.2.6e）
 * <p>
 * 使用文件魔数检测实际格式，不信任 Format 属性。
 *
 * @author 权观宇
 * @since 2.3.9
 */
public class ImageFormatRule implements ArchiveRule {
    public static final String RULE_NAME = "IMAGE_FORMAT";

    /** 允许的图像格式 */
    private static final Set<String> ALLOWED_FORMATS = new HashSet<>(Arrays.asList(
            "BMP", "JPEG", "PNG", "JBIG2", "JPEG2000", "TIFF"));

    /** 图像格式魔数映射 */
    private static final Map<String, byte[]> MAGIC_BYTES = new LinkedHashMap<>();

    static {
        MAGIC_BYTES.put("PNG", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});
        MAGIC_BYTES.put("JPEG", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        MAGIC_BYTES.put("BMP", new byte[]{(byte) 0x42, (byte) 0x4D});
        MAGIC_BYTES.put("JBIG2", new byte[]{(byte) 0x97, 0x4A, 0x42, 0x32});
        MAGIC_BYTES.put("TIFF_LE", new byte[]{0x49, 0x49, 0x2A, 0x00});
        MAGIC_BYTES.put("TIFF_BE", new byte[]{0x4D, 0x4D, 0x00, 0x2A});
    }

    @Override
    public List<ArchiveViolation> check(OFDReader reader, OFDDir ofdDir) {
        List<ArchiveViolation> violations = new ArrayList<>();
        ResourceManage resMgt = reader.getResMgt();
        if (resMgt == null) return violations;

        try {
            List<CT_MultiMedia> mediaList = resMgt.getMultiMedias();
            if (mediaList != null) {
                for (CT_MultiMedia media : mediaList) {
                    // 只检查图像类型
                    if (media.getType() != MediaType.Image) continue;

                    ST_Loc mediaFile = media.getMediaFile();
                    if (mediaFile == null) continue;

                    // 解析文件路径
                    String path = mediaFile.toString();
                    if (path.startsWith("/")) path = path.substring(1);
                    Path filePath = reader.getWorkDir().resolve(path);

                    if (!Files.exists(filePath)) {
                        violations.add(new ArchiveViolation(
                                RULE_NAME, ArchiveViolation.Severity.ERROR,
                                "图像文件不存在: " + path,
                                mediaFile, "不存在", "BMP/JPEG/PNG/JBIG2/JPEG2000/TIFF"));
                        continue;
                    }

                    String detectedFormat = detectFormat(filePath);
                    if (detectedFormat == null) {
                        violations.add(new ArchiveViolation(
                                RULE_NAME, ArchiveViolation.Severity.WARN,
                                "无法识别图像格式 (魔数不匹配): " + path,
                                mediaFile, "未知", "BMP/JPEG/PNG/JBIG2/JPEG2000/TIFF"));
                    } else if (!ALLOWED_FORMATS.contains(detectedFormat)) {
                        violations.add(new ArchiveViolation(
                                RULE_NAME, ArchiveViolation.Severity.ERROR,
                                "图像格式不在允许范围内: " + detectedFormat,
                                mediaFile, detectedFormat, "BMP/JPEG/PNG/JBIG2/JPEG2000/TIFF"));
                    }
                }
            }
        } catch (Exception e) {
            violations.add(new ArchiveViolation(
                    RULE_NAME, ArchiveViolation.Severity.ERROR,
                    "检查图像格式时异常: " + e.getMessage(), null, null, null));
        }
        return violations;
    }

    /**
     * 通过文件魔数检测图像格式
     *
     * @param filePath 图像文件路径
     * @return 检测到的格式名（如 "PNG"、"JPEG"），无法检测时返回 null
     */
    private String detectFormat(Path filePath) {
        try (InputStream in = Files.newInputStream(filePath)) {
            byte[] header = new byte[12];
            int read = in.read(header);
            if (read < 4) return null;

            for (Map.Entry<String, byte[]> entry : MAGIC_BYTES.entrySet()) {
                byte[] magic = entry.getValue();
                if (read >= magic.length && matches(header, magic)) {
                    String fmt = entry.getKey();
                    if (fmt.startsWith("TIFF")) return "TIFF";
                    return fmt;
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    /** 检查头部字节是否匹配魔数 */
    private boolean matches(byte[] header, byte[] magic) {
        for (int i = 0; i < magic.length; i++) {
            if (header[i] != magic[i]) return false;
        }
        return true;
    }
}
