# OFD Reader & Writer 文档操作工具

**注意：文档操作将会导致文档结构内容变更，这将导致数字签名无效，请悉知！**

## 引入依赖

```xml

<dependency>
    <groupId>org.ofdrw</groupId>
    <artifactId>ofdrw-tool</artifactId>
    <version>1.20.1</version>
</dependency>
```

## 页编辑

`OFDMerger` 提供了页面级别的多文档编辑功能，包括：

- 多文档合并
- 文档页裁剪
- 多文档页重组

## 多文档合并

以常见用的多文档合并举例，调用流程如下：

1. 提供合并文件输出位置。
2. 提供待合并文件。
3. 创建合并对象`OFDMerger`。
4. 添加合并文档和页面（支持添加多个文档）。
5. 关闭合并对象，生成文档。

```java
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HelloMerge {
    public static void main(String[] args) throws IOException {
        // 1. 提供合并文件输出位置。
        Path dst = Paths.get("dst.ofd");
        // 2. 提供待合并文件。
        Path d1Path = Paths.get("file1.ofd");
        Path d2Path = Paths.get("file2.ofd");
        Path d3Path = Paths.get("file3.ofd");
        // 3. 创建合并对象
        try (OFDMerger ofdMerger = new OFDMerger(dst)) {
            // 4. 添加合并文档和页面。
            ofdMerger.add(d1Path);
            ofdMerger.add(d2Path);
        }
        // 5. 关闭合并对象，生成文档 (try() 语法自动关闭)
    }
}
```

为了更加灵活的合并文档，`OFDMerger#add`方法支持可选参数，指定需要合并的页面页码（从1开始），通过灵活使用该API可以实现多文档页面级别编辑功能。

### 裁剪

截取文档的部分页面生成新的文档。

```java
public class Hello {
    public static void main(String[] args) {
        Path dst = Paths.get("dst.ofd");
        Path d1Path = Paths.get("file1.ofd");
        try (OFDMerger ofdMerger = new OFDMerger(dst)) {
            ofdMerger.add(d1Path, 1, 2);
        }
    }
}
```

### 多文档页重组

将多个文档中的页面合并到同一份文档中，并可以可用页面在新文档中的顺序。

```java
public class Hello {
    public static void main(String[] args) {
        Path dst = Paths.get("dst.ofd");
        Path d1Path = Paths.get("file1.ofd");
        Path d2Path = Paths.get("file2.ofd");
        try (OFDMerger ofdMerger = new OFDMerger(dst)) {
            ofdMerger.add(d1Path, 1, 2);
            ofdMerger.add(d2Path, 1);
            ofdMerger.add(d1Path, 3);
        }
    }
}
```
