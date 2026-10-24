package org.ofdrw.reader;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * ZIP 文件解压工具
 */
public class ZipUtil {
    /**
     * 设置 解压许可最大字节数
     *
     * @param size 压缩文件解压最大大小,默认值： 100M
     * @deprecated 采用apache compress 默认策略
     */
    @Deprecated
    public static void setMaxSize(long size) {
    }

    /**
     * 设置解压默认字符集
     * <p>
     * 用于解决中文字符集乱码问题
     *
     * @param charset 字符集，如 GBK、UTF8 等
     */
    public static void setDefaultCharset(String charset) {
        ZipUtil.charset = charset;
    }

    /**
     * 默认字符集：GBK
     * 若ZIP压缩包中有默认字符集，则以压缩包中的字符集为准
     */
    private static String charset = "UTF-8";

    /**
     * ZIP 本地文件头（local file header）签名：PK\003\004
     */
    private static final byte[] LOCAL_FILE_HEADER_SIGNATURE = {'P', 'K', 0x03, 0x04};

    /**
     * 解压到指定目录
     *
     * @param zipPath 需要解压的文件路径
     * @param descDir 解压到目录
     * @throws IOException 文件操作IO异常
     */
    public static void unZipFiles(String zipPath, String descDir) throws IOException {
        unZipFiles(new File(zipPath), descDir);
    }

    /**
     * 解压文件到指定目录
     *
     * @param src     压缩文件流
     * @param descDir 解压到目录
     * @throws IOException 文件操作IO异常
     */
    public static void unZipFiles(InputStream src, String descDir) throws IOException {
        unZipFileByApacheCommonCompress(src, descDir);
    }

    /**
     * 解压文件到指定目录
     *
     * @param zipFile 需要解压的文件
     * @param descDir 解压到目录
     * @throws IOException 文件操作IO异常
     */
    public static void unZipFiles(File zipFile, String descDir) throws IOException {
        unZipFileByApacheCommonCompress(zipFile, descDir);
    }

    /**
     * 使用apache common compress库 解压zipFile，能支持更多zip包解压的特性
     * <p>
     * 对带自解压（SFX）前缀的压缩包：ZIP 数据以本地文件头（PK\003\004）为起点，
     * 前缀部分可能本身含有一条空的 ZIP 结尾记录（EOCD），流式读取会在该处提前终止，
     * 因此这里先定位第一个本地文件头，再从其偏移位置开始流式解压。
     *
     * @param srcFile 带解压的源文件
     * @param descDir 解压到目录
     * @throws IOException IO异常
     */
    public static void unZipFileByApacheCommonCompress(File srcFile, String descDir) throws IOException {
        if (srcFile == null || srcFile.exists() == false) {
            throw new IOException("解压文件不存在: " + srcFile);
        }
        final long dataStart = skipNonZipPrefix(srcFile);
        try (FileInputStream fin = new FileInputStream(srcFile)) {
            skipFully(fin, dataStart);
            unzipStream(fin, descDir);
        }
    }

    /**
     * apache common compress库 解压zipFile
     *
     * @param src     带解压的源文件流，流由调用者负责关闭
     * @param descDir 解压到目录
     * @throws IOException IO异常
     */
    public static void unZipFileByApacheCommonCompress(InputStream src, String descDir) throws IOException {
        if (!(src instanceof FileInputStream)) {
            // 非文件流一次性读取：定位第一个 ZIP 本地文件头，跳过其前面的自解压前缀
            final byte[] bytes = org.apache.commons.io.IOUtils.toByteArray(src);
            final int start = findDataStartInBytes(bytes);
            unzipStream(new ByteArrayInputStream(bytes, start, bytes.length - start), descDir);
            return;
        }
        /*
         * 文件流随机访问定位前缀偏移，再复位到该偏移流式解压。
         * 注意不能关闭 FileInputStream 的通道（该流由调用者负责关闭）。
         */
        final FileInputStream fin = (FileInputStream) src;
        final FileChannel channel = fin.getChannel();
        final long dataStart;
        if (channel != null && channel.isOpen()) {
            final long origin = channel.position();
            dataStart = skipNonZipPrefix(channel);
            channel.position(origin);
        } else {
            // 通道已关闭（无随机访问能力），退化为一次性读取
            final byte[] bytes = org.apache.commons.io.IOUtils.toByteArray(fin);
            final int start = findDataStartInBytes(bytes);
            unzipStream(new ByteArrayInputStream(bytes, start, bytes.length - start), descDir);
            return;
        }
        skipFully(fin, dataStart);
        unzipStream(fin, descDir);
    }

    /**
     * 在内存字节中定位 ZIP 数据起点
     *
     * @param bytes 完整字节内容
     * @return ZIP 数据起点偏移，单位：字节；未找到本地文件头时返回 0
     */
    private static int findDataStartInBytes(byte[] bytes) {
        final int idx = indexOfLocalFileHeader(bytes, bytes.length);
        return idx < 0 ? 0 : idx;
    }

