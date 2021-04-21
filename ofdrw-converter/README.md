# OFD Reader & Writer 文档转换

> *致谢:*
> 
> - *[DLTech21](https://github.com/DLTech21) OFD转换PDF*
> - *[QAQtutu](https://github.com/QAQtutu) OFD转换图片*
> 
> *OFDRW社区感谢你们对convert模块的辛勤开发！*

OFDR&W文档转换支持

- **OFD `=>` PDF**
- **OFD `=>` 图片**

PDF转换概述： 通过对OFD的文档进行解析，使用 Apache Pdfbox生成并转换OFD中的元素为PDF内的元素实现PDF的转换。

图片转换概述： 通过对OFD的文档进行解析，采用`java.awt`绘制图片，支持转换为`PNG`、`JPEG`图片格式。

## Quick Start

pom引入相关模块

```xml

<dependency>
    <groupId>org.ofdrw</groupId>
    <artifactId>ofdrw-converter</artifactId>
    <version>1.8.6</version>
</dependency>
```

### 转换PDF

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

### 转换图片

转换文档你需要做:

1. 提供待转换OFD文档。
2. 配置字体(非必须，自定义字体目录时使用)。
3. 创建转换转换对象，并设置PPM。
4. 指定页码转换图片。
5. 存储为指定格式图片。

```java
public class HelloWorld {
    public static void main(String[] args) {
        // 1. 文件输入路径
        Path src = Paths.get("发票示例.ofd");
        // 2. 加载指定目录字体(非必须)
        // FontLoader.getInstance().scanFontDir(new File("src/test/resources/fonts"));
        // 3. 创建转换转换对象，设置 每毫米像素数量(Pixels per millimeter)
        ImageMaker imageMaker = new ImageMaker(new DLOFDReader(src), 15);
        for (int i = 0; i < imageMaker.pageSize(); i++) {
            // 4. 指定页码转换图片
            BufferedImage image = imageMaker.makePage(i);
            Path dist = Paths.get("target", i + ".png");
            // 5. 存储为指定格式图片
            ImageIO.write(image, "PNG", dist.toFile());
        }
    }
}
```

- [测试用例](./src/test/java/OFD2IMGTest.java) 


效果图如下：

![转图片效果](src/test/resources/转图片效果.png)