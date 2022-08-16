package org.ofdrw.layout;

import org.junit.jupiter.api.Test;
import org.ofdrw.core.action.actionType.actionGoto.CT_Dest;
import org.ofdrw.core.action.actionType.actionGoto.DestType;
import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.core.basicStructure.doc.bookmark.Bookmark;
import org.ofdrw.core.basicStructure.doc.bookmark.Bookmarks;
import org.ofdrw.core.basicStructure.pageObj.layer.Type;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.layout.edit.AdditionVPage;
import org.ofdrw.layout.edit.Attachment;
import org.ofdrw.layout.element.*;
import org.ofdrw.reader.OFDReader;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文档编辑示例
 *
 * @author 权观宇
 * @since 2021-06-01 22:57:16
 */
public class DocEditDemos {

    /**
     * 向文档中插入页面新的页面使用模板作为背景
     */
    @Test
    void vPageUseTemplateTest() throws IOException {
        Path srcP = Paths.get("src/test/resources", "fptpl.ofd");
        Path outP = Paths.get("target/template_insert.ofd");
        try (OFDReader reader = new OFDReader(srcP);
             OFDDoc ofdDoc = new OFDDoc(reader, outP)) {
            final ST_Box box = reader.getPageSize(1);

            final PageLayout pageBox = new PageLayout(box);
            VirtualPage vPage2 = new VirtualPage(pageBox);

            Path imgPath = Paths.get("src/test/resources", "eg_tulip.jpg");
            Img img = new Img(80, 53, imgPath);
            double x = (pageBox.getWidth() - img.getWidth()) / 2;
            double y = (pageBox.getHeight() - img.getHeight()) / 2;
            img.setPosition(Position.Absolute)
                    .setX(x).setY(y);
            img.setBorder(0.5d);
            vPage2.add(img);
            vPage2.addTemplate("9", null);

            // 插入
            ofdDoc.addVPage(vPage2);
        }
        System.out.println(">> 生成文档位置：" + outP.toAbsolutePath());

    }


