package org.ofdrw.pkg.dir;

import org.dom4j.Element;
import org.ofdrw.pkg.tool.DocObjDump;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Test Tools
 *
 * @author 权观宇
 * @since 2020-01-20 15:11:44
 */
public class TT {
    /**
     * 在临时目录中生成
     *
     * @param dir 目录对象
     * @throws IOException IO异常
     */
    public static void dumpToTmpReview(DirCollect dir) throws IOException {
        Path tmp = Files.createTempDirectory(Paths.get("target"), "ofdrwtest-");
        Path result = dir.collect(tmp.toAbsolutePath().toString());
        System.out.println("生成位置: " + result.toAbsolutePath().toString());
    }

    /**
     * 在临时目录中生成XML
     * @param e xml 元素
     * @throws IOException IO异常
     */
    public static void dumpToTmpReview(Element e) throws IOException {
        Path tmp = Files.createTempFile(Paths.get("target"), "ofdrwtest-", ".xml");
        DocObjDump.dump(e, tmp);
        System.out.println("生成位置: " + tmp.toAbsolutePath().toString());
    }
}
