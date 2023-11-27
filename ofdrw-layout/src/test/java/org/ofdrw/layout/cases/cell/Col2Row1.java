package org.ofdrw.layout.cases.cell;

import org.ofdrw.layout.OFDDoc;
import org.ofdrw.layout.element.Display;
import org.ofdrw.layout.element.canvas.Cell;
import org.ofdrw.layout.element.canvas.TextAlign;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 一行两列的表单
 *
 * @author 权观宇
 * @since 2023-11-27 18:51:30
 */
public class Col2Row1 {
    public static void main(String[] args) throws IOException {
        Path outP = Paths.get("cell-col2row1.ofd");
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
}
