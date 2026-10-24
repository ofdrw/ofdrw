# CanvasImageExporter 设计说明

> 状态：待评审（本文档仅描述设计，尚未提交实现代码）
> 关联模块：`ofdrw-converter`
> 参考实现：<https://github.com/ofdrw/ofdrw-viewer/tree/main/packages/ofdrw-viewer-engine>（`page.exportImage` 导出管线）

---

## 1. 背景与目标

### 1.1 现状

`ofdrw-converter` 现有的图片导出实现为：

```
ImageExporter  →  ImageMaker extends AWTMaker  →  BufferedImage + Graphics2D  →  ImageIO.write
```

存在以下问题：

| 编号 | 问题 | 影响 |
| --- | --- | --- |
| P1 | 无像素上限校验 | `ppm` 由调用方任意指定，A0 纸 600dpi 约 1.4 亿像素，直接 OOM |
| P2 | 页面像素尺寸使用 `Math.round` | 与参考实现 `Math.ceil` 不一致，高 dpi 下右侧/底部可能被裁掉 1 像素 |
| P3 | 文件名按“导出序号”而非“页码” | `export(2, 0)` 产出 `0.png`=第 3 页，文件名与页码语义错位 |
| P4 | JPG 无质量参数 | `ImageIO.write` 使用默认质量（约 0.75），输出体积与观感不可控 |
| P5 | 不写入 DPI 元数据 | 图片无 `25.4 / ppm` 的物理分辨率信息，打印/排版工具无法还原真实尺寸 |
| P6 | 导出的 `BufferedImage` 从不 `flush()` | 连续导出大量页面时像素内存堆积 |
| P7 | 不支持背景色/透明与旋转 | 透明印章、扫描件归档、页面方向整理均需外部二次处理 |
| P8 | 印章渲染路径硬编码 | `AWTMaker.writeStampAnnot` 内直接 `new ImageMaker(...)`，替换设备层时印章仍走旧路径 |

（编号 P1–P8 在下文各处被引用。）

### 1.2 目标

新增 `CanvasImageExporter`，以 **像素画布抽象** 为设备层重新实现 OFD → 位图导出：

1. 修复 P1–P8。
2. 与现有 `ImageExporter` 保持 API 兼容，支持并行 A/B 像素回归对比。
3. 设备层与布局层解耦：布局（`AWTMaker`）不感知具体画布实现。
4. 不引入任何新的第三方依赖。

### 1.3 非目标

- 不导出 SVG / PDF（由 `SVGExporter`、`PDFExporter*` 负责）。
- 不支持区域导出、多页拼接、页面拼版。
- 不支持 WebP（见 §2.3）。
- 本次不迁移 `SVGMaker`、`ItextMaker`、`PdfboxMaker`、`HtmlMaker` 到新设备层。
- 不做 DRM / 权限强制（`Permissions/Export=false` 不阻止导出）。

---

## 2. 关键决策

以下决策已经确认，作为后续实现与评审的基线。

| 决策点 | 结论 | 理由 |
| --- | --- | --- |
| D1 画布实现 | `BufferedImage(TYPE_INT_ARGB)` + `Graphics2D`，封装为 `CanvasSurface` | headless 可用、无窗口依赖；以抽象类隔离，将来可替换其他实现 |
| D2 文件命名 | 默认 `page-{页码}.{ext}`，并提供 `setFilenamePattern` 兼容旧序号命名 | 语义清晰；老行为可显式复现 |
| D3 输出格式 | PNG / JPG / BMP，JPG 支持质量参数 | 与现有能力对齐，补上质量与 DPI 元数据 |
| D4 类结构 | 新建 `CanvasImageExporter`；`ImageExporter` 标 `@Deprecated` 并委托新类 | 保留 A/B 对比与回滚能力，兼容 2.4.x 使用者 |
| D5 交付范围 | 先交付设计，评审通过后实现 | 降低返工成本 |
| D6 语言级别 | Java 8（沿用父 POM `maven.compiler.source=1.8`） | 不得使用 9+ 语法 |

