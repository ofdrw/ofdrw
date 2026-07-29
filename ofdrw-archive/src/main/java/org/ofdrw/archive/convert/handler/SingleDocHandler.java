package org.ofdrw.archive.convert.handler;

import org.ofdrw.archive.convert.ArchiveHandler;
import org.ofdrw.core.basicStructure.ofd.DocBody;
import org.ofdrw.core.basicStructure.ofd.OFD;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.dom4j.DocumentException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/**
 * 处理器 2：只保留第一个 DocBody（GB/T 42133-2022 6.2.1c）
 * <p>
 * 从 OFD.xml 中移除多余的 DocBody 节点，删除对应的 Doc_N 目录。
 * 不进行文档合并，只保留第一个文档体。
 *
 * @author xxx
 * @since 2.3.9
 */
public class SingleDocHandler implements ArchiveHandler {

    @Override
    public void handle(OFDReader reader, OFDDir ofdDir) throws IOException {
        OFD ofd;
        try {
            ofd = ofdDir.getOfd();
        } catch (FileNotFoundException | DocumentException e) {
            throw new IOException("无法读取 OFD.xml: " + e.getMessage(), e);
        }
        List<DocBody> docBodies = ofd.getDocBodies();

        if (docBodies.size() <= 1) {
            return;  // 已是单文档，无需处理
        }

        Path workDir = reader.getWorkDir();

        // 删除 DocBody[1..N] 对应的 Doc_N 目录
        for (int i = 1; i < docBodies.size(); i++) {
            DocBody body = docBodies.get(i);
            // 从 DocRoot 路径推断 Doc_N 目录名（如 Doc_1/xxx → Doc_1）
            String docRoot = body.getDocRoot().toString();
            String docDirName = docRoot;
            int slashIdx = docRoot.indexOf('/');
            if (slashIdx > 0) {
                docDirName = docRoot.substring(0, slashIdx);
            }

            Path docDir = workDir.resolve(docDirName);
            if (Files.exists(docDir)) {
                deleteRecursively(docDir);
            }
        }

        // 从 OFD 的 XML 中移除多余 DocBody 元素（保留第一个）
        @SuppressWarnings("unchecked")
        List<org.dom4j.Element> docBodyElements = ofd.elements("DocBody");
        for (int i = docBodyElements.size() - 1; i >= 1; i--) {
            docBodyElements.get(i).detach();
        }

        // 写回修改
        ofdDir.setOfd(ofd);
    }

    /**
     * 递归删除目录
     *
     * @param dir 待删除目录
     */
    private void deleteRecursively(Path dir) throws IOException {
        if (Files.notExists(dir)) return;
        Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                });
    }
}
