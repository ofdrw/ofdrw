package org.ofdrw.pkg.container;

import org.dom4j.Element;
import org.ofdrw.pkg.tool.ElemCup;

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
     * 在临时目录中生成XML
     * @param e xml 元素
     * @throws IOException IO异常
     */
    public static void dumpToTmpReview(Element e) throws IOException {
        Path tmp = Files.createTempFile(Paths.get("target"), "ofdrwtest-", ".xml");
        ElemCup.dump(e, tmp);
        System.out.println("生成位置: " + tmp.toAbsolutePath().toString());
    }
}