### 2.1 画布抽象

设备层对外暴露 `CanvasSurface`（宽度、高度、`Graphics2D`、释放入口），底层由 `BufferedImage(TYPE_INT_ARGB)` 实现。其语义与消费方的使用方式一致：显式尺寸、显式背景、显式释放、绘制后可编码。

`CanvasSurface` 设计为抽象类（Java 8 下无 `sealed`），保留后续新增其他画布实现的扩展点。

### 2.2 布局层复用策略

不重写 `AWTMaker`。`CanvasImageMaker extends AWTMaker` 只做两件事：

1. 把一页绘制到外部给定的 `Graphics2D`（画布由 `CanvasSurface` 分配）。
2. 覆盖印章嵌套渲染，使嵌套 OFD 印章同样使用 Canvas 设备层（修复 P8）。

P8 的实现方式：在 `AWTMaker` 中把硬编码的 `new ImageMaker(...)` 提取为两个可覆盖的 `protected` 方法。

**类型约束**：`ImageMaker` 与 `CanvasImageMaker` 都是 `AWTMaker` 的平级子类，Java 单继承下不存在一个能同时兼容两者的具体类型。同时各 Maker 子类的 `makePage` 签名互不相同（`BufferedImage`、`String`、`PdfPage`、`PDPage`），无法上移到 `AWTMaker`。因此采用“工厂返回 `AWTMaker` + 单独提供渲染位图的方法”的组合：

```java
/**
 * 创建用于渲染嵌套 OFD 印章的绘制器。
 * 默认返回 ImageMaker，保持现有实现行为不变。
 *
 * @param nestedReader 印章 OFD 解析器
 * @param ppm 每毫米像素数量，单位：像素/毫米
 * @return 印章绘制器
 */
protected AWTMaker createStampMaker(OFDReader nestedReader, double ppm) {
    return new ImageMaker(nestedReader, ppm);
}

/**
 * 将印章绘制器的指定页渲染为位图，供正片叠底合成使用。
 * 默认调用 ImageMaker.makePage。
 *
 * @param maker 印章绘制器
 * @param pageIndex 页码，从 0 起
 * @return 位图；页面不可用时返回 null
 */
protected BufferedImage makeStampPage(AWTMaker maker, int pageIndex) {
    return maker instanceof ImageMaker ? ((ImageMaker) maker).makePage(pageIndex) : null;
}
```

`writeStampAnnot` 调用处相应改为先取 `AWTMaker`，再经 `makeStampPage` 取位图。

`CanvasImageMaker` 覆盖这两个方法：工厂返回相同 ppm 的 `CanvasImageMaker`，`makeStampPage` 用自身画布渲染后转成 `BufferedImage`。默认实现不变，因此 `SVGMaker`、`ItextMaker`、`PdfboxMaker`、`HtmlMaker` 的行为完全不受影响；新增的两个方法均为 `protected`，不改变任何公开 API。

### 2.3 不引入 WebP 的理由

WebP 编码需要额外三方依赖（如 `webp-imageio`，含 JNI 本地库），会显著加重 `ofdrw-converter` 的依赖树与跨平台部署成本，与“不引入新依赖”目标冲突。当前 WebP 需求可通过 `ofdrw-viewer-cli` 满足。

---

## 3. 模块与类设计

### 3.1 包结构

```
org.ofdrw.converter
├── canvas/                              ← 新增：设备层
│   ├── CanvasSurface.java               画布抽象（尺寸/Graphics2D/字节源/释放）
│   ├── BufferedImageSurface.java        BufferedImage 实现
│   └── CanvasImageMaker.java            extends AWTMaker，把一页画到画布
└── export/
    ├── CanvasImageExporter.java         ← 新增：OFDExporter 实现
    └── ImageExporter.java               ← 修改：@Deprecated，委托新类
```

`org.ofdrw.converter.image.ImageMedia` 已存在且与图片资源解码相关，故设备层不使用 `image` 作为新包名，避免语义混淆。

### 3.2 `CanvasSurface`

