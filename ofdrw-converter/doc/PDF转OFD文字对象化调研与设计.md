# PDF 转 OFD 文字对象化调研与设计

> 状态：**调研 + 设计，尚未实施**（本文不含代码改动）
> 目标：解决 `PDFConverter` 将整页文字转成矢量路径后 OFD 体积异常膨胀的问题
> 基线：`fb0cc476`（2.5.1，PDFBox 3.0.8）
> 日期：2026-10-30

---

## 1. 结论摘要

1. **根因不是“PDF 太大”，而是转换链路丢失了文字语义。**
   当前 PDF → OFD 走的是 `PDFRenderer → AWT Graphics2D → OFDPageGraphics2D` 桥接：
   PDFBox 的 `PageDrawer` 在绘制文字时，逐字取出字形轮廓并调用 `Graphics2D.fill(Shape)`；
   `OFDPageGraphics2D.fill` 又把每个轮廓写成一个独立的 `PathObject`。
   因此每个字符都变成一条路径，且路径使用**整页绝对坐标**、`Boundary` 固定为整页，重复字形无法复用。

2. **推荐方案：在 PDFBox 的字形级入口做“文字旁路”，输出 OFD `TextObject` + 原样嵌入 PDF 字体。**
   - 入口：继承 `org.apache.pdfbox.rendering.PageDrawer`，重写 `showFontGlyph(...)`；
   - 文字：生成 `TextObject` / `TextCode` / `CGTransform`，**把 PDF 内嵌字体原样作为 OFD 内嵌字体**写入 `Res/`（GID 不变，子集化仅作可选开关）；
   - 图形/图片：仍复用现有 `PageDrawer` 与 `OFDPageGraphics2D`，保证绘制语义不变；
   - 不支持的文字（Type3、CFF、非内嵌且无可用替代字体、文字裁剪、图案填充等）回退到现有路径绘制，必要时再启用“字形轮廓复用”降级。

3. **预期收益（已用仓库内样本实测基线）：**

   | 样本 | 源 PDF | 当前 OFD | 当前/PDF | 参考 OFD（文字对象） | 预计文字模式 |
   |---|---:|---:|---:|---:|---:|
   | `dense-text-perf.pdf`（2 页，约 3.05 万字符） | 381 KB | **9.06 MB** | 23.8× | **213 KB** | **0.3～0.5 MB** |
   | `Test.pdf`（5 页，图文混排） | 591 KB | 2.22 MB | 3.8× | — | 1.2～1.8 MB |
   | `keyword.pdf`（1 页，2 张图） | 297 KB | 731 KB | 2.5× | — | 图片占主导，收益有限 |

   纯文字文档预期可降到当前的 **1/15～1/30**；图文混排文档收益取决于图片占比。`dense-text-perf.ofd` 是同一内容的文字对象版本，仅 213 KB，可作为上限参照。

4. **本次不开发，但已完成基础验证。** 已用 `ofdrw-viewer-cli` 对“手工 TextObject + 内嵌子集字体 + CGTransform + CTM 旋转/平移 + Alpha”做渲染对照，坐标、字号、步进与旋转均符合预期；尚未接入 `PageDrawer.showFontGlyph` 回调，正式实现前仍需完成 P0 的剩余部分（见 7.4、8）。

---

## 2. 问题复现与量化

### 2.1 复现路径

