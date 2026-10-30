package org.ofdrw.converter.pdf.render;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.ofdrw.converter.pdf.font.ImageEncoding;
import org.ofdrw.core.basicStructure.res.CT_MultiMedia;
import org.ofdrw.core.basicStructure.res.MediaType;
import org.ofdrw.core.basicStructure.res.resources.MultiMedias;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.ResourceLocator;
import org.ofdrw.reader.ResourceManage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 图像资源加载器：按资源 ID 取得图像的原始字节与格式（决策 23：保真优先）
 * <p>
 * 之所以直接读取原始字节而不是 {@code ResourceManage#getImage(String)}（会经过 ImageIO 解码）：
 * JPEG 必须原样嵌入、不做重编码，一旦被解码成 {@code BufferedImage} 再压缩就会引入二次损失。
 * <p>
 * 资源查找顺序：文档级资源（DocumentRes）→ 公共资源（PublicRes）→ 资源管理器已索引的全部多媒体对象。
 * 文件定位复用 {@link ResourceLocator}，因此相对路径与大小写不规范的路径都能按 OFD 目录规则解析。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class ImageResourceLoader implements ImageSource {

    /**
     * 日志
     */
    private static final Logger logger = LoggerFactory.getLogger(ImageResourceLoader.class);

    /**
     * JBIG2 文件扩展名（需要解码为位图才能嵌入，见 {@link #loadBitmapBytes}）
     */
    private static final String JBIG2_EXT = ".jb2";

    /**
     * GBIG2 文件扩展名（部分工具生成的 JBIG2 命名）
     */
    private static final String GBIG2_EXT = ".gbig2";

    /**
     * OFD 读取器
     */
    private final OFDReader reader;

    /**
     * 文档级与公共资源中的多媒体对象（ID → 多媒体对象）
     */
    private final Map<String, CT_MultiMedia> mediaMap = new ConcurrentHashMap<>();

    /**
     * 已告警的资源 ID，避免同一缺失资源整页重复刷屏
     */
    private final Map<String, Boolean> reported = new ConcurrentHashMap<>();

    /**
     * 构造图像资源加载器
     *
     * @param reader OFD 读取器，不能为空
     */
    public ImageResourceLoader(OFDReader reader) {
        if (reader == null) {
            throw new IllegalArgumentException("OFD 读取器不能为空");
        }
        this.reader = reader;
        indexResources();
    }

    /**
     * 加载图像资源的原始数据
     * <p>
     * JPEG 与 PNG 等格式返回文件原始字节（不做任何重编码）；返回的编码由资源声明的格式决定。
     *
     * @param resourceId 图像资源 ID（{@code ST_RefID} 的字符串形式）
     * @return 原始数据；资源缺失、不是图片或读取失败时返回 {@code null}（已输出 warning）
     */
    @Override
    public ImageData load(String resourceId) {
        CT_MultiMedia media = find(resourceId);
        if (media == null) {
            warnOnce(resourceId, "图像资源不存在：ID=" + resourceId);
            return null;
        }
        if (media.getType() != null && media.getType() != MediaType.Image) {
            warnOnce(resourceId, "多媒体资源不是图片，已跳过：ID=" + resourceId + "，类型=" + media.getType());
            return null;
        }
        ST_Loc loc = media.getMediaFile();
        if (loc == null || loc.getFileName() == null) {
            warnOnce(resourceId, "图像资源缺少文件定位信息，已跳过：ID=" + resourceId);
            return null;
        }
        try {
            byte[] data = readFile(loc);
            if (data == null || data.length == 0) {
                warnOnce(resourceId, "图像资源文件为空，已跳过：ID=" + resourceId + "，文件=" + loc);
                return null;
            }
            int[] size = readPictureSize(data);
            if (size == null) {
                warnOnce(resourceId, "无法识别图像尺寸，已跳过：ID=" + resourceId + "，文件=" + loc);
                return null;
            }
            ImageEncoding encoding = detectEncoding(media.getFormat(), loc.getFileName());
            logger.debug("图像资源 {} → {}（{}x{}，{}）", resourceId, loc.getFileName(), size[0], size[1], encoding);
            return new ImageData(resourceId, encoding, size[0], size[1], data);
        } catch (IOException | RuntimeException e) {
            warnOnce(resourceId, "图像资源读取失败，已跳过：ID=" + resourceId + "，原因：" + e.getMessage());
            return null;
        }
    }

    /**
     * 读取资源文件原始字节
     *
     * @param loc 资源定位，不能为空
     * @return 文件字节；文件不存在时返回 {@code null}
     * @throws IOException 读取失败
     */
    private byte[] readFile(ST_Loc loc) throws IOException {
        final ResourceLocator rl = reader.getResourceLocator();
        rl.save();
        try {
            Path path = rl.getFile(loc);
            if (path == null || !Files.isReadable(path)) {
                return null;
            }
            // JDK 8 兼容：InputStream.readAllBytes() 是 JDK 9 新增 API，此处必须用 Files.readAllBytes
            return Files.readAllBytes(path);
        } finally {
            rl.restore();
        }
    }

    /**
     * 在资源集合中查找多媒体对象
     *
     * @param resourceId 资源 ID
     * @return 多媒体对象；不存在返回 {@code null}
     */
    private CT_MultiMedia find(String resourceId) {
        if (resourceId == null || resourceId.trim().isEmpty()) {
            return null;
        }
        CT_MultiMedia media = mediaMap.get(resourceId.trim());
        if (media != null) {
            return media;
        }
        ResourceManage resMgt = reader.getResMgt();
        return resMgt == null ? null : resMgt.getMultiMedia(resourceId.trim());
    }

    /**
     * 建立资源索引：文档级资源、公共资源与资源管理器已索引的全部多媒体对象
     */
    private void indexResources() {
        try {
            for (CT_MultiMedia media : loadDocumentResources()) {
                index(media);
            }
        } catch (RuntimeException e) {
            logger.debug("[{}] 文档资源索引异常，已回退到资源管理器：{}", RenderContext.DIAGNOSTIC_TAG, e.getMessage());
        }
        try {
            ResourceManage resMgt = reader.getResMgt();
            List<CT_MultiMedia> all = resMgt == null ? null : resMgt.getMultiMedias();
            if (all != null) {
                for (CT_MultiMedia media : all) {
                    index(media);
                }
            }
        } catch (RuntimeException e) {
            logger.debug("[{}] 资源管理器多媒体索引异常：{}", RenderContext.DIAGNOSTIC_TAG, e.getMessage());
        }
    }

    /**
     * 把多媒体对象放入索引
     *
     * @param media 多媒体对象，可为空
     */
    private void index(CT_MultiMedia media) {
        if (media == null || media.getID() == null) {
            return;
        }
        mediaMap.putIfAbsent(media.getID().toString(), media);
    }

    /**
     * 读取文档级与公共资源中的多媒体对象
     * <p>
     * 资源文件路径以文档根目录（DocRoot）为基准：{@code DocRoot.cat(PublicRes/DocumentRes)}，
     * 媒体文件路径以资源文件所在目录为基准；全部转为绝对路径后再交给资源定位器读取，
     * 避免相对路径随定位器当前工作目录漂移（与 {@code ResourceManage} 的做法一致）。
     *
     * @return 多媒体对象列表（可能为空）
     */
    private List<CT_MultiMedia> loadDocumentResources() {
        final ResourceLocator rl = reader.getResourceLocator();
        rl.save();
        try {
            org.ofdrw.core.basicStructure.doc.Document document = reader.cdDefaultDoc();
            if (document == null || document.getCommonData() == null) {
                return java.util.Collections.emptyList();
            }
            org.ofdrw.core.basicStructure.ofd.DocBody docBody = reader.getOFDDir().getOfd().getDocBody(0);
            ST_Loc docRoot = docBody.getDocRoot();
            List<CT_MultiMedia> result = new java.util.ArrayList<>();
            for (ST_Loc publicRes : document.getCommonData().getPublicResList()) {
                collectResources(rl, docRoot, publicRes, result);
            }
            for (ST_Loc documentRes : document.getCommonData().getDocumentResList()) {
                collectResources(rl, docRoot, documentRes, result);
            }
            return result;
        } catch (IOException | org.dom4j.DocumentException | RuntimeException e) {
            // 资源索引只是优化：失败后仍会回退到资源管理器查找，属于内部细节，用 debug 记录
            logger.debug("[{}] 文档资源索引失败，已回退到资源管理器：{}", RenderContext.DIAGNOSTIC_TAG, e.getMessage());
            return java.util.Collections.emptyList();
        } finally {
            rl.restore();
        }
    }

    /**
     * 从一个资源描述文件中收集多媒体对象（媒体文件路径转为绝对路径）
     *
     * @param rl        资源定位器
     * @param docRoot   文档根目录
     * @param loc       资源描述文件定位
     * @param result    收集结果
     */
    private void collectResources(ResourceLocator rl, ST_Loc docRoot, ST_Loc loc, List<CT_MultiMedia> result) {
        if (loc == null || docRoot == null) {
            return;
        }
        try {
            ST_Loc absRes = docRoot.cat(loc);
            org.ofdrw.core.basicStructure.res.Res res = rl.get(absRes.getLoc(), org.ofdrw.core.basicStructure.res.Res::new);
            if (res == null) {
                return;
            }
            ST_Loc baseLoc = res.getBaseLoc() == null ? absRes.parentLoc() : absRes.parentLoc().cat(res.getBaseLoc());
            List<org.ofdrw.core.basicStructure.res.OFDResource> resources = res.getResources();
            if (resources == null) {
                return;
            }
            for (org.ofdrw.core.basicStructure.res.OFDResource resource : resources) {
                if (!(resource instanceof MultiMedias)) {
                    continue;
                }
                for (CT_MultiMedia media : ((MultiMedias) resource).getMultiMedias()) {
                    if (media == null) {
                        continue;
                    }
                    if (media.getMediaFile() != null) {
                        media.setMediaFile(baseLoc.cat(media.getMediaFile()));
                    }
                    result.add(media);
                }
            }
        } catch (IOException | org.dom4j.DocumentException | RuntimeException e) {
            // 单个资源描述文件读不到不影响其它资源文件，且资源管理器索引会兜底，因此只记 debug
            logger.debug("[{}] 资源描述文件读取失败，已跳过：{}，原因：{}", RenderContext.DIAGNOSTIC_TAG, loc, e.getMessage());
        }
    }

    /**
     * 识别图像格式
     *
     * @param format   资源声明的格式，可为空
     * @param fileName 文件名，可为空
     * @return 编码；无法识别时按 {@link ImageEncoding#RAW} 处理
     */
    static ImageEncoding detectEncoding(String format, String fileName) {
        ImageEncoding encoding = ImageEncoding.ofFormat(format);
        if (encoding != ImageEncoding.RAW) {
            return encoding;
        }
        String name = fileName == null ? "" : fileName.toLowerCase(java.util.Locale.ROOT);
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return ImageEncoding.JPEG;
        }
        if (name.endsWith(".png")) {
            return ImageEncoding.PNG;
        }
        if (name.endsWith(".jpx") || name.endsWith(".jp2")) {
            return ImageEncoding.JPEG2000;
        }
        if (name.endsWith(JBIG2_EXT) || name.endsWith(GBIG2_EXT)) {
            return ImageEncoding.JBIG2;
        }
        return ImageEncoding.RAW;
    }

    /**
     * 读取图像像素尺寸（不完整解码，只解析文件头）
     *
     * @param data 图像原始字节
     * @return {@code [width, height]}；无法识别返回 {@code null}
     */
    static int[] readPictureSize(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        int[] size = readPngSize(data);
        if (size == null) {
            size = readJpegSize(data);
        }
        if (size == null) {
            size = readJpeg2000Size(data);
        }
        if (size == null) {
            size = readGifSize(data);
        }
        if (size == null) {
            size = readBmpSize(data);
        }
        if (size != null && size[0] > 0 && size[1] > 0) {
            return size;
        }
        logger.debug("[{}] 未识别的图像头，已跳过尺寸解析", RenderContext.DIAGNOSTIC_TAG);
        return null;
    }

    /**
     * 解析 PNG 的 IHDR 尺寸
     *
     * @param data 图像字节
     * @return 尺寸或 {@code null}
     */
    private static int[] readPngSize(byte[] data) {
        if (data.length < 24) {
            return null;
        }
        if ((data[0] & 0xFF) != 0x89 || data[1] != 'P' || data[2] != 'N' || data[3] != 'G') {
            return null;
        }
        int width = readInt(data, 16);
        int height = readInt(data, 20);
        return new int[]{width, height};
    }

    /**
     * 解析 JPEG 的 SOF 段尺寸
     *
     * @param data 图像字节
     * @return 尺寸或 {@code null}
     */
    static int[] readJpegSize(byte[] data) {
        if (data.length < 4 || (data[0] & 0xFF) != 0xFF || (data[1] & 0xFF) != 0xD8) {
            return null;
        }
        int pos = 2;
        while (pos + 9 < data.length) {
            if ((data[pos] & 0xFF) != 0xFF) {
                pos++;
                continue;
            }
            int marker = data[pos + 1] & 0xFF;
            if (marker == 0xD8 || marker == 0x01 || (marker >= 0xD0 && marker <= 0xD7)) {
                pos += 2;
                continue;
            }
            int length = ((data[pos + 2] & 0xFF) << 8) | (data[pos + 3] & 0xFF);
            // SOF0~SOF15（除 DHT=C4、JPG=C8、DAC=CC）
            if (marker >= 0xC0 && marker <= 0xCF && marker != 0xC4 && marker != 0xC8 && marker != 0xCC) {
                int height = ((data[pos + 5] & 0xFF) << 8) | (data[pos + 6] & 0xFF);
                int width = ((data[pos + 7] & 0xFF) << 8) | (data[pos + 8] & 0xFF);
                return new int[]{width, height};
            }
            if (length < 2) {
                return null;
            }
            pos += 2 + length;
        }
        return null;
    }

    /**
     * 解析 JPEG2000（JP2 容器）的尺寸
     *
     * @param data 图像字节
     * @return 尺寸或 {@code null}
     */
    private static int[] readJpeg2000Size(byte[] data) {
        if (data.length < 45) {
            return null;
        }
        for (int i = 4; i + 12 < data.length && i < 200; i++) {
            if ((data[i] & 0xFF) == 'i' && data[i + 1] == 'h' && data[i + 2] == 'd' && data[i + 3] == 'r') {
                int height = readInt(data, i + 8);
                int width = readInt(data, i + 12);
                if (width > 0 && height > 0) {
                    return new int[]{width, height};
                }
            }
        }
        return null;
    }

    /**
     * 解析 GIF 尺寸
     *
     * @param data 图像字节
     * @return 尺寸或 {@code null}
     */
    private static int[] readGifSize(byte[] data) {
        if (data.length < 10 || data[0] != 'G' || data[1] != 'I' || data[2] != 'F') {
            return null;
        }
        int width = (data[6] & 0xFF) | ((data[7] & 0xFF) << 8);
        int height = (data[8] & 0xFF) | ((data[9] & 0xFF) << 8);
        return new int[]{width, height};
    }

    /**
     * 解析 BMP 尺寸
     *
     * @param data 图像字节
     * @return 尺寸或 {@code null}
     */
    private static int[] readBmpSize(byte[] data) {
        if (data.length < 26 || data[0] != 'B' || data[1] != 'M') {
            return null;
        }
        int width = Math.abs(readIntLe(data, 18));
        int height = Math.abs(readIntLe(data, 22));
        return new int[]{width, height};
    }

    /**
     * 大端读取 4 字节整数
     *
     * @param data 字节数组
     * @param pos  起始下标
     * @return 整数值
     */
    private static int readInt(byte[] data, int pos) {
        return ((data[pos] & 0xFF) << 24) | ((data[pos + 1] & 0xFF) << 16)
                | ((data[pos + 2] & 0xFF) << 8) | (data[pos + 3] & 0xFF);
    }

    /**
     * 小端读取 4 字节整数
     *
     * @param data 字节数组
     * @param pos  起始下标
     * @return 整数值
     */
    private static int readIntLe(byte[] data, int pos) {
        return (data[pos] & 0xFF) | ((data[pos + 1] & 0xFF) << 8)
                | ((data[pos + 2] & 0xFF) << 16) | ((data[pos + 3] & 0xFF) << 24);
    }

    /**
     * 输出一次告警（同一资源只告警一次）
     *
     * @param resourceId 资源 ID
     * @param message    告警内容
     */
    private void warnOnce(String resourceId, String message) {
        String key = String.valueOf(resourceId);
        if (reported.putIfAbsent(key, Boolean.TRUE) == null) {
            logger.warn("[{}] {}", RenderContext.DIAGNOSTIC_TAG, message);
        }
    }

    /**
     * 图像原始数据
     * <p>
     * {@link #getData()} 为文件原始字节，JPEG 直接嵌入时**不得**再次编码（决策 23）。
     *
     * @author 权观宇
     * @since 2.5.1
     */
    public static final class ImageData {

        /**
         * 资源 ID
         */
        private final String resourceId;

        /**
         * 图像格式
         */
        private final ImageEncoding encoding;

        /**
         * 像素宽度
         */
        private final int width;

        /**
         * 像素高度
         */
        private final int height;

        /**
         * 原始字节
         */
        private final byte[] data;

        /**
         * 构造图像数据
         *
         * @param resourceId 资源 ID
         * @param encoding   图像格式
         * @param width      像素宽度
         * @param height     像素高度
         * @param data       原始字节
         */
        ImageData(String resourceId, ImageEncoding encoding, int width, int height, byte[] data) {
            this.resourceId = resourceId;
            this.encoding = encoding;
            this.width = width;
            this.height = height;
            this.data = data;
        }

        /**
         * @return 资源 ID
         */
        public String getResourceId() {
            return resourceId;
        }

        /**
         * @return 图像格式
         */
        public ImageEncoding getEncoding() {
            return encoding;
        }

        /**
         * @return 像素宽度
         */
        public int getWidth() {
            return width;
        }

        /**
         * @return 像素高度
         */
        public int getHeight() {
            return height;
        }

        /**
         * @return 原始字节副本
         */
        public byte[] getData() {
            return data.clone();
        }

        /**
         * @return 是否为可直接原样嵌入的 JPEG
         */
        public boolean isRawJpeg() {
            return encoding == ImageEncoding.JPEG && ImageResourceLoader.isJpeg(data);
        }
    }

    /**
     * 构造图像数据（供测试与外部加载实现复用）
     *
     * @param resourceId 资源 ID
     * @param encoding   图像格式
     * @param width      像素宽度
     * @param height     像素高度
     * @param data       原始字节
     * @return 图像数据
     */
    public static ImageData imageData(String resourceId, ImageEncoding encoding, int width, int height, byte[] data) {
        return new ImageData(resourceId, encoding, width, height, data);
    }

    /**
     * 判断字节是否为 JPEG
     *
     * @param data 图像字节，可为空
     * @return {@code true} 表示 JPEG
     */
    static boolean isJpeg(byte[] data) {
        return data != null && data.length > 3 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8;
    }
}