```java
package org.ofdrw.converter.canvas;

/**
 * 离屏像素画布抽象。
 * <p>
 * 提供显式尺寸、显式背景、绘制后可编码的画布语义，不绑定窗口系统，
 * 可在 headless 环境使用。
 */
public abstract class CanvasSurface {

    /** 画布宽度，单位：像素 */
    public abstract int getWidth();

    /** 画布高度，单位：像素 */
    public abstract int getHeight();

    /**
     * 获取一个新建的绘制上下文。
     * <p>
     * 每次调用返回独立的 Graphics2D 副本，调用方负责 dispose。
     *
     * @return 绘制上下文
     */
    public abstract Graphics2D createGraphics();

    /**
     * 创建画布。
     *
     * @param width 宽度，单位：像素，必须大于 0
     * @param height 高度，单位：像素，必须大于 0
     * @param background 背景色，为 null 表示保持透明
     * @return 画布实例
     */
    public static CanvasSurface create(int width, int height, Color background) {
        return new BufferedImageSurface(width, height, background);
    }

    /**
     * 释放画布持有的像素资源。
     */
    public abstract void flush();
}
```

`BufferedImageSurface` 的实现要点：

- 底层 `new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)`。
- `background != null` 时在构造阶段用 `Graphics2D` 填充 `SRC` 模式覆盖底色；为 `null` 时保持全透明（不填充）。
- `flush()` 调用 `BufferedImage.flush()` 释放像素缓存（修复 P6）。

### 3.3 `CanvasImageMaker`

```java
package org.ofdrw.converter.canvas;

/**
 * 将 OFD 页面绘制到 CanvasSurface 的图片绘制器。
 */
public class CanvasImageMaker extends AWTMaker {

    public CanvasImageMaker(OFDReader reader, double ppm) {
        super(reader, ppm);
    }

    /**
     * 将指定页面绘制到画布。
     * <p>
     * 不分配画布、不填充背景，画布生命周期由调用方管理。
     *
     * @param surface 目标画布
     * @param pageIndex 页码，从 0 起
     * @throws GeneralConvertException 页码非法
     */
    public void drawPage(CanvasSurface surface, int pageIndex) {
        // 1. 校验页码，越界抛 GeneralConvertException
        // 2. PageInfo pageInfo = pages.get(pageIndex)
        // 3. Graphics2D g = surface.createGraphics()
        // 4. try { writePage(g, pageInfo, null); } finally { g.dispose(); }
    }

    @Override
    protected AWTMaker createStampMaker(OFDReader nestedReader, double ppm) {
        return new CanvasImageMaker(nestedReader, ppm);
    }

    @Override
    protected BufferedImage makeStampPage(AWTMaker maker, int pageIndex) {
        // 用独立画布渲染印章页，再取出 BufferedImage 供正片叠底合成
    }
}
```

**说明**：`AWTMaker.writePage` 的当前实现内部已通过 `renderBoundaryAndSetClip` 完成 mm → 像素的缩放与 clip，因此 `CanvasImageMaker` 不需要额外设置 `setTransform(scale)`，否则会二次缩放。

### 3.4 `CanvasImageExporter`

完整签名（Java 8）：

