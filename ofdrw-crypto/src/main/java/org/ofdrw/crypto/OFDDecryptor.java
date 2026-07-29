package org.ofdrw.crypto;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.engines.SM4Engine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.jetbrains.annotations.NotNull;
import org.ofdrw.core.crypto.encryt.*;
import org.ofdrw.crypto.decryptor.DecryptResult;
import org.ofdrw.crypto.decryptor.UserFEKDecryptor;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.pkg.tool.ElemCup;
import org.ofdrw.reader.ZipUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * OFD解密器 — {@link OFDEncryptor} 的逆操作
 * <p>
 * 将加密的 OFD 文件解密为明文 OFD 文件。解密流程：
 * <ol>
 *   <li>解压加密 OFD 到工作目录</li>
 *   <li>读取 Encryptions.xml，获取所有加密信息（支持多重加密）</li>
 *   <li>对每个加密信息：加载密钥描述文件 → 匹配解密器恢复 FEK → 解密密文映射表 → 逐一解密文件</li>
 *   <li>清理所有加密元数据（Encryptions.xml、decryptseed.dat、entriesmap.dat）</li>
 *   <li>重新打包为明文 OFD</li>
 * </ol>
 * <p>
 * 使用示例 — 口令解密：
 * <pre>{@code
 *     Path src = Paths.get("encrypted.ofd");
 *     Path out = Paths.get("decrypted.ofd");
 *     try (OFDDecryptor d = new OFDDecryptor(src, out)) {
 *         d.addUser(new UserPasswordDecryptor("张三", "12345678"));
 *         d.decrypt();
 *     }
 * }</pre>
 * <p>
 * 支持多重加密（Encryptions.xml 包含多个 CT_EncryptInfo），
 * 需为每个加密层提供对应的解密器。
 *
 * @author 权观宇
 * @since 2.3.9
 */
public class OFDDecryptor implements Closeable {

    /**
     * 工作目录
     * <p>
     * 用于存放解压后的 OFD 容器内容以及解密过程中的临时文件。
     * 若由本类自动创建（通过双参构造器），close 时自动删除；
     * 若由用户指定（通过三参构造器），close 时不删除，由调用者自行管理生命周期。
     */
    private final Path workDir;

    /**
     * 工作目录是否由本类创建
     * <p>
     * true — close 时自动删除工作目录
     * false — close 时保留工作目录
     */
    private final boolean ownWorkDir;

    /**
     * OFD 虚拟容器根目录
     * <p>
     * 对应解压后的 OFD 包根目录，包含 OFD.xml、Doc_N 等
     */
    private final OFDDir ofdDir;

    /**
     * 解密后文件输出位置
     */
    private final Path dest;

    /**
     * 用户提供的解密器列表
     * <p>
     * 解密时遍历此列表，用每个解密器尝试匹配密钥描述文件中的 UserInfo
     */
    private final List<UserFEKDecryptor> decryptors;

    /**
     * 是否已关闭
     */
    private boolean closed = false;

    /**
     * 创建 OFD 解密器（自动创建临时工作目录）
     * <p>
     * 工作目录由系统自动创建在系统临时目录下，close 时自动删除。
     *
     * @param src  加密的 OFD 文件路径，不能为空
     * @param dest 解密后 OFD 文件输出路径，不能为空
     * @throws IOException              解压或文件操作异常
     * @throws IllegalArgumentException src 或 dest 为 null 或不存在
     */
    public OFDDecryptor(@NotNull Path src, @NotNull Path dest) throws IOException {
        this(src, dest, Files.createTempDirectory("ofd-dec-tmp-"), true);
    }

    /**
     * 创建 OFD 解密器（使用用户指定的工作目录）
     * <p>
     * 工作目录由调用者提供和管理，close 时 <b>不会</b> 删除该目录。
     * 适用于需要保留中间文件进行调试，或工作目录有特殊位置要求的场景。
     * <p>
     * 注意：若指定的工作目录已存在且非空，其内容可能被覆盖。
     *
     * @param src     加密的 OFD 文件路径，不能为空
     * @param dest    解密后 OFD 文件输出路径，不能为空
     * @param workDir 用户指定的工作目录（若不存在则自动创建）
     * @throws IOException              解压或文件操作异常
     * @throws IllegalArgumentException src 为 null 或不存在，dest 为 null
     */
    public OFDDecryptor(@NotNull Path src, @NotNull Path dest, @NotNull Path workDir) throws IOException {
        this(src, dest, workDir, false);
    }

