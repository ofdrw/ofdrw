# ofdrw-archive — OFD-A 档案模块

依据 **GB/T 42133-2022《信息技术 OFD 档案应用指南》** 实现 OFD 文件的合规检查和去技术化转换。

## 概述

OFD-A 是 GB/T 33190 OFD 标准的约束性子集，用于电子文件长期保存。核心操作叫"去技术化"：移除未来可能无法读取的技术特性，保证文档格式开放、自包含、持续可解释。

本模块提供两大功能：
- **检查器** — 验证 OFD 文件是否符合 OFD-A 标准
- **转换器** — 将普通 OFD 文件转换为 OFD-A 格式

## 快速开始

### 检查 OFD 文件

```java
OFDArchiveChecker checker = new OFDArchiveChecker();
try (OFDReader reader = new OFDReader(Paths.get("doc.ofd"))) {
    List<ArchiveViolation> violations = checker.check(reader);
    for (ArchiveViolation v : violations) {
        System.out.println(v);
    }
}
```

### 转换为 OFD-A

```java
OFDArchiveConverter converter = new OFDArchiveConverter();
converter.convert(Paths.get("input.ofd"), Paths.get("output.ofd"));
```

### 转换加密的 OFD 文件

```java
OFDArchiveConverter converter = new OFDArchiveConverter();
converter.addDecryptor(new UserPasswordDecryptor("admin", "password123"));
converter.convert(Paths.get("encrypted.ofd"), Paths.get("output.ofd"));
```

## GB/T 42133-2022 约束覆盖表

### 6.1 文件结构

| 条款 | 约束内容 | 方式 | 检查 | 转换 | 实现类 |
|------|---------|------|:--:|:--:|--------|
| 6.1 | 显示/打印/交互所需资源均在包内（无外部依赖） | 检查+转换 | ✅ | ✅ | `ExternalResourceRule` / `ExternalResourceHandler` |
| 6.1 | 去除与主入口及嵌套引出文件无关的文件（防夹带） | 自动 | — | ✅ | Converter 白名单复制机制 |

### 6.2.1 主入口 (OFD.xml)

| 条款 | 约束内容 | 方式 | 检查 | 转换 | 实现类 |
|------|---------|------|:--:|:--:|--------|
| 6.2.1a | DocType 属性取值为 "OFD-A" | 检查+转换 | ✅ | ✅ | `DocTypeRule` / `DocTypeHandler` |
| 6.2.1b | 元数据以自定义元数据存储，名称按 DA/T 46、DA/T 54 | 建议 | — | — | 由生成软件保证 |
| 6.2.1c | 不使用多文档机制（或多文档拆解/合并） | 检查+转换 | ✅ | ✅ | `SingleDocRule` / `SingleDocHandler` |
| 6.2.1d | 保留多版本(Versions)信息 | 建议 | — | — | 不处理（标准要求保留） |
| 6.2.1e | 去除任何与主入口无关的文件 | 自动 | — | ✅ | Converter 白名单复制机制 |

### 6.2.2 文档根节点 (Document.xml)

| 条款 | 约束内容 | 方式 | 检查 | 转换 | 实现类 |
|------|---------|------|:--:|:--:|--------|
| 6.2.2a | 去除权限声明(Permissions) | 检查+转换 | ✅ | ✅ | `PermissionRule` / `PermissionHandler` |
| 6.2.2b | 去除视图首选项(VPreferences) | 转换 | — | ✅ | `VPrefsHandler` |
| 6.2.2c | 去除非 Goto 的文档动作 | 检查+转换 | ✅ | ✅ | `NonGotoActionRule` / `NonGotoActionHandler` |
| 6.2.2d | 不予保留的附件记录摘要 | 转换 | — | ✅ | `AttachmentHandler` |
| 6.2.2e | 去除扩展信息(Extensions)；影响输出的固化到页面内容 | 检查+转换 | ✅ | ✅ | `ExtensionRule` / `ExtensionHandler` |

### 6.2.3 页树