```java
package org.ofdrw.converter.export;

public class CanvasImageExporter implements OFDExporter {

    // ---- 构造器 ----
    public CanvasImageExporter(Path ofdFile, Path outDir) throws IOException;
    public CanvasImageExporter(InputStream ofdInput, Path outDir) throws IOException;
    public CanvasImageExporter(Path ofdFile, Path outDir, String imageFormat, double ppm) throws IOException;
    public CanvasImageExporter(InputStream ofdInput, Path outDir, String imageFormat, double ppm) throws IOException;
    public CanvasImageExporter(OFDReader reader, Path outDir, String imageFormat, double ppm) throws IOException;

    // ---- 导出 ----
    @Override public void export(int... indexes) throws GeneralConvertException;
    @Override public void close() throws IOException;

    // ---- 参数设置（均需在 export 之前调用）----
    /** 设置每毫米像素数量，<= 0 时忽略。 */
    public void setPPM(double ppm);
    /** 设置输出格式，支持 PNG / JPG / BMP（大小写不敏感）。 */
    public void setImageFormat(String imageFormat);
    /** 设置 JPG 质量，取值 0.0–1.0，默认 0.92。 */
    public void setJpegQuality(float quality);
    /** 设置背景色，null 表示透明（仅 PNG 有效）。 */
    public void setBackground(Color background);
    /** 设置顺时针旋转角度，仅接受 0/90/180/270，其余值忽略并告警。 */
    public void setRotation(int degree);
    /** 设置单页像素上限，默认 64_000_000。 */
    public void setMaxPixels(long maxPixels);
    /** 设置文件名模式，使用 String.format 语义，必须恰好一个整型占位符，默认 "page-%d"。 */
    public void setFilenamePattern(String pattern);
    /** 设置文件名占位符取值基准，默认 PAGE（页码）。 */
    public void setFilenameIndexBase(IndexBase base);

    // ---- 结果 ----
    /** 已导出图片路径，顺序与导出顺序一致。 */
    public List<Path> getImgFilePaths();
    /** 获取最近一次导出的图片尺寸与导出参数快照，用于校验。 */
    public List<ImageInfo> getImageInfos();
}
```

`IndexBase` 为独立源文件 `org/ofdrw/converter/export/IndexBase.java` 中的公开枚举：`PAGE`（占位符代入页码，默认）、`SEQUENCE`（占位符代入导出计数）。

`ImageInfo` 为独立源文件 `org/ofdrw/converter/export/ImageInfo.java` 中的公开类，字段：`int pageIndex`、`int width`、`int height`、`int rotation`、`double ppm`、`Path path`。

> 项目约定：公开类型不以内嵌类形式定义，一律独立成文件。若与其他包的名称冲突，单独放到新的包（目录）中。

**关于 `OFDReader` 构造器**：新增该构造器的目的是允许调用方共享同一 `OFDReader`（例如批量导出并复用页面缓存）。此时 `close()` 不关闭外部传入的 `OFDReader`，需在 Javadoc 中明确写出归属约定。

### 3.5 文件命名

`setFilenamePattern(String pattern)` 使用 `String.format` 语义，要求恰好一个整型占位符。占位符代入的取值由 `setFilenameIndexBase` 决定：

| `setFilenameIndexBase` | 占位符取值 | `"page-%d"` 对 `export(2, 0)` 的输出 |
| --- | --- | --- |
| `PAGE`（默认） | 页码（从 0 起） | `page-2.png`、`page-0.png` |
| `SEQUENCE` | 已导出计数（从 0 起） | `page-0.png`、`page-1.png` |

| 常用配置 | 输出 | 说明 |
| --- | --- | --- |
| `"page-%d"` + `PAGE`（默认） | `page-0.png` | 推荐默认值 |
| `"%d"` + `PAGE` | `0.png` | 语义为页码 |
| `"%d"` + `SEQUENCE` | `0.png`、`1.png` | 与旧版 `ImageExporter` 完全一致的行为 |

扩展名由 `imageFormat` 决定，统一小写并做别名归一：`jpeg → jpg`；`tif`、`tiff` 不支持。

**兼容说明**：旧版 `ImageExporter` 使用“导出序号”命名。`ImageExporter` 委托时固定使用 `"%d"` + `SEQUENCE`，因此文件名为 `0.png`、`1.png`，与 2.4.x 行为逐字节一致（见 §6.1）；直接使用 `CanvasImageExporter` 的调用方得到页码语义。

---

## 4. 导出流程

### 4.1 主流程