    /**
     * 内部构造器
     *
     * @param src         加密的 OFD 文件路径
     * @param dest        解密后输出路径
     * @param workDir     工作目录
     * @param ownWorkDir  是否由本类管理 workDir 生命周期
     */
    private OFDDecryptor(@NotNull Path src, @NotNull Path dest, @NotNull Path workDir, boolean ownWorkDir)
            throws IOException {
        if (src == null || Files.notExists(src)) {
            throw new IllegalArgumentException("加密文件(src)不存在: " + src);
        }
        if (dest == null) {
            throw new IllegalArgumentException("解密后文件输出位置(dest)为空");
        }
        this.dest = dest;
        this.workDir = workDir;
        this.ownWorkDir = ownWorkDir;

        // 若指定的目录不存在则创建
        if (Files.notExists(workDir)) {
            Files.createDirectories(workDir);
        }

        // 解压加密 OFD 到工作目录
        ZipUtil.unZipFiles(src.toFile(), workDir.toAbsolutePath() + File.separator);
        this.ofdDir = new OFDDir(workDir.toAbsolutePath());
        this.decryptors = new ArrayList<>(3);
    }

    /**
     * 添加解密用户
     * <p>
     * 解密时会用每个添加的解密器尝试匹配密钥描述文件中的 UserInfo。
     * 建议先添加匹配概率最高的解密器以提高效率。
     *
     * @param decryptor 用户 FEK 解密器，不能为 null
     * @return this（Fluent API）
     */
    public OFDDecryptor addUser(@NotNull UserFEKDecryptor decryptor) {
        if (decryptor != null) {
            this.decryptors.add(decryptor);
        }
        return this;
    }

    /**
     * 执行解密
     * <p>
     * 完整解密流程：
     * <ol>
     *   <li>检查 Encryptions.xml 是否存在，不存在则直接打包退出</li>
     *   <li>读取 Encryptions.xml → CT_EncryptInfo 列表</li>
     *   <li>对每个 CT_EncryptInfo（支持多重加密）：
     *     <ol type="a">
     *       <li>读取 decryptseed.dat → 解析 DecyptSeed</li>
     *       <li>用 UserFEKDecryptor 匹配 UserInfo → 恢复 FEK + IV</li>
     *       <li>读取 entriesmap.dat → SM4-CBC 解密 → 解析 EncryptEntries</li>
     *       <li>对每个 EncryptEntry：SM4-CBC 解密密文 → 写回明文路径 → 删除密文</li>
     *       <li>删除 decryptseed.dat 和 entriesmap.dat</li>
     *     </ol>
     *   </li>
     *   <li>删除 Encryptions.xml</li>
     *   <li>重新打包为明文 OFD 输出到 dest</li>
     * </ol>
     *
     * @throws IOException     文件读写异常
     * @throws CryptoException 解密失败（无匹配的解密器、密钥错误或数据损坏）
     */
    public void decrypt() throws IOException, CryptoException {
        // 检查加密入口文件是否存在
        Path encryptionsPath = workDir.resolve("Encryptions.xml");
        if (Files.notExists(encryptionsPath)) {
            // 文件未加密，直接打包输出
            ofdDir.jar(dest);
            return;
        }

        // 解析加密入口文件
        Encryptions encryptions;
        try {
            org.dom4j.Element encEl = ElemCup.inject(encryptionsPath);
            encryptions = new Encryptions(encEl);
        } catch (DocumentException e) {
            throw new IOException("无法解析 Encryptions.xml: " + e.getMessage(), e);
        }

        // 逐个处理加密信息（支持多重加密）
        List<CT_EncryptInfo> encryptInfoList = encryptions.getEncryptInfos();
        if (encryptInfoList.isEmpty()) {
            // 无加密信息，直接打包
            ofdDir.jar(dest);
            return;
        }

        for (CT_EncryptInfo encryptInfo : encryptInfoList) {
            // a. 读取密钥描述文件
            DecryptResult decryptResult = recoverFek(encryptInfo);

            // b. 解密密文映射表 → 获取 EncryptEntries
            EncryptEntries entries = decryptEntriesMap(encryptInfo, decryptResult);

            // c. 解密所有加密文件
            decryptFiles(entries, decryptResult);

            // d. 删除密钥描述文件和密文映射表文件
            deleteMetadataFile(encryptInfo.getDecryptSeedLoc());
            deleteMetadataFile(encryptInfo.getEntriesMapLoc());
        }

        // 删除 Encryptions.xml
        Files.deleteIfExists(encryptionsPath);

        // 重新打包为明文 OFD
        ofdDir.jar(dest);
    }

