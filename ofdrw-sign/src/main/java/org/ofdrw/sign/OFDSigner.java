package org.ofdrw.sign;


import org.apache.commons.io.FilenameUtils;
import org.dom4j.DocumentException;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.signatures.SigType;
import org.ofdrw.core.signatures.Signatures;
import org.ofdrw.core.signatures.appearance.Seal;
import org.ofdrw.core.signatures.range.Reference;
import org.ofdrw.core.signatures.range.References;
import org.ofdrw.core.signatures.sig.Provider;
import org.ofdrw.core.signatures.sig.Signature;
import org.ofdrw.core.signatures.sig.SignedInfo;
import org.ofdrw.gv.GlobalVar;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.pkg.container.SignDir;
import org.ofdrw.pkg.container.SignsDir;
import org.ofdrw.reader.BadOFDException;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.ResourceLocator;
import org.ofdrw.sign.stamppos.StampAppearance;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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


    /**
     * 时间日期格式
     */
    public static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * @return OFDRW 签名提供者
     */
    public static Provider OFDRW_Provider() {
        return new Provider()
                .setProviderName("ofdrw-sign")
                .setCompany("ofdrw")
                .setVersion(GlobalVar.Version);
    }

    /**
     * OFD虚拟容器
     */
    private OFDDir ofdDir;
    /**
     * OFD解析器
     */
    private OFDReader reader;

    /**
     * 最大签名ID提供者
     */
    private SignIDProvider MaxSignID;

    /**
     * 数字签名模式
     * <p>
     * 默认：保护整个文档的数字签名模式
     */
    private SignMode signMode;


    /**
     * 签名列表文件绝对路径
     * <p>
     * 为空 - 表示需要更新主入口文件OFD.xml；
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
     * 电子签名后文件保存位置
     */
    private Path out;

    /**
     * 电子签名后文件输出流
     */
    private OutputStream outStream;

    /**
     * 是否已经执行exeSign
     */
    private boolean hasSign;


    /**
     * 不允许调用无参数构造器
     */
    private OFDSigner() {
    }

    /**
     * 创建OFD签名对象
     *
     * @param reader     OFD解析器
     * @param outStream  电子签名后文件保存位置
     * @param idProvider 签名文件ID提供器
     * @throws SignatureTerminateException 签名终止异常
     * @since 2020-08-24 20:35:45
     */
    public OFDSigner(OFDReader reader, OutputStream outStream, SignIDProvider idProvider) throws SignatureTerminateException {
        if (reader == null) {
            throw new IllegalArgumentException("OFD解析器（reader）为空");
        }
        if (outStream == null) {
            throw new IllegalArgumentException("电子签名后文件输出流（outStream）为空");
        }
        if (idProvider == null) {
            throw new IllegalArgumentException("签名文件ID提供器（idProvider）为空");
        }

        this.outStream = outStream;
        setProperty(reader, idProvider);
    }

    /**
     * 创建OFD签名对象
     *
     * @param reader     OFD解析器
     * @param out        电子签名后文件保存位置
     * @param idProvider 签名文件ID提供器
     * @throws SignatureTerminateException 签名终止异常
     * @since 2020-08-24 20:35:45
     */
    public OFDSigner(OFDReader reader, Path out, SignIDProvider idProvider) throws SignatureTerminateException {
        if (reader == null) {
            throw new IllegalArgumentException("OFD解析器（reader）为空");
        }
        if (out == null) {
            throw new IllegalArgumentException("电子签名后文件保存位置（out）为空");
        }
        if (idProvider == null) {
            throw new IllegalArgumentException("签名文件ID提供器（idProvider）为空");
        }

        this.out = out;
        setProperty(reader, idProvider);
    }

    /**
     * 数据初始化
     */
    private void setProperty(OFDReader reader, SignIDProvider idProvider) throws SignatureTerminateException {
        this.reader = reader;
        this.ofdDir = reader.getOFDDir();
        this.hasSign = false;
        // 初始化从0起的最大签名ID，如果源文档中已经存在签名文件的情况
        // 会在preChecker 设置为当前文件最大ID
        this.MaxSignID = idProvider;
        apList = new LinkedList<>();
        // 默认采用 保护整个文档的数字签名模式
        signMode = SignMode.WholeProtected;
        signaturesLoc = null;
        // 执行签名预检查
        preChecker();
    }

    /**
     * 创建OFD签名对象
     * <p>
     * 默认使用： s'NNN'格式解析和生成签名ID
     *
     * @param reader OFD解析器
     * @param out    电子签名后文件保存位置
     * @throws SignatureTerminateException 签名终止异常
     */
    public OFDSigner(OFDReader reader, Path out) throws SignatureTerminateException {
        this(reader, out, new StandFormatAtomicSignID());
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
        if (signContainer == null) {
            throw new IllegalArgumentException("签名实现容器（signContainer）为空");
        }
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
     *
     * @throws SignatureTerminateException 不允许继续签名
     */
    private void preChecker() throws SignatureTerminateException {
        ResourceLocator rl = reader.getResourceLocator();
        try {
            rl.save();
            rl.cd("/");
            // 获取Doc_0 的签名列表文件位置
            signaturesLoc = reader.getDefaultDocSignaturesPath();
            // 如果OFD.xml 不含有签名列表文件路径，那么设置需要更新
            if (signaturesLoc == null) {
                // 最大签名ID从0起的
                return;
            }
            // 获取签名列表对象
            Signatures signatures = rl.get(signaturesLoc, Signatures::new);

            // 载入文档中已有的最大签名ID
            String maxSignId = signatures.getMaxSignId();
            // 重新设置当前最大签名ID
            this.MaxSignID.setCurrentMaxSignId(maxSignId);

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
                if (refList.hasFile(signaturesLoc.getLoc())) {
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
        // 文件系统中的容器Unix类型绝对路径，如："/home/root/tmp"
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
     * <p>
     * 1. 构造签名列表。
     * <p>
     * 2. 计算保护文件杂凑值，设置签章显示位置、印章，构造签名文件。
     * <p>
     * 3. 计算签名值。
     *
     * @return Signatures 列表对象
     * @throws BadOFDException          文件解析失败，或文件不存在
     * @throws IOException              签名和文件读写过程中的IO异常
     * @throws GeneralSecurityException 签名异常
     */
    public Signatures exeSign() throws IOException, GeneralSecurityException {
        if (signContainer == null) {
            throw new IllegalArgumentException("签名实现容器（signContainer）为空，请提供签名实现容器");
        }
        hasSign = true;
        // 获取数字签名存储目录
        SignsDir signsDir = ofdDir.obtainDocDefault().obtainSigns();
        // 创建签名容器
        SignDir signDir = signsDir.newSignDir();

        /*
         * 1. 获取签名列表文件对象
         *
         * 根据需要可能需要更新OFD.xml
         */
        Signatures signListObj = null;
        if (signaturesLoc == null) {
            signListObj = new Signatures();
            signsDir.setSignatures(signListObj);

            // 构造签名列表文件路径
            signaturesLoc = signsDir.getAbsLoc()
                    .cat(SignsDir.SignaturesFileName);
            // 设置OFD.xml 的签名列表文件入口
            try {
                ofdDir.getOfd().getDocBody().setSignatures(signaturesLoc);
                // 将更新了的OFD.xml更新到文件系统中
                ofdDir.flushFileByName(OFDDir.OFDFileName);
            } catch (DocumentException e) {
                throw new BadOFDException("OFD.xml 文件解析失败");
            }
        } else {
            signListObj = reader.getDefaultSignatures();
        }

        /*
         * 2. 向签名列表文件中加入数字签名记录
         *
         * 如果签名列表文件不存在那么创建，如果已经存在那么更新到文件系统
         */
        // 签名文件
        ST_Loc signatureLoc = signDir.getAbsLoc().cat(SignDir.SignatureFileName);
        // 构造列表文件中的签名记录并放入签名列表中
        signListObj.addSignature(new org.ofdrw.core.signatures.Signature()
                // 设置ID
                .setID(MaxSignID.incrementAndGet())
                // 设置数字签名类型
                .setType(signContainer.getSignType())
                // 设置签名文件位置
                .setBaseLoc(signatureLoc));
        /*
         * 3. 构建签名文件对象
         *
         * - 设置算法
         * - 设置提供者
         * - 计算保护文件摘要值
         * - 签名文件构造
         */
        Path signatureFilePath = buildSignature(signsDir, signDir, signListObj);
        /*
         * 4. 计算数字签名获取签名值
         */
        // 设置签章原文的保护信息为：签名文件容器中绝对路径。
        String propertyInfo = signDir.getAbsLoc().cat(SignDir.SignatureFileName).toString();
        // 调用容器提供方法计算签章值。
        byte[] signedValue;
        try (InputStream inData = Files.newInputStream(signatureFilePath)) {
            signedValue = signContainer.sign(inData, propertyInfo);
        }
        Path signedValuePath = Paths.get(signDir.getSysAbsPath(), SignDir.SignedValueFileName);
        // 将签名值写入到 SignedValue.dat中
        Files.write(signedValuePath, signedValue);
        return signListObj;
    }

    /**
     * 构造一个签名文件
     * <p>
     * 并写入到签名容器中
     *
     * @param signsDir    签名容器
     * @param signDir     签名资源容器
     * @param signListObj 签名列表描述对象
     * @return 签名文件文件系统路径
     * @throws SignatureException 签名异常
     * @throws IOException        文件读写IO操作异常
     */
    private Path buildSignature(SignsDir signsDir,
                                SignDir signDir,
                                Signatures signListObj) throws IOException, SignatureException {
        // 构造签名信息
        SignedInfo signedInfo = new SignedInfo()
                // 设置签名模块提供者信息
                .setProvider(OFDRW_Provider())
                // 设置签名方法
                .setSignatureMethod(signContainer.getSignAlgOID())
                // 设置签名时间
                .setSignatureDateTime(DF.format(LocalDateTime.now()));

        // 如果是电子签章，那么设置电子印章
        final ST_Loc signDirAbsLoc = signDir.getAbsLoc();
        if (signContainer.getSignType() == SigType.Seal) {
            // 获取电子印章二进制字节
            byte[] sealBin = signContainer.getSeal();
            // 由于电子印章参数为可选参数，这里移除非空检查
            if (sealBin != null && sealBin.length != 0) {
                Path sealPath = Paths.get(signDir.getSysAbsPath(), SignDir.SealFileName);
                // 将电子印章写入文件
                Files.write(sealPath, sealBin);
                // 构造印章信息
                Seal seal = new Seal().setBaseLoc(signDirAbsLoc.cat(SignDir.SealFileName));
                signedInfo.setSeal(seal);
            }
        }

        // 加入签名关联的外观
        if (!apList.isEmpty()) {
            for (StampAppearance sa : apList) {
                // 解析除外观注解然后加入签名信息中
                sa.getAppearance(reader, MaxSignID).forEach(signedInfo::addStampAnnot);
            }
        }

        /*
         * 结束了所有需要分配的签名ID
         *
         * 写入了除了签名值文件之外的所有文件
         *
         * - 设置签名列表描述对象的最大ID
         * - 将签名列表文件更新到文件系统
         */
        signListObj.setMaxSignId(MaxSignID.get());
        signsDir.flushFileByName(SignsDir.SignaturesFileName);

        /*
         * 计算并设置所保护的所有文件的摘要
         */
        MessageDigest md = signContainer.getDigestFnc();
        References references = new References()
                // 设置摘要方法
                .setCheckMethod(md.getAlgorithm());
        // 获取要被保护的文件信息序列
        List<ToDigestFileInfo> toDigestFileInfos = toBeDigestFileList();
        for (ToDigestFileInfo fileInfo : toDigestFileInfos) {
            // 计算文件杂凑值
            byte[] digest = calculateFileDigest(md, fileInfo.getSysPath());
            // 重置杂凑函数
            md.reset();
            Reference ref = new Reference()
                    .setFileRef(fileInfo.getAbsPath())
                    .setCheckValue(digest);
            references.addReference(ref);
        }
        // 设置摘要列表，完成"签名要保护的原文及本次签名相关的信息"的构造
        signedInfo.setReferences(references);

        /*
         * 完成 签名描述文件的根节点 构造
         *
         * 序列化为文件写入到文件系统
         */
        Signature signature = new Signature()
                // 设置签名数据文件位置
                .setSignedValue(signDirAbsLoc.cat(SignDir.SignedValueFileName))
                .setSignedInfo(signedInfo);
        signDir.setSignature(signature);
        // 将签名描述文件根节点写入到文件系统中
        signDir.flushFileByName(SignDir.SignatureFileName);
        // 获取写入文件的操作系统路径
        return Paths.get(signDir.getSysAbsPath(), SignDir.SignatureFileName);
    }

    /**
     * 使用多次读取计算文件杂凑值
     * <p>
     * 减少内存使用
     *
     * @param md   杂凑计算函数
     * @param path 文件路径
     * @return 杂凑值
     * @throws IOException IO读写异常
     */
    private byte[] calculateFileDigest(MessageDigest md, Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path);
             DigestInputStream dis = new DigestInputStream(in, md)) {
            byte[] buffer = new byte[4096];
            // 根据缓存读入
            while (dis.read(buffer) > -1) ;
            // 计算最终文件杂凑值
            return md.digest();
        }
    }

    /**
     * 进行签名/章
     * <p>
     * 然后关闭文档
     *
     * @throws IOException 打包文件过程中IO异常
     */
    @Override
    public void close() throws IOException {
        if (!hasSign) {
            throw new IllegalStateException("请先执行 exeSign在关闭引擎完成数字签名。");
        }
        // 打包电子签名后的OFD文件
        if (out != null) {
            ofdDir.jar(out);
        } else if (outStream != null) {
            ofdDir.jar(outStream);
        } else {
            throw new IllegalArgumentException("OFD文档输出目录错误或没有设置输出流");
        }
        // 关闭OFD解析器
        reader.close();
    }
}