```
export(2, 0)
 └─ 规范化页码序列 targetPages
     ├─ indexes 为空 → [0 .. pageCount-1]
     └─ 否则过滤越界值，按入参顺序保留（允许乱序、重复）
 └─ for each pageIndex:
     ├─ 1. 取得页面尺寸 ST_Box（单位 mm）
     ├─ 2. 几何计算（见 4.2）
     ├─ 3. 像素上限校验（见 4.3）
     ├─ 4. surface = CanvasSurface.create(outW, outH, effectiveBackground)
     ├─ 5. 若 rotation != 0 → 在 surface 上先应用旋转变换
     ├─ 6. maker.drawPage(surface, pageIndex)  或  旋转场景下的等价绘制
     ├─ 7. encode(surface, format, quality) → 写入文件
     └─ 8. surface.flush()（finally 中执行）
```

### 4.2 几何计算

```
scale = ppm                         // 单位：像素 / 毫米
baseW = (int) Math.ceil(ppm * pageWidthMm)     // 修复 P2
baseH = (int) Math.ceil(ppm * pageHeightMm)
baseW = max(1, baseW); baseH = max(1, baseH)

若 rotation ∈ {90, 270}:
    outW = baseH, outH = baseW
否则:
    outW = baseW, outH = baseH
```

**旋转采用“离屏渲染 + 整图旋转”，不使用画布级变换。**

原因：`AWTMaker.writePage` 在绘制各类元素时会无条件调用 `Graphics2D.setTransform`（源码中共 5 处：约 160、433、441、722、773 行），将变换重置为元素自身矩阵。因此若在画布上预先施加旋转，只有位图类元素（`ImageObject`、印章正片叠底合成）会跟随旋转，而 `PathObject`、`TextObject`、`CompositeObject` 等矢量内容不会旋转，产出“部分旋转”的错误图像。

实现步骤：

1. 按未旋转方向渲染到离屏画布，尺寸 `baseW = ceil(ppm * pageWidthMm)`、`baseH = ceil(ppm * pageHeightMm)`。
2. `rotation == 0` 时直接使用该画布。
3. `rotation ∈ {90, 180, 270}` 时新建输出画布（90/270 宽高互换），先铺满背景，再对整幅位图做像素级旋转：

| rotation | 输出尺寸 | 整图变换（作用于输出画布 `Graphics2D`） |
| --- | --- | --- |
| 90 | `outW = baseH`、`outH = baseW` | `translate(outW, 0)` + `rotate(π/2)` + `drawImage(源画布)` |
| 180 | `outW = baseW`、`outH = baseH` | `translate(outW, outH)` + `rotate(π)` + `drawImage(源画布)` |
| 270 | `outW = baseH`、`outH = baseW` | `translate(0, outH)` + `rotate(3π/2)` + `drawImage(源画布)` |

4. 旋转完成后重新铺满背景，避免 ARGB 未初始化区域出现黑边。
5. 编码使用旋转后的最终画布。

代价：90/270 场景多一次位图拷贝。这是为绕开 `AWTMaker` 的 `setTransform` 重置行为所必须付出的成本，且不需要改动 `AWTMaker`，风险最低。

> 说明：`CanvasImageMaker` 提供 `drawPage(CanvasSurface, int, AffineTransform)` 重载，但由于上述 `setTransform` 重置问题，它**不适用于**旋转导出场景。

### 4.3 像素上限

```
if (maxPixels > 0 && (long) outW * outH > maxPixels) {
    throw new GeneralConvertException(...)
}
```

默认 `64_000_000`（与参考实现的 `maxCanvasPixels` 一致）。异常信息必须包含：请求尺寸、像素数、上限值，以及“可通过 setPPM 降低分辨率或 setMaxPixels 调整上限”的提示。

### 4.4 背景色

| `background` | PNG | JPG | BMP |
| --- | --- | --- | --- |
| `null` | 全透明底 | 白底（格式不支持透明） | 白底 |
| `Color` | 指定底色 | 指定底色 | 指定底色 |

JPG/BMP 在编码前将 `TYPE_INT_ARGB` 合成到目标底色上再转换，避免透明像素变成黑块。

### 4.5 异常与生命周期