    /**
     * 恢复文件加密密钥(FEK)和初始化向量(IV)
     * <p>
     * 匹配逻辑：
     * <ol>
     *   <li>读取 decryptseed.dat → 解析 DecyptSeed</li>
     *   <li>获取其中的 UserInfo 列表</li>
     *   <li>遍历解密器，对每个 UserInfo：
     *     <ol type="a">
     *       <li>检查 encryptCaseId 是否与 DecyptSeed.EncryptCaseId 一致</li>
     *       <li>检查 username（非null时需匹配 UserInfo.UserName）</li>
     *       <li>调用 decrypt(userInfo) 尝试解密</li>
     *     </ol>
     *   </li>
     *   <li>全部不匹配则抛出异常</li>
     * </ol>
     *
     * @param encryptInfo 加密描述信息
     * @return 解密结果（FEK + IV）
     * @throws IOException     文件读取异常
     * @throws CryptoException 无匹配的解密器或解密失败
     */
    private DecryptResult recoverFek(CT_EncryptInfo encryptInfo) throws IOException, CryptoException {
        // 读取密钥描述文件（二进制 XML 格式，以 decryptseed.dat 存储）
        Path seedPath = resolveContainerPath(encryptInfo.getDecryptSeedLoc());
        if (Files.notExists(seedPath)) {
            throw new IOException("密钥描述文件不存在: " + encryptInfo.getDecryptSeedLoc());
        }

        DecyptSeed decyptSeed;
        try {
            org.dom4j.Element seedEl = ElemCup.inject(seedPath);
            decyptSeed = new DecyptSeed(seedEl);
        } catch (DocumentException e) {
            throw new IOException("无法解析密钥描述文件: " + seedPath, e);
        }

        // 获取加密方案标识（用于与解密器匹配）
        // 注意：OFDEncryptor 当前版本未设置 EncryptCaseId，可能为 null
        // 为 null 时跳过方案标识检查，直接按用户名匹配
        String encryptCaseId = decyptSeed.getEncryptCaseId();
        List<UserInfo> userInfoList = decyptSeed.getUserInfos();

        if (userInfoList.isEmpty()) {
            throw new CryptoException("密钥描述文件中没有用户信息(UserInfo)");
        }

        // 遍历解密器，匹配 UserInfo
        for (UserFEKDecryptor decryptor : decryptors) {
            // 检查加密方案标识是否匹配
            if (encryptCaseId != null && !encryptCaseId.equals(decryptor.encryptCaseId())) {
                continue;
            }

            // 遍历 UserInfo，匹配用户名
            for (UserInfo userInfo : userInfoList) {
                // 用户名匹配：解密器指定了用户名则需要与 UserInfo 一致
                String targetUsername = decryptor.getUsername();
                if (targetUsername != null && !targetUsername.equals(userInfo.getUserName())) {
                    continue;
                }

                // 尝试解密
                try {
                    return decryptor.decrypt(userInfo);
                } catch (CryptoException e) {
                    // 当前 UserInfo 解密失败，尝试下一个 UserInfo
                    continue;
                }
            }
        }

        // 所有解密器都不匹配
        StringBuilder sb = new StringBuilder("没有匹配的解密器。");
        sb.append(" 文件加密方案: ").append(encryptCaseId);
        sb.append(", 可用解密方案: ");
        for (UserFEKDecryptor d : decryptors) {
            sb.append(d.encryptCaseId()).append(" ");
        }
        throw new CryptoException(sb.toString());
    }

    /**
     * 解密明密文映射表
     * <p>
     * entriesmap.dat 可能被 SM4-CBC 加密（标准做法），也可能为明文 XML。
     * 先尝试作为密文解密，失败则尝试直接作为 XML 解析。
     *
     * @param encryptInfo   加密描述信息
     * @param decryptResult FEK + IV
     * @return 明密文映射表
     * @throws IOException     文件读取异常
     * @throws CryptoException 解密失败
     */
    private EncryptEntries decryptEntriesMap(CT_EncryptInfo encryptInfo, DecryptResult decryptResult)
            throws IOException, CryptoException {
        Path entriesPath = resolveContainerPath(encryptInfo.getEntriesMapLoc());
        if (Files.notExists(entriesPath)) {
            throw new IOException("明密文映射表文件不存在: " + encryptInfo.getEntriesMapLoc());
        }

        byte[] entriesBytes = Files.readAllBytes(entriesPath);
        byte[] plainBytes;

        try {
            // 尝试作为密文解密（标准做法：entriesmap.dat 被 SM4-CBC 加密）
            plainBytes = sm4CbcDecrypt(entriesBytes, decryptResult.getFek(), decryptResult.getIv());
        } catch (CryptoException e) {
            // 解密失败，可能 entriesmap.dat 本身就是明文
            plainBytes = entriesBytes;
        }

        // 解析 EncryptEntries XML
        try {
            SAXReader saxReader = new SAXReader();
            org.dom4j.Document doc = saxReader.read(new ByteArrayInputStream(plainBytes));
            return new EncryptEntries(doc.getRootElement());
        } catch (DocumentException e) {
            throw new IOException("无法解析明密文映射表: " + e.getMessage(), e);
        }
    }

