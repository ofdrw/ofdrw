package org.ofdrw.sign;


import org.dom4j.DocumentException;
import org.ofdrw.core.basicStructure.ofd.DocBody;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.signatures.Signatures;
import org.ofdrw.core.signatures.range.Reference;
import org.ofdrw.core.signatures.range.References;
import org.ofdrw.core.signatures.sig.Signature;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.BadOFDException;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.ResourceLocator;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
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
     * 需要根性文件主入口文件
     * <p>
     * true - 表示需要更新主入口文件OFD.xml；
     * <p>
     * false - 表示不需要更新；
     */
    private boolean needUpdateOFDMainEntry = false;

    private OFDSigner() {
    }

    public OFDSigner(OFDReader reader) {
        if (reader == null) {
            throw new IllegalArgumentException("OFD解析器（reader）为空");
        }
        this.reader = reader;
        this.ofdDir = reader.getOFDDir();
        // 执行签名预检查
        preChecker();
    }

    /**
     * OFD文档预检查
     * <p>
     * 1. 是否需要根性OFD.xml。
     * <p>
     * 2. 是否可以继续数字签名，如果Signatures.xml被包含到SignInfo中，那么则不能再继续签名。
     */
    public void preChecker() {
        DocBody docBody = null;
        ResourceLocator rl = reader.getResourceLocator();
        try {
            docBody = ofdDir.getOfd().getDocBody();
            ST_Loc signaturesLoc = docBody.getSignatures();
            // 签名列表文件完整路径
            String signListFileAbsPath = rl.toAbsolutePath(signaturesLoc);
            // 如果OFD.xml 不含有签名列表文件路径，那么设置需要更新
            if (signaturesLoc == null) {
                needUpdateOFDMainEntry = true;
                // 从0起的最大签名ID
                this.MaxSignID = new AtomicSignID();
                return;
            }
            rl.save();
            rl.cd("/");
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
                if (refList.hasFile(signListFileAbsPath)) {
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
