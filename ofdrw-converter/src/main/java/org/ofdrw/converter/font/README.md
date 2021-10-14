# OFD R&W 字形数据解析

## 介绍

由于OFD中的TrueType（OpenType）字体可能为裁剪字体，仅保留必要的“表”和字形数据。

只要字体中包含，下面4个“表”就可以正确的解析出字形数据：

- `head`  头数据
- `maxp`  字形数量
- `loca`  配置参数
- `glyf`  字形数据

字形数据是一系列的字形点控制点和属性的集合。

OFDRW的TTF解析裁剪自 [PDFBox v3.0.0](https://pdfbox.apache.org/) 。

关于字体入门请见：

- [掘金 . 崔静 . TTF文件探秘 . 2020.02 . https://juejin.cn/post/6844904062928846862](https://juejin.cn/post/6844904062928846862)

OpenType字体数据结构及文档见：

- [微软 . The OpenType Font File . 2020.11 . https://docs.microsoft.com/zh-cn/typography/opentype/spec/otff](https://docs.microsoft.com/zh-cn/typography/opentype/spec/otff)


CFF Adobe的压缩字体格式：

- [Adobe . font-tech-notes CFF . https://adobe-type-tools.github.io/font-tech-notes/pdfs/5176.CFF.pdf](https://adobe-type-tools.github.io/font-tech-notes/pdfs/5176.CFF.pdf)

## API

OFDRW 裁剪了PDFBox的`TrueTypeFont`，在OFDRW字体TTF解析为`org.ofdrw.converter.font.TrueTypeFont`。

解析使用流程如下：

1. 构造随机读取文件对象。
2. 构造解析器，解析字体文件。
3. 通过GID（字形偏移量）得到字形数据。

```java
class Main {
    public static void main(String[]args){
        Path fontPath = Paths.get("ofdrw-converter/src/test/resources/font_10.ttf");
        // 1. 构造随机读取文件对象。
        TTFDataStream dataStream = new MemoryTTFDataStream(Files.newInputStream(fontPath));
        // 2. 构造解析器，解析字体文件。
        TrueTypeFont trueTypeFont = new TrueTypeFont().parse(dataStream);
        // 3. 通过GID（字形偏移量）得到字形数据。
        GlyphData glyph = trueTypeFont.getGlyph(469);
        // 使用 glyph 字形数据对象做你需要的操作。
    }
}
```