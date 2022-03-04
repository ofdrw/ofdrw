package org.ofdrw.crypto.integrity;

import org.apache.commons.io.FileUtils;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.ofdrw.core.Holder;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.integrity.FileList;
import org.ofdrw.core.integrity.OFDEntries;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.pkg.tool.ElemCup;
import org.ofdrw.reader.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.Set;

/**
 * OFD完整性协议校验
 * <p>
 * 协议遵循《GB/T 0099》 7.4.6 校验流程
 *
 * @author 权观宇
 * @since 2021-08-23 19:44:54
 */
public class OFDIntegrityVerifier {

    public OFDIntegrityVerifier() {
    }

    /**
     * 执行OFD的完整性校验流程
     *
     * @param in       待校验的OFD文件路径
     * @param verifier 签名值验证器
     * @return 校验结果：true - 文件完整且没有夹带；false - 文件存在夹带
     * @throws IOException              文件不存在或IO操作异常
     * @throws GeneralSecurityException 安全计算异常
     * @throws DocumentException        无法解析文档内容，可能是文件格式不正确
     */
    public boolean integrity(Path in, ProtectVerifier verifier) throws IOException, GeneralSecurityException, DocumentException {
        if (in == null || Files.notExists(in)) {
            throw new IllegalArgumentException("待校验的OFD文件路径(in)为空或不存在");
        }
        if (verifier == null) {
            throw new IllegalArgumentException("签名值验证器(verifier)为空");
        }

        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("ofd-tmp-");
            // 解压文档，到临时的工作目录
            ZipUtil.unZipFiles(in.toFile(), workDir.toAbsolutePath() + File.separator);
            OFDDir ofdDir = new OFDDir(workDir.toAbsolutePath());
            // a) 读取完整性保护描述文件
            // 此处如果文件不存在会抛出FNE异常
            final Path ofdEntriesPath = ofdDir.getFile(OFDDir.OFDEntriesFileName);
            // b) 根据签名方案，调用杂凑算法计算完整性保护文件得到杂凑值
            // c) 读取签名值文件，进行签名验证
            final Element e = ElemCup.inject(ofdEntriesPath);
            OFDEntries ofdEntries = new OFDEntries(e);
            final ST_Loc signedValueLoc = ofdEntries.getSignedValueLoc();
            final Path sigValuePath = Paths.get(workDir.toString(), signedValueLoc.toString());
            final byte[] signedValue = Files.readAllBytes(sigValuePath);
            // 调用杂凑算法计算完整性保护文件得到杂凑值，进行签名验证
            boolean integrity = verifier.digestThenVerify(ofdEntriesPath, signedValue);
            // 为了保证没有而外夹带的文件还需要，根据文件表和示例包内的文件进行对比
            boolean hasExtraFile = checkNoExtraFile(ofdDir, ofdEntries);
            return integrity && hasExtraFile;
        } finally {
            if (workDir != null) {
                // 删除用于校验的临时目录
                FileUtils.deleteDirectory(workDir.toFile());
            }
        }
    }

    /**
     * 检查是否存在 防止夹带文件列表 中存在的夹带文件
     *
     * @param ofdDir     OFD容器
     * @param ofdEntries 完整性保护描述对象
     * @return true - 没有夹带; false - 存在夹带
     */
    private boolean checkNoExtraFile(OFDDir ofdDir, OFDEntries ofdEntries) throws IOException {
        final FileList fileList = ofdEntries.getFileList();
        // 建立文件MAP 用于检查映射情况
        Set<String> pkgFileSet = new HashSet<>();
        fileList.getFiles().forEach((f) -> {
            String loc = f.getFileLoc().toString();
            if (loc.charAt(0) != '/') {
                loc = String.format("/%s", loc);
            }
            pkgFileSet.add(loc);
        });
        // 签名值文件也加入 Set中用于检查
        final ST_Loc signedValueLoc = ofdEntries.getSignedValueLoc();
        String loc = signedValueLoc.toString();
        if (loc.charAt(0) != '/') {
            loc = String.format("/%s", loc);
        }
        pkgFileSet.add(loc);

        Holder<String> invalidFileLocHolder = new Holder<>(null);
        // 遍历包内出现的所有文件
        ofdDir.walk(((pkgAbsPath, path) -> {
            // 忽略防止夹带文件本身
            if ("/OFDEntries.xml".equals(pkgAbsPath)){
                return true;
            }
             boolean exist = pkgFileSet.contains(pkgAbsPath);
            if (!exist) {
                // 文件不在 完整性保护文件列表中，那么认为是发生了文件的夹带
                invalidFileLocHolder.value = pkgAbsPath;
                // 停止文件的继续遍历
                return false;
            }
            return true;
        }));
        // 如果非法文件路径为空表示没有出现夹带情况
        return invalidFileLocHolder.value == null;
    }
}
