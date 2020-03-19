# OFD Reader & Writer

根据[《GB/T 33190-2016 电子文件存储与交换格式版式文档》](./GBT_33190-2016_电子文件存储与交换格式版式文档.pdf)标准实现版式文档OFD库（含有书签）。


项目结构

- [**ofdrw-core**](./ofdrw-core/) 核心API，参考[《GB/T 33190-2016 电子文件存储与交换格式版式文档》](./GBT_33190-2016_电子文件存储与交换格式版式文档.pdf)实现的基础
    - 实施状态： **阶段性完成**。
- [**ofdrw-pkg**](./ofdrw-pkg) OFD文件的容器以及用于文档的打包。
    - 实施状态：*达成基本功能*
- [**ofdrw-layout**](./ofdrw-layout) OFD布局。
    - 实时状态： *顶层构建设计*
- [**ofdrw-font**](./ofdrw-font) OFD生成需要的常规字体（OpenType）
    - 实时状态： *待测试*

## 进展

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
> 目前OFD R&W 还处于开发阶段...

## 项目关注度

> 项目获得 Star曲线

[![Stargazers over time](https://starchart.cc/Trisia/ofdrw.svg)](https://starchart.cc/Trisia/ofdrw)
