package org.ofdrw.layout.element.canvas;

import org.junit.jupiter.api.Test;
import org.ofdrw.layout.OFDDoc;
import org.ofdrw.layout.VirtualPage;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

class CellContentDrawerTest {


    /**
     * 默认样式测试
     */
    @Test
    void addCellDefault() throws IOException {
        Path outP = Paths.get("target/cell-default.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());
            CellContentDrawer.DEBUG = true;

            CellContentDrawer cell = new CellContentDrawer(50, 50,100d, 50d);
            cell.setValue("OFD Reader And Writer");

            Canvas canvas = cell.getCanvas();
            canvas.setBorder(0.353d);
            vPage.add(canvas);

            ofdDoc.addVPage(vPage);
        }
        System.out.println(">> 生成文档位置：" + outP.toAbsolutePath());
    }


    /**
     * 右浮动
     */
    @Test
    void cellTextAlignRight() throws IOException {
        Path outP = Paths.get("target/cell-textalign-right.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());
            CellContentDrawer.DEBUG = true;

            CellContentDrawer cell = new CellContentDrawer(50, 50,100d, 50d);
            Canvas canvas = cell.getCanvas();
            canvas.setBorder(0.353d);
            cell.setValue("OFD Reader And Writer");
            cell.setTextAlign(TextAlign.right);

            vPage.add(canvas);

            ofdDoc.addVPage(vPage);
        }
        System.out.println(">> 生成文档位置：" + outP.toAbsolutePath());
    }

    /**
     * 右浮动
     */
    @Test
    void cellTextAlignCenter() throws IOException {
        Path outP = Paths.get("target/cell-textalign-center.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());
            CellContentDrawer.DEBUG = true;

            CellContentDrawer cell = new CellContentDrawer(50, 50,100d, 50d);
            Canvas canvas = cell.getCanvas();
            canvas.setBorder(0.353d);
//            canvas.setPadding(5d);
            cell.setValue("OFD Reader And Writer");
            cell.setTextAlign(TextAlign.center);

            vPage.add(canvas);

            ofdDoc.addVPage(vPage);
        }
        System.out.println(">> 生成文档位置：" + outP.toAbsolutePath());
    }

    /**
     * 顶部浮动
     */
    @Test
    void cellVerticalTop() throws IOException {
        Path outP = Paths.get("target/cell-top.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());
            CellContentDrawer.DEBUG = true;

            CellContentDrawer cell = new CellContentDrawer(50, 50,100d, 50d);
            Canvas canvas = cell.getCanvas();
            canvas.setBorder(0.353d);
            cell.setValue("OFD Reader And Writer");
            cell.setVerticalAlign(VerticalAlign.top);
            cell.setTextAlign(TextAlign.left);

            vPage.add(canvas);
            ofdDoc.addVPage(vPage);
        }
        System.out.println(">> 生成文档位置：" + outP.toAbsolutePath());
    }

    /**
     * 顶部浮动
     */
    @Test
    void cellVerticalBottomRight() throws IOException {
        Path outP = Paths.get("target/cell-bottom-right.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());
            CellContentDrawer.DEBUG = true;

            CellContentDrawer cell = new CellContentDrawer(50, 50,100d, 50d);
            Canvas canvas = cell.getCanvas();
            canvas.setBorder(0.353d);
            cell.setValue("OFD Reader And Writer");
            cell.setVerticalAlign(VerticalAlign.bottom);
            cell.setTextAlign(TextAlign.right);

            vPage.add(canvas);
            ofdDoc.addVPage(vPage);
        }
        System.out.println(">> 生成文档位置：" + outP.toAbsolutePath());
    }

