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

