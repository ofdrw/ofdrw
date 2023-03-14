package org.ofdrw.converter.export;

import org.ofdrw.converter.GeneralConvertException;
import org.ofdrw.converter.HtmlMaker;
import org.ofdrw.converter.SVGMaker;
import org.ofdrw.reader.OFDReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

/**
 * 基于SVG转换的 OFD HTML转换器
 *
 * @author 权观宇
 * @since 2023-3-8 21:57:03
 */
public class HTMLExporter implements OFDExporter {

    /**
     * OFD解析器
     */
    final OFDReader ofdReader;
    /**
     * HTML转换器
     */
    final HtmlMaker htmlMaker;

    /**
     * SVG转换器
     */
    final SVGMaker svgMaker;

    /**
     * 文件输出位置
     */
    final OutputStream output;


    /*
<!DOCTYPE html>
<html lang="en">

<head>
  <meta charset="UTF-8">
  <meta http-equiv="X-UA-Compatible" content="IE=edge">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>文件预览</title>
</head>
<body style="margin: 0;background: #E6E8EB;height: 100%">
  <div style="display: flex; flex-direction: column;align-items: center;">
     <!--内容 -->
  </div>
</body>
</html>
     */

    /**
     * HTML文件头部
     * <p>
     * 您可以通过继承的方式重写HTML内容
     */
    byte[] header = ("<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "\n" +
            "<head>\n" +
            "  <meta charset=\"UTF-8\">\n" +
            "  <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n" +
            "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "  <title>文件预览</title>\n" +
            "</head>\n" +
            "<body style=\"margin: 0;background: #E6E8EB;height: 100%\">\n" +
            "  <div style=\"display: flex; flex-direction: column;align-items: center;\">" +
            "    <div style=\"height: 10px;\"></div>").getBytes(StandardCharsets.UTF_8);
    /**
     * 文件标签闭合
     * <p>
     * 您可以通过继承的方式重写HTML内容
     */
    byte[] booter = ("" +
            "  </div>\n" +
            "</body>\n" +
            "</html>").getBytes(StandardCharsets.UTF_8);

    /**
     * 每一页之间的间隔
     * <p>
     * 您可以通过继承的方式重写HTML内容
     */
    byte[] margin_bottom = "<div style=\"height: 10px;\"></div>".getBytes(StandardCharsets.UTF_8);

    /**
     * 是否已经关闭
     */
    private boolean closed = false;

    /**
     * 构造图片转换器
     *
     * @param ofdFilePath  待转换OFD文件
     * @param htmlFilePath 生成HTML存放目录
     * @throws IOException 文件解析异常
     */
    public HTMLExporter(Path ofdFilePath, Path htmlFilePath) throws IOException {
        if (htmlFilePath == null) {
            throw new IllegalArgumentException("导出HTML文件路径为空");
        }
        htmlFilePath = htmlFilePath.toAbsolutePath();
        if (!Files.exists(htmlFilePath)) {
            Path parent = htmlFilePath.getParent();
            if (Files.exists(parent)) {
                if (!Files.isDirectory(parent)) {
                    throw new IllegalArgumentException("已经存在同名文件: " + parent);
                }
            } else {
                Files.createDirectories(parent);
            }
            Files.createFile(htmlFilePath);
        }

        ofdReader = new OFDReader(ofdFilePath);
        htmlMaker = new HtmlMaker(ofdReader, 1000);
        svgMaker = new SVGMaker(ofdReader, 0);
        output = Files.newOutputStream(htmlFilePath);
        svgMaker.config.setDrawBoundary(false);
        svgMaker.config.setClip(false);
        output.write(header);
    }

    /**
     * 构造图片转换器
     *
     * @param ofdInput   待转换OFD文件流，该流由调用者负责关闭
     * @param htmlOutput 生成HTML输出流
     * @throws IOException 文件解析异常
     */
    public HTMLExporter(InputStream ofdInput, OutputStream htmlOutput) throws IOException {
        if (ofdInput == null) {
            throw new IllegalArgumentException("OFD流为空");
        }
        if (htmlOutput == null) {
            throw new IllegalArgumentException("HTML流为空");
        }
        ofdReader = new OFDReader(ofdInput);
        htmlMaker = new HtmlMaker(ofdReader, 1000);
        svgMaker = new SVGMaker(ofdReader, 0);
        svgMaker.config.setDrawBoundary(false);
        svgMaker.config.setClip(false);
        output = htmlOutput;
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

        try {
            for (Integer index : targetPages) {
                // 生成HTML Div
                String pageDiv = htmlMaker.makePageDiv(svgMaker, index);
                output.write(pageDiv.getBytes(StandardCharsets.UTF_8));
                output.write(margin_bottom);
            }
        } catch (IOException|RuntimeException e) {
            throw new RuntimeException("文件转换或文件写入异常", e);
        }
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        if (ofdReader != null) {
            ofdReader.close();
        }

        if (output != null) {
            output.write(booter);
            output.close();
        }
    }

}
