package org.ofdrw.converter.export;

import org.ofdrw.converter.GeneralConvertException;
import org.ofdrw.reader.ContentExtractor;
import org.ofdrw.reader.OFDReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

/**
 * OFD导出为纯文本
 * <p>
 * 注意：部分OFD文档由于采用字形索引来定位文字、有个OFD整个页面均为路径数据图元而不是文字图元、有的OFD页面整个都为图片等诸多原因。
 * 因此该导出器可能文档文本，另外由于文本布局等各种因素，导出文档也难以保证文本顺序与预期一致。
 *
 * @author 权观宇
 * @since 2023-3-14 21:07:41
 */
public class TextExporter implements OFDExporter {

    /**
     * OFD解析器
     */
    final OFDReader ofdReader;

    /**
     * 文本提取器
     */
    final ContentExtractor extractor;

    /**
     * 文字输出流
     */
    final PrintStream out;

    /**
     * 是否已经关闭
     */
    private boolean closed = false;

    /**
     * 构造图片转换器
     *
     * @param ofdFilePath 待转换OFD文件
     * @param txtPath     生成图片存放目录
     * @throws IOException 文件解析异常
     */
    public TextExporter(Path ofdFilePath, Path txtPath) throws IOException {
        ofdReader = new OFDReader(ofdFilePath);
        if (txtPath == null) {
            throw new IllegalArgumentException("导出文本文件路径为空");
        }
        txtPath = txtPath.toAbsolutePath();
        if (!Files.exists(txtPath)) {
            Path parent = txtPath.getParent();
            if (Files.exists(parent)) {
                if (!Files.isDirectory(parent)) {
                    throw new IllegalArgumentException("已经存在同名文件: " + parent);
                }
            } else {
                Files.createDirectories(parent);
            }
            Files.createFile(txtPath);
        }
        extractor = new ContentExtractor(ofdReader);
        out = new PrintStream(Files.newOutputStream(txtPath));
    }

    /**
     * 构造图片转换器
     *
     * @param ofdInput  待转换OFD文件流，该文件流由调用者负责关闭
     * @param txtOutput 文本输出流
     * @throws IOException 文件解析异常
     */
    public TextExporter(InputStream ofdInput, OutputStream txtOutput) throws IOException {
        ofdReader = new OFDReader(ofdInput);
        if (txtOutput == null) {
            throw new IllegalArgumentException("导出流为空");
        }
        extractor = new ContentExtractor(ofdReader);
        out = new PrintStream(txtOutput);
    }

    /**
     * 导出指定OFD页为图片
     *
     * @param indexes 页码序列，如果为空表示全部页码（注意：页码从0起）
     * @throws GeneralConvertException 转换异常
     */
    @Override
    public void export(int... indexes) throws GeneralConvertException {
        List<Integer> targetPages = new LinkedList<>();
        if (indexes == null || indexes.length == 0) {
            for (int i = 0; i < ofdReader.getNumberOfPages(); i++) {
                targetPages.add(i);
            }
        } else {
            int maxPageIndex = ofdReader.getNumberOfPages();
            // 获取指定页面信息
            for (int index : indexes) {
                if (index < 0 || index >= maxPageIndex) {
                    continue;
                }
                targetPages.add(index);
            }
        }

        for (Integer index : targetPages) {
            List<String> pageContent = extractor.getPageContent(index + 1);
            pageContent.forEach(out::println);
        }
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        if (out != null) {
            out.close();
        }

        if (ofdReader != null) {
            ofdReader.close();
        }
    }
}
