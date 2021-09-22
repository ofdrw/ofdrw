package org.ofdrw.crypto.integrity;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.integrity.FileList;
import org.ofdrw.core.integrity.OFDEntries;
import org.ofdrw.gv.GlobalVar;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.pkg.tool.ElemCup;
import org.ofdrw.reader.ZipUtil;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * OFD完整性保护协议实现
 *
 * @author 权观宇
 * @since 2021-08-17 19:41:53
 */
public class OFDIntegrity implements Closeable {
    /**
     * OFD虚拟容器根目录
     */
    private OFDDir ofdDir;

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

    private boolean closed;

    private AtomicInteger idProvider;

    public OFDIntegrity(@NotNull Path ofdFile, @NotNull Path dest) throws IOException {
        if (ofdFile == null || Files.notExists(ofdFile)) {
            throw new IllegalArgumentException("待保护文件位置(ofdFile)不正确");
        }
        if (dest == null) {
            throw new IllegalArgumentException("完整性保护文件输出位置(out)为空");
        }
        idProvider = new AtomicInteger(0);
        this.dest = dest;
        this.workDir = Files.createTempDirectory("ofd-tmp-");
        // 解压文档，到临时的工作目录
        ZipUtil.unZipFiles(ofdFile.toFile(), this.workDir.toAbsolutePath() + File.separator);
        this.ofdDir = new OFDDir(workDir.toAbsolutePath());
    }

    /**
     * 对文件实行完整性保护
     * <p>
     * 请在完成保护后务必调用{@link #close()} 以清除工作过程中的临时文件！
     *
     * @param signer 签名实现
     * @throws IOException IO操作异常
     * @throws GeneralSecurityException 密码运算相关问题
     */
    public void protect(@NotNull ProtectSigner signer) throws IOException, GeneralSecurityException {
        if (signer == null) {
            throw new IllegalArgumentException("请提供 签名实现");
        }
        final FileList fileList = new FileList();
        final OFDEntries ofdEntries = new OFDEntries()
                .setID(String.valueOf(this.idProvider.incrementAndGet()))
                .setCreatorName("ofdrw-crypto")
                .setVersion(GlobalVar.Version)
                .setCreationDate(LocalDateTime.now());

        ofdEntries.setSignedValueLoc(ST_Loc.getInstance("/signedvalue.dat"))
                .setFileList(fileList);

        Path ofdEntriesPath = this.workDir.resolve("OFDEntries.xml");
        Path signedValuePath = this.workDir.resolve("signedvalue.dat");
        // a) 确认文件包内的所有文件
        // 文件系统中的容器Unix类型绝对路径，如："/home/root/tmp"
        String sysRoot = FilenameUtils.separatorsToUnix(this.workDir.toAbsolutePath().toString());
        Files.walkFileTree(this.workDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                // 路径转换为Unix类型的绝对路径
                String abxFilePath = FilenameUtils.separatorsToUnix(file.toAbsolutePath().toString());
                // 替换文件系统的根路径，这样就为容器系统中的绝对路径
                abxFilePath = abxFilePath.replace(sysRoot, "");
                String id = String.valueOf(idProvider.incrementAndGet());
                // b) 组装 签名完整性保护文件
                fileList.addFile(id, abxFilePath);
                return FileVisitResult.CONTINUE;
            }
        });
        // 把清单写入文件
        ElemCup.dump(ofdEntries, ofdEntriesPath);
        // c) 根据签名方案，计算完整性保护文件的杂凑值；
        // d) 根据签名方案，使用版式文件合成者的签名私钥对杂凑值进行数字签名；
        // 执行签名
        final byte[] signature = signer.digestThenSign(ofdEntriesPath);
        // e) 将数字签名结果写入签名值文件
        // 把签名值写入文件
        Files.write(signedValuePath, signature);
        // 执行打包程序
        this.ofdDir.jar(dest);
    }


    /**
     * 请务必在程序结束时调用该方法释放
     * <p>
     * 工作过程中产生的临时文件
     *
     * @throws IOException 文件删除IO异常
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
