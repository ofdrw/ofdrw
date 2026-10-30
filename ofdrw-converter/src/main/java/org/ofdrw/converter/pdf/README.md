# org.ofdrw.converter.pdf 包骨架与契约（P0）

> 依据：`ofdrw-converter/doc/PDF转换重构设计.md` 第 4 章（目标架构）与第 9 章已定决策。
> 本包是**内部实现细节**，不对外承诺稳定性；对外唯一新 API 是 `org.ofdrw.converter.export.PDFExporter`。

## 1. 包划分与职责

| 包 | 职责 | 所有者阶段 |
|---|---|---|
| `model/` | 与库无关的中间模型（页内容 + 路径/文本/图像/渐变/底纹/裁剪指令对象） | P0（本任务） |
| `render/` | 渲染管线：页框、模板页、图层 ZOrder、绘制参数、路径、文字、图像、渐变、底纹、裁剪 | P1~P4 |
| `font/` | 字体选择与 PDF 对象映射（解析/度量/嵌入数据来自 `ofdrw-font`） | P2 |
| `structure/` | 大纲、附件、注释、动作、元数据、签名外观 | P5 |
| `CoordSystem` | **唯一**坐标换算入口（mm→pt、Y 翻转、对象空间→页面坐标） | P0（本任务） |

## 2. 强制约定

1. **渲染器无关**：`model/`、`render/`、`font/`、`structure/` 中**禁止**出现 `org.apache.pdfbox.*`、`org.apache.fontbox.*` 类型；
   PDFBox 只允许出现在写入器实现中。P0 起有静态检查（见第 6 节）兜底。
2. **坐标换算集中**：任何代码都不得再写 `×72/25.4` 或手写 Y 翻转，必须调用 `CoordSystem`。
3. **逐点换算（决策 1）**：中间模型指令默认使用 `CoordSpace.PAGE`（已换算为 PDF 页面坐标，单位 pt，Y 轴向上），
   页面 CTM 保持恒等；只有尚未烘焙几何变换的调试/中间结果才用 `CoordSpace.OBJECT`。
4. **q/Q 成对（设计约束 1）**：`PdfPageContent` 提供 `saveState()`/`restoreState()` 与 `isBalanced()`，写入器可断言。
5. **CMYK 不转 RGB（设计 3.4 节）**：`PdfColor` 保留原颜色空间；`ImageEncoding.JPEG.isLossy()` 为 true 表示原样嵌入。
6. **Alpha 缺省不透明（决策 2）**：`SetAlpha.fromOfd(null)` 等价于 1.0。
7. **兜底不中断**：渲染器对单图元异常/未实现特性跳过并打 warning 日志；渲染器接口不声明受检异常。

## 3. 中间模型速查

| 类型 | 对应 PDF | 说明 |
|---|---|---|
| `SaveState` / `RestoreState` | `q` / `Q` | 图形状态成对 |
| `ConcatMatrix` | `cm` | 显式 CTM 叠加（禁止用作页面级 Y 翻转） |
| `StrokeStyle` | `w`/`J`/`j`/`M`/`d` | 有效绘制参数一次性写入 |
| `SetAlpha` | `ca`/`CA` | 颜色 Alpha × 图元 Alpha |
| `PathCommand` | `m/l/c/h` + `f`/`f*`/`S`/`B`/`B*` | 填充与描边合一，避免重复绘制 |
| `MoveTo`/`LineTo`/`CurveTo`/`ClosePath` | `m`/`l`/`c`/`h` | 圆弧 `A` 先由 `ArcConverter` 转为 `CurveTo` |
| `ClipCommand` | `W`/`W*` + `n` | 多 Clip 取交集；同一 Clip 多 Area 取并集（`isUnionArea`） |
| `TextCommand` + `GlyphRun` | `BT`/`Tf`/`Tm`/`TJ` | 字形优先（GID）+ Unicode（ToUnicode），决策 3 |
| `ImageCommand` | `cm` + `Do` | `getPlacementMatrix()` 即放置 `cm` |
| `ImageBorderCommand` | 额外路径 | Border 不能依赖图像 XObject |
| `ShadingCommand` | `sh` | 轴向/径向，含 Extend、停止点 |
| `PatternCommand` | Tiling Pattern | 决策 17：`isApproximate()` 标记近似 |
| `FormCommand` | `/XObject` + `Do` | 模板页/复合对象复用，放置矩阵显式给出 |
| `RectCommand` | `m/l/l/l/h` + `f` | 简单实心矩形快捷指令 |
| `MarkerCommand` | 注释行 | 图元 ID 标记，便于测试定位与日志回溯 |

