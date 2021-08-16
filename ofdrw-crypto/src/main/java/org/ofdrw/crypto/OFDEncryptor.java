package org.ofdrw.crypto;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.SM4Engine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.dom4j.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ofdrw.core.crypto.ProtectionCaseID;
import org.ofdrw.core.crypto.encryt.*;
import org.ofdrw.core.signatures.sig.Parameters;
import org.ofdrw.crypto.enryptor.UserFEKEncryptor;
import org.ofdrw.gv.GlobalVar;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.pkg.tool.ElemCup;
import org.ofdrw.reader.ZipUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OFD加密器
 *
 * @author 权观宇
 * @since 2021-07-13 18:10:12
 */
public class OFDEncryptor implements Closeable {


    /**
     * OFD虚拟容器根目录
     */
    private final OFDDir ofdDir;

    /**
     * 加密后文件输出位置
     */
    private Path dest;

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

    /**
     * 容器文件过滤器
     * <p>
     * 用于判断文件是否需要被加密，返回false表示不需要加密
     */
    private ContainerFileFilter cfFilter;

    /**
     * 加密操作的附加描述集合
     */
    private Parameters parameters;

    private OFDEncryptor() {
        this.ofdDir = null;
    }

    /**
     * 创建OFD加密器
     *
     * @param ofdFile 待加密的OFD文件路径
     * @param dest    加密后的OFD路径
     * @throws IOException IO操作异常
     */
    public OFDEncryptor(@NotNull Path ofdFile, @NotNull Path dest) throws IOException {
        if (ofdFile == null || Files.notExists(ofdFile)) {
            throw new IllegalArgumentException("文件位置(ofdFile)不正确");
        }
        if (dest == null) {
            throw new IllegalArgumentException("加密后文件位置(out)为空");
        }
        this.dest = dest;
        this.workDir = Files.createTempDirectory("ofd-tmp-");
        // 解压文档，到临时的工作目录
        ZipUtil.unZipFiles(ofdFile.toFile(), this.workDir.toAbsolutePath() + File.separator);
        this.userEncryptorList = new ArrayList<>(3);
        this.ofdDir = new OFDDir(workDir.toAbsolutePath());
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
     * 设置 容器文件过滤器
     * <p>
     * 该过滤器用于决定哪些文件将会被加密
     * <p>
     * 过滤器结果为false 那么该文件将不会被加密
     *
     * @param filter 过滤器
     * @return this
     */
    public OFDEncryptor setContainerFileFilter(ContainerFileFilter filter) {
        if (filter == null) {
            return this;
        }
        this.cfFilter = filter;
        return this;
    }

    /**
     * 执行加密
     *
     * @return this
     * @throws IOException              加密
     * @throws CryptoException          加密异常
     * @throws GeneralSecurityException 证书解析异常
     */
    public OFDEncryptor encrypt() throws IOException, CryptoException, GeneralSecurityException {
        if (this.userEncryptorList.isEmpty()) {
            throw new IllegalArgumentException("没有可用加密用户(UserFEKEncryptor)");
        }
        if (!Files.exists(this.dest)) {
            Files.createDirectories(this.dest.getParent());
        }
        // 获取 解密入口文件对象。
        final Encryptions encryptions = this.ofdDir.obtainEncryptions();
        // 获取新的加密标识符：内最大 加密标识符 + 1
        final String id = Integer.toString(encryptions.maxID() + 1);

        // 待加密文件列表
        final List<ContainerPath> tbEncArr = this.getToBeEncFiles();
        // 加密块大小
        int blockSize = blockCipher.getBlockSize();
        byte[] fek = new byte[blockSize];
        byte[] iv = new byte[blockSize];
        // a) 生成用于ZIP包内文件加密的对称密钥
        random.nextBytes(fek);
        random.nextBytes(iv);
        ParametersWithIV keyParam = new ParametersWithIV(new KeyParameter(fek), iv);

        // b) 根据加密方案，使用步骤 a)生成的文件加密对称密钥调用对称密码算法加密包内文件并写入ZIP包内；
        // c) 根据加密方案，对已经生成密文的明文文件进行处理，部分写入ZIP包；
        final EncryptEntries encryptEntries = this.encryptFiles(tbEncArr, keyParam);
        encryptEntries.setID(id);
        // d) 组装明密文映射表文件，根据加密方案对齐进行加密后或直接写入ZIP包。
        final ContainerPath entriesMapCp = encryptElement(encryptEntries, "entriesmap.dat", keyParam);
        // e) 组装加密入口文件，明文写入ZIP包内
        CT_EncryptInfo encryptInfo = newEncryptInfo(id);
        encryptions.addEncryptInfo(encryptInfo);
        encryptInfo.setEncryptScope("All");
        // 密钥描述文件位置配置
        final ContainerPath decryptseedCp = ContainerPath.newDatFile("decryptseed", this.workDir.toAbsolutePath());
        encryptInfo.setDecryptSeedLoc(decryptseedCp.getPath());
        // 明密文映射表或其加密后的文件存储的路径
        encryptInfo.setEntriesMapLoc(entriesMapCp.getPath());
        // 创建密钥描述文件
        DecyptSeed decyptSeedObj = new DecyptSeed()
                .setID(id)
                .setExtendParams(new ExtendParams());
        // f) 根据加密方案，对文件加密对称密钥进行密钥包装或非对称加密生成文件对称加密的包装密钥；
        String encryptCaseId = null;
        for (UserFEKEncryptor fekEncryptor : userEncryptorList) {
            if (encryptCaseId == null) {
                encryptCaseId = fekEncryptor.encryptCaseId();
            }
            // 加密 文件加密对称密钥，生成
            final UserInfo userInfo = fekEncryptor.encrypt(fek, iv);

            // 如果是证书加密，需要获取证书
            if (ProtectionCaseID.EncryptGMCert.getId().equals(fekEncryptor.encryptCaseId())) {
                // 证书加密使用的证书
                final byte[] certBin = fekEncryptor.userCert();
                if (certBin == null || certBin.length == 0) {
                    throw new CryptoException("无法获取加密证书");
                }
                userInfo.setUserCert(certBin);
            }
            decyptSeedObj.addUserInfo(userInfo);
            // g) 如果电子文件访问者为多人，则重复 7.3.4 的步骤 e)；
        }
        // h) 组装密钥描述文件，并写入ZIP包。
        ElemCup.dump(decyptSeedObj, decryptseedCp.getAbs());

        // 执行打包程序
        this.ofdDir.jar(dest);
        return this;
    }

    /**
     * 创建 加密描述信息
     *
     * @param id 加密操作标识
     * @return 加密描述信息
     */
    private CT_EncryptInfo newEncryptInfo(String id) {
        CT_EncryptInfo encryptInfo = new CT_EncryptInfo().setID(id);
        if (this.parameters != null) {
            // 设置 加密操作的附加描述集合
            encryptInfo.setParameters(this.parameters);
        }

        // 设置提供者信息
        final Provider provider = new Provider();
        provider.setCompany("ofdrw.org")
                .setProviderName("ofdrw-crypto")
                .setVersion(GlobalVar.Version);
        encryptInfo.setProvider(provider);
        // 设置加密时间
        encryptInfo.setEncryptDate(LocalDateTime.now());

        return encryptInfo;
    }


    /**
     * 获取待加密文件列表
     * <p>
     * 执行过滤器过滤文件
     *
     * @return 待加密文件列表
     * @throws IOException IO读写异常
     */
    private List<ContainerPath> getToBeEncFiles() throws IOException {
        return Files.walk(this.workDir)
                // 移除目录文件
                .filter(Files::isRegularFile)
                // 通过过滤器过滤加密得到文件
                .map((path) -> {
                    String cfPath = path.toAbsolutePath().toString()
                            .replace(this.workDir.toAbsolutePath().toString(), "")
                            .replace("\\", "/");
                    return new ContainerPath(cfPath, path);
                })
                .filter((cf) -> {
                    if (this.cfFilter == null) {
                        return true;
                    }
                    // 过滤
                    return this.cfFilter.filter(cf.getPath(), cf.getAbs());
                }).collect(Collectors.toList());
    }

    /**
     * 加密文件
     *
     * @param tbEncArr 待加密文件列表
     * @param keyParam 加密密钥参数
     * @return 明密文映射表
     * @throws IOException                文件读写异常
     * @throws InvalidCipherTextException 加密过程中异常
     */
    private EncryptEntries encryptFiles(List<ContainerPath> tbEncArr, ParametersWithIV keyParam) throws IOException, InvalidCipherTextException {
        // 创建明密文映射表
        EncryptEntries entriesMap = new EncryptEntries();
        for (ContainerPath plaintextCp : tbEncArr) {
            // 根据加密方案，使用步骤 a)生成的文件加密对称密钥调用对称密码算法加密包内文件并写入ZIP包内
            // 并且对已经生成密文的明文文件进行删除；
            final ContainerPath encryptedCp = encryptFile(plaintextCp, keyParam);
            // 添加明密文映射表的映射关系
            entriesMap.addEncryptEntry(plaintextCp.getPath(), encryptedCp.getPath());
        }
        return entriesMap;
    }

    /**
     * 加密单个文件
     * <p>
     * 加密后原文件将被删除
     *
     * @param plaintextCp 待加密容器内文件
     * @param keyParam    加密密钥
     * @return 加密后文件在容器内的位置
     * @throws IOException                文件读写异常
     * @throws InvalidCipherTextException 加密运算异常
     */
    private ContainerPath encryptFile(ContainerPath plaintextCp, CipherParameters keyParam) throws IOException, InvalidCipherTextException {
        int bytesProcessed = 0;
        int len = 0;
        byte[] buffIn = new byte[4096];
        byte[] buffOut = new byte[4096 + this.blockCipher.getBlockSize()];
        // 创建加密后的文件
        final ContainerPath encryptedCp = plaintextCp.createEncryptedFile();
        try (InputStream in = Files.newInputStream(plaintextCp.getAbs());
             OutputStream out = Files.newOutputStream(encryptedCp.getAbs())) {
            this.blockCipher.init(true, keyParam);
            while ((len = in.read(buffIn)) != -1) {
                // 分块加密
                bytesProcessed = this.blockCipher.processBytes(buffIn, 0, len, buffOut, 0);
                if (bytesProcessed > 0) {
                    out.write(buffOut, 0, bytesProcessed);
                }
            }
            // 执行最后一个分块的加密和填充
            bytesProcessed = this.blockCipher.doFinal(buffOut, 0);
            out.write(buffOut, 0, bytesProcessed);
            this.blockCipher.reset();
        }
        // 加密完成后，删除源文件
        Files.delete(plaintextCp.getAbs());
        return encryptedCp;
    }

    /**
     * 加密OFD对象
     *
     * @param obj      OFD对象
     * @param outPath  加密后存放路径，容器内路径
     * @param keyParam 加密密钥
     * @return 加密后文件在容器的路径
     * @throws IOException                文件读写异常
     * @throws InvalidCipherTextException 加密运算异常
     */
    private ContainerPath encryptElement(Element obj, String outPath, CipherParameters keyParam) throws IOException, InvalidCipherTextException {
        if (outPath.startsWith("/")) {
            outPath = outPath.substring(1);
        }
        // 创建加密后保存的文件路径
        final ContainerPath res = ContainerPath.newDatFile(outPath, workDir);

        int len = 0;
        int bytesProcessed = 0;
        byte[] buffIn = new byte[4096];
        byte[] buffOut = new byte[4096 + this.blockCipher.getBlockSize()];
        ByteArrayInputStream in = new ByteArrayInputStream(ElemCup.dump(obj));
        this.blockCipher.init(true, keyParam);
        try (OutputStream out = Files.newOutputStream(res.getAbs())) {
            while ((len = in.read(buffIn)) != -1) {
                // 分块加密
                bytesProcessed = this.blockCipher.processBytes(buffIn, 0, len, buffOut, 0);
                if (bytesProcessed > 0) {
                    out.write(buffOut, 0, bytesProcessed);
                }
            }
            // 执行最后一个分块的加密和填充
            bytesProcessed = this.blockCipher.doFinal(buffOut, 0);
            out.write(buffOut, 0, bytesProcessed);
            this.blockCipher.reset();
        }
        return res;
    }

    /**
     * 获取 加密操作的附加描述集合
     *
     * @return 加密操作的附加描述集合，可能为空
     */
    @Nullable
    public Parameters getParameters() {
        return parameters;
    }

    /**
     * 设置 加密操作的附加描述集合
     *
     * @param parameters 加密操作的附加描述集合，可能为空
     * @return this
     */
    public OFDEncryptor setParameters(Parameters parameters) {
        this.parameters = parameters;
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
