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
// 获取门禁处理器并配置解密密钥
EncryptionHandler encHandler = new EncryptionHandler();
encHandler.addDecryptor(new UserPasswordDecryptor("admin", "password123"));

converter = new OFDArchiveConverter(Arrays.asList(
    encHandler,
    new SingleDocHandler(),
    new DocTypeHandler(),
    // ... 其他处理器
));
converter.convert(Paths.get("encrypted.ofd"), Paths.get("output.ofd"));
```

## 架构

采用规则引擎模式，检查规则和转换处理器分离：

```
ofdrw-archive/
├── check/                    # 检查器（只读）
│   ├── ArchiveRule           # @FunctionalInterface — 单条规则
│   ├── rule/                 # 规则实现（XxxRule）
│   └── OFDArchiveChecker     # 入口，聚合规则
├── convert/                  # 转换器（就地修改）
│   ├── ArchiveHandler        # @FunctionalInterface — 单条转换
│   ├── handler/              # 处理器实现（XxxHandler）
│   └── OFDArchiveConverter   # 入口，编排管道
└── model/
    └── ArchiveViolation      # 违规描述（规则名/严重度/位置/实际值/期望值）
```

### 转换流程

```
convert(srcPath, dstPath):
    tempDir/
    ├── src/     ← 解压源 OFD
    └── output/  ← 打包目标

    1. 解压 → src/
    2. 按顺序执行 handler.handle()（就地修改 src/）
    3. DFS 收集可达文件白名单 → 复制到 output/（排除非法夹带）
    4. 打包 output/ → OFD-A 文件
```

### 处理器编排顺序

```
0. EncryptionHandler   ← 门禁，最先执行，失败则中断
1. SingleDocHandler    ← 结构变更
2. DocTypeHandler
3. ExternalResourceHandler
4. PermissionHandler
5. VPrefsHandler
6. NonGotoActionHandler
7. OutlineActionHandler
8. ExtensionHandler
9. AudioVideoHandler
10. ImageExtensionHandler
11. PageBlockFlattenHandler
12. LayerNameHandler
13. ImageInterpolateHandler
14. CleanStrokeAttrHandler
15. CleanFillAttrHandler
16. AnnotationHandler
```

## 规则清单

| # | 规则 | 标准条款 | 检查 | 转换 | 说明 |
|---|------|---------|------|------|------|
| 1 | DOC_TYPE | 6.2.1a | ✅ ERROR | ✅ | DocType="OFD-A" |
| 2 | SINGLE_DOC | 6.2.1c | ✅ ERROR | ✅ | 只保留第一个 DocBody |
| 3 | EXTERNAL_RESOURCE | 6.1 | ✅ ERROR | ✅ | 删除外部引用，不下载 |
| 4 | PERMISSION | 6.2.2a | ✅ WARN | ✅ | 删除权限声明 |
| 5 | VPREFERENCES | 6.2.2b | ❌ | ✅ | 删除视图首选项 |
| 6 | NON_GOTO_ACTION | 6.2.2c/3c | ✅ ERROR | ✅ | Goto→Link，其余删除 |
| 7 | OUTLINE_ACTION | 6.2.5a/b | ✅ ERROR | ✅ | 大纲独立检查 |
| 8 | EXTENSION | 6.2.2e | ✅ WARN | ✅ | 删除扩展信息 |
| 9 | COLOR_SPACE | 6.3.1b | ✅ ERROR | ❌ | 仅 GRAY/RGB/CMYK |
| 10 | IMAGE_FORMAT | 6.2.6e | ✅ ERROR | 待完善 | 魔数检测 6 种格式 |
| 11 | FONT_SUBSET | 6.2.6b | ✅ ERROR | ❌ | 字体必须嵌入 |
| 12 | AUDIO_VIDEO | 6.2.6g | ✅ ERROR | 待完善 | 禁止音视频 |
| 13 | IMAGE_EXTENSION | 6.2.6f | ✅ WARN | ✅ | 删除自定义扩展 |
| 14 | PAGEBLOCK_DEPTH | 6.2.3e | ✅ ERROR | ✅ | 嵌套 ≤3，超限展平 |
| 15 | CLIP_AREA | 6.3.2 | ✅ INFO/WARN | 待完善 | 裁剪区优化 |
| 16 | ENCRYPTION | 6.16 | ✅ ERROR | ✅ | 门禁解密 |
| 17 | IMAGE_INTERPOLATE | 6.5b | ✅ WARN | ✅ | Interpolate=false |
| 18 | CLEAN_STROKE_ATTR | 6.3.3c/d | ❌ | ✅ | Stroke=false 清理 |
| 19 | CLEAN_FILL_ATTR | 6.3.3d/e | ❌ | ✅ | 图像/复合对象清理 |
| 20 | LAYER_NAME | 6.2.3d | 待完善 | ✅ | 图层名唯一化 |
| 21 | TEXT_SIZE | 6.6c | ✅ WARN | ❌ | Size 属性检查 |
| 22 | ANNOTATION | 6.10.1 | ✅ WARN | ✅ | ReadOnly/NoZoom/NoRotate, 去嵌套 |
| 23 | RESOURCE_PLACEMENT | 6.2.6a | 框架 | 框架 | 资源位置验证 |
| 24 | IMAGE_RESOURCE_REG | 6.5a | 框架 | 框架 | 多页图像注册优化 |
| 25 | COLOR_PROFILE | 6.3.1c | ✅ INFO | ❌ | ICC Profile 建议 |
| 26 | TEXT_HSCALE | 6.6d | ✅ WARN | ❌ | 横向缩放 HScale |

## 依赖模块

```
ofdrw-archive
├── ofdrw-crypto    (OFDDecryptor — 解密能力)
├── ofdrw-reader     (OFDReader, ResourceLocator, ResourceManage)
│   └── ofdrw-pkg    (OFDDir, DocDir, PageDir)
│       └── ofdrw-core (OFD, Document, 全部数据模型)
└── JUnit 5 (test)
```

## ofdrw-core 修改

`OFD.java` — `getDocType()` 改为从 XML 属性读取实际值，新增 `setDocType(String)`。向下兼容。

## ofdrw-crypto 解密能力

新增：

| 类 | 说明 |
|---|------|
| `UserFEKDecryptor` | FEK 解密接口（`UserFEKEncryptor` 的逆） |
| `DecryptResult` | 解密结果：FEK(16B) + IV(16B) |
| `UserPasswordDecryptor` | 口令 → KDF → SM4-CBC 解密 WK → FEK |
| `UserCertDecryptor` | SM2 私钥解密 C1C3C2 → FEK |
| `OFDDecryptor` | 完整解密流程（支持多重加密） |

解密流程：
1. 解压 OFD → 读取 Encryptions.xml
2. 对每个 CT_EncryptInfo：加载 decryptseed.dat → 匹配 UserFEKDecryptor → 恢复 FEK
3. 解密 entriesmap.dat → 按映射表逐一解密文件
4. 清理加密元数据 → 重新打包

## 测试

```bash
# ofdrw-crypto 测试（含解密）
mvn test -pl ofdrw-crypto -DskipTests=false -Dtest=OFDDecryptorTest

# 编译
mvn compile -pl ofdrw-core,ofdrw-pkg,ofdrw-reader,ofdrw-crypto,ofdrw-archive -DskipTests=true
```

## 参考资料

- GB/T 42133-2022 信息技术 OFD 档案应用指南
- GB/T 33190-2016 电子文件存储与交换格式 版式文档
- GM/T 0099-2020 OFD 密码应用技术规范
