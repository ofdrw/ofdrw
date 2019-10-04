package org.ofdrw;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.ofdrw.core.basicType.ST_Loc;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.function.Consumer;


public class TestTool {
    private  static  OutputFormat FORMAT = OutputFormat.createPrettyPrint();

    public static void genXml(String testName, Consumer<Document> call) {
        Document doc = DocumentHelper.createDocument();
        try (FileOutputStream out = new FileOutputStream(testName + ".xml")) {
            call.accept(doc);

            // 格式化打印到控制台
            XMLWriter writerToSout = new XMLWriter(System.out, FORMAT);
            writerToSout.write(doc);

            // 打印打到文件
            XMLWriter writeToFile = new XMLWriter(out, FORMAT);
            writeToFile.write(doc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
