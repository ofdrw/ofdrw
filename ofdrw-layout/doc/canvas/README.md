# OFD R&W Canvas APi

为了更加自由和灵活的绘制OFD文档，OFD R&W提供了一个特殊的元素`Canvas`。
通过Canvas开发者可以自由绘制曲线、放置文字图形、操作形变等。

Canvas采用了与 [HTML中Canvas类似的API设计](https://www.w3school.com.cn/tags/html_ref_canvas.asp) ，便于开发者快速上手。

1. 创建Canvas，提供宽度和高度。
2. 实现绘制器。
3. 使用上下文提供的API绘制。

如下：

```java
try (OFDDoc ofdDoc = new OFDDoc(path)) {
    Canvas canvas = new Canvas(20d, 20d);
    canvas.setDrawer(ctx -> {
        // 调用API绘制图形
        ctx.beginPath();
        ctx.arc(10, 10, 5, 0, 360);
        ctx.stroke();
    });
    ofdDoc.add(canvas)
}
```

更多调用示例请参考 [DrawContextTest](../../src/test/java/org/ofdrw/layout/element/canvas/DrawContextTest.java)

## HTML Canvas模拟开发

由于OFDRW中的Canvas实现了与HTML Canvas基本一致的API接口，因此可以使用HTML的Canvas来实现OFD固定布局模式快速可视化开发。

[在线调试入口](https://www.w3school.com.cn/tiy/t.asp?f=html5_canvas_transform)

以下以水印为例，希望开发一个覆盖整页面的谁赢，进行首先在HTML Canvas中实现水印效果。

[1] 调整HTML Canvas的大小为A4纸页面大小（210x297），也就是设置Canvas元素的width和height，如果觉得比较小可以适当的等比例放大如：420x594。
```html
<canvas id="myCanvas" width="210" height="297">
```

[2] 获取Canvas上下，调用API进行绘制
```js
var c = document.getElementById("myCanvas");
var ctx = c.getContext("2d");

// 下面使用 ctx 调用HTML Canvas API进行绘制
```

```js
<!DOCTYPE html>
<html>
<body>

<canvas id="myCanvas" width="210" height="297" style="border:1px solid #d3d3d3;">
Your browser does not support the HTML5 canvas tag.
</canvas>

<script>

var c=document.getElementById("myCanvas");
var ctx=c.getContext("2d");

ctx.fillStyle = "rgb(255, 0, 0)";
ctx.globalAlpha=0.8;
ctx.font="8px SimSun"

for (var i = 0; i <= 8; i++) {
    for (var j = 0; j <= 8; j++) {
        ctx.save();
        ctx.translate(22.4 * i, j * 50);
        ctx.rotate(45*Math.PI/180);
        ctx.fillText( "保密资料", 10, 10);
        ctx.restore();
    }
}
</script>

</body>
</html>
```

[3] 调整代码适应Java代码，注意：上面的字体配置和`rotate`的角度参数与js略有不同，调整后代码如下：
```java
public void main() throws IOException {
    Path out = Paths.get("target/ComplexTransformCase.ofd");
    try (OFDDoc ofdDoc = new OFDDoc(out)) {
        VirtualPage vPage = new VirtualPage(300d, 150d);
        PageLayout style = vPage.getStyle();
        Canvas canvas = new Canvas(style.getWidth(), style.getHeight());
        canvas.setPosition(Position.Absolute)
                .setX(0d).setY(0d)
                .setBorder(0d);

        canvas.setDrawer(ctx -> {
            FontSetting setting = new FontSetting(8, FontName.SimSun.font());

            ctx.setFillColor(255, 0, 0)
                    .setFont(setting)
                    .setGlobalAlpha(0.8);
            for (int i = 0; i <= 8; i++) {
                for (int j = 0; j <= 8; j++) {
                    ctx.save();
                    ctx.translate(22.4 * i, j * 50);
                    ctx.rotate(45);
                    ctx.fillText("保密资料", 10, 10);
                    ctx.restore();
                }
            }
        });
        vPage.add(canvas);
        ofdDoc.addVPage(vPage);
    }
    System.out.println("生成文档位置：" + out.toAbsolutePath().toString());
}
```

## API列表

**路径相关**

| 方法名 | 意义 |
| --- | --- |
| `beginPath` | 开启一段路径 |
| `closePath` | 关闭路径 |
| `clip` | 从原始画布中剪切任意形状和尺寸 |
| `moveTo` | 移动绘制点到指定位置 |
| `lineTo` | 从当前点连线到指定点 |
| `quadraticCurveTo` | 通过二次贝塞尔曲线的指定控制点，向当前路径添加一个点。 |
| `bezierCurveTo` | 方法三次贝塞尔曲线的指定控制点，向当前路径添加一个点。 |
| `arc` | 创建弧/曲线（用于创建圆或部分圆） |
| `rect` | 创建矩形路径 |
| `stroke` | 描边路径 |
| `fill` | 填充一个关闭的路径 |

**矩形填充和描边**

以下API会使当前图形绘制路径重置。图形的绘制不会

| 方法名 | 意义 |
| --- | --- |
| `fillRect` | 创建并填充矩形路径 |
| `strokeRect` | 创建并描边矩形路径 |


**图形变换**

| 方法名 | 意义 |
| --- | --- |
| `scale` | 缩放当前绘图，更大或更小 |
| `rotate` | 旋转当前的绘图 |
| `translate` | 重新映射画布上的 (0,0) 位置 |
| `transform` | 在已有当前变换矩阵基础上，再变换矩阵 |
| `setTransform` | 设置变换矩阵，替代当前的变换矩阵 |

**文字**

| 方法名 | 意义 |
| --- | --- |
| `fillText` | 填充文字 |
| `measureText` | 测量文本的宽度或高度 |

**图形图像**

| 方法名 | 意义 |
| --- | --- |
| `drawImage` | 绘制图片 |

**属性配置**

| 方法名 | 意义 |
| --- | --- |
| `TextAlign` | 文本对齐方式(get/set)，默认start |
| `StrokeColor` | 描边颜色(get/set)，默认黑色 |
| `FillColor` | 填充颜色(get/set)，默认黑色 |
| `LineWidth` | 线宽度(get/set)，默认0.353mm |
| `GlobalAlpha` | 透明度(get/set) |
| `FontSetting` | 文字配置(get/set) |
| `LineCap`| 回线条的结束端点样式(get/set) |
| `LineJoin`| 两条线相交时，所创建的拐角类型(get/set) |
| `MiterLimit` | 最大斜接长度，也就是结合点长度截断值(get/set) |
| `LineDash` | 填充线时使用虚线模式，设置虚线间隔(get/set) |

**FontSetting**

| 方法名 | 意义 |
| --- | --- |
| `font` | 字体 |
| `fontSize` | 字号，单位（毫米mm） |
| `italic` | 斜体 |
| `fontWeight` | 字体的粗细，可能值为： 100、200、300、400、500、600、700、800、900，默认值： 400 |
| `letterSpacing` | 字间距，默认0 |
| `charDirection` | 字符方向，可取值：0(默认)、90、180、270 |
| `readDirection` | 阅读方向，可取值：0(默认)、90、180、270 |

**工作区**

| 方法名 | 意义 |
| --- | --- |
| `save` | 保存当前工作区：描边颜色、填充颜色、变换矩阵、线宽度、绘制文字配置（字号、加粗、斜体、字体）、透明值 |
| `restore` | 还原绘图状态 |

