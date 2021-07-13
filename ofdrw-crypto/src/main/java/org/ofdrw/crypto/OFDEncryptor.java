package org.ofdrw.crypto;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.crypto.engines.SM4Engine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.jetbrains.annotations.NotNull;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.ZipUtil;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * OFD加密器
 *
 * @author 权观宇
 * @since 2021-07-13 18:10:12
 */
public class OFDEncryptor implements Closeable {


    /**
     * OFD虚拟容器对象
     */
    private final OFDDir ofdDir;

    /**
     * 工作过程中的工作目录
     * <p>
     * 用于存放解压后的OFD文档容器内容
     */
    private Path workDir;

    /**
     * 是否已经关闭
     */
    private boolean closed;

    /**
     * 随机源
     * <p>
     * 默认使用软件随机源
     */
    private SecureRandom random;

    /**
     * CBC模式分块加密，填充模式PKCS#7
     */
    private PaddedBufferedBlockCipher blockCipher;

    /**
     * 用户提供的加密器列表
     */
    private List<UserFEKEncryptor> userEncryptorList;

    private OFDEncryptor() {
    }

    public OFDEncryptor(@NotNull Path ofdFile) throws IOException {
        if (ofdFile == null || Files.notExists(ofdFile)) {
            throw new IllegalArgumentException("文件位置(ofdFile)不正确");
        }
        this.workDir = Files.createTempDirectory("ofd-tmp-");
        // 解压文档，到临时的工作目录
        ZipUtil.unZipFiles(ofdFile.toFile(), this.workDir.toAbsolutePath() + File.separator);
        this.userEncryptorList = new ArrayList<>(3);
        this.ofdDir = new OFDDir(workDir);
        this.random = new SecureRandom();
        this.blockCipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new SM4Engine()), new PKCS7Padding());
    }

    /**
     * 添加加密用户
     *
     * @param encryptor 加密用户的加密器
     * @return this
     */
    public OFDEncryptor addUser(@NotNull UserFEKEncryptor encryptor) {
        if (encryptor == null) {
            return this;
        }
        this.userEncryptorList.add(encryptor);
        return this;
    }

    /**
     * 设置随机源
     *
     * @param random 随机源
     * @return this
     */
    public OFDEncryptor setRandom(@NotNull SecureRandom random) {
        if (random == null) {
            return this;
        }
        this.random = random;
        return this;
    }

    /**
     * 关闭文档
     * <p>
     * 删除工作区
     *
     * @throws IOException 工作区删除异常
     */
    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        if (workDir != null && Files.exists(workDir)) {
            try {
                FileUtils.deleteDirectory(workDir.toFile());
            } catch (IOException e) {
                throw new IOException("无法删除Reader的工作空间，原因：" + e.getMessage(), e);
            }
        }
    }
}