| 场景 | 抛出 |
| --- | --- |
| 页码非法 | 跳过（沿用 `OFDExporter` 既有约定） |
| 输出目录是文件 | `IllegalArgumentException`（构造期） |
| 像素超限 | `GeneralConvertException("...超过像素上限...")` |
| 格式不支持 | `IllegalArgumentException`（`setImageFormat` 时即校验） |
| 编码失败 / IO 失败 | `GeneralConvertException`，保留 cause |
| `close()` 后调用 `export` | `GeneralConvertException("导出器已关闭")` |

`export` 过程中若第 N 页失败：已完成页面保留在磁盘，`getImgFilePaths()` 只包含成功项，异常向上抛出且不吞掉后续页面。此行为与现有实现一致。

---

## 5. 编码实现

统一使用 `ImageWriter` 而非 `ImageIO.write`，以便设置质量与物理分辨率元数据：

```java
ImageWriter writer = ImageIO.getImageWritersByFormatName(format).next();
ImageWriteParam param = writer.getDefaultWriteParam();
if (param.canWriteCompressed()) {
    param.setCompressionMode(MODE_EXPLICIT);
    param.setCompressionQuality(quality);      // 仅 JPG 生效，PNG/BMP 传入被忽略
}
IIOMetadata meta = writer.getDefaultImageMetadata(type, param);
// 写入 horizontalPixelSize / verticalPixelSize = 25.4 / ppm （单位：mm/像素）
        // 修复 P5：horizontalPixelSize 的语义是“一个像素的宽度的毫米数”
try (ImageOutputStream ios = ImageIO.createImageOutputStream(dst.toFile())) {
    writer.setOutput(ios);
    writer.write(null, new IIOImage(image, null, meta), param);
} finally {
    writer.dispose();
}
```

要点：

- PNG 的 `quality` 不生效（`PNGImageWriteParam` 不支持压缩质量），文档中说明。
- DPI 元数据写入失败不中断导出，降级为无元数据并告警（部分 `ImageWriter` 对自定义元数据节点较严格，需用 `IIOMetadataNode` 逐格式构造）。
- `ppm` 与 DPI 换算：`dpi = ppm * 25.4`，`mmPerPixel = 1 / ppm`。

---

## 6. 兼容与迁移

### 6.1 `ImageExporter` 的去向

- 类级加 `@Deprecated`，`@deprecated` Javadoc 指向 `CanvasImageExporter`。
- 内部完全委托：构造期创建 `CanvasImageExporter`，`export`/`close`/`setPPM`/`getImgFilePaths` 透传。
- **保留序号命名**：委托时显式设置 `setFilenamePattern("%d")` + `setFilenameIndexBase(SEQUENCE)`，产物为 `0.png`、`1.png`，与 2.4.x 行为完全一致，确保既有测试与用户脚本不改文件就能跑通。
- **关闭像素上限**：委托时设置 `setMaxPixels(0)`，保持旧实现“不限制尺寸”的行为。

#### 6.1.1 为何旧 API 不启用像素上限

决策：**`ImageExporter` 委托时不启用像素上限，P1 的“像素上限校验”仅对 `CanvasImageExporter` 生效。**

依据（按 ARGB 每像素 4 字节实算）：

| 幅面 | 300 dpi | 600 dpi |
| --- | --- | --- |
| A4 | 8.7M ✅ | 34.8M ✅ |
| A3 | 17.4M ✅ | **69.6M 超 64M 上限** |
| A1 | **69.7M 超限** | 279M 超限 |
| A0 | 139M 超限 | 558M 超限 |

64M 上限会在 **A3@600dpi、A1@300dpi、A0@300dpi** 等常见归档配置上误伤，属于破坏性行为变更。同时该上限并不能真正防止 OOM：以常见 JVM 默认堆 4GB 计，A0@600dpi 单页像素约 2.1GB，旋转场景需源画布与输出画布两份、峰值约 4.2GB，256M 上限同样拦不住。上限的实际价值是“快速失败”，而非防 OOM。

综合权衡，在 2.4.x 小版本内不改变已广泛使用的 `ImageExporter` 的失败模式；`ImageExporter` 已标记 `@Deprecated`，迁移到 `CanvasImageExporter` 即可获得上限保护。需要限制的调用方可在迁移后自行调用 `setMaxPixels`。

