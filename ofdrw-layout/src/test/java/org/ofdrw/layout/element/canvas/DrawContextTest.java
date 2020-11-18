package org.ofdrw.layout.element.canvas;

import org.junit.jupiter.api.Test;
import org.ofdrw.core.pageDescription.drawParam.LineCapType;
import org.ofdrw.core.pageDescription.drawParam.LineJoinType;
import org.ofdrw.font.FontName;
import org.ofdrw.layout.OFDDoc;
import org.ofdrw.layout.VirtualPage;
import org.ofdrw.layout.element.Position;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Canvas 测试
 *
 * @author 权观宇
 * @since 2020-05-02 18:25:25
 */
class DrawContextTest {


    @Test
    void stroke() throws IOException {
        Path outP = Paths.get("target/CanvasLineCreate.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());

            Canvas canvas = new Canvas(200d, 200d);
            canvas.setPosition(Position.Absolute)
                    .setX(5d).setY(45d)
                    .setBorder(1d);

            canvas.setDrawer(ctx -> {
                ctx.beginPath()
                        .moveTo(20, 20)
                        .lineTo(20, 100)
                        .lineTo(70, 100)
                        .setStrokeColor(0, 255, 0)
                        .stroke();
            });
            vPage.add(canvas);

            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }


    @Test
    void fill() throws IOException {
        Path outP = Paths.get("target/Canvas-fill.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());

            Canvas canvas = new Canvas(200d, 200d);
            canvas.setPosition(Position.Absolute)
                    .setX(5d).setY(45d)
                    .setBorder(1d);

            canvas.setDrawer(ctx -> {
                ctx.beginPath()
                        .moveTo(20, 20)
                        .lineTo(20, 100)
                        .lineTo(70, 100)
                        .closePath()
                        .setFillColor(255, 0, 0)
                        .stroke()
                        .fill();
            });
            vPage.add(canvas);

            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

    @Test
    void beginPath() throws IOException {
        Path outP = Paths.get("target/Canvas-beginPath.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());

            Canvas canvas = new Canvas(200d, 200d);
            canvas.setPosition(Position.Absolute)
                    .setX(5d).setY(45d)
                    .setBorder(1d);

            canvas.setDrawer(ctx -> {
                ctx.beginPath();
                ctx.setLineWidth(3d);
                ctx.setStrokeColor(255, 0, 0);
                ctx.moveTo(0, 75);
                ctx.lineTo(150, 75);
                ctx.stroke();

                ctx.beginPath();
                ctx.setStrokeColor(0, 0, 255);
                ctx.moveTo(10, 0);
                ctx.lineTo(150, 130);
                ctx.stroke();
            });
            vPage.add(canvas);

            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

    @Test
    void clip() throws IOException {
        Path outP = Paths.get("target/Canvas-clip.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());

            Canvas canvas = new Canvas(200d, 200d);
            canvas.setPosition(Position.Absolute)
                    .setX(5d).setY(45d)
                    .setBorder(1d);

            canvas.setDrawer(ctx -> {
                // 剪切矩形区域
                ctx.rect(50, 20, 200, 120);
                ctx.stroke();
                ctx.clip();
                // 在 clip() 之后绘制绿色矩形
                ctx.setFillColor(0, 255, 0);
                ctx.fillRect(0, 0, 150, 100);
            });
            vPage.add(canvas);

            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

    @Test
    void quadraticCurveTo() throws IOException {
        Path outP = Paths.get("target/Canvas-quadraticCurveTo.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());

            Canvas canvas = new Canvas(200d, 200d);
            canvas.setPosition(Position.Absolute)
                    .setX(5d).setY(45d)
                    .setBorder(1d);

            canvas.setDrawer(ctx -> {
                ctx.beginPath();
                ctx.moveTo(20, 20);
                ctx.quadraticCurveTo(20, 100, 200, 20);
                ctx.stroke();
            });
            vPage.add(canvas);

            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

    @Test
    void bezierCurveTo() throws IOException {
        Path outP = Paths.get("target/Canvas-bezierCurveTo.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());

            Canvas canvas = new Canvas(200d, 200d);
            canvas.setPosition(Position.Absolute)
                    .setX(5d).setY(45d)
                    .setBorder(1d);

            canvas.setDrawer(ctx -> {
                ctx.beginPath();
                ctx.moveTo(20, 20);
                ctx.bezierCurveTo(20, 100, 200, 100, 200, 20);
                ctx.stroke();
            });
            vPage.add(canvas);

            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

    @Test
    void arc() throws IOException {
        Path outP = Paths.get("target/Canvas-arc.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());

            Canvas canvas = new Canvas(200d, 200d);
            canvas.setPosition(Position.Absolute)
                    .setX(5d).setY(45d)
                    .setBorder(1d);

            canvas.setDrawer(ctx -> {
                ctx.beginPath();
                ctx.arc(100, 75, 50, 0, 360);
                ctx.stroke();
            });
            vPage.add(canvas);

            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }


    @Test
    void rect() throws IOException {
        Path outP = Paths.get("target/Canvas-rect.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());

            Canvas canvas = new Canvas(200d, 200d);
            canvas.setPosition(Position.Absolute)
                    .setX(5d).setY(45d)
                    .setBorder(1d);

            canvas.setDrawer(ctx -> {
                ctx.rect(20, 20, 150, 100);
                ctx.stroke();
            });
            vPage.add(canvas);

            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

    @Test
    void strokeRect() throws IOException {
        Path outP = Paths.get("target/Canvas-strokeRect.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());

            Canvas canvas = new Canvas(200d, 200d);
            canvas.setPosition(Position.Absolute)
                    .setX(5d).setY(45d)
                    .setBorder(1d);

            canvas.setDrawer(ctx -> {
                ctx.strokeRect(20, 20, 150, 100);
            });
            vPage.add(canvas);

            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

    @Test
    void fillRect() throws IOException {
        Path outP = Paths.get("target/Canvas-fillRect.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());

            Canvas canvas = new Canvas(200d, 200d);
            canvas.setPosition(Position.Absolute)
                    .setX(5d).setY(45d)
                    .setBorder(1d);

            canvas.setDrawer(ctx -> {
                ctx.fillRect(20, 20, 150, 100);
            });
            vPage.add(canvas);

            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }


    @Test
    void drawImage() throws IOException {
        Path outP = Paths.get("target/Canvas-drawImage.ofd");
        Path imgPath = Paths.get("src/test/resources/eg_tulip.jpg");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());

            Canvas canvas = new Canvas(200d, 200d);
            canvas.setPosition(Position.Absolute)
                    .setX(5d).setY(45d)
                    .setBorder(1d);

            canvas.setDrawer(ctx -> {
                ctx.drawImage(imgPath, 10, 10, 120, 80);
            });
            vPage.add(canvas);

            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

    @Test
    void drawImage2() throws IOException {
        Path outP = Paths.get("target/Canvas-drawImage2.ofd");
        Path imgPath = Paths.get("src/test/resources/eg_tulip.jpg");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());

            Canvas canvas = new Canvas(200d, 200d);
            canvas.setPosition(Position.Absolute)
                    .setX(5d).setY(45d)
                    .setBorder(1d);

            canvas.setDrawer(ctx -> {
                ctx.rotate(20);
                ctx.drawImage(imgPath, 10, 10, 60, 40);
            });
            vPage.add(canvas);

            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

    @Test
    void scale() throws IOException {
        Path outP = Paths.get("target/Canvas-scale.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());

            Canvas canvas = new Canvas(200d, 200d);
            canvas.setPosition(Position.Absolute)
                    .setX(5d).setY(45d)
                    .setBorder(1d);

            canvas.setDrawer(ctx -> {
                ctx.strokeRect(5, 5, 10, 6);
                ctx.scale(2, 2);
                ctx.strokeRect(5, 5, 10, 6);
                ctx.scale(2, 2);
                ctx.strokeRect(5, 5, 10, 6);
                ctx.scale(2, 2);
                ctx.strokeRect(5, 5, 10, 6);
            });
            vPage.add(canvas);

            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

    @Test
    void rotate() throws IOException {
        Path outP = Paths.get("target/Canvas-rotate.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());

            Canvas canvas = new Canvas(200d, 200d);
            canvas.setPosition(Position.Absolute)
                    .setX(5d).setY(45d)
                    .setBorder(1d);

            canvas.setDrawer(ctx -> {
                ctx.rotate(20);
                ctx.fillRect(50, 20, 100, 50);
            });
            vPage.add(canvas);

            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

    @Test
    void translate() throws IOException {
        Path outP = Paths.get("target/Canvas-translate.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());

            Canvas canvas = new Canvas(200d, 200d);
            canvas.setPosition(Position.Absolute)
                    .setX(5d).setY(45d)
                    .setBorder(1d);

            canvas.setDrawer(ctx -> {
                ctx.fillRect(10, 10, 50, 25);
                ctx.translate(30, 30);
                ctx.fillRect(10, 10, 50, 25);
            });
            vPage.add(canvas);

            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

    @Test
    void transform() throws IOException {
        Path outP = Paths.get("target/Canvas-transform.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());

            Canvas canvas = new Canvas(200d, 200d);
            canvas.setPosition(Position.Absolute)
                    .setX(5d).setY(45d)
                    .setBorder(1d);

            canvas.setDrawer(ctx -> {

                ctx.setFillColor(255, 255, 0);
                ctx.fillRect(0, 0, 100, 50);

                ctx.transform(1, 0.5, -0.5, 1, 30, 10);
                ctx.setFillColor(255, 0, 0);
                ctx.fillRect(0, 0, 100, 50);

                ctx.transform(1, 0.5, -0.5, 1, 30, 10);
                ctx.setFillColor(0, 0, 255);
                ctx.fillRect(0, 0, 100, 50);

            });
            vPage.add(canvas);

            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

    @Test
    void setTransform() throws IOException {
        Path outP = Paths.get("target/Canvas-setTransform.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());

            Canvas canvas = new Canvas(200d, 200d);
            canvas.setPosition(Position.Absolute)
                    .setX(5d).setY(45d)
                    .setBorder(1d);

            canvas.setDrawer(ctx -> {
                ctx.setFillColor(255, 255, 0);
                ctx.fillRect(0, 0, 100, 50);

                ctx.setTransform(1, 0.5, -0.5, 1, 30, 10);
                ctx.setFillColor(255, 0, 0);
                ctx.fillRect(0, 0, 100, 50);

                ctx.setTransform(1, 0.5, -0.5, 1, 30, 10);
                ctx.setFillColor(0, 0, 255);
                ctx.fillRect(0, 0, 100, 50);

            });
            vPage.add(canvas);

            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

    @Test
    void getGlobalAlpha() throws IOException {
        Path outP = Paths.get("target/Canvas-globalAlpha.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());

            Canvas canvas = new Canvas(200d, 200d);
            canvas.setPosition(Position.Absolute)
                    .setX(5d).setY(45d)
                    .setBorder(1d);

            canvas.setDrawer(ctx -> {
                ctx.setFillColor(255, 0, 0);
                ctx.fillRect(20, 20, 75, 50);
                // 调节透明度
                ctx.setGlobalAlpha(0.2);
                ctx.setFillColor(0, 0, 255);
                ctx.fillRect(50, 50, 75, 50);
                ctx.setFillColor(0, 255, 0);
                ctx.fillRect(80, 80, 75, 50);

            });
            vPage.add(canvas);

            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

    @Test
    void save() throws IOException {
        Path outP = Paths.get("target/Canvas-save.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());

            Canvas canvas = new Canvas(200d, 200d);
            canvas.setPosition(Position.Absolute)
                    .setX(5d).setY(45d)
                    .setBorder(1d);

            canvas.setDrawer(ctx -> {
                ctx.save();
                ctx.setFillColor(0, 255, 0);
                ctx.fillRect(10, 10, 30, 30);
                ctx.restore();
                ctx.fillRect(40, 50, 30, 30);
            });
            vPage.add(canvas);

            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

    @Test
    void fillText() throws IOException {
        Path outP = Paths.get("target/Canvas-fillText.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());

            Canvas canvas = new Canvas(200d, 200d);
            canvas.setPosition(Position.Absolute)
                    .setX(5d).setY(45d)
                    .setBorder(1d);

            canvas.setDrawer(ctx -> {
                FontSetting fontSetting = new FontSetting(5,FontName.SimSun.font())
                        .setCharDirection(180)
                        .setReadDirection(90);
                ctx.setFont(fontSetting);
                ctx.fillText("你好 Hello World!", 10, 50);
            });
            vPage.add(canvas);

            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

    @Test
    void fillTextDefault() throws IOException {
        Path outP = Paths.get("target/Canvas-fillText-Default-font.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());

            Canvas canvas = new Canvas(200d, 200d);
            canvas.setPosition(Position.Absolute)
                    .setX(5d).setY(45d)
                    .setBorder(1d);

            canvas.setDrawer(ctx -> {
                ctx.fillText("你好 Hello World!", 10, 50);
            });
            vPage.add(canvas);

            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

    @Test
    void measureText() throws IOException {
        Path outP = Paths.get("target/Canvas-measureText.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());

            Canvas canvas = new Canvas(200d, 200d);
            canvas.setPosition(Position.Absolute)
                    .setX(5d).setY(45d)
                    .setBorder(1d);

            canvas.setDrawer(ctx -> {
                FontSetting fontSetting = new FontSetting(5, FontName.SimSun.font());
                ctx.setFont(fontSetting);

                String text = "你好 Hello World!";
                double width = ctx.measureText(text).width;
                System.out.println(">> 文字宽度: " + width + "mm");
                ctx.fillText(text, 10, 50);
                ctx.fillText(text, 10 + width, 50);
//                Assertions.assertEquals();
            });
            vPage.add(canvas);

            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

    @Test
    void fillTextAllDirection() throws IOException {
        int[] readDirect = {0, 180, 90, 270};
        int[] charDirect = {0, 180, 90, 270};
        for (int r : readDirect) {
            for (int c : charDirect) {
                String fileName = String.format("Canvas-fillText-C%dR%d.ofd", c, r);
                Path outP = Paths.get("target", fileName);
                try (OFDDoc ofdDoc = new OFDDoc(outP)) {
                    VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());

                    Canvas canvas = new Canvas(200d, 200d);
                    canvas.setPosition(Position.Absolute)
                            .setX(5d).setY(45d)
                            .setBorder(1d);

                    canvas.setDrawer(ctx -> {
                        FontSetting fontSetting = new FontSetting(5, FontName.SimSun.font())
                                .setCharDirection(c)
                                .setReadDirection(r);
                        ctx.setFont(fontSetting);

                        ctx.fillText("li 你好 Hello World!", 100, 100);
                        ctx.beginPath();
                        if (r == 0 || r == 180) {
                            ctx.moveTo(100, 90);
                            ctx.lineTo(100, 110);

                            ctx.moveTo(40, 100);
                            ctx.lineTo(160, 100);
                            ctx.stroke();
                        } else {
                            ctx.moveTo(90, 100);
                            ctx.lineTo(110, 100);

                            ctx.moveTo(100, 40);
                            ctx.lineTo(100, 160);
                            ctx.stroke();
                        }
                    });
                    vPage.add(canvas);

                    ofdDoc.addVPage(vPage);
                }
                System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
            }
        }
    }


    @Test
    void textAlign() throws IOException {

        Path outP = Paths.get("target/Canvas-textAlign.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());

            Canvas canvas = new Canvas(200d, 200d);
            canvas.setPosition(Position.Absolute)
                    .setX(5d).setY(45d)
                    .setBorder(1d);

            canvas.setDrawer(ctx -> {
                // 在位置 150 创建蓝线
                ctx.setStrokeColor(0, 0, 255);
                ctx.moveTo(150, 20);
                ctx.lineTo(150, 170);
                ctx.stroke();

                ctx.setFont(new FontSetting(5, FontName.SimSun.font()));

                // 显示不同的 textAlign 值
                ctx.setTextAlign(TextAlign.start);
                ctx.fillText("textAlign=start", 150, 60);
                ctx.setTextAlign(TextAlign.end);
                ctx.fillText("textAlign=end", 150, 80).setTextAlign(TextAlign.left);
                ctx.fillText("textAlign=left", 150, 100);
                ctx.setTextAlign(TextAlign.center);
                ctx.fillText("textAlign=center", 150, 120);
                ctx.setTextAlign(TextAlign.right);
                ctx.fillText("textAlign=right", 150, 140);

            });
            vPage.add(canvas);

            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

    /**
     * 设置端点样式
     *
     * @throws IOException
     */
    @Test
    void setLineCap() throws IOException {
        Path outP = Paths.get("target/LineCap.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());

            Canvas canvas = new Canvas(200d, 200d);
            canvas.setPosition(Position.Absolute)
                    .setX(5d).setY(45d)
                    .setBorder(1d);

            canvas.setDrawer(ctx -> {
                ctx.beginPath();
                ctx.setLineWidth(10d);
                ctx.setLineCap(LineCapType.Butt);
                ctx.moveTo(20, 20);
                ctx.lineTo(150, 20);
                ctx.stroke();

                ctx.beginPath();
                ctx.setLineCap(LineCapType.Round);
                ctx.moveTo(20, 40);
                ctx.lineTo(150, 40);
                ctx.stroke();

                ctx.beginPath();
                ctx.setLineCap(LineCapType.Square);
                ctx.moveTo(20, 60);
                ctx.lineTo(150, 60);
                ctx.stroke();
            });
            vPage.add(canvas);

            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

    /**
     * 两条线相交时，所创建的拐角类型
     *
     * @throws IOException
     */
    @Test
    void setLineJoin() throws IOException {
        Path outP = Paths.get("target/LineJoin.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());

            Canvas canvas = new Canvas(200d, 200d);
            canvas.setPosition(Position.Absolute)
                    .setX(5d).setY(45d)
                    .setBorder(1d);

            canvas.setDrawer(ctx -> {
                ctx.beginPath();
                ctx.setLineWidth(5d);
                ctx.setLineJoin(LineJoinType.Round);
                ctx.moveTo(20, 30);
                ctx.lineTo(50, 50);
                ctx.lineTo(20, 60);
                ctx.stroke();

                ctx.beginPath();
                ctx.setLineJoin(LineJoinType.Bevel);
                ctx.moveTo(80, 30);
                ctx.lineTo(110, 50);
                ctx.lineTo(80, 60);
                ctx.stroke();

                ctx.beginPath();
                ctx.setLineJoin(LineJoinType.Miter);
                ctx.moveTo(140, 30);
                ctx.lineTo(170, 50);
                ctx.lineTo(140, 60);
                ctx.stroke();
            });
            vPage.add(canvas);

            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }


    /**
     * 两条线相交时，所创建的拐角类型
     *
     * @throws IOException
     */
    @Test
    void setMiterLimit() throws IOException {
        Path outP = Paths.get("target/MiterLimit.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());

            Canvas canvas = new Canvas(200d, 200d);
            canvas.setPosition(Position.Absolute)
                    .setX(5d).setY(45d)
                    .setBorder(1d);

            canvas.setDrawer(ctx -> {
                ctx.beginPath();
                ctx.setLineWidth(5d);
                ctx.setLineJoin(LineJoinType.Miter);
                ctx.setMiterLimit(4d);
                ctx.moveTo(20, 20);
                ctx.lineTo(50, 27);
                ctx.lineTo(20, 34);
                ctx.stroke();

            });
            vPage.add(canvas);

            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }

    /**
     * 设置线段虚线样式
     *
     * @throws IOException
     */
    @Test
    void setLineDash() throws IOException {
        Path outP = Paths.get("target/LineDash.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outP)) {
            VirtualPage vPage = new VirtualPage(ofdDoc.getPageLayout());

            Canvas canvas = new Canvas(200d, 200d);
            canvas.setPosition(Position.Absolute)
                    .setX(5d).setY(45d)
                    .setBorder(1d);

            canvas.setDrawer(ctx -> {

                ctx.beginPath();
                ctx.setLineWidth(10d);
                ctx.moveTo(20, 30);
                ctx.lineTo(150, 30);
                ctx.stroke();

                ctx.beginPath();
                ctx.setLineDash(15d, 15d);
                ctx.moveTo(20, 45);
                ctx.lineTo(150, 45);
                ctx.stroke();

                ctx.beginPath();
                ctx.setLineDash(5d, new Double[]{15d, 15d});
                ctx.moveTo(20, 65);
                ctx.lineTo(150, 65);
                ctx.stroke();

                ctx.beginPath();
                ctx.setLineDash(15d, 7.5d);
                ctx.moveTo(20, 80);
                ctx.lineTo(150, 80);
                ctx.stroke();
            });
            vPage.add(canvas);

            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());
    }
}