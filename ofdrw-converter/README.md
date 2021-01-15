# OFD Reader & Writer 文档转换

通过对OFD的文档进行解析，使用 Apache Pdfbox生成并转换OFD中的元素为PDF内的元素实现PDF的转换。

通过`org.ofdrw.converter.ConvertHelper`对象对转换过程API进行封装，提供便捷的工具方法，简化转换开发者的调用负担。

## Quick Start

pom引入相关模块

```xml
<dependency>
    <groupId>org.ofdrw</groupId>
    <artifactId>ofdrw-converter</artifactId>
    <version>1.7.3</version>
</dependency>
```

转换文档你需要做:

1. 提供待转换OFD文档，支持Path、InputStream。
2. 提供转换后PDF文档位置。
3. 调用转换工具执行文档转换。

```java
public class HelloWorld {

    public static void main(String[] args) {
        // 1. 文件输入路径
        Path src = Paths.get("发票示例.ofd");
        // 2. 转换后文件输出位置
        Path dst = Paths.get("发票示例.pdf");
        try {
            // 3. OFD转换PDF
            ConvertHelper.toPdf(src, dst);
            System.out.println("生成文档位置: " + dst.toAbsolutePath());
        } catch (GeneralConvertException e) {
            // GeneralConvertException 类型错误表明转换过程中发生异常
            e.printStackTrace();
        }
    }
}
```


**转换得到PDF效果如图**

![转换结果截图](src/test/resources/转换结果截图.jpg)

注意：如果OFD中的字体来自于操作系统，可能会导致转换过程中由于缺少字体导致的异常，请在部署环境的操作系统目录中加入相关字体。

## PDF转换OFD

目前PDF转换OFD还在开发中，欢迎各位前往支持。

[>> **PDF2OFD**](https://github.com/tyztech2019/PDF2OFD)