### 6.2 `AWTMaker` 的改动面

唯一改动是新增两个 `protected` 方法：`AWTMaker createStampMaker(OFDReader, double)` 与 `BufferedImage makeStampPage(AWTMaker, int)`，默认实现分别返回 `new ImageMaker(...)` 与调用 `ImageMaker.makePage`，行为逐字节等价。其余方法签名与逻辑不动，不改变任何公开 API。

### 6.3 文档更新

文档与代码在同一提交内完成，作为本期交付内容的一部分。

#### 6.3.1 待改文件与改动点

| 文件 | 位置 | 改动 |
| --- | --- | --- |
| `ofdrw-converter/doc/EXPORTER.md` | §入门 / 第 48 行示例 | `new ImageExporter(...)` → `new CanvasImageExporter(...)` |
| 同上 | §OFD文档导出 / 第 120 行 | 类命名格式说明示例改用 `CanvasImageExporter` |
| 同上 | §OFDExporter支持导出类型 / 第 124 行 | 图片导出实现改为 `CanvasImageExporter`（标注 `ImageExporter` 已废弃） |
| 同上 | §导出为图片 | 整节重写：实现类、构造器、参数表（PPM / 格式 / JPG 质量 / 背景 / 旋转 / 像素上限 / 命名）、示例、异常说明、特有方法表 |
| 同上 | §导出为图片 | 新增“从 ImageExporter 迁移”对照表，说明行为差异（`ceil` 尺寸、页码命名、透明背景、旋转） |
| `ofdrw-converter/doc/CONVERTER.md` | 第 124 / 128 行 | 修正既有笔误：`DocConverter` 实现类名误写为 `TextExporter`、`ImageExporter`，应为 `TextConverter`、`ImageConverter`；并去掉不存在的 `PDFExporterPDFBox` |
| 同上 | §图片转换OFD | 修正 `BPM` → `BMP`；删除“可通过构造器指定导出的图片类型”这一与 `ImageConverter` 实际 API 不符的描述 |
| `README.md`（根目录） | §图片转换 | 补充说明 `ofdrw-converter` 提供 `CanvasImageExporter` 图片导出能力；保留 ofdbox 致谢不动 |
| `releasenotes.md` | 新增条目 | 记录 `CanvasImageExporter` 新增、`ImageExporter` 废弃、`AWTMaker.createStampMaker` 扩展点 |
| `ofdrw-converter/README.md` | 无 | 当前无 `ImageExporter` 引用，保持不动 |
| `ofdrw-converter/doc/CanvasImageExporter设计.md` | 本文件 | 实现完成后更新状态行与实现偏差记录 |

补充：`EXPORTER.md` 第 134、140 行的 `BPM` 为 `BMP` 笔误，随“导出为图片”整节重写一并修正。

#### 6.3.2 文档维护约束

- `EXPORTER.md` 的参数表必须与 `CanvasImageExporter` 的实际 setter 一一对应，默认值以代码为准。
- 示例代码需可编译，不使用 Java 9+ 语法（与模块语言级别一致）。
- 迁移对照表必须显式列出所有**行为变化**，避免使用者升级后产物悄变而不知情。
- 文档中的类名、方法名必须与源码一致，交付前用 `grep` 交叉核对一遍。

---

## 7. 测试方案

### 7.1 单元测试

| 用例 | 断言 |
| --- | --- |
| 基本导出 `999.ofd` | 页数与文件数一致，`page-0.png`… 存在 |
| 尺寸 `ceil` 语义 | 页宽非整数像素时输出宽度 = `ceil(ppm * w)`（P2） |
| 旋转 90/180/270 | 宽高互换正确；用非正方形页面 + 四角标记色块校验方向 |
| 背景 `null` + PNG | 存在 alpha=0 的像素 |
| 背景 `null` + JPG | 全图无不透明以外的残留透明像素 |
| 像素上限 | `ppm` 极大时抛 `GeneralConvertException` 且信息含尺寸与上限 |
| 页码顺序 `export(2,0)` | 文件名按页码 `page-2.png`、`page-0.png` |
| 重复导出 | 同名文件被覆盖，`getImgFilePaths()` 不重复累积 |
| `close()` 后导出 | 抛异常 |
| 外部 `OFDReader` 构造 | `close()` 不关闭外部 Reader |

