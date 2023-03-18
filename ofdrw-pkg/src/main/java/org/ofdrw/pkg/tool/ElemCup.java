package org.ofdrw.pkg.tool;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 元素杯子
 * <p>
 * 对象序列化和反序列化工具
 * <p>
 * 反序列化 向杯子中注入水
 * <p>
 * 序列化把杯子中的水倒出
 *
 * @author 权观宇
 * @since 2020-4-2 20:20:57
 */
public class ElemCup {

    /**
     * 命名空间修改器
     */
    private static final OFDNameSpaceModifier SpaceModifier = new OFDNameSpaceModifier();

    /**
     * 输出模式
     */
    private static final OutputFormat PrettyPrint = OutputFormat.createPrettyPrint();

    /**
     * 是否允许调试模式输出XML内容
     * <p>
     * true - 开启，XML含有空格和换行
     * false - 关闭。XML采用紧凑模式输出，（默认）
     */
    public static boolean ENABLE_DEBUG_PRINT = false;

    /**
     * 从文件加载反序列化元素对象
     *
     * @param file 文件路径对象
     * @return 反序列化的元素对象
     * @throws DocumentException 文件解析异常
     */
    public static Element inject(Path file) throws DocumentException {
        SAXReader reader = SAXReaderFactory.create();
        try (InputStream in = Files.newInputStream(file)) {
            Document document = reader.read(in);
            return document.getRootElement();
        } catch (IOException e) {
            throw new DocumentException(e);
        }
    }

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
        if (e.getDocument() != null) {
            // 如果元素所属文档不为空，说明是从文件中加载得到，此时需要Clone这个对象以放入新的Document中
            e = (Element) e.clone();
        }
        doc.add(e);
        try (OutputStream out = Files.newOutputStream(to)) {
            XMLWriter writeToFile;
            if (ENABLE_DEBUG_PRINT) {
                writeToFile = new XMLWriter(out, PrettyPrint);
            } else {
                writeToFile = new XMLWriter(out);
            }
            writeToFile.write(doc);
            writeToFile.close();
        }
    }

    /**
     * 序列化元素并升级命名空间
     * <p>
     * 命名空间为 {@link org.ofdrw.core.Const#OFD_NAMESPACE}
     *
     * @param e  元素
     * @param to 存储位置
     * @throws IOException IO异常
     */
    public static void dumpUpNS(Element e, Path to) throws IOException {
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
        if (e.getDocument() != null) {
            // 如果元素所属文档不为空，说明是从文件中加载得到，此时需要Clone这个对象以放入新的Document中
            e = (Element) e.clone();
        }
        doc.add(e);
        doc.accept(SpaceModifier);
        try (OutputStream out = Files.newOutputStream(to)) {
            XMLWriter writeToFile;
            if (ENABLE_DEBUG_PRINT) {
                writeToFile = new XMLWriter(out, PrettyPrint);
            } else {
                writeToFile = new XMLWriter(out);
            }
            writeToFile.write(doc);
            writeToFile.close();
        }
    }

    /**
     * 序列化DOM元素为字节序列
     *
     * @param e 元素
     * @return XML UTF-8编码后的字节数字
     * @throws IOException IO异常
     */
    public static byte[] dump(Element e) throws IOException {
        Document doc = DocumentHelper.createDocument();
        if (e.getDocument() != null) {
            // 如果元素所属文档不为空，说明是从文件中加载得到，此时需要Clone这个对象以放入新的Document中
            e = (Element) e.clone();
        }
        doc.add(e);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        XMLWriter writeToFile;
        if (ENABLE_DEBUG_PRINT) {
            writeToFile = new XMLWriter(bout, PrettyPrint);
        } else {
            writeToFile = new XMLWriter(bout);
        }
        writeToFile.write(doc);
        writeToFile.close();
        return bout.toByteArray();
    }
}
