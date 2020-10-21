package org.ofdrw.sign.signContainer;

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
import org.ofdrw.sign.*;
import org.ofdrw.sign.stamppos.StampAppearance;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;

public class OFDComSigner implements Closeable {


    /**
     * 时间日期格式
     */
    public static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * OFDRW 签名提供者
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
     * 是否已经执行exeSign
     */
    private boolean hasSign;


    /**
     * 不允许调用无参数构造器
     */
    private OFDComSigner() {
    }

    /**
     * 创建OFD签名对象
     *
     * @param reader     OFD解析器
     * @param out        电子签名后文件保存位置
     * @param idProvider 签名文件ID提供器
     * @since 2020-08-24 20:35:45
     */
    public OFDComSigner(OFDReader reader, Path out, SignIDProvider idProvider) throws SignatureTerminateException {
        if (reader == null) {
            throw new IllegalArgumentException("OFD解析器（reader）为空");
        }
        if (out == null) {
            throw new IllegalArgumentException("电子签名后文件保存位置（out）为空");
        }
        if (idProvider == null) {
            throw new IllegalArgumentException("签名文件ID提供器（idProvider）为空");
        }

        this.reader = reader;
        this.out = out;
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
     */
    public OFDComSigner(OFDReader reader, Path out) throws SignatureTerminateException {
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
    public OFDComSigner setSignMode(SignMode signMode) {
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
    public OFDComSigner setSignContainer(ExtendSignatureContainer signContainer) {
        if (signContainer == null) {
            throw new IllegalArgumentException("签名实现容器（signContainer）为空");
        }
        this.signContainer = signContainer;
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
         * 根据需要可能需要根性OFD.xml
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

        Path signatureFilePath = Paths.get(signDir.getSysAbsPath(), SignDir.SignatureFileName);
        // 写入到 Signature.xml
        Files.write(signatureFilePath, ((SignComContainer)signContainer).getXmlByte());

        Path SealFilePath = Paths.get(signDir.getSysAbsPath(), SignDir.SealFileName);
        // 写入到 seal.esl
        Files.write(SealFilePath, signContainer.getSeal());

        Path signedValuePath = Paths.get(signDir.getSysAbsPath(), SignDir.SignedValueFileName);
        // 将签名值写入到 SignedValue.dat中
        Files.write(signedValuePath, ((SignComContainer)signContainer).getSignedValue());
        return signListObj;
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
        ofdDir.jar(out);
        // 关闭OFD解析器
        reader.close();
    }
}