| 条款 | 约束内容 | 方式 | 检查 | 转换 | 实现类 |
|------|---------|------|:--:|:--:|--------|
| 6.2.3a | 常用 PageArea 设为文档默认，在 CommonData 中描述 | 建议 | — | — | 由生成软件保证 |
| 6.2.3b | 页面设置同默认时省略 PageArea 节点 | 建议 | — | — | 由生成软件保证 |
| 6.2.3c | 去除非 Goto 的页面动作 | 检查+转换 | ✅ | ✅ | `NonGotoActionRule` / `NonGotoActionHandler` |
| 6.2.3d | 同名图层(Layer)重命名使其可区分 | 检查+转换 | ✅ | ✅ | `LayerNameHandler` |
| 6.2.3e | 去除 PageBlock 嵌套，不可避免时不超过 3 层 | 检查+转换 | ✅ | ✅ | `PageBlockDepthRule` / `PageBlockFlattenHandler` |

### 6.2.4 页对象

| 条款 | 约束内容 | 方式 | 检查 | 转换 | 实现类 |
|------|---------|------|:--:|:--:|--------|
| 6.2.4a | 主要内容安排在正文层(Body)和背景层(Background) | 建议 | — | — | 由生成软件保证 |
| 6.2.4b | 扫描图像安排在背景层，识别文字在正文层 | 建议 | — | — | 由生成软件保证 |
| 6.2.4c | 页面内容流按语义顺序组织 | 建议 | — | — | 由生成软件保证 |

### 6.2.5 大纲

| 条款 | 约束内容 | 方式 | 检查 | 转换 | 实现类 |
|------|---------|------|:--:|:--:|--------|
| 6.2.5a | 去除大纲节点中非 Goto 的动作 | 检查+转换 | ✅ | ✅ | `OutlineActionRule` / `OutlineActionHandler` |
| 6.2.5b | Goto 目标书签/页面不存在则去除该动作 | 检查+转换 | ✅ | ✅ | `OutlineActionRule` / `OutlineActionHandler` |

### 6.2.6 资源

| 条款 | 约束内容 | 方式 | 检查 | 转换 | 实现类 |
|------|---------|------|:--:|:--:|--------|
| 6.2.6a | ColorSpace/Font 在 PublicRes，MultiMedia/VectorG/DrawParam 在 DocumentRes | 检查+转换 | ✅ | ✅ | `ResourcePlacementRule` / `ResourcePlacementHandler` |
| 6.2.6b | 页面使用的字型嵌入子集化数据 | 检查 | ✅ | — | `FontSubsetRule` |
| 6.2.6c | 子集化保留 cmap/loca/glyf/head/hhea/hmtx/maxp/name/OS2/post 等表 | 检查 | ✅ | — | 合并于 FontSubsetRule (Phase 2+) |
| 6.2.6d | 字型字符集与 GB 18030 一致，与 GB/T 13000 建立映射 | 建议 | — | — | 由生成软件保证 |
| 6.2.6e | 栅格图像限于 BMP/JPEG/PNG/JBIG2/JPEG2000/TIFF | 检查+转换 | ✅ | ✅ | `ImageFormatRule` / `ImageConvertHandler` |
| 6.2.6f | 栅格图像不使用扩展机制加入自定义数据 | 检查+转换 | ✅ | ✅ | `ImageExtensionRule` / `ImageExtensionHandler` |
| 6.2.6g | 去除音频视频资源，保留摘要 | 检查+转换 | ✅ | ✅ | `AudioVideoRule` / `AudioVideoHandler` |

### 6.3.1 颜色

| 条款 | 约束内容 | 方式 | 检查 | 转换 | 实现类 |
|------|---------|------|:--:|:--:|--------|
| 6.3.1a | 可定义默认颜色空间(DefaultCS) | 建议 | — | — | 由生成软件保证 |
| 6.3.1b | 颜色空间类型限于 Gray/GRAY/RGB/CMYK | 检查 | ✅ | — | `ColorSpaceRule` |
| 6.3.1c | 颜色空间宜带有颜色配置文件(Color Profile) | 检查(INFO) | ✅ | — | `ColorProfileRule` |

### 6.3.2 裁剪区

| 条款 | 约束内容 | 方式 | 检查 | 转换 | 实现类 |
|------|---------|------|:--:|:--:|--------|
| 6.3.2a | 裁剪区包含图元外接矩形则去除 | 检查+转换 | ✅ | ✅ | `ClipAreaRule` / `ClipAreaHandler` |
| 6.3.2b | 裁剪区面积=0，去除并设 Visible=false | 检查+转换 | ✅ | ✅ | `ClipAreaRule` / `ClipAreaHandler` |