`PdfContentWriter.dump(PdfPageContent)`（包级可见）输出稳定的伪操作符文本，测试通过它断言绘制顺序与参数。

## 4. 坐标换算 API（`CoordSystem`）

| 方法 | 用途 |
|---|---|
| `mmToPt(double)` / `ptToMm(double)` | 长度换算（`MM_TO_PT = 72/25.4`） |
| `yToPt(yMm, pageHMm)` / `yFromPt(...)` | 单点 Y 翻转 |
| `pointToPage(xMm, yMm, pageHMm)` | 页面坐标点 mm→pt + 翻转 |
| `boxToPage(Box, pageHMm)` | OFD 矩形 → PDF 页面矩形（左下角语义） |
| `pageBoxToPt(x0, y0, w, h)` | 决策 26：`MediaBox = CropBox = PhysicalBox` |
| `objectToPage(localBox, ctm[, pageX0, pageY0, pageH])` | 对象空间 (mm) → PDF 页面坐标 (pt) 的完整矩阵 |
| `objectPointToPage(...)` | 单点便捷换算 |
| `fromCtm(double[])` | OFD CTM 数组 → `AffineMatrix` |

`objectToPage` 内部顺序固定为：**先按外接矩形平移 → 再叠加 CTM → 最后 mm→pt 与 Y 翻转**（设计 3.2 节）。

## 5. 与其他阶段的接口契约

- **P1（图形/裁剪）**：实现 `PathRenderer`、`ClipApplier` 与 `ArcConverter.toCubicSegments`；
  输出 `PathCommand`/`ClipCommand`/`StrokeStyle`，坐标必须为 `CoordSystem.objectToPage(...)` 变换后的页面坐标。
- **P2（文字/字体）**：实现 `TextRenderer` 与 `font.FontSelector`；字体引用通过 `PdfFontRef` 暴露，
  替换字体时 `isSubstituted()=true` 并打 warning；`TextCommand` 使用 `GlyphRun(gid, unicode, x, y, advancePt)`。
- **P3（颜色/渐变/底纹）**：实现 `ShadingRenderer`、`PatternRenderer`；`PdfColor` 保留原颜色空间。
- **P4（图像/版式）**：实现 `ImageRenderer`、模板页与图层调度（`PageRenderer` 的具体实现）。
- **P5（结构）**：实现 `OutlineMapper`/`AttachmentMapper`/`AnnotationMapper`/`MetadataMapper`，
  在 `PDFExporter#close()` 阶段写入文档级信息。
- **写入器（P0 后续）**：把 `PdfPageContent` 指令序列写成 PDFBox 3 内容流；测试可用 `PdfContentWriter` 做替代。

## 6. 验证方式

P0 阶段的验证（因本地 Maven 离线仓缺少 PDFBox 3.0.8，`mvn -o` 无法解析依赖）：

1. 用 JDK 编译器 API（`javax.tools`）对 `org/ofdrw/converter/pdf` 主代码与测试代码做完整编译，`-Xlint:all` 无警告；
2. 直接用反射调用 JUnit 5 `@Test` 方法执行单测（不依赖 Maven/surefire）；
3. 静态检查：`pdf` 包内不存在 `org.apache.pdfbox` / `org.apache.fontbox` 引用与 `PD*` 类型。

依赖就位后应回归 `mvn -o -pl ofdrw-full -am test-compile` 与 `mvn -o test -DskipTests=false`。
