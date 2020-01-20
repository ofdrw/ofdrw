package org.ofdrw.pkg.tool;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.XMLWriter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 对象序列化
 *
 * @author 权观宇
 * @since 2020-01-20 14:45:02
 */
public class DocObjDump {

    /**
     * 序列化文档对象
     *
     * @param e  文档对象
     * @param to 目标目录
     * @throws IOException IO异常
     */
    public static void dump(Element e, Path to) throws IOException {
        if (e == null) {
            return;
        }
        if (to == null) {
            throw new IllegalArgumentException("文档元素序列化目标目录（to）为空");
        }
        if (Files.notExists(to)) {
            if (Files.notExists(to.getParent())) {
                Files.createDirectories(to.getParent());
            }
            Files.createFile(to);
        }

        Document doc = DocumentHelper.createDocument();
        doc.add(e);
        try (OutputStream out = Files.newOutputStream(to)) {
            XMLWriter writeToFile = new XMLWriter(out);
            writeToFile.write(doc);
            writeToFile.close();
        }
    }
}