### 6.3.3 页面对象

| 条款 | 约束内容 | 方式 | 检查 | 转换 | 实现类 |
|------|---------|------|:--:|:--:|--------|
| 6.3.3a | 图元外接矩形以文字可见部分最小矩形为基础 | 建议 | — | — | 由生成软件保证 |
| 6.3.3b | 去除页面图元对象动作；Goto→链接注释 | 检查+转换 | ✅ | ✅ | `NonGotoActionRule` / `NonGotoActionHandler` |
| 6.3.3c | Text/Path Stroke=false 时去除 LineWidth/Cap/Join/Dash 等修饰属性 | 转换 | — | ✅ | `CleanStrokeAttrHandler` |
| 6.3.3d | 复合对象去除 Stroke/Fill/LineWidth/Cap/Join/Dash/StrokeColor/FillColor | 转换 | — | ✅ | `CleanStrokeAttrHandler` + `CleanFillAttrHandler` |
| 6.3.3e | 图像/视频对象去除 Fill/FillColor | 转换 | — | ✅ | `CleanFillAttrHandler` |

### 6.4 图形 (PathObject)

| 条款 | 约束内容 | 方式 | 检查 | 转换 | 实现类 |
|------|---------|------|:--:|:--:|--------|
| 6.4a | 重复的 StrokeColor/FillColor 注册为 DrawParam | 建议 | — | — | Phase 2+ |
| 6.4b | 重复的 LineWidth/Cap/Join/Dash 等注册为 DrawParam | 建议 | — | — | Phase 2+ |

### 6.5 图像 (ImageObject)

| 条款 | 约束内容 | 方式 | 检查 | 转换 | 实现类 |
|------|---------|------|:--:|:--:|--------|
| 6.5a | 多页共用的栅格图像注册到 DocumentRes，单页共用到 PageRes | 检查+转换 | ✅ | ✅ | `ImageResourceRegRule` / `ImageResourceRegHandler` |
| 6.5b | 图像 Interpolate 属性设为 false | 检查+转换 | ✅ | ✅ | `ImageInterpolateRule` / `ImageInterpolateHandler` |

### 6.6 文字 (TextObject)

| 条款 | 约束内容 | 方式 | 检查 | 转换 | 实现类 |
|------|---------|------|:--:|:--:|--------|
| 6.6a | 同行相邻字体相同用单个 TextObject，多行用多个 | 建议 | — | — | 由生成软件保证 |
| 6.6b | 超出自带字型范围的字符代码拆分为多个 TextObject | 建议 | — | — | 由生成软件保证 |
| 6.6c | 文字大小使用 Size 属性标识 | 检查 | ✅ | — | `TextSizeRule` |
| 6.6d | 仅横向缩放时用 HScale 属性 | 检查 | ✅ | — | `TextHScaleRule` |
| 6.6e | 重复的勾边/填充颜色注册为 DrawParam | 建议 | — | — | Phase 2+ |

### 6.7 视频

| 条款 | 约束内容 | 方式 | 检查 | 转换 | 实现类 |
|------|---------|------|:--:|:--:|--------|
| 6.7 | 视频对象按有视频动作的图像对象处理 | 检查+转换 | ✅ | ✅ | 合并于 `AudioVideoRule` / `AudioVideoHandler` |

### 6.8 复合对象 (CompositeObject)

| 条款 | 约束内容 | 方式 | 检查 | 转换 | 实现类 |
|------|---------|------|:--:|:--:|--------|
| 6.8 | 多页共用矢量图像注册到 DocumentRes，单页共用到 PageRes | 建议 | — | — | Phase 2+ |

### 6.9 动作 (Action)

| 条款 | 约束内容 | 方式 | 检查 | 转换 | 实现类 |
|------|---------|------|:--:|:--:|--------|
| 6.9a | 不保留依赖外部资源的动作 | 检查+转换 | ✅ | ✅ | 合并于 `ExternalResourceRule` / `NonGotoActionHandler` |
| 6.9b | 去除动作时用自定义参数保留摘要 | 建议 | — | — | Phase 2+ |

### 6.10.1 归档前的注释