### 7.2 回归对比（A/B）

同一 `999.ofd`、`ppm=15`、PNG，分别用 `ImageExporter` 与 `CanvasImageExporter` 导出：

- 逐像素比较，统计差异像素比例。
- 期望差异来源仅限：抗锯齿相位、`round` vs `ceil` 造成的边缘 1 像素。
- 门槛：差异像素比例 `< 0.5%`；同时抽样人工比对文字边缘与图形边界。

### 7.3 印章与特殊页面

- `zsbk.ofd`：正片叠底（MULTIPLY）印章位置与方向正确，印章透明底图不出现白框（验证 P8 与背景处理）。
- `透明度文字.ofd`：文字 alpha 与原实现一致。
- `containsJPEG.ofd`：内嵌 JPEG 资源绘制正确。

### 7.4 性能与内存

- 10 页 `999.ofd` @ 20ppm：记录耗时与峰值内存，与旧实现对比。
- 连续导出 100 页：验证 `flush()` 生效，堆占用不随页数线性增长（P6）。

---

## 8. 风险与对策

| 风险 | 影响 | 对策 |
| --- | --- | --- |
| 像素级细微差异 | A/B 无法 0 差异 | 阈值 + 关键区域抽样视觉确认（§7.2） |
| 大尺寸内存峰值 | 单页仍可能数 GB | 默认 64M 像素上限，超限快速失败并提示 |
| `AWTMaker` 印章钩子遗漏其他硬编码点 | 部分场景未走新设备层 | 实现时全局搜索 `new ImageMaker(`，逐一评估 |
| `ImageWriter` 元数据兼容性 | 部分 JDK 写入 DPI 元数据失败 | 降级为无元数据 + 告警，不中断导出 |
| 旋转后 clip 与坐标偏差 | 边缘内容被裁 | 正方形/非正方形页面各加角标用例 |
| Java 8 语法约束 | 误用 9+ API | 编译期即为 1.8 target，CI 自然拦截 |

---

## 9. 实施计划（评审通过后执行）

| 步骤 | 内容 | 产出 |
| --- | --- | --- |
| 1 | `AWTMaker` 新增 `createStampMaker` 与 `makeStampPage` 扩展点 | 2 个 protected 方法 + 现有测试全绿 |
| 2 | 新增 `canvas` 包：`CanvasSurface`、`BufferedImageSurface`、`CanvasImageMaker` | 设备层可用 |
| 3 | 新增 `CanvasImageExporter`（含编码、命名、旋转、背景、上限） | 主功能 |
| 4 | `ImageExporter` 改造为委托 + `@Deprecated`、Javadoc 迁移指引 | 兼容性 |
| 5 | 单元测试 + A/B 回归 + 印章/特殊页面用例 | §7 全部通过 |
| 6 | 同步更新 `EXPORTER.md`、`CONVERTER.md`、根 `README.md`、`releasenotes.md`（见 §6.3） | 文档与代码同批交付 |

步骤 6 与步骤 1–5 在同一提交中完成，不接受“代码先行、文档后补”。

---

## 10. 待评审确认项

1. 默认像素上限 `64_000_000` 是否合适（参考实现取值，A3@600dpi 约 4900 万，可覆盖常规归档；若要支持 A1 幅面需放宽）。
2. `ImageExporter` 保留“序号命名”作为兼容行为（而非改为页码命名）是否可接受。
3. 新增 `CanvasImageExporter(OFDReader, ...)` 构造器是否纳入本期（涉及 Reader 归属约定）。
4. 是否需要提供“导出为 `BufferedImage` 返回而不落盘”的重载（供内存内使用场景），本期是否纳入。
