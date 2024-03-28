# OFDRW 转换为OFD

OFD Reader & Writer Convert to OFD (OFDRW C2O)

OFDRW提供了将其它类型媒体文件或文档转换成OFD文档内容功能，例如：

- PDF转换OFD
- 文本转换OFD
- 图片转换OFD

## 入门

使用Maven 引入相关模块

```xml

<dependency>
    <groupId>org.ofdrw</groupId>
    <artifactId>ofdrw-converter</artifactId>
    <version>2.2.11</version>
</dependency>
```

> - 若您没有采用Maven管理项目，请参阅项目中`pom.xml`文件中的依赖，手动解决三方依赖包问题。


OFDRW 转换模块在`2.0.0`之后开始提供其它文档或媒体类型向OFD文档转换功能。

在开始文档转换之前您可以需要花几分钟，学习一下`DocConverter`如何使用。

`org.ofdrw.converter.ofdconverter.DocConverter`为OFD文档转换器接口，其关键方法`convert`方法签名如下所示：

```
void convert(Path filepath, int... indexes) throws GeneralConvertException;
```

**注意：`convert`方法的参数页码均从0起，例如文档中的第1页的Index也就是0，并非所有媒体格式都有页码，在转换无页码的媒体时，页码参数无效。
**

`DocConverter`使用方式如下：

1. 新建一个目标对象的转换对象。
2. 调用设置路径以及页码调用转换方法添加内容到OFD文档中。
3. 关闭转换对象。

API调用形式如下：

```
DocConverter converter = new TextConverter(dst);
converter.convert(src);
converter.close();
```

`DocConverter`接口中的`convert`
需要您传入待转换或导入的媒体文件，当待转换的媒体为文档类型时该接口还支持传入多个可变参数的页码，并且支持多次调用，通过灵活的调整参数可以实现意想不到的页面组装效果，若在调用时不传任何参数则表示全文转换后导入，下面给出几种示例。

转换媒体文件到OFD中，若原媒体文件为文档格式那么转换原文档中所有页面到OFD中：

```
PDFConverter converter = new PDFConverter(dst);
converter.convert(src);
converter.close();
```

若原媒体文件为文档格式，从原文档中转换指定页码到OFD中，导出第1、3页：

```
PDFConverter converter = new PDFConverter(dst);
converter.convert(src, 0, 2);
converter.close();
```

若原媒体文件为文档格式，从原文档转换指定页码，并对转换后的文档页序进行重组，例如导出1、2、3页，导出的文档中页序原来文档的3、1、2页：

```
PDFConverter converter = new PDFConverter(dst);
converter.convert(src, 2, 0, 1);
converter.close();
```

若原媒体文件为文档格式，从原文档中转换指定页码，前几页或后几页，例如一份文档总计3页，现需要转换第1、3页，除了上述可变参数的方式转换还可以使用多次调用的方式实现：

```
PDFConverter converter = new PDFConverter(dst);
converter.convert(src,  0);
converter.convert(src,  2);
converter.close();
```

若原媒体文件为文档格式，可以重复转换某一页来实现重复页面的效果，例如转换第1页然后重复转换2页第3页：

```
PDFConverter converter = new PDFConverter(dst);
converter.convert(src,  0);
converter.convert(src,  1);
converter.convert(src,  1);
converter.close();
```

若原媒体文件为文档格式，在某些情况您可能需要通过指定转换指定范围的页面，这时可以使用数组的方式构造页码，例如原文档总计4页，转换1-2页、4页：

```
int[] range1 = new int[]{0, 1};
int[] range2 = new int[]{3};

PDFConverter converter = new PDFConverter(dst);
converter.convert(src,  range1);
converter.convert(src,  range2);
converter.close();
```

若数组`null`或长度为0同样表示转换全部页面。

## 转换为OFD文档

若您尚未阅读 **入门** 章节，请先阅读上述章节再继续学习。

`DocConverter`接口具有多个实现，其实现与转换的原媒体文件有关。

接口实现类命名格式为： ***原媒体格式+Converter***

例如：`TextExporter`表示将文本转换为OFD文件中的页面，`Text`表示待转换的媒体格式是文本。

目前DocConverter支持转换为以下类型：