| 条款 | 约束内容 | 方式 | 检查 | 转换 | 实现类 |
|------|---------|------|:--:|:--:|--------|
| 6.10.1a1 | 去除影响注释外观显示的参数 | 转换 | — | — | 用户要求跳过（不固化外观） |
| 6.10.1a2 | 去除注释外观的 PageBlock 嵌套 | 检查+转换 | ✅ | ✅ | `AnnotationRule` / `AnnotationHandler` |
| 6.10.1a3 | ReadOnly/NoZoom/NoRotate 设为 true | 检查+转换 | ✅ | ✅ | `AnnotationRule` / `AnnotationHandler` |
| 6.10.1b | 注释外观合并到页面内容（备选方案） | 未实现 | — | — | 与 6.10.1a 二选一，默认选 a) |

### 6.10.2 归档后的注释

| 条款 | 约束内容 | 方式 | 检查 | 转换 | 实现类 |
|------|---------|------|:--:|:--:|--------|
| 6.10.2 | 归档章/页码等注释与原注释分文件存储 | 建议 | — | — | 由生成软件保证 |

### 6.11 自定义标引

| 条款 | 约束内容 | 方式 | 检查 | 转换 | 实现类 |
|------|---------|------|:--:|:--:|--------|
| 6.11 | 保留自定义标引，内容包含在包内不依赖外部 | 检查 | ✅ | — | Phase 2+ |

### 6.12 扩展信息

| 条款 | 约束内容 | 方式 | 检查 | 转换 | 实现类 |
|------|---------|------|:--:|:--:|--------|
| 6.12a | 去除扩展信息 | 检查+转换 | ✅ | ✅ | 合并于 `ExtensionRule` / `ExtensionHandler` |
| 6.12b | 影响显示/打印的扩展固化到页面内容 | 转换 | — | — | 用户要求跳过（直接删除） |

### 6.13.1 签名数据格式

| 条款 | 约束内容 | 方式 | 检查 | 转换 | 实现类 |
|------|---------|------|:--:|:--:|--------|
| 6.13.1a | Sign 按 GB/T 35275 组织+CA 证书→可不去技术化 | 检查 | ✅ | — | `SignatureRule` |
| 6.13.1b | Seal 按 GB/T 38540 组织+印章备案+CA 证书→可不去技术化 | 检查 | ✅ | — | `SignatureRule` |
| 6.13.1c | CA 证书状态查询/吊销列表可持续性被认可 | 检查 | ✅ | — | `SignatureRule` |

### 6.13.2 签名的去技术化 (Type=Sign)

| 条款 | 约束内容 | 方式 | 检查 | 转换 | 实现类 |
|------|---------|------|:--:|:--:|--------|
| 6.13.2a1 | 注释自定义参数增加签名人/时间/文档摘要值 | 转换 | — | — | `SignatureHandler` (框架) |
| 6.13.2a2 | 去除注释 SignRef 属性 | 转换 | — | — | `SignatureHandler` (框架) |
| 6.13.2b | 不保留注释时外观→页面内容 | 转换 | — | — | 用户跳过 |
| 6.13.2c | 从 Signatures.xml 移除签名记录+对应文件夹 | 转换 | — | ✅ | `SignatureHandler` |

### 6.13.3 签章的去技术化 (Type=Seal)

| 条款 | 约束内容 | 方式 | 检查 | 转换 | 实现类 |
|------|---------|------|:--:|:--:|--------|
| 6.13.3a1 | 外观→注释，自定义参数保留签章人/证书/时间/摘要 | 转换 | — | — | `SignatureHandler` (框架) |
| 6.13.3a2 | 外观→页面内容，BlendMode="Darken" | 转换 | — | — | 备选方案，默认选 6.13.3a1 |
| 6.13.3b | 从 Signatures.xml 移除签章记录+对应文件夹 | 转换 | — | ✅ | `SignatureHandler` |

### 6.14 版本

| 条款 | 约束内容 | 方式 | 检查 | 转换 | 实现类 |
|------|---------|------|:--:|:--:|--------|
| 6.14 | 保留多版本信息 | 建议 | — | — | 不处理（标准要求保留） |

### 6.15.1 附件保留

| 条款 | 约束内容 | 方式 | 检查 | 转换 | 实现类 |
|------|---------|------|:--:|:--:|--------|
| 6.15.1a | 保留 TXT/XML 等文本格式附件 | 检查+转换 | ✅ | ✅ | `AttachmentRule` / `AttachmentHandler` |
| 6.15.1b | 保留有公开发布标准说明且已归档的技术文档附件 | 建议 | — | — | 由生成软件保证 |
| 6.15.1c | 保留含按国标组织的数字签名的附件 | 建议 | — | — | 由生成软件保证 |

