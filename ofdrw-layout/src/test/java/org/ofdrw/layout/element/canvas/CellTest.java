package org.ofdrw.layout.element.canvas;

import org.junit.jupiter.api.Test;
import org.ofdrw.layout.OFDDoc;
import org.ofdrw.layout.VirtualPage;
import org.ofdrw.layout.element.AFloat;
import org.ofdrw.layout.element.Display;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class CellTest {


    /**
     * 流式页面布局
     */
    @Test
    void cell() throws IOException {
        Path outP = Paths.get("target/cell-element.ofd");
//        CellContentDrawer.DEBUG = true;
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            Cell cell1 = new Cell(20d, 10d);
            cell1.setBorder(0.353);
            cell1.setTextAlign(TextAlign.center);
            cell1.setPadding(1d, 3d, 1d, 3d);
            cell1.setValue("名 称");
            cell1.setFontSize(5d);
            cell1.setDisplay(Display.inlineBlock);
            cell1.setBackgroundColor("#CDD0D6");


            Cell cell2 = new Cell(50d, 10d);
            cell2.setBorder(0.353);
            cell2.setTextAlign(TextAlign.center);
            cell2.setPadding(1d, 3d, 1d, 3d);
            cell2.setValue("OFD Reader & Writer");
            cell2.setFontSize(5d);
            cell2.setMarginLeft(-0.353);
            cell2.setDisplay(Display.inlineBlock);

            ofdDoc.add(cell1);
            ofdDoc.add(cell2);
        }
        System.out.println(">> 生成文档位置：" + outP.toAbsolutePath());
    }

    /**
     * 虚拟页面绝对定位布局
     */
    @Test
    void cellVPage() throws IOException {
        Path outP = Paths.get("target/cell-element-vpage.ofd");
//        CellContentDrawer.DEBUG = true;
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());
            Cell cell1 = new Cell(10, 10, 20d, 10d);
            cell1.setBorder(0.353);
            cell1.setTextAlign(TextAlign.center);
            cell1.setPadding(1d, 3d, 1d, 3d);
            cell1.setValue("名 称");
            cell1.setFontSize(5d);
            cell1.setBackgroundColor("#CDD0D6");

            // 元素实际大小为： margin-left + border-left + padding-left + width + padding-right + border-right + margin-right
            // 偏移坐标为： 原始偏移量 + 元素实际宽度 - 右边宽度
            Cell cell2 = new Cell(10 + 0.353 + 3d + 20d + 3d /* ,0.353 */, 10, 50d, 10d);
            cell2.setBorder(0.353);
            cell2.setTextAlign(TextAlign.center);
            cell2.setPadding(1d, 3d, 1d, 3d);
            cell2.setValue("OFD Reader & Writer");
            cell2.setFontSize(5d);

            vPage.add(cell1);
            vPage.add(cell2);

            ofdDoc.addVPage(vPage);
        }
        System.out.println(">> 生成文档位置：" + outP.toAbsolutePath());
    }


    /**
     * 图片内容
     */
    @Test
    void cellImage() throws IOException {
        Path outP = Paths.get("target/cell-image.ofd");
        Path img = Paths.get("src/test/resources/rhino.jpg");

//        CellContentDrawer.DEBUG = true;
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            Cell cell1 = new Cell(20d, 50d);
            cell1.setBorder(0.353);
            cell1.setTextAlign(TextAlign.center);
            cell1.setPadding(1d, 3d, 1d, 3d);

            cell1.setValue("头 像");

            cell1.setFontSize(5d);
            cell1.setDisplay(Display.inlineBlock);
            cell1.setBackgroundColor("#CDD0D6");


            Cell cell2 = new Cell(50d, 50d);
            cell2.setBorder(0.353);
            cell2.setTextAlign(TextAlign.center);
            cell2.setPadding(1d, 3d, 1d, 3d);

            cell2.setValue(img, 40, 30);

            cell2.setFontSize(5d);
            cell2.setMarginLeft(-0.353);
            cell2.setDisplay(Display.inlineBlock);

            ofdDoc.add(cell1);
            ofdDoc.add(cell2);
        }
        System.out.println(">> 生成文档位置：" + outP.toAbsolutePath());
    }

    /**
     * 流式布局内容居中
     */
    @Test
    void cellCenter() throws IOException {
        Path outP = Paths.get("target/cell-element-center.ofd");
//        CellContentDrawer.DEBUG = true;
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            Cell cell1 = new Cell(20d, 10d);
            cell1.setBorder(0.353);
            cell1.setTextAlign(TextAlign.center);
            cell1.setPadding(1d, 3d, 1d, 3d);
            cell1.setValue("名 称");
            cell1.setFontSize(5d);
            cell1.setDisplay(Display.inlineBlock);
            cell1.setBackgroundColor("#CDD0D6");

            cell1.setFloat(AFloat.center);


            Cell cell2 = new Cell(50d, 10d);
            cell2.setBorder(0.353);
            cell2.setTextAlign(TextAlign.center);
            cell2.setPadding(1d, 3d, 1d, 3d);
            cell2.setValue("OFD Reader & Writer");
            cell2.setFontSize(5d);
            cell2.setMarginLeft(-0.353);
            cell2.setDisplay(Display.inlineBlock);

            cell2.setFloat(AFloat.center);

            ofdDoc.add(cell1);
            ofdDoc.add(cell2);
        }
        System.out.println(">> 生成文档位置：" + outP.toAbsolutePath());
    }

    /**
     * 设置字间距
     */
    @Test
    void setLetterSpacing() throws IOException {
        Path outP = Paths.get("target/cell-element-letterSpacing.ofd");
        CellContentDrawer.DEBUG = true;
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            Cell cell1 = new Cell(20d, 10d);
            cell1.setBorder(0.353);
            cell1.setTextAlign(TextAlign.center);
            cell1.setPadding(1d, 3d, 1d, 3d);
            cell1.setValue("名称");
            // 设置字间距
            cell1.setLetterSpacing(10d);

            cell1.setFontSize(5d);
            cell1.setDisplay(Display.inlineBlock);
            cell1.setBackgroundColor("#CDD0D6");
            cell1.setFloat(AFloat.center);


            Cell cell2 = new Cell(50d, 10d);
            cell2.setBorder(0.353);
            cell2.setTextAlign(TextAlign.center);
            cell2.setPadding(1d, 3d, 1d, 3d);
            cell2.setValue("OFD Reader & Writer");
            cell2.setFontSize(5d);
            cell2.setMarginLeft(-0.353);
            cell2.setDisplay(Display.inlineBlock);
            cell2.setFloat(AFloat.center);
            ofdDoc.add(cell1);
            ofdDoc.add(cell2);
        }
        System.out.println(">> 生成文档位置：" + outP.toAbsolutePath());
    }

    /**
     * 设置行间距
     */
    @Test
    void setLineHeight() throws IOException {
        Path outP = Paths.get("target/cell-element-lineHeight.ofd");
        CellContentDrawer.DEBUG = true;
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            Cell cell1 = new Cell(20d, 80d);
            cell1.setBorder(0.353);
            cell1.setTextAlign(TextAlign.center);
            cell1.setPadding(1d, 3d, 1d, 3d);
            cell1.setValue("名 称");
            cell1.setFontSize(5d);
            cell1.setDisplay(Display.inlineBlock);
            cell1.setBackgroundColor("#CDD0D6");
            cell1.setFloat(AFloat.center);


            Cell cell2 = new Cell(40d, 80d);
            cell2.setBorder(0.353);
            cell2.setTextAlign(TextAlign.center);
            cell2.setPadding(1d, 3d, 1d, 3d);
            cell2.setValue("OFD Reader & Writer OFD Reader & Writer OFD Reader & Writer OFD Reader & Writer OFD Reader & Writer OFD Reader & Writer");

            // 设置行间距
            cell2.setLineSpace(3d);

            cell2.setFontSize(5d);
            cell2.setMarginLeft(-0.353);
            cell2.setDisplay(Display.inlineBlock);
            cell2.setFloat(AFloat.center);

            ofdDoc.add(cell1);
            ofdDoc.add(cell2);
        }
        System.out.println(">> 生成文档位置：" + outP.toAbsolutePath());
    }
}