    /**
     * 解密所有加密文件
     * <p>
     * 遍历 EncryptEntries 中的每条映射关系：
     * <ol>
     *   <li>读取密文文件（EPath）</li>
     *   <li>SM4-CBC 解密</li>
     *   <li>写入明文路径（Path），自动创建父目录</li>
     *   <li>删除密文文件</li>
     * </ol>
     *
     * @param entries       明密文映射表
     * @param decryptResult FEK + IV
     * @throws IOException     文件读写异常
     * @throws CryptoException 解密失败
     */
    private void decryptFiles(EncryptEntries entries, DecryptResult decryptResult)
            throws IOException, CryptoException {
        for (EncryptEntry entry : entries.getEncryptEntries()) {
            // 密文文件路径（容器内绝对路径，如 /enc/a.dat）
            Path cipherPath = resolveContainerPath(entry.getEPath());
            // 明文文件路径（原始路径，如 /Doc_0/Pages/Page_0/Content.xml）
            Path plainPath = resolveContainerPath(entry.getPathA());

            if (Files.notExists(cipherPath)) {
                continue;  // 密文不存在，跳过
            }

            // 读取密文 → SM4-CBC 解密 → 写入明文
            byte[] cipherBytes = Files.readAllBytes(cipherPath);
            byte[] plainBytes = sm4CbcDecrypt(cipherBytes, decryptResult.getFek(), decryptResult.getIv());

            // 确保明文父目录存在
            Files.createDirectories(plainPath.getParent());
            // 写入明文
            Files.write(plainPath, plainBytes);
            // 删除密文
            Files.deleteIfExists(cipherPath);
        }
    }

    /**
     * SM4-CBC 解密
     * <p>
     * 分组长度 16 字节，IV 长度 16 字节，填充方式 PKCS#7。
     * 与加密时使用的参数（{@link OFDEncryptor}）完全一致。
     *
     * @param cipherBytes 密文数据
     * @param fek         文件加密密钥（16字节）
     * @param iv          初始化向量（16字节）
     * @return 明文数据
     * @throws CryptoException 解密失败（密钥错误或数据损坏）
     */
    private byte[] sm4CbcDecrypt(byte[] cipherBytes, byte[] fek, byte[] iv) throws CryptoException {
        PaddedBufferedBlockCipher blockCipher =
                new PaddedBufferedBlockCipher(new CBCBlockCipher(new SM4Engine()), new PKCS7Padding());
        blockCipher.init(false, new ParametersWithIV(new KeyParameter(fek), iv));

        byte[] buffOut = new byte[cipherBytes.length + blockCipher.getBlockSize()];
        int n = blockCipher.processBytes(cipherBytes, 0, cipherBytes.length, buffOut, 0);
        try {
            n += blockCipher.doFinal(buffOut, n);
        } catch (org.bouncycastle.crypto.InvalidCipherTextException e) {
            throw new CryptoException("SM4-CBC 解密失败：密钥错误或密文数据损坏", e);
        }

        byte[] plainBytes = new byte[n];
        System.arraycopy(buffOut, 0, plainBytes, 0, n);
        return plainBytes;
    }

    /**
     * 将容器内绝对路径解析为文件系统路径
     * <p>
     * ST_Loc 的路径以 "/" 开头表示容器根目录，
     * 解析到 workDir 对应的文件系统路径。
     *
     * @param loc 容器内路径
     * @return 文件系统中的绝对路径
     */
    private Path resolveContainerPath(org.ofdrw.core.basicType.ST_Loc loc) {
        String path = loc.toString();
        // 去掉开头的 "/"，转为相对路径后拼接到工作目录
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return workDir.resolve(path);
    }

    /**
     * 删除加密元数据文件
     *
     * @param loc 容器内路径
     * @throws IOException 删除异常
     */
    private void deleteMetadataFile(org.ofdrw.core.basicType.ST_Loc loc) throws IOException {
        Path absPath = resolveContainerPath(loc);
        Files.deleteIfExists(absPath);
    }

    /**
     * 关闭解密器，清理临时文件
     * <p>
     * 若工作目录为本类自动创建（双参构造器），则删除整个工作目录；
     * 若为用户指定（三参构造器），则保留工作目录。
     *
     * @throws IOException 删除工作目录异常
     */
    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        if (ownWorkDir && workDir != null && Files.exists(workDir)) {
            try {
                FileUtils.deleteDirectory(workDir.toFile());
            } catch (IOException e) {
                throw new IOException("无法删除解密器的工作目录: " + e.getMessage(), e);
            }
        }
    }
}
