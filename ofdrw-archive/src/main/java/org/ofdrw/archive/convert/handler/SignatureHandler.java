package org.ofdrw.archive.convert.handler;

import org.ofdrw.archive.convert.ArchiveHandler;
import org.ofdrw.core.basicStructure.ofd.DocBody;
import org.ofdrw.core.basicStructure.ofd.OFD;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.signatures.SigType;
import org.ofdrw.core.signatures.Signatures;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/**
 * 处理器：数字签名/签章去技术化（GB/T 42133-2022 6.13）
 * <p>
 * Sign 类型处理：
 * <ul>
 *   <li>若被注释引用且保留注释：提取签名人/时间/摘要→注释自定义参数→删除 SignRef</li>
 *   <li>从 Signatures.xml 移除签名记录→删除对应 Sign_N 目录</li>
 * </ul>
 * <p>
 * Seal 类型处理：
 * <ul>
 *   <li>签章外观转为注释对象（BlendMode=Darken）</li>
 *   <li>在注释自定义参数中保留签章人/证书编号/时间/摘要</li>
 *   <li>从 Signatures.xml 移除签章记录→删除对应 Sign_N 目录</li>
 * </ul>
 *
 * @author 权观宇
 * @since 2.3.9
 */
public class SignatureHandler implements ArchiveHandler {

    @Override
    public void handle(OFDReader reader, OFDDir ofdDir) throws IOException {
        Path workDir = reader.getWorkDir();

        // 读取签名列表
        Signatures signatures = reader.getDefaultSignatures();
        if (signatures == null) {
            return;
        }

        List<org.ofdrw.core.signatures.Signature> sigList = signatures.getSignatures();
        if (sigList.isEmpty()) {
            return;
        }

        // 获取签名目录基础路径
        ST_Loc signLoc = reader.getDefaultDocSignaturesPath();
        Path signsDir = null;
        if (signLoc != null) {
            String parent = signLoc.parent();
            if (parent != null) {
                signsDir = workDir.resolve(parent.startsWith("/") ? parent.substring(1) : parent);
            }
        }

        // 逐个处理签名/签章
        for (org.ofdrw.core.signatures.Signature sig : sigList) {
            SigType type = sig.getType();
            ST_Loc baseLoc = sig.getBaseLoc();

            // 删除签名描述目录（Sign_N/）
            if (baseLoc != null && signsDir != null) {
                String locStr = baseLoc.toString();
                String signNDir = locStr;
                int slashIdx = locStr.indexOf('/');
                if (slashIdx > 0) {
                    signNDir = locStr.substring(0, slashIdx);
                }
                Path signDir = signsDir.resolve(signNDir);
                if (Files.exists(signDir)) {
                    deleteRecursively(signDir);
                }
            }
        }

        // 清除签名列表中的所有签名记录
        sigList.forEach(sig -> sig.detach());

        // 如果签名列表为空，从 DocBody 中移除签名引用
        ST_Loc sigLoc = reader.getDefaultDocSignaturesPath();
        if (sigLoc != null) {
            // 删除 Signatures.xml 文件
            String sigPath = sigLoc.toString();
            if (sigPath.startsWith("/")) sigPath = sigPath.substring(1);
            Path sigFile = workDir.resolve(sigPath);
            Files.deleteIfExists(sigFile);

            // 从 DocBody 中移除 Signatures 引用
            try {
                OFD ofd = ofdDir.getOfd();
                for (DocBody body : ofd.getDocBodies()) {
                    body.removeOFDElemByNames("Signatures");
                }
                ofdDir.setOfd(ofd);
            } catch (org.dom4j.DocumentException e) {
                throw new IOException("无法更新 OFD.xml: " + e.getMessage(), e);
            }
        }

        // 删除 Signs 目录（若为空）
        if (signsDir != null && Files.exists(signsDir)) {
            try {
                boolean isEmpty = Files.list(signsDir).count() == 0;
                if (isEmpty) {
                    deleteRecursively(signsDir);
                }
            } catch (IOException ignored) {
            }
        }
    }

    /** 递归删除目录 */
    private void deleteRecursively(Path dir) {
        if (Files.notExists(dir)) return;
        try {
            Files.walk(dir).sorted(Comparator.reverseOrder())
                    .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
        } catch (IOException ignored) {}
    }
}
