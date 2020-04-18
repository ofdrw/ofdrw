package org.ofdrw.sign;


import org.apache.commons.io.FilenameUtils;
import org.dom4j.DocumentException;
import org.ofdrw.core.basicStructure.ofd.DocBody;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.signatures.Signatures;
import org.ofdrw.core.signatures.range.Reference;
import org.ofdrw.core.signatures.range.References;
import org.ofdrw.core.signatures.sig.Signature;
import org.ofdrw.pkg.container.DocDir;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.pkg.container.SignDir;
import org.ofdrw.reader.BadOFDException;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.ResourceLocator;
import org.ofdrw.sign.stamppos.StampAppearance;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;

/**
 * OFD文档数字签章引擎
 * <p>
 * 签章和验证操作均针对于OFD文档中的第一个文档
 *
 * @author 权观宇
 * @since 2020-04-17 02:11:56
 */
public class OFDSigner implements Closeable {

    private OFDDir ofdDir;
    /**
     * OFD解析器
     */
    private OFDReader reader;

    /**
     * 最大签名ID提供者
     */
    private AtomicSignID MaxSignID;

    /**
     * 数字签名模式
     * <p>
     * 默认：保护整个文档的数字签名模式
     */
    private SignMode signMode;

    /**
     * 需要根性文件主入口文件
     * <p>
     * true - 表示需要更新主入口文件OFD.xml；
     * <p>
     * false - 表示不需要更新；
     */
    private boolean needUpdateOFDMainEntry = false;

    /**
     * 签名列表文件绝对路径
     */
    private ST_Loc signaturesLoc;

    /**
     * 签章外观列表
     */
    private List<StampAppearance> apList;

    /**
     * 签名实现容器
     */
    private ExtendSignatureContainer signContainer;

    /**
     * 不允许调用无参数构造器
     */
    private OFDSigner() {
    }

    /**
     * 创建OFD签名对象
     *
     * @param reader        OFD解析器
     * @param signContainer 签名容器
     */
    public OFDSigner(OFDReader reader, ExtendSignatureContainer signContainer) {
        if (reader == null) {
            throw new IllegalArgumentException("OFD解析器（reader）为空");
        }
        if (signContainer == null) {
            throw new IllegalArgumentException("签名实现容器（signContainer）为空");
        }
        this.reader = reader;
        this.ofdDir = reader.getOFDDir();
        this.signContainer = signContainer;
        apList = new LinkedList<>();
        // 默认采用 保护整个文档的数字签名模式
        signMode = SignMode.WholeProtected;
        signaturesLoc = null;
        // 执行签名预检查
        preChecker();
    }


    /**
     * 获取签章模式
     *
     * @return 签章模式
     */
    public SignMode getSignMode() {
        return signMode;
    }

    /**
     * 设置签章模式
     *
     * @param signMode 签章模式
     * @return this
     */
    public OFDSigner setSignMode(SignMode signMode) {
        if (signMode == null) {
            signMode = SignMode.WholeProtected;
        }
        this.signMode = signMode;
        return this;
    }

    /**
     * 设置电子签名实现容器
     *
     * @param signContainer 实现容器
     * @return this
     */
    public OFDSigner setSignContainer(ExtendSignatureContainer signContainer) {
        this.signContainer = signContainer;
        return this;
    }

    /**
     * 增加签章外观位置
     *
     * @param sa 签章外观位置
     * @return this
     */
    public OFDSigner addApPos(StampAppearance sa) {
        if (sa == null) {
            return this;
        }
        this.apList.add(sa);
        return this;
    }