    /**
     * 超长内容自动换行
     */
    @Test
    void cellLongTextAutoLineBreak() throws IOException {
        Path outP = Paths.get("target/cell-long-text.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());
            CellContentDrawer.DEBUG = true;

            CellContentDrawer cell = new CellContentDrawer(50, 50,100d, 50d);
            Canvas canvas = cell.getCanvas();
            canvas.setBorder(0.353d);
            cell.setValue("OFD Reader And Writer OFD Reader And Writer OFD Reader And Writer OFD Reader And Writer");
            vPage.add(canvas);
            ofdDoc.addVPage(vPage);
        }
        System.out.println(">> 生成文档位置：" + outP.toAbsolutePath());
    }

    /**
     * 超长内容含换行符换行
     */
    @Test
    void cellLongText() throws IOException {
        Path outP = Paths.get("target/cell-long-text-line-break.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());
            CellContentDrawer.DEBUG = true;

            CellContentDrawer cell = new CellContentDrawer(50, 50,100d, 50d);
            Canvas canvas = cell.getCanvas();
            canvas.setBorder(0.353d);
            cell.setValue("OFD Reader An\nd Writer OFD R\nea\n\nder And Writer OFD Reader And Writer OFD Reader And Writer");

            vPage.add(canvas);
            ofdDoc.addVPage(vPage);
        }
        System.out.println(">> 生成文档位置：" + outP.toAbsolutePath());
    }
    /**
     * 超长内容换行 右浮动
     */
    @Test
    void cellLongTextRight() throws IOException {
        Path outP = Paths.get("target/cell-long-text-right.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());
            CellContentDrawer.DEBUG = true;

            CellContentDrawer cell = new CellContentDrawer(50, 50,100d, 50d);
            Canvas canvas = cell.getCanvas();
            canvas.setBorder(0.353d);
            cell.setTextAlign(TextAlign.right);
            cell.setValue("OFD Reader An\nd Writer OFD R\nea\n\nder And Writer OFD Reader And Writer OFD Reader And Writer");

            vPage.add(canvas);
            ofdDoc.addVPage(vPage);
        }
        System.out.println(">> 生成文档位置：" + outP.toAbsolutePath());
    }

    /**
     * 加粗
     */
    @Test
    void cellBlob() throws IOException {
        Path outP = Paths.get("target/cell-blob.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());
            CellContentDrawer.DEBUG = true;

            CellContentDrawer cell = new CellContentDrawer(50, 50,100d, 50d);
            Canvas canvas = cell.getCanvas();
            canvas.setBorder(0.353d);
            cell.setValue("OFD Reader And Writer OFD Reader And Writer OFD Reader And Writer OFD Reader And Writer");
            cell.setBlob(true);

            vPage.add(canvas);
            ofdDoc.addVPage(vPage);
        }
        System.out.println(">> 生成文档位置：" + outP.toAbsolutePath());
    }

    /**
     * 加粗
     */
    @Test
    void cellItalic() throws IOException {
        Path outP = Paths.get("target/cell-italic.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());
            CellContentDrawer.DEBUG = true;

            CellContentDrawer cell = new CellContentDrawer(50, 50,100d, 50d);
            Canvas canvas = cell.getCanvas();
            canvas.setBorder(0.353d);
            cell.setValue("OFD Reader And Writer OFD Reader And Writer OFD Reader And Writer OFD Reader And Writer");
            cell.setItalic(true);

            vPage.add(canvas);
            ofdDoc.addVPage(vPage);
        }
        System.out.println(">> 生成文档位置：" + outP.toAbsolutePath());
    }

    /**
     * 设置字间距
     */
    @Test
    void cellLetterSpace() throws IOException {
        Path outP = Paths.get("target/cell-letter-space.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {

            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());
            CellContentDrawer.DEBUG = true;
            CellContentDrawer cell = new CellContentDrawer(50, 50,100d, 50d);
            Canvas canvas = cell.getCanvas();
            canvas.setBorder(0.353d);
            cell.setValue("OFD Reader And Writer OFD Reader And Writer OFD Reader And Writer OFD Reader And Writer");
            cell.setLetterSpacing(3d);

            vPage.add(canvas);
            ofdDoc.addVPage(vPage);
        }
        System.out.println(">> 生成文档位置：" + outP.toAbsolutePath());
    }
}