package org.ofdrw;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.util.function.Consumer;


public class TestTool {
    private static final OutputFormat FORMAT = OutputFormat.createPrettyPrint();

    private static final String TEST_DEST = "./target";

    /**
     * 读入文件验证
     *
     * @param name 文件
     * @param fn   验证方法
     */
    public static void validate(String name, Consumer<Element> fn) {
        try {
            SAXReader reader = new SAXReader();
            String filePath = TEST_DEST + File.separator + name + ".xml";
            Document document = reader.read(filePath);
            fn.accept(document.getRootElement());
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 传入XML文本段验证
     * @param content  XML文本段
     * @param fn 验证方法
     */
    public static void validateWithXML(String content, Consumer<Element> fn) {
        try {
            SAXReader reader = new SAXReader();
            Document document = reader.read(new ByteArrayInputStream(content.getBytes()));
            fn.accept(document.getRootElement());
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 获取对应元素的XML文本文件的字节内容
     *
     * @param element 元素
     * @return 字节内容
     */
    public static byte[] xmlByte(Element element) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            XMLWriter writerToSout = new XMLWriter(out, FORMAT);
            writerToSout.write(element);
            writerToSout.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 生成XML 并打印打控制台
     *
     * @param name 文件名称
     * @param call 元素添加方法
     */
    public static void genXml(String name, Consumer<Document> call) {
        Document doc = DocumentHelper.createDocument();
        String filePath = TEST_DEST + File.separator + name + ".xml";
        try (FileOutputStream out = new FileOutputStream(filePath)) {
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


    /**
     * 加入待测试元素，生成XML 并打印打控制台
     *
     * @param name    文件名称
     * @param element 元素
     */
    public static void genXml(String name, Element element) {
        genXml(name, doc -> doc.add(element));
    }
}