    /**
     * 流式解压 ZIP 数据到指定目录
     *
     * @param src     已定位到 ZIP 数据起点的输入流
     * @param descDir 解压到目录
     * @throws IOException IO异常
     */
    private static void unzipStream(InputStream src, String descDir) throws IOException {
        Path pathFile = Files.createDirectories(Paths.get(descDir));

        try (ZipArchiveInputStream zipFile = new ZipArchiveInputStream(src, charset, false, true)) {
            ZipArchiveEntry entry = null;

            while ((entry = zipFile.getNextEntry()) != null) {
                //校验路径合法性
                Path f = null;
                try {
                    f = pathFile.resolve(entry.getName());
                } catch (InvalidPathException e) {
                    // 尝试使用GBK解析
                    f = pathFile.resolve(new String(entry.getRawName(), "GBK"));
                }

                if (f == null || f.startsWith(pathFile) == false) {
                    throw new IOException(String.format("不合法的路径：%s", f));
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(f);
                } else {
                    Files.createDirectories(f.getParent());
                    try (OutputStream o = Files.newOutputStream(f)) {
                        org.apache.commons.io.IOUtils.copy(zipFile, o);
                    }
                }
            }
        } catch (EOFException e) {
            //捕获异常的文件结尾，部份OFD压缩包会报此错误，忽略异常结尾以期望可以继续处理该OFD文件
        }
    }

    /**
     * 定位 ZIP 数据起点：文件中第一个本地文件头（PK\003\004）的偏移。
     * <p>
     * 普通 ZIP（以 PK\003\004 开头）返回 0，走原有的流式解压路径；
     * 带自解压前缀的 ZIP 返回前缀长度，从该处开始解压。
     *
     * @param file 待解压的文件
     * @return ZIP 数据起点在文件中的偏移，单位：字节
     * @throws IOException IO异常
     */
    private static long skipNonZipPrefix(File file) throws IOException {
        try (FileInputStream fin = new FileInputStream(file)) {
            return skipNonZipPrefix(fin.getChannel());
        }
    }

    /**
     * 定位 ZIP 数据起点：通道中第一个本地文件头（PK\003\004）的偏移。
     * <p>
     * 不移动通道位置，由调用方负责定位。
     *
     * @param channel 可随机访问的文件通道
     * @return ZIP 数据起点在通道中的偏移，单位：字节；未找到本地文件头时返回 0（交由流式解析处理）
     * @throws IOException IO异常
     */
    private static long skipNonZipPrefix(FileChannel channel) throws IOException {
        final long origin = channel.position();
        try {
            // 读取窗口：按块增量定位，命中前不读取整个压缩包
            final int window = 65536;
            final int bufferSize = Math.max(4, Math.min(window, (int) Math.min(Integer.MAX_VALUE, channel.size() - origin)));
            final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
            long base = origin;
            int carry = 0;
            while (true) {
                buffer.clear();
                buffer.position(carry);
                final int read = channel.read(buffer, base);
                if (read <= 0) {
                    return 0;
                }
                final int filled = carry + read;
                final int idx = indexOfLocalFileHeader(buffer.array(), filled);
                if (idx >= 0) {
                    return base - carry + idx;
                }
                // 保留末尾 3 字节，避免签名跨越两次读取的边界
                carry = Math.min(3, filled);
                final byte[] keep = new byte[carry];
                System.arraycopy(buffer.array(), filled - carry, keep, 0, carry);
                System.arraycopy(keep, 0, buffer.array(), 0, carry);
                base += read;
            }
        } finally {
            channel.position(origin);
        }
    }

    /**
     * 在字节数组中查找第一个 ZIP 本地文件头（PK\003\004）的位置
     *
     * @param buf   字节数组
     * @param limit 有效数据长度，单位：字节
     * @return 本地文件头的偏移；未找到返回 -1
     */
    private static int indexOfLocalFileHeader(byte[] buf, int limit) {
        final int end = limit - LOCAL_FILE_HEADER_SIGNATURE.length;
        for (int i = 0; i <= end; i++) {
            if (buf[i] == LOCAL_FILE_HEADER_SIGNATURE[0]
                    && buf[i + 1] == LOCAL_FILE_HEADER_SIGNATURE[1]
                    && buf[i + 2] == LOCAL_FILE_HEADER_SIGNATURE[2]
                    && buf[i + 3] == LOCAL_FILE_HEADER_SIGNATURE[3]) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 跳过输入流中的指定字节数
     *
     * @param in     输入流
     * @param length 需要跳过的字节数，单位：字节
     * @throws IOException IO异常
     */
    private static void skipFully(InputStream in, long length) throws IOException {
        long remaining = length;
        while (remaining > 0) {
            final long skipped = in.skip(remaining);
            if (skipped > 0) {
                remaining -= skipped;
            } else if (in.read() == -1) {
                throw new EOFException("跳过 ZIP 前缀失败：文件长度不足");
            } else {
                remaining--;
            }
        }
    }
}
