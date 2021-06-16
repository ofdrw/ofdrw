package org.ofdrw.sign;

import org.dom4j.DocumentException;
import org.ofdrw.core.basicStructure.ofd.DocBody;
import org.ofdrw.core.basicStructure.ofd.OFD;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.pkg.container.VirtualContainer;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.ResourceLocator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 数字签名清除工具
 *
 * @author 权观宇
 * @since 2021-05-24 20:02:56
 */
public class SignCleaner {
    /**
     * OFD解析器
     */
    private OFDReader reader;

    /**
     * 输出位置
     */
    private Path out;

    private SignCleaner() {
    }

    /**
     * 构造实例
     *
     * @param reader 解析器
     * @param out    清空后输出的位置
     */
    public SignCleaner(OFDReader reader, Path out) {
        if (reader == null) {
            throw new IllegalArgumentException("OFD解析器(reader)为空");
        }
        if (out == null) {
            throw new IllegalArgumentException("输出位置(out)为空");
        }
        this.reader = reader;
        this.out = out;
    }

    /**
     * 所有文档中所有全部数字签名
     * <p>
     * 该操作会关闭Reader
     *
     * @throws IOException 文件操作异常
     * @throws DocumentException 文档结构无法解析
     */
    public void clean() throws IOException, DocumentException {
        final OFDDir ofdDir = reader.getOFDDir();
        OFD ofd = ofdDir.getOfd();
        final ResourceLocator rl = reader.getResourceLocator();
        for (DocBody docBody : ofd.getDocBodies()) {
            // 1. 获取并删除签名列表文件入口
            final ST_Loc signListFileLoc = docBody.getSignatures();
            // 删除
            docBody.removeOFDElemByNames("Signatures");
            if (signListFileLoc == null) {
                continue;
            }
            Path signaturesXMLFile = rl.getFile(signListFileLoc);
            // 2. save 进入签名列表文件目录
            String parent = signListFileLoc.parent();
            // 3. 判断上一级目录名称
            final VirtualContainer container = rl.getContainer(parent);
            if ("Signs".equalsIgnoreCase(container.getContainerName())) {
                container.clean();
            }
            // 某些不规范的文件Signatures.xml 不在Signs目录下，因此需要删除签名列表文件
            if (Files.exists(signaturesXMLFile)) {
                Files.delete(signaturesXMLFile);
            }
            // 4. 打包输出文件
            ofdDir.jar(out.toAbsolutePath());
        }
    }
}