    /**
     * OFD文档预检查
     * <p>
     * 1. 是否需要根性OFD.xml。
     * <p>
     * 2. 是否可以继续数字签名，如果Signatures.xml被包含到SignInfo中，那么则不能再继续签名。
     */
    private void preChecker() {
        ResourceLocator rl = reader.getResourceLocator();
        try {
            rl.save();
            rl.cd("/");
            // 获取Doc_0 的签名列表文件位置
            signaturesLoc = reader.getDefaultDocSignaturesPath();
            // 如果OFD.xml 不含有签名列表文件路径，那么设置需要更新
            if (signaturesLoc == null) {
                needUpdateOFDMainEntry = true;
                // 从0起的最大签名ID
                this.MaxSignID = new AtomicSignID();
                return;
            }
            // 获取签名列表对象
            Signatures signatures = rl.get(signaturesLoc, Signatures::new);

            // 载入文档中已有的最大签名ID
            String maxSignId = signatures.getMaxSignId();
            this.MaxSignID = new AtomicSignID(maxSignId);

            // 获取签名文件所在路径
            String parent = signaturesLoc.parent();
            // 切换工作路径到签名容器中
            rl.cd(parent);
            List<org.ofdrw.core.signatures.Signature> signatureList = signatures.getSignatures();
            // 遍历所有签名容器，判断保护文件中是否包含Signatures.xml
            for (org.ofdrw.core.signatures.Signature sig : signatureList) {
                ST_Loc baseLoc = sig.getBaseLoc();
                Signature sigObj = rl.get(baseLoc, Signature::new);
                References refList = sigObj.getSignedInfo().getReferences();
                if (refList.hasFile(signaturesLoc.getFileName())) {
                    throw new SignatureTerminateException("签名列表文件（Signatures.xml）已经被保护，文档不允许继续追加签名");
                }
            }
        } catch (FileNotFoundException | DocumentException e) {
            throw new BadOFDException("错误OFD结构和文件格式", e);
        } finally {
            rl.restore();
        }
    }


    /**
     * 获取文档中待杂凑文件流
     *
     * @return 文件信息流
     */
    private List<ToDigestFileInfo> toBeDigestFileList() throws IOException {
        List<ToDigestFileInfo> res = new LinkedList<>();

        // 获取OFD容器在文件系统中的路径
        Path containerPath = ofdDir.getContainerPath();
        // 文件系统中的容器Unix类型绝对路径，如："/Doc_0/Pages"
        String sysRoot = FilenameUtils.separatorsToUnix(containerPath.toAbsolutePath().toString());
        // 遍历OFD文件目录中的所有文件
        Files.walkFileTree(containerPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                // 路径转换为Unix类型的绝对路径
                String abxFilePath = FilenameUtils.separatorsToUnix(file.toAbsolutePath().toString());
                // 替换文件系统的根路径，这样就为容器系统中的绝对路径
                abxFilePath = abxFilePath.replace(sysRoot, "");
                // 如果采用继续签章模式，那么跳过对 Signatures.xml 的文件
                if (signMode == SignMode.ContinueSign
                        && abxFilePath.equals(signaturesLoc.getLoc())) {
                    return FileVisitResult.CONTINUE;
                }
                // 构造加入文件信息列表
                res.add(new ToDigestFileInfo(abxFilePath, file));
                return FileVisitResult.CONTINUE;
            }
        });
        return res;
    }

    /**
     * 签名或签章执行器
     * @return Signatures 列表对象
     */
    private Signatures exeSign() {
        Signatures signListObj = null;
        if (signaturesLoc == null) {
            signListObj = new Signatures();
        } else {
            signListObj = reader.getDefaultSignatures();
        }
        // 创建一个新的签名容器
        SignDir signDir = ofdDir.obtainDocDefault()
                .obtainSigns()
                .newSignDir();
        // 签名文件相对路径
        ST_Loc signatureLoc = ST_Loc.getInstance(signDir.getContainerName() + "/" + SignDir.SignatureFileName);
        // 构造签名记录
        org.ofdrw.core.signatures.Signature signRecord =
                new org.ofdrw.core.signatures.Signature()
                        .setID(MaxSignID.incrementAndGet())
                        .setType(signContainer.getSignType())
                        .setBaseLoc(signatureLoc);
        signListObj.addSignature(signRecord);

        // TODO 创建对象

        return signListObj;


    }

    /**
     * 进行签名/章
     * <p>
     * 然后关闭文档
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {

    }
}