    /**
     * 向已有文档中 插入 流式布局的内容
     */
    @Test
    void streamInsertTest() throws IOException {
        Path srcP = Paths.get("src/test/resources", "拿来主义_page6.ofd");
        Path outP = Paths.get("target/StreamInserted.ofd");
        try (OFDReader reader = new OFDReader(srcP);
             OFDDoc ofdDoc = new OFDDoc(reader, outP)) {
            // 插入到第1页 位置，后面的所有内容从第1页开始递增
            StreamCollect sPage1 = new StreamCollect(1);
            sPage1.setPageNum(1);
            Paragraph p = new Paragraph("封面", 30d).setWidth(100d);
            sPage1.add(p);
            // 换页： 插入占位符告诉解析器
            sPage1.add(new PageAreaFiller());
            // 这里开始 第2页 内容
            Paragraph p2 = new Paragraph("前言", 15d).setWidth(40d);
            sPage1.add(p2);
            ofdDoc.addStreamCollect(sPage1);

            // 插入到最后一页
            Paragraph p3 = new Paragraph("尾页", 15d).setWidth(100d);
            ofdDoc.add(p3);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

    /**
     * 向文档的指定页码插入页面
     * 原来位置上以及之后的页面，页面将会往后移动
     */
    @Test
    void pageInsertTest() throws IOException {
        Path srcP = Paths.get("src/test/resources", "拿来主义_page6.ofd");
        Path outP = Paths.get("target/PageInserted.ofd");
        try (OFDReader reader = new OFDReader(srcP);
             OFDDoc ofdDoc = new OFDDoc(reader, outP)) {
            PageLayout pageLayout = ofdDoc.getPageLayout();
            VirtualPage vPage1 = new VirtualPage(pageLayout);
            // 插入到第1页
            vPage1.setPageNum(1);
            Paragraph p = new Paragraph("封面", 30d);
            p.setPosition(Position.Absolute);
            p.setXY(60d, 60d);
            p.setWidth(100d);
            vPage1.add(p);
            ofdDoc.addVPage(vPage1);

            VirtualPage vPage2 = new VirtualPage(pageLayout);
            // 插入到第2页
            vPage2.setPageNum(2);
            Paragraph p2 = new Paragraph("前言", 15d);
            p2.setPosition(Position.Absolute);
            p2.setXY(20d, 20d);
            p2.setWidth(40d);
            vPage2.add(p2);
            ofdDoc.addVPage(vPage2);

            // 插入到最后一页
            VirtualPage vPage3 = new VirtualPage(pageLayout);
            Paragraph p3 = new Paragraph("尾页", 15d);
            p3.setPosition(Position.Absolute);
            p3.setXY(60d, 60d);
            p3.setWidth(100d);
            vPage3.add(p3);
            ofdDoc.addVPage(vPage3);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());

    }


    /**
     * 向已经存在页面内追加内容那
     */
    @Test
    void addContent2ExistPageTest() throws IOException {
        Path srcP = Paths.get("src/test/resources", "helloworld.ofd");
        Path outP = Paths.get("target/EditedDoc.ofd");
        try (OFDReader reader = new OFDReader(srcP);
             OFDDoc ofdDoc = new OFDDoc(reader, outP)) {
            AdditionVPage avPage = ofdDoc.getAVPage(1);
            Div e = new Div(10d, 10d)
                    .setPosition(Position.Absolute)
                    .setX(70d).setY(113.5)
                    .setBackgroundColor(255, 192, 203)
                    .setBorder(0.353d)
                    .setPadding(5d);
            Paragraph p = new Paragraph("测试文字测试文字测试文字测试文字", 15d)
                    .setPosition(Position.Absolute)
                    .setX(50d).setY(50d)
                    .setWidth(50d)
                    .setBorderColor(255, 0, 0)
                    .setBorder(3d)
                    .setPadding(3d);
            avPage.add(e);
            avPage.add(p);

        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }


    /**
     * 向文档末尾追加内容，新的内容会以新的页面追加到文档的最后一页
     */
    @Test
    void appendLasTest() throws IOException {
        Path srcP = Paths.get("src/test/resources", "helloworld.ofd");
        Path outP = Paths.get("target/AppendNewPage.ofd");
        try (OFDReader reader = new OFDReader(srcP);
             OFDDoc ofdDoc = new OFDDoc(reader, outP)) {
            String plaintext = "小时候\n" +
                    "乡愁是一枚小小的邮票\n" +
                    "我在这头\n" +
                    "母亲在那头\n" +
                    "\n" +
                    "长大后\n" +
                    "乡愁是一张窄窄的船票\n" +
                    "我在这头\n" +
                    "新娘在那头\n" +
                    "\n" +
                    "后来啊\n" +
                    "乡愁是一方矮矮的坟墓\n" +
                    "我在外头\n" +
                    "母亲在里头\n" +
                    "\n" +
                    "而现在\n" +
                    "乡愁是一湾浅浅的海峡\n" +
                    "我在这头\n" +
                    "大陆在那头\n";
            Span titleContent = new Span("乡愁").setBold(true).setFontSize(13d).setLetterSpacing(10d);
            Paragraph title = new Paragraph().add(titleContent);
            title.setFloat(AFloat.center).setMarginBottom(5d);
            ofdDoc.add(title);
            final String[] txtCollect = plaintext.split("\\\n");
            for (String txt : txtCollect) {
                Paragraph p = new Paragraph().setFontSize(4d)
                        .setLineSpace(3d)
                        .add(txt);
                ofdDoc.add(p);
            }
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

    /**
     * 设置不同的页面大小
     */
    @Test
    void setDiffPageSizeTest() throws IOException {
        Path srcP = Paths.get("src/test/resources", "helloworld.ofd");
        Path outP = Paths.get("target/SetDiffPageSizeTest.ofd");
        try (OFDReader reader = new OFDReader(srcP);
             OFDDoc ofdDoc = new OFDDoc(reader, outP)) {
            PageLayout pageLayout = ofdDoc.getPageLayout().clone();
            pageLayout.setWidth(595d).setHeight(842d);
            VirtualPage vPage1 = new VirtualPage(pageLayout);
            Paragraph p = new Paragraph("测试内容", 30d);
            p.setPosition(Position.Absolute);
            p.setXY(60d, 60d);
            p.setWidth(100d);
            vPage1.add(p);
            ofdDoc.addVPage(vPage1);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }


    /**
     * 向文件中加入附件文件
     */
    @Test
    void addAttachmentTest() throws IOException {
        Path outP = Paths.get("target/AddAttachment.ofd");
        Path file = Paths.get("src/test/resources", "eg_tulip.jpg");
        Path file2 = Paths.get("src/test/resources", "NotoSerifCJKsc-Regular.otf");

        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            Paragraph p = new Paragraph();
            Span span = new Span("这是一个带有附件的OFD文件").setFontSize(10d);
            p.add(span);
            ofdDoc.add(p);

            // 加入附件文件
            ofdDoc.addAttachment(new Attachment("Gao", file));
            ofdDoc.addAttachment(new Attachment("FontFile", file2));
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

    /**
     * 替换附件文件
     */
    @Test
    void replaceAttachmentTest() throws IOException {
        Path srcP = Paths.get("src/test/resources/AddAttachment.ofd");
        Path outP = Paths.get("target/ReplaceAttachment.ofd");
        Path file = Paths.get("src/test/resources", "ASCII字体宽度测量.html");

        try (OFDReader reader = new OFDReader(srcP);
             OFDDoc ofdDoc = new OFDDoc(reader, outP)) {
            // 加入附件文件
            ofdDoc.addAttachment(new Attachment("Gao", file));
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }


    /**
     * 添加书签
     */
    @Test
    void addBookmarkTest() throws IOException {
        Path srcP = Paths.get("src/test/resources/Page5.ofd");
        Path outP = Paths.get("target/addBookmark.ofd");
        try (OFDReader reader = new OFDReader(srcP);
             OFDDoc ofdDoc = new OFDDoc(reader, outP)) {
            Document document = ofdDoc.getOfdDocument();
            ST_ID pageID = document.getPages().getPageByIndex(3).getID();
            CT_Dest dest = new CT_Dest()
                    .setType(DestType.Fit)
                    .setPageID(pageID.ref());
            Bookmarks bookmarks = document.getBookmarks();
            if (bookmarks == null) {
                bookmarks = new Bookmarks();
                document.setBookmarks(bookmarks);
            }
            bookmarks.addBookmark(new Bookmark("美好一天", dest));
        }
    }
}
