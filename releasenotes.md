# 发布记录和特性

## 进展

## Version 1.6.0 2020-10-13 20:22:28

新功能：

- [关键定位功能](ofdrw-reader/src/test/java/org/ofdrw/reader/keyword/KeywordExtractorTest.java)
- [骑缝章的自由定位](ofdrw-sign/src/test/java/org/ofdrw/sign/stamppos/RidingStampPosTest.java)
- [不同版本电子签章兼容解析](ofdrw-gm/src/test/java/org/ofdrw/gm/ses/parse/VersionParserTest.java)

修复:

- 修复了 CustomDatas 在解析CustomData 序列时候错误的关键字使用。
- 修复了V4版本电子签章可选参数tag0的错误。
- 兼容非标准推荐的签名ID，如"sN"、"N"类型的解析。
- 支持了坐标偏移中厂商定义的`g`参数偏移量。

## Version 1.5.6 2020-9-21 23:14:58

- 修复了无法解析OFD内含有中文目录的文件错误。
- 新增了OFD页面文字抽取的方法：
    
    文字抽取参考[ContentExtractorTest.java](ofdrw-reader/src/test/java/org/ofdrw/reader/ContentExtractorTest.java)

## Version 1.5.5 2020-9-15 21:43:31

- 修复了ST_Array 数组含有多空格抛出IAE的问题。
- 兼容了旧的OFD命名空间。

## Version 1.5.4 2020-8-26 20:10:15

- 简化了`ofdrw-font`字体库以及相关API，移除了内嵌的几个noto字体，减少库体积。

### Version 1.5.3 2020-8-24 21:02:49

- OFDSigner 可以使用自己构造的签名ID提供器，而不是标准推荐的 "s'NNN'"格式
    
    需要手动实现 `org.ofdrw.sign.SignIDProvider`接口

### Version 1.5.2 2020-6-20 10:04:37

- 修复了注释对象参数无法添加多个的问题。

### Version 1.5.1 2020-6-6 11:06:28

- CVE-2020-10683 dom4j库可能造成XXE 攻击，升级该库至2.1.3。
- 增加了替换附件的功能。

### Version 1.5.0 2020-5-19 20:45:50

- 首页加入了免责声明，以及咨询入口。
- 修复了电子印章为非必选参数，签章容器不设置电子印章就无法进行签章操作的问题。
- 为Canvas API 提供线条的各种参数设置包括
    - 回线条的结束端点样式
    - 两条线相交时，所创建的拐角类型
    - 最大斜接长度，也就是结合点长度截断值
    - 填充线时使用虚线模式，设置虚线间隔

Canvas 测试用例见 [DrawContextTest](./ofdrw-layout/src/test/java/org/ofdrw/layout/element/canvas/DrawContextTest.java)

- `#setLineDash`
- `#setMiterLimit`
- `#setLineJoin`
- `#setLineCap`

签章测试用用例见 [NoSealSignTest](./ofdrw-sign/src/test/java/org/ofdrw/sign/signContainer/NoSealSignTest.java)

### Old

- *2020-05-15* 完成OFD的注释功能

    测试用例参考`org.ofdrw.layout.OFDDocTest#addAnnotation(void)`。
- *2020-05-14* 启动注解对象构想，用于支持水印等注解的加入。
- *2020-05-10* 完成了Canvas系列API的开发。
- *2020-05-01* 增加了Canvas设计，用于更加灵活的绘制和水印效果。
- *2020-04-23* 发布`1.2.0`版本，支持OFD电子签章。
- *2020-04-22* 实现了行内的换行符换行功能。
- *2020-04-20* ofdrw-sign 成功完成一次数字签名。
- *2020-04-19* 增加了ofdrw-gm 模块用于支持电子签章。
- *2020-04-18* 增加了ofdrw-sign模块【未完成】，加入了ofdrw-gv用于共享全局变量。
- *2020-04-15* 启动了ofdrw-sign 模块分析设计。
- *2020-04-12* 发布ofdrw 1.1.0版本。
- *2020-04-11* 【里程碑】实现了向已有OFD增加内容，以及追加内容的功能，考虑发布 1.1.0-RELEASE版本。
- *2020-04-04* 采用虚拟容器的方式重写ofdrw-pkg，为文档的反序列化和修改做准备。
- *2020-04-01* 启动ofdrw-reader的初期设计。
- *2020-03-31* 【里程碑】 OFD R&W 正式发布第一个版本，版本号 1.0.0。
- *2020-03-29* 完成了《OFD R&W 布局设计》的各种情况测试，准备发布版本。
- *2020-03-28* 增加了文字下划线、段落的首行缩进的支持。
- *2020-03-26* 完善了少内容段落的布局，能够使用center居中，增加HelloWorld Demo。
- *2020-03-24* 【里程碑】 基本完成段落渲染器开发，第一次成功生成一份含有文字的OFD文档。
    
    可以运行`org.ofdrw.layout.OFDDocTest#paragraphTest`测试用用例，查看效果。
- *2020-03-23* 确定了图片渲染方式，并生成了含有图片的OFD文档，开始策划开发段落渲染器。

    可以运行`org.ofdrw.layout.OFDDocTest#imgTest`测试用用例，查看效果。
- *2020-03-22* 使用ofdrw-layout 采用固定布局虚拟页面的方式生成了第一个图形OFD文档。

    可以运行`org.ofdrw.layout.OFDDocTest#divBoxTest`测试用用例，查看效果。
- *2020-03-18* 策划并启动 font的开发。
- *2020-02-28* 开始Layout的开发。
- *2020-01-22* 启动 Layout 设计。
- *2020-01-20* ofd r&w 首个OFD文件生成成功，并能够通过 [数科OFD阅读器](http://www.suwell.cn/product/index.html) 正确打开。
    
    可通过运行`org.ofdrw.pkg.dir.OFDDirTest#jar` 在项目target目录下生成一个名为hello.ofd的文件。
- *2019-11-21* 完成基础库的开发工作，开始策划`ofdrw-pkg`。
- *2019-09-27* 项目策划完成并开始实施。

如果该项目有兴趣不妨给个Star，欢迎大家一同参与项目。