### 6.15.2 附件去除

| 条款 | 约束内容 | 方式 | 检查 | 转换 | 实现类 |
|------|---------|------|:--:|:--:|--------|
| 6.15.2a | 可转版式→转为 OFD 插入主文档后（附录A） | 未实现 | — | — | Phase 3+ |
| 6.15.2b1 | 音视频附件移出，保留摘要 | 检查+转换 | ✅ | ✅ | `AttachmentRule` / `AttachmentHandler` |
| 6.15.2b2 | 超大图像附件移出，保留摘要 | 未实现 | — | — | Phase 3+ |
| 6.15.2b3 | 电子表格附件移出，保留摘要 | 未实现 | — | — | Phase 3+ |
| 6.15.2b4 | 网页/演示文稿(含动画3D)附件移出，保留摘要 | 未实现 | — | — | Phase 3+ |

### 6.16 加密和解密

| 条款 | 约束内容 | 方式 | 检查 | 转换 | 实现类 |
|------|---------|------|:--:|:--:|--------|
| 6.16 | 不使用任何加密选项，归档前解密为明文 | 检查+转换(门禁) | ✅ | ✅ | `EncryptionRule` / `EncryptionHandler` |

### 7.1 生成软件

| 条款 | 约束内容 | 方式 | 检查 | 转换 | 实现类 |
|------|---------|------|:--:|:--:|--------|
| 7.1 | 具有将文件转换/生成为 OFD 及处理为适于长期保存的功能 | — | — | — | 本模块即为此功能 |

---

## 架构

采用规则引擎模式，检查规则和转换处理器分离：

```
ofdrw-archive/
├── check/                    # 检查器（只读）
│   ├── ArchiveRule           # @FunctionalInterface
│   ├── rule/                 # 规则实现（XxxRule）
│   └── OFDArchiveChecker     # 入口，聚合 28 条规则
├── convert/                  # 转换器（就地修改）
│   ├── ArchiveHandler        # @FunctionalInterface
│   ├── handler/              # 处理器实现（XxxHandler）
│   └── OFDArchiveConverter   # 入口，编排 20 步管道
└── model/
    └── ArchiveViolation      # 违规描述
```

### 处理器编排顺序

```
 0. EncryptionHandler          ← 门禁（最先，失败中断）
 1. SingleDocHandler           ← 结构变更
 2. DocTypeHandler
 3. ExternalResourceHandler
 4. PermissionHandler
 5. VPrefsHandler
 6. NonGotoActionHandler
 7. OutlineActionHandler
 8. ExtensionHandler
 9. AudioVideoHandler
10. ImageExtensionHandler
11. ResourcePlacementHandler
12. ImageResourceRegHandler
13. PageBlockFlattenHandler
14. ClipAreaHandler
15. ImageConvertHandler
16. LayerNameHandler
17. ImageInterpolateHandler
18. CleanStrokeAttrHandler
19. CleanFillAttrHandler
20. AnnotationHandler
21. SignatureHandler
22. AttachmentHandler
```

## 依赖模块

```
ofdrw-archive
├── ofdrw-crypto    (OFDDecryptor — 解密能力)
├── ofdrw-reader     (OFDReader, ResourceLocator, ResourceManage)
│   └── ofdrw-pkg    (OFDDir, DocDir, PageDir)
│       └── ofdrw-core (OFD, Document, 全部数据模型)
└── JUnit 5 / ofdrw-layout (test)
```

## 测试

```bash
# 编译
mvn compile -pl ofdrw-core,ofdrw-pkg,ofdrw-reader,ofdrw-crypto,ofdrw-archive -DskipTests=true

# ofdrw-crypto 测试（含解密）
mvn test -pl ofdrw-crypto -DskipTests=false

# ofdrw-archive 测试
mvn test -pl ofdrw-archive -DskipTests=false
```

## 参考资料

- GB/T 42133-2022 信息技术 OFD 档案应用指南
- GB/T 33190-2016 电子文件存储与交换格式 版式文档
- GM/T 0099-2020 OFD 密码应用技术规范
