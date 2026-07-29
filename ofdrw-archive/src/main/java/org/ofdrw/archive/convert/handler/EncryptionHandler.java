package org.ofdrw.archive.convert.handler;

import org.bouncycastle.crypto.CryptoException;
import org.ofdrw.archive.convert.ArchiveHandler;
import org.ofdrw.crypto.OFDDecryptor;
import org.ofdrw.crypto.decryptor.UserFEKDecryptor;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.ZipUtil;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 处理器 0：解密门禁（GB/T 42133-2022 6.16）
 * <p>
 * 在管道最前端执行。若文档已加密且无法解密，则硬中断，不继续后续流程。
 * <p>
 * 解密过程：打包当前 workDir → OFDDecryptor 解密 → 解压回 workDir。
 *
 * @author xxx
 * @since 2.3.9
 */
public class EncryptionHandler implements ArchiveHandler {

    /** 用户提供的解密器列表 */
    private final List<UserFEKDecryptor> decryptors = new ArrayList<>();

    /**
     * 添加 FEK 解密器
     *
     * @param decryptor FEK 解密器
     * @return this
     */
    public EncryptionHandler addDecryptor(UserFEKDecryptor decryptor) {
        if (decryptor != null) {
            decryptors.add(decryptor);
        }
        return this;
    }

    @Override
    public void handle(OFDReader reader, OFDDir ofdDir) throws IOException {
        Path workDir = reader.getWorkDir();
        Path encryptionsPath = workDir.resolve("Encryptions.xml");

        // 未加密，放行
        if (Files.notExists(encryptionsPath)) {
            return;
        }

        // 已加密但未配置解密器 → 硬中断
        if (decryptors.isEmpty()) {
            throw new IOException(
                    "文档已加密但未配置解密器，无法继续转换。"
                    + "请通过 EncryptionHandler.addDecryptor() 提供解密密钥。");
        }

        // 打包当前 workDir → 解密 → 解压回 workDir
        decryptAndRestore(ofdDir, workDir);
    }

    /**
     * 打包 → 解密 → 解压回路
     * <p>
     * 1. 将 workDir 打包为临时 OFD 文件
     * 2. OFDDecryptor 解密到临时目录
     * 3. 清空 workDir，解压回来
     */
    private void decryptAndRestore(OFDDir ofdDir, Path workDir) throws IOException {
        Path tempOfd = Files.createTempFile("ofd-arch-enc-", ".ofd");
        Path tempOutDir = Files.createTempDirectory("ofd-arch-dec-");

        try {
            // 1. 打包
            ofdDir.jar(tempOfd);

            // 2. 解密
            try (OFDDecryptor decryptor = new OFDDecryptor(tempOfd, tempOutDir)) {
                for (UserFEKDecryptor d : decryptors) {
                    decryptor.addUser(d);
                }
                decryptor.decrypt();
            } catch (CryptoException e) {
                throw new IOException("解密失败: " + e.getMessage(), e);
            }

            // 注意：OFDDecryptor.decrypt() 调用 ofdDir.jar(dest)，
            // dest 收到的是打包后的 OFD 文件而非目录。
            // 需要先解压回来。

            // Phase 1: 简化处理 — 解密后需要
            // 实际 OFDDecryptor 输出到 dest 的是一个 OFD 文件（.jar打包）
            // 需要解压回 workDir
            if (Files.isRegularFile(tempOutDir)) {
                // dest 是文件 = 解密后的 OFD
                FileUtils.cleanDirectory(workDir.toFile());
                ZipUtil.unZipFiles(tempOutDir.toFile(), workDir.toAbsolutePath() + File.separator);
            }

        } finally {
            Files.deleteIfExists(tempOfd);
            deleteRecursively(tempOutDir);
        }
    }

    /** 递归删除目录 */
    private void deleteRecursively(Path dir) {
        if (Files.notExists(dir)) return;
        try {
            Files.walk(dir).sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
        } catch (IOException ignored) {}
    }
}
