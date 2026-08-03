package org.ofdrw.archive.convert.handler;

import org.ofdrw.archive.convert.ArchiveHandler;
import org.ofdrw.core.basicStructure.ofd.OFD;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.dom4j.DocumentException;

/**
 * 处理器 1：设置 DocType = "OFD-A"（GB/T 42133-2022 6.2.1a）
 * <p>
 * 将 OFD.xml 根节点的 DocType 属性修改为 "OFD-A"。
 *
 * @author 权观宇
 * @since 2.3.9
 */
public class DocTypeHandler implements ArchiveHandler {

    @Override
    public void handle(OFDReader reader, OFDDir ofdDir) throws IOException {
        try {
            OFD ofd = ofdDir.getOfd();
            ofd.setDocType("OFD-A");
            // 写回修改到 OFDDir
            ofdDir.setOfd(ofd);
        } catch (FileNotFoundException | DocumentException e) {
            throw new IOException("无法读取 OFD.xml: " + e.getMessage(), e);
        }
    }
}
