# OFD Reader & Writer 文档操作工具

## 引入依赖

```xml

<dependency>
    <groupId>org.ofdrw</groupId>
    <artifactId>ofdrw-tool</artifactId>
    <version>1.17.0</version>
</dependency>
```

## 多文档合并

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

为了更加灵活的合并文档，`OFDMerger#add`方法支持可选参数，指定需要合并的页面页码（从1开始）


例如：

- 选取`file1.ofd`的第3、1页作为新文档的第1、2页。
- 选择`file2.ofd`的第1页作为新文档的第3、4页内容。

```java
// Path d1Path = Paths.get("file1.ofd");
// Path d2Path = Paths.get("file2.ofd");

ofdMerger.add(d1Path, 3, 1);
ofdMerger.add(d2Path, 1, 1);
```