- 图片：`ImageExporter`
- PDF文档：`PDFConverter`、`PDFExporterPDFBox`
- 纯文本：`TextExporter`

不同的转换实现结合目标类型特性具有不同的接口，下面将介绍各种转换器的使用方式，以及特有方法。

### PDF转换OFD

将PDF中页面转换为OFD页面，采用PDFBox `PDFRenderer`接口，以AWT
graphics2d接口桥接，并通过[ofdrw-graphics2d](../../ofdrw-graphics2d) 模块完成转换功能。

实现类：`org.ofdrw.converter.ofdconverter.PDFConverter`

注意事项：

- 转换后的页面将采用PDF中页面尺寸。
- **目前该转换器任然有改进空间**，可能存在部分特性在转换过程中丢失，显示效果与原PDF文档不一致。

示例：

```
Path src = Paths.get("src/test/resources/Test.pdf");
Path dst = Paths.get("target/convert.ofd");
try (PDFConverter converter = new PDFConverter(dst)) {
    converter.convert(src);
}
System.out.println(">> " + dst.toAbsolutePath());
```

| 特有方法                                                           | 用途                  |
|:---------------------------------------------------------------|:--------------------|
| `void setEnableCopyAttachFiles(boolean enableCopyAttachFiles)` | 设置是否复制附件（默认复制）。     |
| `void setEnableCopyBookmarks(boolean enableCopyBookmarks)`     | 设置是否复制书签（默认复制）。     |
| `void setUUPMM(double UUPMM)`                                  | 设置毫米表示的用户单元数（PDF单位） |

> 详见 [测试用例](../src/test/java/org/ofdrw/converter/ofdconverter/PDFConverterTest.java)

### 图片转换OFD

导入图片到OFD中，图片格式支持PNG、BPM、JPG。

实现类：`org.ofdrw.converter.ofdconverter.ImageConverter`

注意事项：

- 可以通过构造器指定导出的图片类型，目前支持`PNG`、`JPG`、`BPM`，默认为`PNG`格式。
- 若图片格式不在上述范围您可能需要通过手动的方式设置加入图片大小。
- 可以通过方法设置导出图片的质量，也就是`ppm`参数，默认`ppm`为**15**（15像素1毫米）。
- 每个添加的图片都将独立为一页，并且居中。

示例：

```
Path dst = Paths.get("target/IMAGE-PAGES.ofd");
Path img1 = Paths.get("src/test/resources/img.jpg");
Path img2 = Paths.get("src/test/resources/ofd2html.jpg");
try (ImageConverter converter = new ImageConverter(dst)) {
    converter.convert(img1);
    converter.convert(img2);
    converter.convert(img1);
}
```

| 特有方法                                                      | 用途                  |
|:----------------------------------------------------------|:--------------------|
| `void setPPM(double ppm)`                                 | 设置图片质量，单位为：每毫米像素数量。 |
| `void append(Path filepath, double width, double height)` | 追加图片到新页面并指定显示大小。    |
| `void setPageSize(PageLayout pageLayout)`                 | 设置OFD页面尺寸。          |

> 详见 [测试用例](../src/test/java/org/ofdrw/converter/ofdconverter/ImageConverterTest.java)

### 文本转换OFD

将文本转换为OFD。

实现类：`org.ofdrw.converter.ofdconverter.TextConverter`

注意事项：

- 文本文件为无格式文件，若您需要设置文本格式请使用ofdrw-layout模块。

示例：

```
Path src = Paths.get("doc/EXPORTER.md");
Path dst = Paths.get("target/EXPORTER.ofd");
try (TextConverter converter = new TextConverter(dst)) {
    converter.convert(src);
}
System.out.println(">> " + dst.toAbsolutePath());
```

| 特有方法                                      | 用途            |
|:------------------------------------------|:--------------|
| `void append(String txt)`                 | 追加文本，文本将新起一行。 |
| `void setPageSize(PageLayout pageLayout)` | 设置OFD页面尺寸。    |
| `void setFontSize(double fontSize)`       | 设置字号，单位毫米。    |

> 详见 [测试用例](../src/test/java/org/ofdrw/converter/ofdconverter/TextConverterTest.java)
