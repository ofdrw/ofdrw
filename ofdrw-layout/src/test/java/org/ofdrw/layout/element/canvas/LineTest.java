package org.ofdrw.layout.element.canvas;

import org.junit.jupiter.api.Test;
import org.ofdrw.layout.OFDDoc;
import org.ofdrw.layout.element.Position;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

class LineTest {
    
    @Test
    void testLine() throws IOException {
        Path outP = Paths.get("target/cell-as-line.ofd");
        
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            ofdDoc.add(new Line(50d, 50d, 100d, 100d)
                    .setBeginPoint(0,0)
                    .setEndPoint(50d,50d)
                    .setLineWidth(0.75d)
                    .setLineOpacity(0.5)
                    .setLineColor("rgb(255,0,0)"));
        }
        System.out.println(">> 生成文档位置：" + outP.toAbsolutePath());
    }
    
    @Test
    void cellAsLine() throws IOException {
        Path outP = Paths.get("target/cell-as-line.ofd");
        
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            //VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout()); // 绝对定位布局
            /**
             * Canvas 绘制原理：
             * 1、渲染器根据背景色和边框判断元素是否有绘制的必要，如果既没有背景色，又没有边框，则不会被绘制；
             * 2、元素绘制的时候，会根据元素坐标、大小、margin值创建画布，所有的绘制动作，都将在画布内完成，超出画布的信息，会被忽略掉
             * 3、ctx 的坐标系，基于画布定位
             */
            Canvas cell1 = new Canvas(50d, 50d);
            cell1.setMargin(10d, 10d, 10d, 10d);
            cell1.setX(50d);
            cell1.setY(50d);
            //cell1.setPosition(Position.Absolute);  // 绝对定位布局
            cell1.setPosition(Position.Static); //非绝对布局
            // 必须设置一个背景色
            cell1.setBackgroundColor("#ffffff");
            cell1.setDrawer(ctx -> {
                ctx.save();
                ctx.setLineWidth(0.5d);
                ctx.beginPath();
                ctx.strokeStyle = "rgb(255,0,0)";
                ctx.moveTo(0, 0);
                ctx.lineTo(50d , 50d);
                //ctx.rotate(45d);
                ctx.stroke();
                ctx.restore();
            });
            //vPage.add(cell1); // 绝对定位布局
            //ofdDoc.addVPage(vPage); // 绝对定位布局
            ofdDoc.add(cell1); // 流式布局
        }
        System.out.println(">> 生成文档位置：" + outP.toAbsolutePath());
    }
}