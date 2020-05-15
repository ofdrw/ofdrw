# OFD Reader & Writer

> 如何clone和预览存在困难，请移步 [https://gitee.com/Trisia/ofdrw](https://gitee.com/Trisia/ofdrw)

> **Talk is cheap,Show me the code. ——Linus Torvalds**

**像写HTML和Word那样简单的编写OFD。**

根据[《GB/T 33190-2016 电子文件存储与交换格式版式文档》](./GBT_33190-2016_电子文件存储与交换格式版式文档.pdf)标准实现版式文档OFD库（含有书签）。

项目结构：

- [**ofdrw-core**](./ofdrw-core) OFD核心API，参考[《GB/T 33190-2016 电子文件存储与交换格式版式文档》](./GBT_33190-2016_电子文件存储与交换格式版式文档.pdf)实现的基础
    - 实施状态： **阶段性完成**。
- [**ofdrw-font**](./ofdrw-font) 生成OFD需要的常规字体（OpenType）。
    - 实时状态： **阶段性完成**。
- [**ofdrw-layout**](./ofdrw-layout) OFD布局引擎库，用于文档构建和渲染。
    - 实时状态： **阶段性完成**。
- [**ofdrw-pkg**](./ofdrw-pkg) OFD文件的容器，用于文档的打包。
    - 实施状态：**重构完成**。
- [**ofdrw-reader**](./ofdrw-reader) OFD文档解析器，用于OFD的反序列化以及签名签章。
    - 实施状态：**阶段性完成**。
- [**ofdrw-sign**](./ofdrw-sign) OFD文档数字签章。
    - 实施状态：**阶段性完成**。
- [**ofdrw-gm**](./ofdrw-gm) 用于支持签章模块需要的国密电子签章数据结构。
    - 实施状态：**阶段性完成**。
- [**ofdrw-gv**](./ofdrw-gv) OFDRW 所有模块所共用的全局变量。
- [**ofdrw-full**](./ofdrw-full) 上述所有模块整合包，用于简化依赖引入。

## QuickStart

引入依赖
```xml
<dependency>
    <groupId>commons-io</groupId>
    <artifactId>commons-io</artifactId>
    <version>2.6</version>
</dependency>

<dependency>
  <groupId>org.ofdrw</groupId>
  <artifactId>ofdrw-full</artifactId>
  <version>1.4.0</version>
</dependency>
```


如何生成一份OFD文档？

> 如何把大象放入冰箱。

```java
public class HelloWorld {
    public static void main(String[] args) throws IOException {
        Path path = Paths.get("HelloWorld.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            Paragraph p = new Paragraph("你好呀，OFD Reader&Writer！");
            ofdDoc.add(p);
        }
        System.out.println("生成文档位置: " + path.toAbsolutePath());
    }
}
```

效果如下：

![示例](./ofdrw-layout/doc/示例.png)

- [生成示例](https://github.com/Trisia/ofdrw/blob/master/ofdrw-layout/src/test/java/org/ofdrw/layout/OFDDocTest.java)
- [布局示例](https://github.com/Trisia/ofdrw/blob/master/ofdrw-layout/src/test/java/org/ofdrw/layout/LayoutTest.java)
- [Canvas示例](https://github.com/Trisia/ofdrw/blob/master/ofdrw-layout/src/test/java/org/ofdrw/layout/element/canvas/DrawContextTest.java)
> - **关于OFD布局设计，请参考 [《OFD R&W 布局设计》](./ofdrw-layout/doc/README.md)**
> 
> - **Canvas API 请参考 [OFD R&W Canvas](./ofdrw-layout/doc/canvas/README.md)**

---

> 推荐的免费OFD阅读器
> 
> [福昕OFD版式办公套件 . http://www.htfoxit.com/Download/index/id/712](http://www.htfoxit.com/Download/index/id/712)


 - [OFD 签名签章快速入门](ofdrw-sign/doc/quickstart/README.md)

## 源码安装

在项目根目录下运行

```bash
mvn install
```

就可以完成项目的构建打包，安装到本地Maven仓库中。

## 交流

***Share and Communicate***

为了方便大家的交流提供QQ群

![QQ群](./img/QQLink.png)


群号： **577682453**

## 项目情况

### TODO 列表

期望新功能

- 页面模板、页面模板导入使用。
- 文字提取。
- ~~OFD转图片、PDF。~~，无计划
- ~~段落的行内布局控制（text-align）能让固定布局样式文字居中~~，需求不足。

### 进展

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

> 如果各位对 OFD R&W 有 **问题** 或是 **建议** 欢迎提交issue和PullRequest，这样的大家的问题都可以很好的得到分享，我也很乐意解答各位问题。
> 
> 如果你有好的意见建议，欢迎在issue区域提交issue，我都会看的哦。

### 项目关注度

> 项目获得 Star曲线

[![Stargazers over time](https://starchart.cc/Trisia/ofdrw.svg)](https://starchart.cc/Trisia/ofdrw)