当前转换入口在 [`PDFConverter.java`](../src/main/java/org/ofdrw/converter/ofdconverter/PDFConverter.java#L179-L180)：

```java
OFDPageGraphics2D ofdPageG2d = ofdDoc.newPage(width / uuPmm, height / uuPmm);
pdfRender.renderPageToGraphics(index, ofdPageG2d, (float) (1d / uuPmm));
```

`PDFRenderer` 内部使用 PDFBox 的 `PageDrawer`。`PageDrawer.showFontGlyph` 的实际行为是：

```text
GlyphCache.getPathForCharacterCode(code)  →  GeneralPath（字形轮廓）
graphics.fill(shape)                      →  OFDPageGraphics2D.fill(Shape)
```

而 [`OFDPageGraphics2D.fill`](../../ofdrw-graphics2d/src/main/java/org/ofdrw/graphics2d/OFDPageGraphics2D.java#L547-L577) 会：

1. 把 `Shape` 转成 `AbbreviatedData`（绝对页面坐标）；
2. 新建 `PathObject`；
3. `setBoundary(this.size)` —— **Boundary 恒为整页**；
4. `container.addPageBlock(...)` —— **每个字形一个 PathObject**。

即使走 `drawString` / `drawGlyphVector`，最终也是 [`drawGlyphVector`](../../ofdrw-graphics2d/src/main/java/org/ofdrw/graphics2d/OFDPageGraphics2D.java#L274-L277) → `fill(g.getOutline(...))`，结果相同。

### 2.2 测量方法

- 用当前 `PDFConverter` 转换 `ofd_pdf_match/dense-text-perf.pdf`、`Test.pdf`、`keyword.pdf`；
- 解包 OFD，统计 `PathObject` 数量、`AbbreviatedData` 字符数、`Content.xml` 大小；
- 与同目录的 `dense-text-perf.ofd`（文字对象版本）对照。

测量命令（本地探针，仅用于本次调研）：

```bash
# 转换
java -Djava.awt.headless=true -cp ... ConvertProbe \
  ofd_pdf_match/dense-text-perf.pdf target/probe/dense.ofd

# 统计
unzip -l target/probe/dense.ofd
grep -o '<ofd:PathObject' Doc_0/Pages/Page_0/Content.xml | wc -l
```

### 2.3 实测数据

| 指标 | 当前路径模式（dense） | 参考文字模式（dense-text-perf.ofd） |
|---|---:|---:|
| OFD 文件大小 | **9,058,628 B（9.06 MB）** | **213,077 B（213 KB）** |
| 页数 | 2 | 2 |
| `Content.xml` 未压缩总量 | 35,259,033 B | 1,595,465 B |
| `PathObject` 数量 | **29,068**（14,425 + 14,643） | 0 |
| `TextObject` 数量 | 0 | 8,478（147 + 8,331） |
| `AbbreviatedData` 字符数 | **30,618,336** | — |
| 字符总数 | — | 30,516 |
| 唯一字符数 | — | **636** |
| 内嵌字体 | 无 | `SimSun.ttf`，149,524 B，645 个字形 |

补充：

- `Test.pdf`：5 页，5,771 个 `PathObject`，路径数据 6,897,838 字符，OFD 2.22 MB；
- `keyword.pdf`：1 页，212 个 `PathObject`，路径数据 215,214 字符，OFD 731 KB，主要体积来自 2 张图片；
- 当前模式压缩比：`35.26 MB → 9.06 MB`，约 **3.9×**（路径坐标尾部差异大，deflate 收益有限）；
- 参考文字模式压缩比：`1.60 MB → 213 KB`，约 **7.5×**（文字内容重复度高，压缩更有效）。

### 2.4 体积模型

设：

- `N`：页面字符数；
- `U`：文档唯一字形数；
- `P`：单个字形轮廓的平均路径字节数（实测约 0.8～1.1 KB）；
- `C`：单个字符的 TextCode/Delta 平均字节数（实测文字 OFD 约 20～50 B/字符，未压缩）；
- `F`：内嵌字体大小（PDF 内嵌字体原样嵌入，常见数十 KB～数百 KB；完整 CJK 字体可达 5～10 MB，见 5.6.4）。

则：

```text
路径模式未压缩体积 ≈ N × P
文字模式未压缩体积 ≈ N × C + U × P_glyph + F
```

对 `dense-text-perf` 而言：`N≈30,516`、`U≈636`、`P≈1 KB`：

```text
路径模式 ≈ 30.6 MB
文字模式 ≈ 30,516 × 30 B + 636 × 0.3 KB + 150 KB ≈ 1.2～1.5 MB
```

压缩后正好落在 0.3～0.5 MB 量级，与参考 213 KB 同数量级。**结论：只要文字能回到 TextObject，体积问题就解决了大半；剩余体积主要由内嵌字体决定，而 PDF 自带的内嵌字体通常已经是子集，原样复用即可。**

---

## 3. 根因分析

### 3.1 PDFBox 为什么把文字画成路径

`PageDrawer` 是 PDF 的“渲染器”，不是“结构提取器”。它需要支持：

- Type3 字体（字形本身就是内容流）；
- 任意文字矩阵（旋转、剪切、镜像）；
- 文字裁剪（RenderingMode 4～7）；
- 图案/渐变/软掩码填充文字；
- 字体缺失时的替代。

因此 PDFBox 统一把字形转成 `GeneralPath` 后 `fill`，这是渲染正确性的选择，但**丢失了字符编码、字体、字距等语义**。

### 3.2 现有 OFD 输出为什么特别大

即使同样是“路径”，当前 OFD 输出还有三个放大因素：

1. **每个字形一个 `PathObject`**：没有按行/按 run 合并，XML 对象开销大；
2. **绝对坐标**：每个字形路径都带整页绝对坐标，相同字形在不同位置无法复用；
3. **`Boundary` 恒为整页**：每个图元都携带整页范围，选择/裁剪/渲染都更重。

`OFDShapes.path` 输出的是页面绝对坐标（见 `fill`/`draw`），所以即使两个字形轮廓完全一样，路径字符串也不同，无法通过简单去重复用。

### 3.3 为什么“只做压缩/后处理”不够

- 路径坐标是高熵数值，deflate 只有约 3.9×；
- 降低小数精度只能省 10%～20%，且会影响还原精度；
- 去重需要先把路径改成局部坐标并引入可复用资源，本质仍是“字形复用”，复杂度不低于文字对象化；
- 参考文字 OFD 已经证明：**同样的内容，语义化后未压缩就小 20 倍以上**。

---

## 4. 方案调研

### 4.1 候选方案对比

| 方案 | 原理 | 体积 | 视觉保真 | 可选中/可搜索 | 兼容性/复杂度 | 结论 |
|---|---|---|---|---|---|---|
| **A. TextObject + 原样嵌入 PDF 字体** | 拦截字形，写 OFD 文字对象，PDF 内嵌字体原样嵌入 | **最好**（10～30×） | 高（同字体轮廓） | **好** | 中；需定位/回退，无需重排 GID | **推荐** |
| B. TextObject + 系统字体引用 | 只写字体名，不嵌字体 | 最小 | 依赖查看器字体 | 好 | 低；但不可移植、易缺字 | 不采用（存档场景不可接受） |
| C. 字形轮廓复用 | 每个唯一字形做成 `CompositeObject`/`CT_VectorG`，出现处引用 | 中（5～15×） | **最高**（精确轮廓） | 差（仍是路径） | 中；查看器支持与性能待验证 | 作为字体不可用时的降级 |
| D. 路径局部化 + 去重 | 路径改局部坐标，按轮廓哈希去重 | 低（2～5×） | 最高 | 差 | 中；OFD 无路径资源，需 Template/Composite | 收益不足，不单独采用 |
| E. 文字栅格化为图片 | 整页/文字转图 | 取决于压缩 | 差（放大模糊） | 无 | 低 | 不采用 |
| F. 仅后处理优化 | 去重图片/字体、降精度、重压缩 | 很有限 | 高 | 不变 | 低 | 只能作为补充 |
| G. PDFTextStripper 文字覆盖 | 提取文本与坐标，再在路径上覆盖文字 | 小 | 低（z-order/旋转/裁剪难对齐） | 好 | 低 | 不采用（仅应急） |

### 4.2 业界与开源做法

| 来源 | 做法 | 对本项目的启示 |
|---|---|---|
| **GB/T 33190-2016** 第 11 章 | `TextObject` + `TextCode`（X/Y/DeltaX/DeltaY）+ `CGTransform`；`Font` 可带 `FontFile`，要求 OpenType | 目标格式原生支持文字对象与字体嵌入，方案 A 是标准路径 |
| **libofd / pdf2ofd**（C++，Poppler） | 用 Poppler `drawChar(x,y,dx,dy,code,unicode)` 逐字回调；用 FontForge 把字体转成可嵌入格式并子集化；同时导入 path/image | 证明“字形级拦截 + 字体子集”可行；FontForge 那一步在 Java 侧可用 PDFBox `TTFSubsetter` 替代（仅 TrueType 轮廓） |
| **java-ys/PDF2OFD-1**（Java） | 继承 `PDFGraphicsStreamEngine`，在 `showGlyph` 中生成 `TextObject`；直接嵌入 `FontFile2` 原始数据（`.otf`），未做子集 | 证明 PDFBox 字形入口能拿到足够信息；但其“整字体嵌入 + 无子集”会导致 CJK 文档反而变大，不能照搬 |
| **Suwell / Foxit 等商业转换器** | 生成 `TextObject` + 内嵌子集字体 | `dense-text-perf.ofd` 即文字对象产物：213 KB、645 字形 SimSun 子集，可作为验收参照 |
| **PDFBox `TTFSubsetter`** | 按 GID 子集 TrueType，返回 `new GID → old GID` 映射（需反转得到旧→新） | 可直接用于 OFD 字体子集；子集后 `CGTransform` 必须使用新 GID |
| **ofdrw-font `EmbeddedFontData`** | 字体格式识别、TTC 单字体提取、sfnt 重建 | 可复用于“从 PDF FontFile2/3 提取可嵌入数据”，避免重复造轮子 |
| **OFD `CompositeObject`（第 13 章）** | 矢量图资源 `CT_VectorG` 可被多个 `CompositeObject` 引用 | 字形复用降级方案的标准依据 |

### 4.3 方案结论

- **主方案选 A**：只有把文字还原为 `TextObject`，才能同时解决体积、可搜索、可编辑/可重排的基础问题；
- **C 作为降级**：当字体不可嵌入（Type3、CFF 不支持、授权限制、非内嵌且找不到替代字体）时，用 `CompositeObject` 复用字形轮廓，仍可比“每字一个绝对坐标路径”小一个数量级；
- **B 不采用**：OFD 归档必须跨机器可读，不能依赖查看器本机字体；
- **D/E/F 不作为主方案**；**G 只适合应急验证**，因为 `PDFTextStripper` 的坐标不含完整 CTM/裁剪/连字信息，覆盖后容易出现文字与路径错位、z-order 错误。

---

## 5. 推荐方案设计

### 5.1 目标与非目标

**目标**

1. 纯文字/文字为主的 PDF，OFD 体积降到当前的 1/10 以下（目标 1/15～1/30）；
2. 视觉与源 PDF 一致：文字位置、字号、颜色、旋转、字距不出现系统性偏移；
3. 文字可选中、可搜索（有 ToUnicode 时），文本提取与源 PDF 一致；
4. 任何不支持的场景都能安全回退，不产生错位、缺字、重复绘制；
5. 不破坏现有 `PDFConverter` 公开 API 与书签/附件/页面组装行为。

**非目标（首版）**

- 不追求 100% PDF 特性覆盖；
- 不做字体子集化（原样嵌入；`subsetFont` 可选开关后置）；
- 不做图片重编码/降采样（另立优化项）；
- 不改变 OFD → PDF 方向。

### 5.2 总体架构

```text
PDFConverter（现有入口，保持签名）
  └─ PdfToOfdPipeline（新增，内部包）
       ├─ OfdPageDrawer extends PageDrawer
       │    ├─ showFontGlyph(...)  ──► TextRunCollector
       │    ├─ fillPath/strokePath/drawImage/... ──► super（现有 OFDPageGraphics2D）
       │    └─ 不支持的文字 ──► super.showFontGlyph（回退路径）
       ├─ TextRunBuilder ──► TextObject / TextCode / CGTransform
       ├─ OfdPageSink ──► 按内容顺序 append 到 OFDPageGraphics2D
       ├─ PdfFontRegistry ──► PDFont → OFD Font 资源；原样导出字体程序；close 时写文件
       └─ FallbackRenderer ──► CompositeObject 字形复用 / 路径
```

**职责边界**

| 组件 | 职责 |
|---|---|
| `OfdPageDrawer` | 只做“拦截与分流”：能文字化就交给 `TextRunCollector`，否则 `super` |
| `TextRunBuilder` | 把连续字形组织成 `TextObject`，计算坐标/字号/CTM/Delta/CGTransform |
| `OfdPageSink` | 在正确 z-order 位置把 `TextObject` 追加到页面内容；对 `OFDPageGraphics2D` 做一层薄适配 |
| `PdfFontRegistry` | 字体去重、GID 收集、字体程序原样导出、字体资源 XML、字体文件写入 |
| `FallbackRenderer` | 字体/绘制模式不支持时的路径或 `CompositeObject` 复用 |

### 5.3 拦截点与 z-order

**拦截 `showFontGlyph`，不拦截 `showGlyph`。**

PDFBox 3.0.8 的签名是：

```java
protected void showFontGlyph(Matrix textRenderingMatrix, PDFont font,
                             int code, Vector displacement) throws IOException
```

原因：

- `PDFStreamEngine.showGlyph` 只做 Type3 / 非 Type3 分派；
- `PageDrawer.showFontGlyph` 才拿到 `textRenderingMatrix`、`PDFont`、`code`、`displacement`；
- 保留 `showGlyph` 的 Type3 分派，Type3 可以直接回退到 `super.showType3Glyph`（路径）；
- `PageDrawer` 是 public、非 final，`PDFRenderer.createPageDrawer(PageDrawerParameters)` 是 protected，可被子类覆盖以注入自定义 Drawer。

**z-order 保证：**

- `PageDrawer` 按内容流顺序回调 `showFontGlyph`、`fillPath`、`drawImage`；
- 每处理完一个 `Tj`/`TJ` 字符串段（`showText` 返回时）就 flush 当前文字 run；
- 在 `fillPath` / `strokePath` / `drawImage` / `shadingFill` 等图形操作前，也 flush 当前 run；
- `OFDPageGraphics2D` 暴露一个“追加 PageBlock”的内部方法（如 `appendPageBlock(OFDElement)`），`OfdPageSink` 通过它在当前容器尾部追加 `TextObject`；
- 这样文字和图形严格按内容流顺序进入 `CT_PageBlock`，不会出现“文字永远在最上层/最下层”。

**必须回退到 `super` 的场景（关键）：**

- 当前 `Graphics2D` 不是页面主画布（透明组、软掩码、平铺图案等离屏绘制）；
  - 判断方式：`getGraphics() != pageGraphics`；
  - 这些内容最终以图片形式合成，若在其中追加 TextObject 会打乱 z-order；
- 文字渲染模式为裁剪（4～7）；
- 文字填充是 Pattern/Shading/SoftMask，或颜色空间无法转成 OFD 纯色；
- Type3 字体、字体程序无法解析/不可嵌入、授权不允许嵌入；
- 文本 run 中出现无法用 `TextCode/CGTransform` 表达的字形映射。

### 5.4 几何：位置、字号、CTM、Delta

这是本方案最容易出错的部分，必须先定公式再实现。

#### 5.4.1 坐标链

设：

- `Pg`：`getGraphics().getTransform()`，把 PDF 用户空间（pt，y 向上）映射到 OFD 页面 mm（y 向下）；
- `TRM`：PDFBox 传入的 `textRenderingMatrix`，把文字空间（em 单位）映射到 PDF 用户空间；
- `N = Pg × TRM`：把文字空间（em 单位）映射到页面 mm。

对每个字形：

```text
origin   = N · (0, 0)                         // 字形原点，页面 mm
em       = |N · (0, 1) - N · (0, 0)|          // 1 em 在页面上的长度，mm
linear   = N 的线性部分 / em                   // y 列长度为 1；x 列长度即水平缩放
```

- `TextObject.Size = em`（mm）；
- `TextObject.Boundary` = 该 run 字形包围盒（页面 mm，见 5.4.4）；
- `TextObject.CTM` = `linear`（6 个参数按 OFD 的 `[a b c d e f]` 约定，**不含平移**）；
- **`TextCode.X/Y` 是相对于 `TextObject.Boundary` 左上角的偏移，不是页面绝对坐标**：
  - `X = origin.x − boundary.x`（水平起始文字通常为 0）；
  - `Y = origin.y − boundary.y`（水平文字通常≈`Size`，即基线在 Boundary 顶部下移一个 em）；
- **首版不要写 `HScale`**：非等比缩放已经包含在 `linear` 的 x 列中，再写 `HScale` 会二次缩放。

> **实测依据**：参考 OFD `dense-text-perf.ofd` 的全部 147 个 `TextObject` 均为 `Boundary="8 4.5 70.9083 2.54"`、`Size="2.1167"`、无 `CTM`、`TextCode X="0" Y="2.1167"`，可确认 Y 取“相对 Boundary 顶部的一个 em”，而非页面绝对纵坐标。早期版本的本节写成“绝对页面坐标”，已据此更正。
>
> 对最常见的水平未旋转文字：`CTM` 省略，`Boundary = (originX, originY − asc) …`，`X=0`、`Y=originY − boundary.y`。
>
> **CTM 不得携带平移**：GB/T 33190-2016 表 34/46 规定 `X/Y` 为对象坐标系下第一个字形原点的坐标，`CTM` 是对象空间内的图元变换矩阵。把平移写进 `CTM` 并令 `X=Y=0` 虽然部分查看器能渲染，但属于依赖查看器行为的非标准写法，第三方兼容性无保障。
>
> **旋转/镜像文字为何仍走路径回退（实测结论，2024-10 补充）**：按上述标准写法实现 CTM 分支后，用 ofdrw-viewer 0.1.5 做单对象对照实验。
>
> 1. 查看器的对象包围盒按 `Boundary + CTM` 计算，符合 GB/T 语义；
> 2. 但文字路径只画出基线附近 1~2 px（`Boundary="150 100 …"`、无 CTM、`Y=2.5` 的同一对象可完整渲染，加上任意 CTM 后整块空白）；
> 3. 根因在查看器字体运行时段：它把 `findResource(0x40)` 视为“翻转标记”并从 `FT_Matrix` 中剔除，缺少该标记时字形单位矩阵按字体单位（1em→1px）解释，再被 `Size/unitsPerEm`（如 2.822/1000）压扁到约 2 px。
>
> 结论：带 `CTM` 的 `TextObject` 对当前参考查看器不可见，属查看器缺陷。为保证“对象化后仍可在参考查看器正确渲染”，**旋转/镜像文字继续回退路径绘制**，`TextRunBuilder` 的 CTM 分支保留待查看器修复后启用。
>
> 进一步地，**无 CTM** 的文字对象在参考查看器里也只画出基线附近 1~2 px：把仓库内参考 OFD `文字横向-数科.ofd` 的 `Size` 与 `CTM` 同比放大 10 倍后，字形依旧是细线，说明是同一个“字形单位矩阵按字体单位解释”的缺陷，与是否写 CTM 无关；同一查看器渲染该参考 OFD（未放大）时文字完整，是因为 `Size` 很小（≈0.02 mm）时压扁量不可见。参考语料 63 个 OFD 中共 7,021 个 `<ofd:TextObject>` 使用非单位 `CTM`，可见 CTM 写法本身是这类文档的常见形态。
>
> **PDF 的翻转位**：PDF 用户空间 y 向上、OFD 页面 y 向下，字形矩阵需翻转，而 PDFBox `PageDrawer` 以 `Matrix` 的 SFNT 资源位 `0x40` 传递该标记（矩阵本身不含此信息）。对 `ofd_pdf_match` 全部 63 个 PDF 逐字形统计（`target/pdf-textobj/p2src/probe3/FlipProbe.java`）：152,638 个字形（5 个样本为纯图片、无字形）全部按 `Matrix` 数值判定为“需翻转”，0 个携带该标记，说明按 `Matrix` 数值统一翻转对现有语料是安全的。
>
> **竖排文字（WMode=1）继续回退**：临时实验显示，PDF 竖排字形以 90° 基线竖直排列，而 OFD `TextObject` 只支持 `ReadDirection` 0/90/180/270 的整体方向，无法表达“每个字形自身旋转 90°、基线竖直”的组合；对象化后参考查看器直接不显示这些文字（`文字横向-数科.pdf` 竖排标题），故保持路径回退。

#### 5.4.2 DeltaX / DeltaY

OFD 的 `DeltaX/DeltaY` 表示“后一个文字与前一个文字之间在 X/Y 方向的偏移”。它不是“相对自然字宽的额外增量”，而是**相邻字形原点的位移**（`ofdrw-layout` 的 `Span#getDeltaX` 也是按字宽生成）。

对第 `i` 个字形（`i ≥ 1`）：

```text
delta_page  = origin_i - origin_{i-1}                 // 页面 mm
delta_local = inverse(linear) · delta_page             // 对象坐标系
DeltaX[i-1] = delta_local.x
DeltaY[i-1] = delta_local.y
```

- 对水平文字，`delta_local` 就是页面上的 `dx/dy`；
- 对旋转文字，先反变换到文字局部坐标，再写入 Delta；
- 若某个 delta 为 0，也要保留占位，保证数组与字形数量对齐；
- 单个字形的 run 不写 Delta。

PDF 的 `Tc`（字间距）、`Tw`（词间距）、`TJ` 调整、`Tz`（水平缩放）、`Ts`（rise）都已经体现在**实际 origin** 中，因此不需要单独换算，直接用 origin 差值即可。这是本方案比“按字体宽度推算”更稳的地方。

#### 5.4.3 字号与 unitsPerEm

- OFD `Size` 是 em 大小（mm），不是“1000 单位字号”；
- 内嵌字体的 `unitsPerEm` 可能是 1000、2048、甚至 256（参考 OFD 的 SimSun 子集就是 256），OFD 查看器按字体 `head.unitsPerEm` 缩放；
- 因此 `Size = em` 即可，不需要按 `unitsPerEm/1000` 修正；
- 但必须在 P0 用 `unitsPerEm=256` 的字体实测，确认查看器没有“假定 1000”的兼容问题。

#### 5.4.4 Boundary

`TextObject.Boundary` 取该 run 所有字形在页面 mm 下的并集包围盒：

```text
glyphBox_page = N · (glyphPathBounds / 1000)   // PDFBox 的字形路径是 1000 单位/em
runBox        = union(glyphBox_page)
```

- 对空格等无轮廓字形，用“origin + 字宽 × em”构造一个薄盒；
- 包围盒向外扩 1～2%，避免查看器因浮点误差裁掉边缘；
- Boundary 必须与 CTM/坐标一致，否则部分查看器会按 Boundary 裁剪。

#### 5.4.5 旋转/镜像/剪切

- `linear` 支持任意 2×2 仿射：旋转、镜像、剪切、非等比缩放都能表达；
- 旋转、剪切、非等比缩放全部放进 `linear`，不要拆到 `HScale`；
- 若 `det(linear) < 0`（镜像），保留负行列式在 CTM 中；
- 若 `linear` 近似单位矩阵，走“无 CTM”分支（直接写 X/Y），降低查看器差异；
- `HScale` 仅在确认查看器语义后使用，首版保持缺省 1.0；
- 竖排（PDF `WMode=1`）首版回退路径；P3 再映射为 `ReadDirection=90`、`CharDirection=0`，用 `DeltaY` 表达字距；
- 阿拉伯文/希伯来文等需要字形整形的文本，首版回退路径，避免 `TextCode` 与字形顺序不一致。

### 5.5 文字内容与 CGTransform

#### 5.5.1 Unicode

对每个字形 `code`：

1. `font.toUnicode(code)`；
2. 为空时，尝试字形名 → Unicode（`PDFont` 的 glyph name / Adobe Glyph List）；
3. 仍为空时，使用 OFD 规定的占位符 `¤`（U+00A4），并记 warning。

注意：

- `toUnicode` 可能返回多字符（连字 `ffi`）、控制字符或空串；
- 写入 XML 前要做 XML 转义；空格按 OFD 要求处理（实现时验证查看器对首尾空格/连续空格的保留行为）；
- 代理对（emoji、CJK 扩展 B）按 UTF-16 长度计入 `CodecPosition`，需在 P0 用样本验证。

#### 5.5.2 字形索引 GID

| PDF 字体类型 | 取 GID 的方式 |
|---|---|
| `PDType0Font` + `PDCIDFontType2`（TrueType/CID） | `((PDType0Font) font).codeToGID(code)` |
| `PDTrueTypeFont` | `((PDTrueTypeFont) font).codeToGID(code)` |
| `PDType1CFont`（CFF） | `codeToName(code)` + `CFFType1Font.nameToGID(name)`（首版可回退） |
| `PDType3Font` | 无字体程序，回退路径 |
| 未知/取不到 | 回退路径 |

#### 5.5.3 CGTransform 写法

`TextCode` 的内容是 Unicode 串；`CGTransform` 描述“字符位置 → 字形索引”：

```text
CodecPosition = 当前字形在 TextCode 中的 UTF-16 起始偏移
CodecCount    = 该字形对应的 Unicode 字符数（至少 1）
GlyphCount    = 1
Glyphs        = [GID]
```

- **一对一且字体 cmap 一致**：可以省略 `CGTransform`，由查看器用内嵌字体 cmap 查字形；这样 XML 最小（参考 OFD 就没有 CGTransform）。
- **以下情况必须写 CGTransform**：
  - Identity-H/CID 字体、cmap 与 GID 不一致；
  - 连字（多字符 → 1 字形）；
  - 一个字符 → 多个字形；
  - 缺 ToUnicode 的占位符；
  - 完整字体与 PDF 内嵌字体程序不一致（例如改用系统匹配字体，GID 可能不同）。
- 首版建议：**CID/Identity 字体总是写 CGTransform**；简单 TrueType 字体先校验 cmap 映射，一致则省略；后续再按样本优化体积。

### 5.6 字体资源（默认原样嵌入，子集化为可选开关）

#### 5.6.1 OFD 字体资源

```xml
<ofd:Font ID="1" FontName="SimSun" FamilyName="宋体" Charset="unicode"
          Serif="false" Bold="false" Italic="false" FixedWidth="false">
  <ofd:FontFile>SimSun.ttf</ofd:FontFile>
</ofd:Font>
```

- `FontName`：PostScript 名，去掉 `ABCDEF+` 子集前缀；
- `FamilyName`：`PDFontDescriptor#getFontFamily()`，为空时退回 `FontName`；
- `Charset`：一般 `unicode`；中文可 `prc`，但 `unicode` 更通用；
- `Serif/Bold/Italic/FixedWidth`：来自 descriptor 的 flags / 名称；
- `FontFile`：`Res/<name>.ttf`，由 `DocDir.addResourceWithPath` 写入；
- OFD 规范要求嵌入字体使用 OpenType 格式；TrueType 轮廓的 `.ttf` 属于 OpenType 的 TrueType 轮廓分支，参考 OFD 也使用 `.ttf`。

#### 5.6.2 字体来源优先级

> **默认策略（用户明确要求）：PDF 的内嵌字体直接作为 OFD 的内嵌字体，原样嵌入，不重新子集化。**

| 优先级 | 来源 | 默认策略 |
|---|---|---|
| 1 | PDF 内嵌 `FontFile2`（TrueType / CIDFontType2） | **原样嵌入**，字节与 PDF 一致，GID 不变 |
| 2 | PDF 内嵌 `FontFile3`（CFF / OpenType） | **原样嵌入** `.otf`；查看器不支持时回退路径 |
| 3 | PS 字体集合（TTC/OTC） | 提取指定字体重建 sfnt 后原样嵌入，GID 不变 |
| 4 | 裸 CFF / Type1（PFB） | 尝试原样嵌入；不可嵌入时回退路径 |
| 5 | 非内嵌字体 + 系统/配置字体目录精确匹配 | 嵌入匹配到的完整字体；记 warning |
| 6 | Type3 / 字体程序损坏 / 授权限制 / 无可用匹配 | 回退路径或 `CompositeObject` 字形复用 |

**硬约束：**

1. 原样嵌入时**不得**调用 `TTFSubsetter`，**不得**做 sfnt 表裁剪，字节内容必须与 PDF 内嵌字体程序一致（TTC 场景允许“提取单字体”后的重建）；
2. GID 保持 PDF 原值，`CGTransform` 直接使用 `PDType0Font/PDTrueTypeFont.codeToGID()`，不存在“旧 GID → 新 GID”映射；
3. 每个内嵌字体需标注来源类型，便于验证与排查：`embedded-verbatim` / `ttc-extracted` / `system-matched` / `path-fallback`；
4. 整字体嵌入导致的体积增量只做**记录**，不改变默认行为。

**P3 实测结论（`ofd_pdf_match` 63 个样本，2026-10）**

1. **CFF / 裸 CFF（`FontFile3` Subtype=Type1C）**：样本中共 3 条（`intro-数科`：HiraginoSansGB-W6 4,006 B、PingFangSC-Regular 79,986 B、DINPro-Black；`signout`：MinionPro-Regular 583 B），
   全部由 `PdfFontRegistry` 读取并包装为最小 OpenType 外壳（`head/hhea/hmtx/loca/maxp/OS2/name` + `CFF ` 原样数据，无 cmap），写出为 `Res/<PS名>-<hash>.otf`；
   `ofdrw-viewer` 能解析该包结构，但**文字对象化当前仍对 CFF 走路径回退**——`OfdPageDrawer#unsupportedReason` 只接受 TrueType 轮廓（`PDCIDFontType2`），
   这是因为「字素 → GID」只有 TrueType 路径有验证过的取数（`codeToGID`），CFF 需要 `codeToName` + CFF charset 解析。CFF 资源已嵌入，后续放开时只需增加 GID 取数；
2. **Chrome 导出的 `FontFile3 /Subtype /OpenType`**（`fontTools` 的静默改造）：`intro-数科` 中 9 条此类字体（含 PingFangSC-Semibold 103,140 B、HelveticaNeue 系列）字节头为 `0x00010000`/`0x74727565`，
   实际是 TrueType 轮廓的 OpenType，按 **TrueType 原样嵌入**，GID 与 PDF 一致；
3. **Type1（PFB）**：`发票监制章-数科` 的 Helvetica 为 Base14 未内嵌，无字体程序 → `path-fallback`；PFB 本身不是 OpenType，仍不写 `FontFile`；
4. **非内嵌字体 + 字体目录精确匹配（可选，默认关闭）**：`PdfFontRegistry#setFontDir` / `PDFConverter#setFontDir` 需显式调用；
   实测开启后 63 个样本中 40+ 个由 `path-fallback` 转为 `system-matched`，但**体积从 107.5 MB 增至 427 MB**（每份 ~10 MB 完整 CJK 字体逐样本嵌入），
   渲染与 PDF 一致（`hello`/`1-1` 与参考 OFD 逐像素比较 MAE < 1）；因此保持默认关闭，作为“有本地字体授权且可接受体积”场景的可选项；
   匹配到字体后按**原样嵌入**（与内嵌字体同一策略），只有开启 `subsetFont` 时才参与子集化；
5. **子集化 + GID 重映射（`subsetFont=true`，默认关闭）**：`1-1` 样本从 12.7 MB（两个 ~10 MB 完整字体）降到 **4.6 KB**，CGTransform 的 GID 在打包前按 old→new 改写
   （`PdfTextFonts#resolvePendingGids`），viewer 渲染与未子集化结果逐像素一致（MAE 0.71/255，仅抗锯齿差异）。

#### 5.6.3 子集化策略（可选能力，默认关闭）

子集化**不是默认路径**，仅作为后续可选的体积优化开关（`subsetFont`，默认 `false`）：

- 默认关闭时：完全按 5.6.2 原样嵌入；
- 开启时：对完整 CJK 字体可用 `TTFSubsetter` 按实际使用的 GID 子集，并需要配套的 cmap 重建、sfnt 表裁剪、`getGIDMap()` 反转（new→old → old→new）与 `CGTransform` 改写；
- 原样嵌入的字体**永远不参与**表裁剪；
- 若实测发现某类 PDF（例如内嵌完整 10 MB CJK 字体）因原样嵌入导致 OFD 明显变大，只记录数据与建议，默认行为不变。

**`TTFSubsetter` 用法要点：**

> 以下内容仅在启用可选开关 `subsetFont=true` 时适用；默认（原样嵌入）路径不涉及本节任何一步。

```java
TTFSubsetter subsetter = new TTFSubsetter(trueTypeFont);
// 关键 1：先对每个有 Unicode 的字符调用 add(unicode)，才能填充 uniToGID、重建 cmap
for (int unicode : usedUnicodes) {
    subsetter.add(unicode);             // 注意：add(int) 的参数是 Unicode，不是 GID
}
subsetter.addGlyphIds(usedGids);        // 关键 2：把 PDF 实际使用的 GID 也加进来
Map<Integer,Integer> newToOld = subsetter.getGIDMap(); // 注意：返回 new -> old
// CGTransform 需要 old -> new，必须反转
Map<Integer,Integer> oldToNew = invert(newToOld);
subsetter.writeToStream(out);
```

- 必须包含 GID 0（`.notdef`）；
- 复合字形依赖会自动加入；
- **`addGlyphIds` 不会填充 `uniToGID`**：只调用它时，`buildCmapTable()` 直接返回 null，子集字体可能保留原始 cmap（指向旧 GID）。实测 637 字形的子集 cmap 仍有 **28,214 条失效映射**；补上 `add(unicode)` 后，cmap 变为 636 条有效映射，`A→22、B→23、测→404、试→541`。
- **`getGIDMap()` 返回的是 `new GID -> old GID`**（PDFBox 3.0.8 源码注释明确），不要直接当 old→new 用；否则 `CGTransform` 会指向错误字形。
- 子集后 `CGTransform.Glyphs` 必须使用反转后的新 GID；
- 即使 cmap 已重建，建议 CID/Identity 字体仍写 `CGTransform`，以 PDF 的实际 GID 为准；
- 子集后重新用 `TTFParser` 解析，校验 `numGlyphs`、`unitsPerEm`、`cmap`；
- 子集失败（无 Unicode cmap、表损坏、复合字形异常）→ 回退整字体嵌入或路径。

#### 5.6.4 子集字体的表裁剪（仅在 `subsetFont=true` 时使用）

`TTFSubsetter` 会按 GID 裁剪 `glyf/loca/hmtx`，并在调用 `add(unicode)` 后重建 `cmap`；但它**不会主动删除与轮廓无关的表**。对 Windows SimSun（`unitsPerEm=256`）用 `dense-text-perf` 的 636 个实际字符（637 个 GID）做子集，实测：

| 表 | 大小（字节） |
|---|---:|
| `glyf` | 178,256 |
| `EBDT`（内嵌位图） | 5,532,156 |
| `EBLC`（位图位置） | 683,696 |
| `vmtx`（竖排度量） | 115,488 |
| `cmap` / `hmtx` / `loca` / `name` / `post` | 约 9,650 |
| 其他表 | 约 5,900 |
| **合计** | **6,525,120** |

裁剪 `EBDT/EBLC/vmtx` 等表后：**192,644 B**；参考 OFD 的 SimSun 子集为 **149,524 B**。如果不做表裁剪，字体子集反而可能比“路径模式”还大，文字模式的体积收益会被完全吃掉。

**实现要求：**

1. 白名单保留：`head`、`hhea`、`maxp`、`hmtx`、`loca`、`glyf`、`cmap`、`name`、`OS/2`、`post`、`cvt `、`fpgm`、`prep`、`gasp`；
2. 删除位图表：`EBDT/EBLC/EBSC`、`bdat/bloc/bdsc`、`CBDT/CBLC`、`sbix`、`SVG `；
3. 水平文字删除 `vmtx/vhea`；后续支持竖排时再保留/重建；
4. 删除 `GSUB/GPOS/GDEF/kern`（本方案逐字形定位，不依赖排版表；复杂脚本再评估）；
5. 删除 `DSIG/LTSH/VDMX/hdmx/MERG/meta` 等非必要表；
6. 重建 sfnt 表目录、重算 `checkSumAdjustment`，用 `TTFParser` 重新解析校验；
7. 裁剪/重建失败时回退整字体嵌入或路径。

fontbox 已提供按表读取能力：`TrueTypeFont#getTables()`、`TTFTable#getTag()`、`TrueTypeFont#getTableBytes(...)`。因此“子集 → 解析 → 按白名单重写 sfnt”可以纯 Java 完成，不必引入外部 FontForge/fontTools；只有 CFF 字体的转换/子集才需要另行评估。

> 说明：如果 PDF 内嵌字体本身已经是子集（例如 `Test.pdf` 的 188 KB FontFile2），通常已经不含大块位图表，可以原样嵌入，不必再裁剪；表裁剪主要针对“完整 CJK 字体 → 子集”的场景。

**两遍式写入：**

1. 渲染页面时：为每个字体分配资源 ID，记录 `usedGids` 和 Unicode 映射；`TextObject` 只引用字体 ID；
2. `close()` 前：对每个字体做子集化/写文件，补齐 `CT_Font.FontFile`；再打包 OFD。

这样避免“每页都重写字体”，也避免字体资源 ID 变化导致页面 XML 失效。

**去重：**

- 以“字体程序字节 SHA-256 + 是否子集 + 使用 GID 集合”为 key；
- 同一字体在多页/多文档复用同一个 `CT_Font`；
- 同一字体程序的不同编码（Identity-H / WinAnsi）可共享字体资源，映射由 `CGTransform`/TextCode 决定。

**成本决策：**

- 若某字体只用于少量字形（例如 3 个装饰字），子集字体可能仍有几十 KB，而路径只有几 KB → 该字体回退路径；
- 若字体用于大量字符，文字模式明显更小；
- 首版可以“优先文字”，后续按 font 统计做成本决策。

### 5.7 颜色、透明度、描边、裁剪、渲染模式

| PDF 状态 | OFD 映射 | 不支持时 |
|---|---|---|
| `DeviceGray/RGB/CMYK` 填充/描边色 | `CT_Color` 对应颜色空间 | — |
| `ICCBased/Indexed/Separation/DeviceN` | `PDColor.toRGB()` 近似 | 颜色空间复杂/图案 → 回退 |
| 非描边 Alpha | `TextObject.Alpha`（0～255） | 与颜色 Alpha 相乘后写入 |
| 描边 Alpha | `TextObject.Alpha` 或拆成两个对象 | 填充/描边 Alpha 不同 → 回退或拆分 |
| RenderingMode 0（填充） | `Fill=true, Stroke=false` | — |
| RenderingMode 1（描边） | `Stroke=true, Fill=false` + 线宽 | 虚线/线帽复杂 → 回退 |
| RenderingMode 2（填充+描边） | 两者都 true | 同上 |
| RenderingMode 3（不可见） | `Alpha=0` 的 TextObject（保留可搜索） | 查看器不支持则跳过 |
| RenderingMode 4～7（裁剪） | — | **回退路径** |
| Pattern / Shading / SoftMask 填充 | — | **回退路径** |
| Clip | `TextObject.Clips`（路径裁剪） | 复杂裁剪/文字裁剪 → 回退 |

补充：

- `Alpha` 缺省为 255；只有显式非 255 才写；
- 颜色 Alpha 与文字 Alpha 同时存在时相乘，避免双重透明；
- 混合模式（BlendMode）OFD 不一定支持；非 Normal 时回退；
- 文字描边的线宽单位是 PDF 用户空间，需按 `Pg` 换算到 mm。

### 5.8 降级策略（AUTO 模式）

**能文字化的条件（全部满足）：**

1. 字体是 TrueType/`PDCIDFontType2`，或能找到可嵌入的精确替代字体；
2. 字体程序**可原样嵌入**（能解析为 sfnt/OpenType；TTC 能提取单字体）；是否需要子集化与能否文字化无关；
3. 文字是水平或可用 CTM 表达的旋转/剪切；
4. 填充/描边是 OFD 支持的纯色；
5. 不是文字裁剪模式，不在透明组/离屏画布内；
6. 没有 Pattern/Shading/SoftMask；
7. `CGTransform` 能正确表达该 run 的字符↔字形关系。

**否则：**

- 默认：`super.showFontGlyph(...)`，保持当前路径绘制（视觉最稳）；
- 可选（P3）：对重复出现的 Type3/不可嵌入字形，先渲染一次轮廓，做成 `CT_VectorG` 资源，再用 `CompositeObject` 引用；
- 所有降级按“原因 + 字体 + 页”聚合 warning，避免每字一条日志。

### 5.9 字形轮廓复用（降级方案 C）

当文字不能对象化时，仍可避免“每字一条绝对路径”：

```text
唯一字形轮廓（局部坐标）
  → Res 中的 CT_VectorG（Width/Height + Content/PathObject）
  → 页面中的 CompositeObject（ResourceID + Boundary + CTM）
```

- 每个唯一字形只存一份轮廓；
- 每个出现位置只存 `ResourceID + CTM`（约几十字节）；
- 对 `dense-text-perf`：636 个唯一字形 × 约 0.3 KB ≈ 190 KB 轮廓，3 万个引用 × 约 60 B ≈ 1.8 MB，压缩后约 0.3～0.5 MB；
- 代价：文字不可选中/搜索；查看器对大量 `CompositeObject` 的渲染性能未知；
- 因此只作为 Type3/CFF/授权受限字体的降级，不作为主方案。

**P3 可行性度量结论：暂不落地**

| 场景 | 当前回退路径体积 | 复用方案（估算） | 结论 |
|---|---|---|---|
| `intro-数科`（CFF 3 条，18,607 次字形） | 35.3 MB（其中路径约 1.5 MB，其余为图片与已嵌入字体） | 需先按 CFF charset 取 GID 才能复用 glyph 轮廓，否则只能复用“渲染后的轮廓”，每个字体约 0.6 MB 轮廓 + 35 万次引用 × 60 B ≈ 21 MB | 体积反而变大，不落地 |
| Type1/Base14（`发票监制章-数科` 220 个路径） | 37 KB | 约 220 次引用 × 60 B + 少量轮廓 ≈ 15 KB | 体积收益有限，但需新增 `CT_VectorG` 资源管理与查看器兼容验证 |
| `dense-text-perf`（参考数据） | — | 636 字形 × 0.3 KB + 3 万引用 × 60 B ≈ 0.3 MB | 文字对象化已能做到 0.13 MB，复用方案无优势 |

判定依据：

1. **可复用的前提是“同一字体的同一字形可以取到稳定轮廓”**。CFF 目前走路径回退，正是因为取不到 GID；
   若要复用，必须先实现 CFF 的 `codeToName + charset` 解析（与让 CFF 直接文字对象化的工作量相同），而后者收益更大（文字可选中/可搜索）；
2. 复用方案的**每个引用仍需 `Boundary + CTM`**，体积与路径压缩后的单个字形相当，只在“同一字形出现数百次”时才有明显收益，
   而这类文本（如密集 CJK）在字体可嵌入时已直接走文字对象化；
3. 查看器对大量 `CompositeObject` 的渲染性能未验证，且文字不可选中，与“文字对象化”目标冲突。

**结论**：优先级低于“补 CFF 的 GID 取数（让 CFF 直接文字对象化）”；在 CFF 仍无法对象化且单字体轮廓重复次数 > 50 的样本上，
可作为 P4 的备选优化重新评估，当前不实现。

### 5.10 二次优化（文字模式之后）

1. **字体去重**：同一字体程序只嵌入一次；
2. **图片去重**：按内容哈希复用 `CT_MultiMedia`（现有 `ResManager` 对图片未做内容哈希去重）；
3. **坐标精度**：统一保留 2～3 位小数（0.01 mm 级），避免 `209.906` 这类长尾；
4. **路径局部化**：非文字路径的 `Boundary` 改为实际包围盒，路径数据改局部坐标；
5. **删除未使用资源**：转换结束后清理未被引用的字体/图片/DrawParam；
6. **ZIP 压缩级别**：在打包层统一 deflate level；不建议引入非标准 LZMA，除非确认查看器支持。

### 5.11 API 与兼容性

> **前提（用户明确要求）：不需要兼容旧实现。** 旧版“整页路径渲染”的输出不再作为兼容目标，`PDFConverter` 默认行为直接改为文字对象化；旧输出只作为**对照基线**，不是需要保持的契约。

设计要点：

1. **默认即文字对象化**，不依赖任何开关；`PDFConverter` 的既有公开方法签名保持不变（`convert(Path)`、`convert(Path, Path, int)` 等），避免破坏调用方编译；
2. **不引入 `PdfTextMode` 之类的模式开关**做新旧切换，避免出现“两套输出契约”；若排障确需对照旧行为，用测试侧的独立入口（测试代码，不入主流 API）；
3. 新增的字体查找目录能力提供最小公开方法（如 `setFontDir(Path)`），无默认值影响：默认按“PDF 内嵌字体优先”，找不到内嵌字体才查系统字体；
4. 新增内部类全部 package-private，不扩大公开 API 面；
5. 诊断信息沿用仓库习惯：warning 日志，按“原因 + 字体 + 页”聚合；不新增 Result 对象；
6. **回退路径仍然保留**（Type3、不可嵌入字体、文字裁剪等），但回退是“文字对象化内部的技术降级”，不是“兼容旧实现”，不需要为此保留旧的行为契约；
7. 测试与验证不再要求 `PATH_ONLY` 输出与旧版逐字节一致；旧实现仅用于 A/B 体积与像素对照。

### 5.11.1 两套实现并存（文字对象化 vs 图形化）

> **后续修订**：第 1 条"不需要兼容旧实现"指的是**不保留旧的默认行为契约**。实际交付时，旧版"整页路径渲染"以**另一个独立类**的形式保留下来，作为与 `PDFConverter` 并列的可选实现，供"只要视觉一致、不需要文本检索"的场景使用。两套实现互不复用、互不影响。

| | `PDFConverter`（推荐，默认方案） | `PDFGraphicsConverter`（可选，图形化） |
|:--|:--|:--|
| 所在包 | `org.ofdrw.converter.ofdconverter` | 同左，与 `PDFConverter` 并列 |
| 实现方式 | `OfdPageDrawer extends PageDrawer`，在 `showFontGlyph` 拦截并输出 `TextObject` | `PDFRenderer.renderPageToGraphics(index, OFDPageGraphics2D, scale)`，纯 `Graphics2D` 接口 |
| 文本 | `TextObject` + 内嵌字体 + `CGTransform` | 全部展开为 `PathObject`，**无 `TextObject`** |
| 字体资源 | 内嵌字体（可子集化） | **完全不嵌入字体** |
| 文本可检索/可复制 | 支持 | **不支持**（字形已是路径） |
| 体积（`dense-text-perf`） | 305 KB | **9 MB（约 30 倍）** |
| 依赖 | PDFBox `PageDrawer` 子类 + 字体嵌入链路 | 仅 `ofdrw-graphics2d` |
| 页面旋转 `/Rotate` 90/270 | 由 PDFBox 渲染管线处理，页面尺寸交换宽高 | 同左（显式交换宽高） |

**设计理由**：文字对象化解决的是"PDF 转 OFD 后文字仍可检索、体积可控"的核心诉求，但它的可用性依赖字体能否嵌入。对字形无法嵌入、或调用方只要"看起来一样"的可视化归档场景，保留一条纯 `Graphics2D` 的渲染路径更简单也更稳妥——这条路径不碰字体、不碰文字定位，因此不会引入文字对象化侧的回归。

**适用场景划分**：

| 场景 | 推荐实现 |
|:--|:--|
| 正式文档归档、需要全文检索或复制文本 | `PDFConverter` |
| 追求体积最小 | `PDFConverter` |
| 字形无法嵌入（非内嵌且无系统字体匹配）、或字体嵌入后仍显示异常 | `PDFGraphicsConverter` |
| 只要视觉一致的截图式归档 | `PDFGraphicsConverter` |
| 对转换结果做像素级对照（A/B 验证） | 两者都可用，`PDFGraphicsConverter` 更接近"旧基线" |

**约束**：新增 `PDFGraphicsConverter` 不改动 `PDFConverter`、`OfdPageDrawer`、`TextRunBuilder`、`OFDPageGraphics2D` 的任何行为；两套实现可在同一工程内并行使用。

### 5.12 关键伪代码

```java
// OfdPageDrawer
@Override
protected void showFontGlyph(Matrix trm, PDFont font, int code, Vector disp) throws IOException {
    if (!canTextualize(font, code, trm)) {
        super.showFontGlyph(trm, font, code, disp);   // 路径回退
        return;
    }
    Glyph g = buildGlyph(trm, font, code, disp);
    if (run == null || !run.accepts(g)) {
        flushRun();
        run = new TextRun(g);
    }
    run.add(g);
    if (run.size() >= MAX_GLYPHS_PER_RUN) {
        flushRun();
    }
}

@Override
public void fillPath(int windingRule) throws IOException {
    flushRun();          // 保证 z-order
    super.fillPath(windingRule);
}

@Override
public void drawImage(PDImage img) throws IOException {
    flushRun();
    super.drawImage(img);
}

@Override
protected void showText(byte[] string) throws IOException {
    super.showText(string);   // 内部逐字回调 showFontGlyph
    flushRun();               // 一个 Tj/TJ 段结束
}

private void flushRun() {
    if (run == null) return;
    TextObject obj = run.build(fontRegistry, pageSink);
    pageSink.append(obj);     // 追加到当前 CT_PageBlock 尾部
    run = null;
}
```

`TextRunBuilder.build` 的核心：

```text
1. 取首个字形 origin / em / linear，构造 CTM 与 Size；
2. 逐字形累加 TextCode 内容，构造 DeltaX/DeltaY；
3. 必要时构造 CGTransform（使用 PDF 实际 GID，原样嵌入下即 PDF GID）；
4. 设置 Fill/Stroke/颜色/Alpha/CTM/Boundary/Clips（首版不写 HScale）；
5. 注册字体与 GID 使用集合；
6. 返回 TextObject。
```

### 5.13 改动范围（预估）

| 模块 | 改动 | 说明 |
|---|---|---|
| `ofdrw-converter` | 新增 `OfdPageDrawer`、`TextRunBuilder`、`PdfFontRegistry`、`OfdPageSink`；`PDFConverter` 默认改为文字对象化（不加模式开关） | 主体 |
| `ofdrw-graphics2d` | 增加“追加 PageBlock/TextObject”的内部方法 | 小改，保持现有 `fill/draw` 行为 |
| `ofdrw-font` | 默认无改动，复用 `EmbeddedFontData` 原样导出；仅当启用可选 `subsetFont` 时才需要 sfnt 表裁剪/重建工具 | 复用 `TrueTypeFont` 表读取能力 |
| `ofdrw-core` | 无 | `TextObject`/`TextCode`/`CGTransform`/`Font` 已具备 |
| 文档 | 更新 `CONVERTER.md`、release notes | 说明默认行为变化与回退开关 |

---

## 6. 预期收益

### 6.1 体积

| 场景 | 当前 | 文字模式预期 | 说明 |
|---|---:|---:|---|
| 纯文字，CJK，嵌入字体 | 9.06 MB（dense） | **0.3～0.5 MB** | 参考 OFD 213 KB；差距来自对象分组/字体子集 |
| 纯文字，拉丁，嵌入字体 | 1～3 MB/百页 | 0.1～0.4 MB/百页 | 字形少，字体子集更小 |
| 图文混排 | 2～10 MB | 降低 30%～70% | 图片体积不变，文字部分大幅下降 |
| 图片为主/扫描件 | 取决于图片 | 基本不变 | 需要单独的图片优化 |
| 非内嵌字体且无替代 | 大 | 不改善（回退路径） | 可选 `CompositeObject` 降级 |

### 6.2 性能

- 文字模式省去“每字构造 PathObject + 长路径 XML”，预计转换速度不降反升；
- 字体子集化是一次性成本（每个字体几十毫秒到几百毫秒），对多页文档可忽略；
- 内存主要增加“每字体 GID 集合 + 字体字节”，远小于路径 XML 的内存占用。

### 6.3 质量

- 视觉：同一字体轮廓 + 精确 origin/delta，理论上与路径模式一致；差异主要来自字体 hinting/抗锯齿；
- 文本：有 ToUnicode 时可选中、可搜索、可提取；无 ToUnicode 时用占位符，视觉正确但搜索性下降；
- 结构：文字对象可按行/按 run 组织，便于后续编辑/重排。

---

## 7. 风险与验证

### 7.1 风险清单

| 风险 | 概率 | 影响 | 缓解 |
|---|---|---|---|
| OFD 的 Delta/CTM 语义与 PDF 不完全一致，文字整体偏移 | 中 | 高 | P0 用 3～5 个样本校准；像素 + 文字包围盒双门禁 |
| 字体子集后 GID 重排，CGTransform 写错，字形错乱 | 中 | 高 | 子集后用 `TTFParser` 重解析；逐字形渲染比对；GIDMap 单测 |
| `TTFSubsetter` 保留 EBDT/EBLC/vmtx，子集字体反而巨大 | 高 | 高 | 子集后按白名单裁剪表并重建 sfnt；实测 SimSun 6.52 MB → 192 KB |
| 查看器不支持内嵌 TTF/CFF，或对 `unitsPerEm=256` 处理异常 | 中 | 高 | 建立查看器矩阵（ofdrw-viewer、Foxit、WPS、数科）；准备回退开关 |
| CFF/Type1/Type3 字体无法对象化 | 高 | 中 | 首版回退路径；P3 做 `CompositeObject` 复用/CFF 包装 |
| 非内嵌字体找不到、或授权不允许嵌入 | 高 | 中 | 配置字体目录；精确匹配才嵌入；否则回退；记录 warning |
| 缺 ToUnicode，文本不可搜索 | 中 | 中 | 字形名 → AGL 反查；占位符 + warning；不阻断转换 |
| 文字裁剪/透明组/图案填充导致 z-order 错乱 | 中 | 高 | 检测离屏 Graphics2D；不支持的模式一律 `super` 回退 |
| 连字/代理对/多对多 CGTransform 表达错误 | 中 | 中 | 首版只处理一对一；异常回退路径；专项样本 |
| 字体子集体积反而大于路径（少量字形） | 低 | 低 | 按字体成本决策；少量字形回退路径 |
| 输出契约改变，现有依赖旧输出的用例受影响 | 中 | 中 | 按要求不兼容旧实现；对 58 组样本做 A/B 对照，旧实现只作基线；更新受影响用例与文档 |
| PDFBox 升级导致 `PageDrawer` 受保护 API 变化 | 低 | 中 | 锁定 3.0.8；升级时跑 Drawer 子类兼容测试 |

### 7.2 验证矩阵

**单元级**

- 几何：给定 `Pg`/`TRM`，断言 origin、em、CTM、Delta（若启用 HScale 再补 HScale）；
- 文本：一对一、连字、空格、代理对、缺 ToUnicode；
- 字体：`TTFSubsetter` GIDMap、子集解析、`unitsPerEm=256`；
- 颜色/Alpha/描边/裁剪映射。

**集成级**

- 58 组 `ofd_pdf_match` PDF 全量转换：
  - 体积对比（当前 vs 文字模式 vs 参考 OFD）；
  - 像素对比：OFD 渲染 vs 源 PDF 渲染（72/144 dpi）；
  - 文本对比：OFD 提取文本 vs PDF 提取文本（相似度 ≥ 0.99）；
  - 文字包围盒：`xMin/yMin` 偏差 ≤ 2 pt（沿用 PDF 导出重构的经验：纯像素门禁对文字整体偏移不敏感）；
- 关键样本：`dense-text-perf`、`Test`、`manual`（89 页）、`intro-数科`（CFF 字体）、`透明度文字`、`chineseDir`（非内嵌字体）、`DigitalSign`/`AddWatermarkAnnot`（注释外观）。

**查看器矩阵**

- ofdrw-viewer（首要）；
- Foxit Reader / Foxit PDF SDK；
- WPS OFD；
- 数科 OFD；
- 浏览器/移动端若有。

**字体有效性**

- 子集字体用 `TTFParser` 解析通过；
- `numGlyphs`、`unitsPerEm`、`cmap` 正常；
- 随机抽取 20 个字形，用字体渲染与路径渲染比对轮廓。

### 7.3 验收标准（建议）

| 指标 | 阈值 |
|---|---|
| `dense-text-perf` OFD 体积 | ≤ 0.6 MB（当前 9.06 MB，参考 213 KB；实测已达成 294,766 B） |
| 纯文字样本平均体积 | ≤ 当前的 1/10 |
| 像素差异（72 dpi） | 不以绝对 0.5% 为门禁；以“参考文字 OFD / 当前路径 OFD”为基线，文字模式不劣于基线；辅以 SSIM/边缘对比 |
| **文字基线位置** | **按“行内字形像素位置 / 行重心”判定**（见下方说明），**不以“提取基线绝对值”为准** |
| 文字包围盒偏差 | ≤ 2 pt |
| 文本相似度 | ≥ 0.99（有 ToUnicode 样本；实测 dense 达到 1.0000） |
| 字体解析 | 100% 内嵌字体可被 `TTFParser` 解析（原样嵌入，不要求裁剪 `EBDT/EBLC/vmtx`） |
| 字体来源 | 默认态只用 PDF 内嵌字体（`embedded-verbatim` / `ttc-extracted`）；无内嵌字体走 `path-fallback`；系统字体匹配默认关闭 |
| 回退 | 不支持场景不出现缺字/错位/重复绘制 |
| 输出契约 | 默认即文字对象化；旧实现仅作 A/B 基线，不要求逐字节一致 |

> **基线位置门禁为什么改口径（实测结论）**
>
> P1 阶段实测基线偏差均值 0.850 mm、最大 1.021 mm，曾按“≤0.2 mm”判为未达标。排查后确认这不是实现缺陷：
>
> 1. `TextObject` 已按 `Boundary` 顶端 = `origin − ascender`、`TextCode.Y ≈ ascender` 构造，语义与参考 OFD 一致；
> 2. ofdrw-viewer 按 `baseline = Boundary.y + Y` 定位，受控实验（把 Y 递增 0.1/0.2/0.3 mm）验证文字整体随之平移，公式与实现自洽；
> 3. 剩余差值来自 PDF 行距（1.95 mm）与字形高（约 1.1 mm）不整除时落在行间的剩余量；若继续抬高 Y 让“提取基线”对齐，字形会在行内上移，**与原 PDF 视觉错位，MAE 反而变差**（已实测）。
>
> 因此基线门禁改为衡量“字形在所在行内的像素位置 / 行重心”，衡量的是视觉对齐而非提取坐标绝对值。

### 7.4 ofdrw-viewer-cli 对照验证（本次已做）

#### 7.4.1 现有 OFD 与源 PDF 对照

```bash
pdftoppm -r 96 -png ofd_pdf_match/dense-text-perf.pdf target/probe/render/pdf/dense
ofdrw-viewer-cli target/probe/dense.ofd -o target/probe/render/current -d 96 -f png
ofdrw-viewer-cli ofd_pdf_match/dense-text-perf.ofd -o target/probe/render/reference -d 96 -f png
```

三组图片均为 794×1123。像素对比（RGB 绝对差）：

| 页 | 对比 | MAE | 差值>16 的像素占比 |
|---|---|---:|---:|
| 1 | 当前路径 OFD vs PDF | 12.070 | 25.96% |
| 1 | 参考文字 OFD vs PDF | 12.447 | 26.31% |
| 1 | 当前路径 OFD vs 参考文字 OFD | 7.642 | 15.69% |
| 2 | 当前路径 OFD vs PDF | 11.954 | 25.91% |
| 2 | 参考文字 OFD vs PDF | 11.902 | 25.70% |
| 2 | 当前路径 OFD vs 参考文字 OFD | 4.342 | 8.80% |

人工查看三组图片，版面、字号、行距、断行一致；差异主要来自 PDF 渲染器与 OFD 查看器的抗锯齿/字体 hinting，而不是文字位置。**这证明：不能用“像素差 ≤0.5%”这类绝对值做门禁**，参考文字 OFD 自己也会“失败”。验证应以“文字模式不劣于当前路径/参考 OFD 基线”+ 文字包围盒 + 文本提取为准。

#### 7.4.2 手工 TextObject POC

用参考 OFD 的包结构，替换为裁剪后的 192,644 B SimSun 子集，手工构造 5 个 `TextObject`：

> 注：该 POC 构造于“绝对坐标”假设之下（`Boundary` 取整页、`X/Y` 直接写页面坐标）。此写法 ofdrw-viewer 能渲染，但与参考 OFD 的“相对 Boundary”语义不一致，**不能作为实现依据**；实现以 §5.4.1 更正后的语义为准。

| 对象 | 配置 | 实测页面包围盒（mm） | 结论 |
|---|---|---|---|
| A | `X=10,Y=50,Size=10,DeltaX=10` + `CGTransform` | x 10.05～49.48，y 41.80～50.80 | X/基线/步进正确 |
| B | `CTM="1 0 0 1 10 80"`，`X=Y=0` | x 10.05～49.48，y 71.97～80.70 | CTM 平移正确，与 A 同 x |
| C | 同 A，但不写 `CGTransform` | x 10.05～49.48，y 111.92～120.65 | 重建后的 cmap 可直接定位字形 |
| D | 同 A，红色 + `Alpha=128` | x 10.32～49.21，y 142.08～150.55 | 颜色/Alpha 生效 |
| E | `CTM="0.866025 0.5 -0.5 0.866025 100 150"` | x 100.01～137.06，y 145.26～170.39 | 旋转原点/角度正确 |

POC 文件：`target/probe/textobject-poc.ofd`；渲染结果：`target/probe/render/poc/textobject-poc-p001.png`。

**CGTransform 隔离验证**：把子集字体的 cmap 中 `A/B`、`测/试` 对调，再渲染同一份 OFD：

- 带 `CGTransform` 的对象仍显示 `AB测试`；
- 不带 `CGTransform` 的对象显示 `BA试测`（被 cmap 带偏）。

说明 `ofdrw-viewer` 在存在 `CGTransform` 时优先使用显式 GID。文件：`target/probe/textobject-poc-swap.ofd`。

验证覆盖：

- `Size` 以 mm 为单位，`unitsPerEm=256` 的字体渲染尺寸正确；
- `TextCode X/Y` 是基线原点，`DeltaX` 是相邻字形绝对步进；
- `CTM` 的平移/旋转、`FillColor`、`Alpha` 生效；
- `CGTransform` 指向新 GID 时字形正确，且优先于 cmap；
- 裁掉 `EBDT/EBLC/vmtx` 的 192 KB 子集字体可被查看器正常解析。

**未覆盖**：真实 `PageDrawer.showFontGlyph` 回调、TJ/字距、文字裁剪、透明组、CFF/Type3、多字体混排；这些仍需 P0/P1 接入后验证。

#### 7.4.3 对设计的修正

本次验证修正了三个容易踩坑的点，已写回 5.6.3/5.6.4：

1. `TTFSubsetter.getGIDMap()` 返回 **new → old**，不是 old → new；`CGTransform` 使用前必须反转。
2. 只用 `addGlyphIds` 不会重建 cmap，可能保留 2.8 万条指向旧 GID 的失效映射；必须对每个 Unicode 字符调用 `add(unicode)`，否则只能完全依赖 `CGTransform`。
3. `TTFSubsetter` 会保留 `EBDT/EBLC/vmtx`，完整 SimSun 子集 6.52 MB；裁剪后 192 KB，这一步不能省。

### 7.5 图形化实现 `PDFGraphicsConverter` 验证（§5.11.1 配套）

对并存的图形化实现做独立验证，确认它作为"当作图片"的定位成立，且不影响文字对象化方案。

#### 7.5.1 输出结构（"当作图片"的判定依据）

| 样本 | `TextObject` | `PathObject` | 内嵌字体 | 输出体积 |
|---|---:|---:|---:|---:|
| `helloworld` | **0** | 22 | **0** | 5,270 B |
| `dense-text-perf` | **0** | 29,068 | **0** | **9,058,629 B** |
| `透明度文字` | **0** | 102 | **0** | 26,695 B |

`TextObject` 与内嵌字体均为 0，文字全部展开为路径——与设计定位一致。

#### 7.5.2 与文字对象化方案的对照（`dense-text-perf`，96 dpi）

| 指标 | `PDFConverter`（文字对象化） | `PDFGraphicsConverter`（图形化） |
|:--|--:|--:|
| 输出体积 | 305 KB | **9,058,629 B（9 MB）** |
| `TextObject` 数量 | 8,478 | **0** |
| 内嵌字体 | 有 | **无** |
| MAE（vs 期望 OFD，96 dpi） | 6.99 | **7.64** |
| 墨迹比（转换/期望） | 1.000 | **0.9995** |
| 文本可检索 | 支持 | **不支持** |
| 渲染效果 | 完整清晰 | 完整清晰 |

**结论**：体积约为前者的 **30 倍**，文本不可检索；但视觉一致性（MAE 7.64、墨迹比 0.9995）与文字对象化方案同一量级。体积与可检索性的损失是"文字转路径"的**固有代价**，不做优化。

#### 7.5.3 页面旋转验证

数据集内没有 `/Rotate` 为 90/270 的 PDF，因此**构造**一份 A4 纵向（595.2756 × 841.8898 pt）且 `/Rotate 90` 的 PDF 单独验证：

| 输入 | 输出 OFD `PhysicalBox` | 结论 |
|:--|:--|:--|
| A4 纵向 + `/Rotate 90` | `0 0 297.005 210.003`（mm） | **正确生成横向页**，旧版本缺陷（commit `57c65e32`）未复现 |

实现方式：读取 `page.getRotation()`，为 90/270 时交换 `BBox` 的宽高后再创建 OFD 页面。

#### 7.5.4 与源 PDF 的视觉对照（96 dpi MAE）

| 样本 | MAE |
|:--|--:|
| `helloworld` | 0.06 |
| `multiKeywordInTextCode` | 0.26 |
| `透明度文字` | 0.39 |
| `dense-text-perf`（第 1/2 页） | 7.64 / 4.34 |

密集文字样本的 MAE 偏高，来自纯路径渲染与期望 OFD（文字对象）在抗锯齿/字体 hinting 上的差异，与 §7.4.1 记录的"参考文字 OFD 自身也存在 25.70% 像素差"同源，不作为门禁。

#### 7.5.5 回归隔离

- `PDFGraphicsConverter` **未改动** `PDFConverter`、`OfdPageDrawer`、`TextRunBuilder`、`TextObjectPdfRenderer`、`OFDPageGraphics2D` 的任何代码（以文件修改时间与 diff 核实）；除新增类与文档外无其它改动。
- 单元测试 `PDFGraphicsConverterTest` 6 个用例覆盖：产物无 `TextObject`/无字体、页面尺寸、`/Rotate 90` 横向页、指定页码（乱序/重复）、越界页码抛异常、图形化产物无 `TextObject`。

---

## 8. 实施计划（本次不执行）

> 以下为建议拆分，便于后续排期；人日为粗估，需 PoC 后校正。

| 阶段 | 内容 | 产出 | 粗估 |
|---|---|---|---|
| **P0 技术验证** | 基础 POC 已完成（手工 TextObject + 子集字体 + viewer 渲染）；剩余：继承 `PageDrawer` 拦截 `showFontGlyph`，把真实 PDF 的一页接到同一套 TextObject 构造逻辑 | PoC 分支 + 截图 + 体积数据 | 1～2 人日 |
| **P1 主干** | TrueType/CIDFontType2 水平文字；TextCode/Delta/CTM；字体资源原样嵌入（GID 不变）；颜色/Alpha；按 Tj flush；回退路径 | 可转换纯文字 PDF；体积与现有内嵌字体同一量级 | 4～6 人日 |
| **P2 复杂排版** | TJ/字距、旋转/镜像/剪切、描边、文字裁剪回退、透明组检测、Clip 映射、页旋转 | 覆盖 58 组样本的 80% | 4～6 人日 |
| **P3 字体扩展** | CFF/OTF 原样嵌入或包装；非内嵌字体匹配；Type3/不可嵌入的 `CompositeObject` 复用；成本决策 | 覆盖率与体积进一步优化 | 4～6 人日 |
| **P4 优化与回归** | 字体/图片去重、精度、局部路径、查看器矩阵、58 组回归、文档 | 验收报告 + 文档 | 3～5 人日 |

**决策门：**

- P0 后确认：OFD 查看器是否支持内嵌子集 TTF、`unitsPerEm=256`、CGTransform；若否，方案需调整（CFF/系统字体/CompositeObject）。
- P1 后确认：dense 样本体积与视觉是否达标；若字体子集仍大，评估“按字体成本回退”。
- P2 后确认：复杂排版回退比例是否可接受；若回退过多，优先补 CFF/Clip。

---

## 9. 待确认问题

1. **非内嵌字体的策略**：优先“系统/配置字体精确匹配 + 子集嵌入”（体积小、可能字形略有差异），还是“一律回退路径”（视觉最稳、体积不改善）？
2. **CFF/OTF 支持范围**：首版是否必须覆盖 `intro-数科` 这类 CFF 字体？还是允许回退路径，后续再做？
3. ~~默认模式与 `PATH_ONLY` 保留~~ —— 已确认：**不需要兼容旧实现**，默认即文字对象化，不保留模式开关。
4. **字体授权**：是否允许嵌入系统字体（如 SimSun）的子集？是否需要只使用可再分发的开源字体（Noto/思源）做替代？
5. **文本可搜索性**：无 ToUnicode 的 PDF 是否接受占位符（视觉正确但不可搜索），还是必须回退路径/OCR？
6. **目标查看器**：验收以 ofdrw-viewer 为准，还是必须覆盖 Foxit/WPS/数科/浏览器？
7. **体积目标**：纯文字文档是否以“≤ 当前 1/10”为硬指标？还是以“接近参考 OFD”为目标？

---

## 10. 参考

### 10.1 标准

- GB/T 33190-2016《电子文件存储与交换格式 版式文档》
  - 11.1 字型（表 44，`FontFile` 要求 OpenType）
  - 11.2 文字对象（表 45）
  - 11.3 文字定位（表 46，X/Y/DeltaX/DeltaY）
  - 11.4 字形变换（表 48，`CGTransform`）
  - 13 复合对象（`CT_VectorG` / `CompositeObject`）
- 仓库内全文：`src_material/GBT33190-2016/full.md`

### 10.2 代码

- [`PDFConverter.java`](../src/main/java/org/ofdrw/converter/ofdconverter/PDFConverter.java#L179-L180)
- [`OFDPageGraphics2D.java`](../../ofdrw-graphics2d/src/main/java/org/ofdrw/graphics2d/OFDPageGraphics2D.java#L274-L277)（`drawGlyphVector`）
- [`OFDPageGraphics2D.java`](../../ofdrw-graphics2d/src/main/java/org/ofdrw/graphics2d/OFDPageGraphics2D.java#L547-L577)（`fill(Shape)`）
- `org.apache.pdfbox.rendering.PageDrawer#showFontGlyph`
- `org.apache.pdfbox.contentstream.PDFStreamEngine#showGlyph`
- `org.apache.fontbox.ttf.TTFSubsetter`
- `org.ofdrw.converter.font.EmbeddedFontData` / `GlyphIndexProvider`
- `org.ofdrw.core.text.text.CT_Text`、`org.ofdrw.core.text.TextCode`、`org.ofdrw.core.text.CT_CGTransform`

### 10.3 外部资料

- libofd（C++ OFD 库，含 pdf2ofd）：<https://github.com/uukuguy/libofd>
- java-ys/PDF2OFD-1（Java，PDFBox `showGlyph` → OFD TextObject）：<https://github.com/java-ys/PDF2OFD-1>
- wanglrebe/pdf2ofd（Python，SVG 路径方案，可作为“纯路径”体积对照）：<https://github.com/wanglrebe/pdf2ofd>
- 福昕 OFD 优化文档（资源去重、精度、图片重编码）：<https://devdocs.fuxinsoft.cn/development-guide/pdf-ofd-sdk-desktop-server/ofd/optimize.html>
- Apache PDFBox 3.0.8（`PageDrawer` / `TTFSubsetter`）：<https://pdfbox.apache.org/>

---

## 11. 附：本次测量产物

> 以下为本地探针输出，位于 `target/probe/`，未纳入版本库；仅用于复现本文数据。

| 文件 | 说明 |
|---|---|
| `target/probe/dense.ofd` | 当前 `PDFConverter` 转换 `dense-text-perf.pdf` 的结果（9.06 MB） |
| `target/probe/test.ofd` | 当前 `PDFConverter` 转换 `Test.pdf` 的结果（2.22 MB） |
| `target/probe/keyword.ofd` | 当前 `PDFConverter` 转换 `keyword.pdf` 的结果（731 KB） |
| `target/probe/simsun_subset_full_pruned.ttf` | 637 字形 SimSun 子集（已重建 cmap）裁剪冗余表后的结果（192,644 B） |
| `target/probe/textobject-poc.ofd` | 手工构造的 TextObject 验证文件（5 个文字对象） |
| `target/probe/textobject-poc-swap.ofd` | 交换 cmap 后验证 CGTransform 优先级的文件 |
| `ofd_pdf_match/dense-text-perf.ofd` | 同内容的文字对象版本（213 KB，参考） |