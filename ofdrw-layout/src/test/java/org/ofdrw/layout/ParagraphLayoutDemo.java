package org.ofdrw.layout;

import org.ofdrw.font.FontName;
import org.ofdrw.layout.element.AFloat;
import org.ofdrw.layout.element.Clear;
import org.ofdrw.layout.element.Paragraph;
import org.ofdrw.layout.element.Span;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 段落布局演示
 *
 * @author 权观宇
 * @since 2021-05-26 23:11:21
 */
public class ParagraphLayoutDemo {

    public static void main(String[] args) throws IOException {
        Path outP = Paths.get("ofdrw-layout/target/ParagraphLayoutDemo.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            // - 独占整个行
            Paragraph p1 = new Paragraph();
            p1.add("独占整个行").setBorder(0.1d).setMargin(3d, 0d);
            // - 共享模式(inline-block)
            Paragraph p2 = new Paragraph();
            p2.add("共享模式(inline-block),元素等同于内容宽度");
            p2.setClear(Clear.none).setBorder(0.1d).setMargin(3d, 0d);
            // 结束共享模式
            Paragraph hit1 = new Paragraph("插入一个Clear=both的元素中断共享模式");
            // - 手动指定宽度，仅在 Clear != both 模式有效
            Paragraph p3 = new Paragraph();
            p3.add("手动指定宽度，仅在 Clear != both 模式有效");
            p3.setWidth(55d);
            p3.setClear(Clear.none).setBorder(0.1d).setMargin(3d, 0d);
            Paragraph hit2 = new Paragraph("插入一个Clear=both的元素中断共享模式");

            // 居中
            Paragraph pn = new Paragraph();
            pn.setWidth(80d);
            pn.setHeight(20d);
            pn.setBorder(0.4d);
            pn.setPadding(5d);
            pn.setClear(Clear.none).setIntegrity(true);
            Span sp = new Span("序号")
                    .setFont(FontName.SimHei.font())
                    .setFontSize(4d);
            pn.add(sp);
            pn.setFloat(AFloat.center);

            Paragraph hit3 = new Paragraph("插入一个Clear=both的元素中断共享模式");

            // 共享模式下 多元素同行
            Paragraph p4A = new Paragraph("这是A部分");
            p4A.setClear(Clear.none).setFloat(AFloat.center).setBorder(0.1d).setPadding(3d).setMargin(1d);
            Paragraph p4B = new Paragraph("这是B部分");
            p4B.setClear(Clear.none).setFloat(AFloat.center).setBorder(0.1d).setPadding(3d).setMargin(1d);


            ofdDoc.add(p1);
            ofdDoc.add(p2);
            ofdDoc.add(hit1);
            ofdDoc.add(p3);
            ofdDoc.add(hit2);
            ofdDoc.add(pn);
            ofdDoc.add(hit3);
            ofdDoc.add(p4A).add(p4B);

        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath());
    }
}
